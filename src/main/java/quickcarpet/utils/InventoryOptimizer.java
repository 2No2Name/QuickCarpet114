package quickcarpet.utils;

import it.unimi.dsi.fastutil.HashCommon;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.Objects;

//Don't store instances of InventoryOptimizer, unless you sync with the corresponding inventory!
//Instances may suddenly become invalid due to unloading or turning the setting off
public class InventoryOptimizer {
    private final InventoryListOptimized stackList;

    private static final ItemStack STACK_FULL = new ItemStack(Items.CACTUS,64);
    private static final ItemStack STACK_NONFULL = new ItemStack(Items.CACTUS,1);
    private static final ItemStack STACK_ZEROCOUNT = new ItemStack(null,0);

    private static boolean DEBUG = false; //nonfinal to be able to change with debugger

    //256 Bit bloom filter //false positive rates estimated with https://hur.st/bloomfilter/?n=27&p=&m=256&k=
    //Decided on going for a high estimate, as a larger bloom filter probably won't be a lag causer, but can still help
    //Estimate useful for 27 slot inventory
    private static final int hashBits = 6;

    //todo(medium) recalculate Filters when 180+ Bits are set -> ~12% chance that negative filters as false positive
    //maybe use usage stats for this as well

    private long bloomFilter;
    private long bloomFilter1;
    private long bloomFilter2;
    private long bloomFilter3;

    private long nonFullStackBloomFilter;
    private long nonFullStackBloomFilter1;
    private long nonFullStackBloomFilter2;
    private long nonFullStackBloomFilter3;

    //If there is no empty slot, clear this //todo(small)
    private long preEmptyNonFullStackBloomFilter;
    private long preEmptyNonFullStackBloomFilter1;
    private long preEmptyNonFullStackBloomFilter2;
    private long preEmptyNonFullStackBloomFilter3;

    private int occupiedSlots;
    private int fullSlots;
    private int totalSlots;
    private int firstFreeSlot = -1;
    private int firstOccupiedSlot = -1;

    private boolean initialized;

    public InventoryOptimizer(InventoryListOptimized stackList) {
        this.stackList = stackList;
        this.initialized = false;
    }

    private void consistencyCheck(){
        //this is code from recalculate, but instead of changing anything, we just check if the results are conflicting
        if(!initialized) return;

        int occupiedSlots = 0;
        int fullSlots = 0;
        int firstFreeSlot = -1;
        int firstOccupiedSlot = -1;
        int totalSlots = size();


        for (int i = 0; i < totalSlots; i++) {
            ItemStack stack = getSlot(i);
            long hash = hash(stack);
            if (hash != 0 && !filterContains(hash)) throw new IllegalStateException("Itemstack not in bloom filter: " + stack.toString());

            if (!stack.isEmpty()) {
                if(firstOccupiedSlot < 0)
                    firstOccupiedSlot = i;
                occupiedSlots++;
                if (stack.getCount() >= stack.getMaxCount()) fullSlots++;
                else if (hash != 0 && !nonFullStackFilterContains(hash)) throw new IllegalStateException("Itemstack not in nonFull bloom filter: " + stack.toString());

            } else if (firstFreeSlot < 0) {
                firstFreeSlot = i;
                for (int j = 0; j < firstFreeSlot; j++){
                    ItemStack stack1 = getSlot(j);
                    if (stack1.getCount() < stack1.getMaxCount()){
                        long hash1 = hash(stack1);
                        if (hash1 != 0 && !preEmptyNonFullStackFilterContains(hash1)) throw new IllegalStateException("Itemstack not in preEmptyNonFull bloom filter: " + stack1.toString());
                    }
                }
            }
        }
        if(this.occupiedSlots != occupiedSlots) throw new IllegalStateException("occupied slots wrong");
        if(this.fullSlots != fullSlots) throw new IllegalStateException("full slots wrong");
        if(this.firstFreeSlot != firstFreeSlot) throw new IllegalStateException("first free slot wrong");
        if(this.firstOccupiedSlot != firstOccupiedSlot) throw new IllegalStateException("first occupied slot wrong");
    }


    public void onItemStackCountChanged(int index, int countChange){
        if(!initialized) return;

        ItemStack itemStack = getSlot(index);
        int count = itemStack.getCount();
        itemStack.setCount(1);
        boolean wasEmpty = itemStack.isEmpty();
        int max = itemStack.getMaxCount();
        itemStack.setCount(count);
        boolean isEmpty = itemStack.isEmpty();
        boolean wasFull = count-countChange >= max;
        boolean isFull = count >= max;

        if (!wasEmpty && !isEmpty){
            if(wasFull && !isFull) update(index,STACK_FULL);
            if(!wasFull && isFull) update(index,STACK_NONFULL);
        }
        else if (!wasEmpty) update(index, wasFull ? STACK_FULL : STACK_NONFULL);
        else if (!isEmpty) update(index, STACK_ZEROCOUNT);

        if(DEBUG) consistencyCheck();
    }

    protected ItemStack getSlot(int index) {
        return (ItemStack)this.stackList.get(index);
    }

    protected int size() {
        return this.stackList.size();
    }

    public int getOccupiedSlots(){
        return occupiedSlots;
    }

    /**
     * Update the bloom filter after a slot has been modified.
     * @param slot Index of the modified slot
     */
    void update(int slot, ItemStack prevStack){
        if (!initialized) return;

        int oldFirstFreeSlot = firstFreeSlot;

        ItemStack newStack = (ItemStack) stackList.get(slot);
        long hash = hash(newStack);
        filterAdd(hash);
        boolean flag = false;

        if (prevStack.getCount() >= prevStack.getMaxCount())
            --fullSlots;
        if (newStack.getCount() >= newStack.getMaxCount())
            ++fullSlots;
        else {
            //In case of empty stack, filters are unchanged, otherwise add to according filters
            nonFullStackFilterAdd(hash);
        }

        if (!prevStack.isEmpty())
            --occupiedSlots;
        if (!newStack.isEmpty()) {
            ++occupiedSlots;
            firstOccupiedSlot = firstOccupiedSlot > slot || firstOccupiedSlot == -1 ? slot : firstOccupiedSlot;
            if (firstFreeSlot == slot)
                flag = true;
        } else {
            firstFreeSlot = firstFreeSlot > slot || firstFreeSlot == -1? slot : firstFreeSlot;
            if(slot == firstOccupiedSlot)
                flag = true;
        }
        if (flag)
            recalcFirstFreeAndOccupiedSlots(oldFirstFreeSlot);


        if(DEBUG) consistencyCheck();
    }

    private void recalcFirstFreeAndOccupiedSlots(int oldFirstFreeSlot){
        this.totalSlots = size();
        firstOccupiedSlot = -1;
        firstFreeSlot = -1;

        for (int i = 0; i < totalSlots && (firstFreeSlot == -1 || firstOccupiedSlot == -1); i++) {
            ItemStack stack = getSlot(i);
            if (!stack.isEmpty()) {
                if(firstOccupiedSlot < 0)
                    firstOccupiedSlot = i;
            } else if (firstFreeSlot < 0)
                firstFreeSlot = i;
        }

        if(oldFirstFreeSlot < firstFreeSlot || firstFreeSlot == -1){
            if(oldFirstFreeSlot == -1) oldFirstFreeSlot = 0;
            for(int i = oldFirstFreeSlot; i < totalSlots && (i < firstFreeSlot || firstFreeSlot == -1); i++){
                ItemStack itemStack = getSlot(i);
                if(!itemStack.isEmpty() && !(itemStack.getMaxCount() <= itemStack.getCount()))
                    preEmptyNonFullStackFilterAdd(hash(itemStack));
            }
        }
    }

    //old approach: something stupid about conservatively invalidating the bloomfilter everytime the inventory was accessed (bad idea)
    //new approach: assume that nothing besides players and hoppers/droppers change inventory contents
    //control their inventory accesses, notify of the inventory of hidden stacksize changes (see HopperBlockEntityMixin and InventoriesMixin)
    /*/**
     * Remembers that an item escaped to an unknown context. Going to assume its hash and count may change immediately, but not later again.
     * This assumption may lead to incorrect results
     * @param slot Location of the escaped Item
     */
    /*void markEscaped(int slot){
        //possiblyOutdatedSlots |= (1 << slot);
    }//*/

    private void clearFilters(){
        bloomFilter = 0;
        bloomFilter1 = 0;
        bloomFilter2 = 0;
        bloomFilter3 = 0;

        nonFullStackBloomFilter = 0;
        nonFullStackBloomFilter1 = 0;
        nonFullStackBloomFilter2 = 0;
        nonFullStackBloomFilter3 = 0;

        preEmptyNonFullStackBloomFilter = 0;
        preEmptyNonFullStackBloomFilter1 = 0;
        preEmptyNonFullStackBloomFilter2 = 0;
        preEmptyNonFullStackBloomFilter3 = 0;
    }

    public void recalculate() {
        clearFilters();

        int occupiedSlots = 0;
        int fullSlots = 0;
        int firstFreeSlot = -1;
        int firstOccupiedSlot = -1;

        this.totalSlots = size();
        for (int i = 0; i < totalSlots; i++) {
            ItemStack stack = getSlot(i);
            long hash = hash(stack);
            filterAdd(hash);
            /*if (firstFreeSlot < 0){
                if (stack.getCount() < stack.getMaxCount())
                    preEmptyNonFullStackFilterAdd(hash);
            }//*/
            if (!stack.isEmpty()) {
                if(firstOccupiedSlot < 0)
                    firstOccupiedSlot = i;
                occupiedSlots++;
                if (stack.getCount() >= stack.getMaxCount()) fullSlots++;
                else nonFullStackFilterAdd(hash);
            } else if (firstFreeSlot < 0) {
                firstFreeSlot = i;
                for (int j = 0; j < firstFreeSlot; j++){
                    ItemStack stack1 = getSlot(j);
                    if (stack1.getCount() < stack1.getMaxCount()){
                        long hash1 = hash(stack1);
                        preEmptyNonFullStackFilterAdd(hash1);
                    }
                }
            }
        }
        this.occupiedSlots = occupiedSlots;
        this.fullSlots = fullSlots;
        this.firstFreeSlot = firstFreeSlot;
        this.firstOccupiedSlot = firstOccupiedSlot;

        this.initialized = true;
    }

    public int getFirstFreeSlot() {
        return firstFreeSlot;
    }

    private boolean isFull() {
        return fullSlots >= totalSlots;
    }

    private boolean maybeContains(ItemStack stack) {
        if (!initialized) recalculate();

        if (stack.isEmpty()) return getFirstFreeSlot() >= 0;
        if (occupiedSlots == 0) return false;
        long hash = hash(stack);

        return filterContains(hash);
    }

    private boolean maybeContainsNonFullStack(ItemStack stack) {
        if (!initialized) recalculate();

        if (stack.isEmpty()) return getFirstFreeSlot() >= 0;
        if (occupiedSlots == 0) return false;
        if (isFull()) return false;
        return nonFullStackFilterContains(hash(stack));
    }

    private boolean canMaybeInsert(ItemStack stack) {
        if (!initialized) recalculate();

        if (hasFreeSlots()) return true;
        if (isFull()) return false;
        return maybeContainsNonFullStack(stack);
    }

    private static long hash(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        long hash = HashCommon.mix((long) stack.getItem().hashCode());
        hash ^= HashCommon.mix((long) stack.getDamage());
        hash ^= HashCommon.mix((long) Objects.hashCode(stack.getTag()));
        return hash == 0 ? 1 : hash;
    }

    private boolean filterContains(long hash){
        long mask8bit = 0x00000000000000FFL;
        boolean ret = true; //becomes false if any of the corresponding bits is not set

        //Use the lowest 8 bits of the hash hashBits times to set a bit in the filter
        for(int i = 0; i < hashBits && ret; ++i){
            long hIndex = (hash & mask8bit) >> (i*8);
            mask8bit = mask8bit << 8;
            if (hIndex < 128){
                if(hIndex < 64)
                    ret = (bloomFilter & (1L << hIndex)) != 0;
                else
                    ret = (bloomFilter1 & (1L << (hIndex-64))) != 0;
            }else{
                if(hIndex < 192)
                    ret = (bloomFilter2 & (1L << (hIndex-128))) != 0;
                else
                    ret = (bloomFilter3 & (1L << (hIndex-192))) != 0;
            }
        }
        return ret;
    }
    private void filterAdd(long hash){
        if(hash == 0) return;

        long mask8bit = 0x00000000000000FFL;

        //Use the lowest 8 bits of the hash hashBits times to set a bit in the filter
        for(int i = 0; i < hashBits; ++i){
            long hIndex = (hash & mask8bit) >> (i*8);
            mask8bit = mask8bit << 8;
            if (hIndex < 128){
                if(hIndex < 64)
                    bloomFilter |= (1L << hIndex);
                else
                    bloomFilter1 |= (1L << (hIndex-64));
            }else{
                if(hIndex < 192)
                    bloomFilter2 |= (1L << (hIndex-128));
                else
                    bloomFilter3 |= (1L << (hIndex-192));
            }
        }
    }

    //Duplicated Code with filterContains/filterAdd, fix if it becomes a problem
    private boolean preEmptyNonFullStackFilterContains(long hash){
        long mask8bit = 0x00000000000000FFL;
        boolean ret = true; //becomes false if any of the corresponding bits is not set

        //Use the lowest 8 bits of the hash hashBits times to set a bit in the filter
        for(int i = 0; i < hashBits && ret; ++i){
            long hIndex = (hash & mask8bit) >> (i*8);
            mask8bit = mask8bit << 8;
            if (hIndex < 128){
                if(hIndex < 64)
                    ret = (preEmptyNonFullStackBloomFilter & (1L << hIndex)) != 0;
                else
                    ret = (preEmptyNonFullStackBloomFilter1 & (1L << (hIndex-64))) != 0;
            }else{
                if(hIndex < 192)
                    ret = (preEmptyNonFullStackBloomFilter2 & (1L << (hIndex-128))) != 0;
                else
                    ret = (preEmptyNonFullStackBloomFilter3 & (1L << (hIndex-192))) != 0;
            }
        }
        return ret;
    }
    private void preEmptyNonFullStackFilterAdd(long hash){
        if(hash == 0) return;
        long mask8bit = 0x00000000000000FFL;

        //Use the lowest 8 bits of the hash hashBits times to set a bit in the filter
        for(int i = 0; i < hashBits; ++i){
            long hIndex = (hash & mask8bit) >> (i*8);
            mask8bit = mask8bit << 8;
            if (hIndex < 128){
                if(hIndex < 64)
                    preEmptyNonFullStackBloomFilter |= (1L << hIndex);
                else
                    preEmptyNonFullStackBloomFilter1 |= (1L << (hIndex-64));
            }else{
                if(hIndex < 192)
                    preEmptyNonFullStackBloomFilter2 |= (1L << (hIndex-128));
                else
                    preEmptyNonFullStackBloomFilter3 |= (1L << (hIndex-192));
            }
        }
    }
    //Duplicated Code with filterContains/filterAdd, fix if it becomes a problem
    private boolean nonFullStackFilterContains(long hash){
        long mask8bit = 0x00000000000000FFL;
        boolean ret = true; //becomes false if any of the corresponding bits is not set

        //Use the lowest 8 bits of the hash hashBits times to set a bit in the filter
        for(int i = 0; i < hashBits && ret; ++i){
            long hIndex = (hash & mask8bit) >> (i*8);
            mask8bit = mask8bit << 8;
            if (hIndex < 128){
                if(hIndex < 64)
                    ret = (nonFullStackBloomFilter & (1L << hIndex)) != 0;
                else
                    ret = (nonFullStackBloomFilter1 & (1L << (hIndex-64))) != 0;
            }else{
                if(hIndex < 192)
                    ret = (nonFullStackBloomFilter2 & (1L << (hIndex-128))) != 0;
                else
                    ret = (nonFullStackBloomFilter3 & (1L << (hIndex-192))) != 0;
            }
        }
        return ret;
    }
    private void nonFullStackFilterAdd(long hash){
        if(hash == 0) return;
        long mask8bit = 0x00000000000000FFL;

        //Use the lowest 8 bits of the hash hashBits times to set a bit in the filter
        for(int i = 0; i < hashBits; ++i){
            long hIndex = (hash & mask8bit) >> (i*8);
            mask8bit = mask8bit << 8;
            if (hIndex < 128){
                if(hIndex < 64)
                    nonFullStackBloomFilter |= (1L << hIndex);
                else
                    nonFullStackBloomFilter1 |= (1L << (hIndex-64));
            }else{
                if(hIndex < 192)
                    nonFullStackBloomFilter2 |= (1L << (hIndex-128));
                else
                    nonFullStackBloomFilter3 |= (1L << (hIndex-192));
            }
        }
    }


    public int indexOf(ItemStack stack) {
        if (!maybeContains(stack)) return -1;
        for (int i = 0; i < totalSlots; i++) {
            ItemStack slot =  getSlot(i);
            if (areItemsAndTagsEqual(stack, slot)) return i;
        }
        return -1;
    }

    public boolean hasFreeSlots() {
        if (!initialized) recalculate();
        return getFirstFreeSlot() >= 0;
    }

    //public int findInsertSlot(ItemStack stack) {
    //    return findInsertSlot(stack, 0);
    //}

    public int findInsertSlot(ItemStack stack) {
        if (!initialized) recalculate();

        //Empty slot available? Check for non full stacks before.
        int firstFreeSlot = getFirstFreeSlot();
        if (firstFreeSlot >= 0) {
            long hash = hash(stack);
            if (preEmptyNonFullStackFilterContains(hash)){
                for (int i = 0; i < firstFreeSlot; i++) {
                    ItemStack slot = getSlot(i);
                    if (slot.getCount() >= slot.getMaxCount()) continue;
                    if (areItemsAndTagsEqual(stack, slot)) return i;
                }
            }
            return firstFreeSlot;
        }

        //No empty Slot, search everything if there may be a fitting non full stack
        if (!maybeContainsNonFullStack(stack)) return -1;

        int start = 0;
        for (int i = start; i < totalSlots; i++) {
            ItemStack slot = getSlot(i);
            if (slot.getCount() >= slot.getMaxCount()) continue;
            if (areItemsAndTagsEqual(stack, slot)) return i;
        }
        return -1;
    }

    /*public int findExtractSlot(ItemStack stack){
        if (!initialized) recalculate();

        if (stack == null || stack.isEmpty()) return firstOccupiedSlot;
        if (stack.getMaxCount() <= stack.getCount()) return -1;

        if (maybeContains(stack)){
            for (int i = 0; i < totalSlots; i++) {
                ItemStack slot = getSlot(i);
                if (areItemsAndTagsEqual(stack, slot)) return i;
            }
        }

        return -1;
    }*/

    private static boolean areItemsAndTagsEqual(ItemStack a, ItemStack b) {
        if (!ItemStack.areItemsEqual(a, b)) return false;
        return ItemStack.areTagsEqual(a, b);
    }
}
