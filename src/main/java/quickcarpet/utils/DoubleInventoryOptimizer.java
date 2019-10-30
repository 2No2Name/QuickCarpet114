package quickcarpet.utils;

import net.minecraft.item.ItemStack;

//Don't store instances of InventoryOptimizer, unless you sync with the corresponding inventory!
//DoubleInventoryOptimizer actually handles that in DoubleInventoryMixin
public class DoubleInventoryOptimizer extends InventoryOptimizer {
    private final OptimizedInventory first;
    private final InventoryOptimizer firstOpt;
    private final OptimizedInventory second;
    private final InventoryOptimizer secondOpt;




    public DoubleInventoryOptimizer(OptimizedInventory first, OptimizedInventory second) {
        super(null);
        this.first = first;
        this.second = second;
        this.firstOpt = first.getOptimizer();
        this.secondOpt = second.getOptimizer();
    }

    @Override
    public void onItemStackCountChanged(int index, int countChange) {
        int firstSize = first.getInvSize();
        if (index >= firstSize) {
            if (secondOpt != null) secondOpt.onItemStackCountChanged(index - firstSize, countChange);
        } else {
            if (firstOpt != null) firstOpt.onItemStackCountChanged(index, countChange);
        }
    }

    public int indexOf(ItemStack stack) {
        int ret = firstOpt.indexOf(stack);
        if(ret == -1){
            ret = secondOpt.indexOf(stack);
            if(ret != -1)
                ret += first.getInvSize();
        }
        return ret;
    }

    public boolean hasFreeSlots() {
        return firstOpt.hasFreeSlots() || secondOpt.hasFreeSlots();
    }

    public int findInsertSlot(ItemStack stack) {
        int ret = firstOpt.findInsertSlot(stack);
        if(ret == -1){
            ret = secondOpt.findInsertSlot(stack);
            if(ret != -1)
                ret += first.getInvSize();
        }
        return ret;
    }


    void markEscaped(int slot){
        //see InventoryOptimizer, not implemented yet, hopefully not required
    }


    @Override
    public void recalculate() {
        throw new UnsupportedOperationException("InventoryOptimizer parts have to be calculated individually");
    }
    @Override
    public int getFirstFreeSlot() {
        int ret = firstOpt.getFirstFreeSlot();
        if(ret == -1){
            ret = secondOpt.getFirstFreeSlot();
            if(ret != -1)
                ret += first.getInvSize();
        }
        return ret;
    }

    @Override
    protected ItemStack getSlot(int index) {
        if (index < 0) return ItemStack.EMPTY;
        int firstSize = first.getInvSize();
        if (index < firstSize) return first.getInvStack(index);
        return second.getInvStack(index - firstSize);
    }

    @Override
    protected int size() {
        return first.getInvSize() + second.getInvSize();
    }
}
