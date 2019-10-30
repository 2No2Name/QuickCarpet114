package quickcarpet.mixin;

import net.minecraft.block.HopperBlock;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.entity.Hopper;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DefaultedList;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import quickcarpet.annotation.Feature;
import quickcarpet.helper.HopperCounter;
import quickcarpet.helper.WoolTool;
import quickcarpet.settings.Settings;
import quickcarpet.utils.InventoryListOptimized;
import quickcarpet.utils.InventoryOptimizer;
import quickcarpet.utils.OptimizedInventory;

import javax.annotation.Nullable;
import java.util.Arrays;

@Feature("hopperCounters")
@Mixin(HopperBlockEntity.class)
public abstract class HopperBlockEntityMixin extends LootableContainerBlockEntity implements OptimizedInventory {

    protected HopperBlockEntityMixin(BlockEntityType<?> blockEntityType_1) {
        super(blockEntityType_1);
    }

    @Shadow private static boolean extract(Hopper hopper_1, Inventory inventory_1, int int_1, Direction direction_1) {
        throw new AssertionError();
    }

    @Shadow public abstract double getHopperX();

    @Shadow public abstract double getHopperY();

    @Shadow public abstract double getHopperZ();

    @Shadow public abstract void setInvStack(int int_1, ItemStack itemStack_1);

    @Shadow public abstract int getInvSize();

    @Shadow private static ItemStack transfer(@Nullable Inventory from, Inventory to, ItemStack stack, int index, @Nullable Direction direction) {
        throw new AssertionError();
    }

    @Feature("hopperCounters")
    @Inject(method = "insert", at = @At("HEAD"), cancellable = true)
    private void onInsert(CallbackInfoReturnable<Boolean> cir) {
        if (Settings.hopperCounters) {
            DyeColor wool_color = WoolTool.getWoolColorAtPosition(
                    getWorld(),
                    new BlockPos(getHopperX(), getHopperY(), getHopperZ()).offset(this.getCachedState().get(HopperBlock.FACING)));


            if (wool_color != null) {
                for (int i = 0; i < this.getInvSize(); ++i) {
                    if (!this.getInvStack(i).isEmpty()) {
                        ItemStack itemstack = this.getInvStack(i);//.copy();
                        HopperCounter.COUNTERS.get(wool_color).add(this.getWorld().getServer(), itemstack);
                        this.setInvStack(i, ItemStack.EMPTY);
                    }
                }
                cir.setReturnValue(true);
            }
        }
    }

    @Feature("optimizedInventories")
    @Inject(method = "transfer(Lnet/minecraft/inventory/Inventory;Lnet/minecraft/inventory/Inventory;Lnet/minecraft/item/ItemStack;Lnet/minecraft/util/math/Direction;)Lnet/minecraft/item/ItemStack;", at = @At("HEAD"), cancellable = true)
    private static void optimizedTransfer(Inventory from, Inventory to, ItemStack stack, Direction direction, CallbackInfoReturnable<ItemStack> cir) {
        if (Settings.optimizedInventories && to instanceof OptimizedInventory) {
            OptimizedInventory optoTo = (OptimizedInventory) to;
            InventoryOptimizer optimizer = optoTo.getOptimizer();
            if (optimizer == null) return;
            while (!stack.isEmpty()) {
                int index = optimizer.findInsertSlot(stack);
                if (index == -1) break;
                int count = stack.getCount();
                stack = transfer(from, to, stack, index, direction);
                if (stack.getCount() == count) break;
            }
            cir.setReturnValue(stack);
        }
    }

    //Now handled by InventoriesMixin
    /*@Inject(method = "Lnet/minecraft/block/entity/HopperBlockEntity;insert()Z", at = @At(value= "INVOKE", target = "Lnet/minecraft/inventory/Inventory;markDirty()V"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void notifyOptimizedInventoryAboutChangedItemStack(CallbackInfoReturnable<Boolean> cir, Inventory inventory_1, Direction direction_1, int int_1, ItemStack itemStack_1, ItemStack itemStack_2){
        if(!Settings.optimizedInventories) return;
        InventoryOptimizer opt = this.getOptimizer();
        if (opt != null)
            opt.onItemStackCountChanged(int_1,-1);
    }//*/

    /*@Inject(method = "extract(Lnet/minecraft/block/entity/Hopper;Lnet/minecraft/inventory/Inventory;ILnet/minecraft/util/math/Direction;)Z", at = @At(value= "INVOKE", target = "Lnet/minecraft/inventory/Inventory;markDirty()V"), locals = LocalCapture.CAPTURE_FAILHARD)
    private static void notifyOptimizedInventoryAboutChangedItemStack1(Hopper hopper_1, Inventory inventory_1, int int_1, Direction direction_1, CallbackInfoReturnable<Boolean> cir, ItemStack itemStack_1, ItemStack itemStack_2, ItemStack itemStack_3){
        if(!Settings.optimizedInventories) return;
        InventoryOptimizer opt = inventory_1 instanceof OptimizedInventory ? ((OptimizedInventory)inventory_1).getOptimizer() : null;
        if (opt != null)
            opt.onItemStackCountChanged(int_1,1);
    }//*/

    @Inject(method = "transfer(Lnet/minecraft/inventory/Inventory;Lnet/minecraft/inventory/Inventory;Lnet/minecraft/item/ItemStack;ILnet/minecraft/util/math/Direction;)Lnet/minecraft/item/ItemStack;", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;increment(I)V"), locals = LocalCapture.CAPTURE_FAILHARD)
    private static void notifyOptimizedInventoryAboutChangedItemStack2(Inventory inventory_1, Inventory inventory_2, ItemStack itemStack_1, int int_1, Direction direction_1, CallbackInfoReturnable<ItemStack> cir, ItemStack itemStack_2, boolean boolean_1, boolean boolean_2, int int_2, int int_3){
        if(!Settings.optimizedInventories) return;
        InventoryOptimizer opt = inventory_2 instanceof OptimizedInventory ? ((OptimizedInventory)inventory_2).getOptimizer() : null;
        if (opt != null)
            opt.onItemStackCountChanged(int_1,int_3);
    }


    @Inject(method = "extract(Lnet/minecraft/block/entity/Hopper;)Z", at = @At(value = "FIELD", target = "Lnet/minecraft/util/math/Direction;DOWN:Lnet/minecraft/util/math/Direction;", shift = At.Shift.AFTER),cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    private static void optimizeExtract(Hopper to, CallbackInfoReturnable<Boolean> cir, Inventory from){
        if(Settings.optimizedInventories){
            InventoryOptimizer toOpt;
            if (to instanceof OptimizedInventory) {
                toOpt = ((OptimizedInventory) to).getOptimizer();
                if(toOpt == null) return;
                if(!toOpt.hasFreeSlots()){
                    InventoryOptimizer fromOpt;
                    if(from instanceof OptimizedInventory && (fromOpt = ((OptimizedInventory) from).getOptimizer()) != null && fromOpt.getOccupiedSlots() > 5){
                        for(int i = 0; i < to.getInvSize(); i++) {
                            ItemStack stack = to.getInvStack(i);
                            if(stack.getMaxCount() > stack.getCount()){
                                int j = fromOpt.indexOf(stack);
                                if(j == -1) continue;
                                if (extract(to, from, j, Direction.DOWN)) {
                                    cir.cancel();
                                    return;
                                }else {
                                    System.out.println("Item Transfer Error, falling back to vanilla");
                                    return;
                                }
                            }
                        }
                        cir.cancel();
                    }
                    //else use vanilla (with optimized transfer) implementation
                }

            }
        }
    }




    //Redirects and Injects to replace the inventory with an optimized Inventory
    @Shadow private DefaultedList<ItemStack> inventory;

    @Redirect(method = "<init>()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/DefaultedList;ofSize(ILjava/lang/Object;)Lnet/minecraft/util/DefaultedList;"))
    private DefaultedList<ItemStack> createInventory(int int_1, Object object_1){
        return InventoryListOptimized.ofSize(int_1,(ItemStack) object_1);
    }

    @Inject(method = "setInvStackList", at = @At("RETURN"))
    private void onSetStackList(DefaultedList<ItemStack> stackList, CallbackInfo ci) {
        if(!(inventory instanceof InventoryListOptimized))
            inventory = new InventoryListOptimized<>(Arrays.asList((ItemStack[])inventory.toArray()),ItemStack.EMPTY);
    }

    @Redirect(method = "fromTag", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/DefaultedList;ofSize(ILjava/lang/Object;)Lnet/minecraft/util/DefaultedList;"))
    private DefaultedList<ItemStack> createInventory2(int int_1, Object object_1) {
        return InventoryListOptimized.ofSize(int_1,(ItemStack) object_1);
    }

    @Nullable
    public InventoryOptimizer getOptimizer() {
        return mayHaveOptimizer() && inventory instanceof InventoryListOptimized ? ((InventoryListOptimized)inventory).getCreateOrRemoveOptimizer(): null;
    }

    @Override
    public void killOptimizer() {
        if(inventory instanceof InventoryListOptimized) ((InventoryListOptimized)inventory).killOptimizer();
    }


    private int viewerCount = 0;
    //@Inject(method = "onInvOpen(Lnet/minecraft/entity/player/PlayerEntity;)V", at = @At(value = "HEAD"))
    public void onInvOpen(PlayerEntity playerEntity_1) {
        if (!playerEntity_1.isSpectator()) {
            killOptimizer();
            viewerCount++;
        }
    }

    public void onInvClose(PlayerEntity playerEntity_1) {
        if (!playerEntity_1.isSpectator()) {
            viewerCount--;
            if (viewerCount < 0) {
                System.out.println("Hopper viewer count inconsistency, may affect correctness of optimizedInventories!");
                viewerCount = 0;
            }
        }
    }

    @Override
    public boolean mayHaveOptimizer() {
        return viewerCount <= 0;
    }
}
