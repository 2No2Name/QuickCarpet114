package quickcarpet.mixin;


import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.StorageMinecartEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DefaultedList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import quickcarpet.utils.InventoryListOptimized;
import quickcarpet.utils.InventoryOptimizer;
import quickcarpet.utils.OptimizedInventory;

import javax.annotation.Nullable;


@Mixin(StorageMinecartEntity.class)
public abstract class StorageMinecartEntityMixin implements OptimizedInventory {

    //Redirects and Injects to replace the inventory with an optimized Inventory
    @Shadow private DefaultedList<ItemStack> inventory;

    @Redirect(method = "<init>(Lnet/minecraft/entity/EntityType;Lnet/minecraft/world/World;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/DefaultedList;ofSize(ILjava/lang/Object;)Lnet/minecraft/util/DefaultedList;"))
    private DefaultedList<ItemStack> createInventory(int int_1, Object object_1){
        DefaultedList<ItemStack> ret = InventoryListOptimized.ofSize(int_1,(ItemStack) object_1);
        ((InventoryListOptimized)ret).setSize(this.getInvSize());
        return ret;
    }

    @Redirect(method = "<init>(Lnet/minecraft/entity/EntityType;DDDLnet/minecraft/world/World;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/DefaultedList;ofSize(ILjava/lang/Object;)Lnet/minecraft/util/DefaultedList;"))
    private DefaultedList<ItemStack> createInventory1(int int_1, Object object_1){
        DefaultedList<ItemStack> ret =  InventoryListOptimized.ofSize(int_1,(ItemStack) object_1);
        ((InventoryListOptimized)ret).setSize(this.getInvSize());
        return ret;
    }

    @Redirect(method = "readCustomDataFromTag(Lnet/minecraft/nbt/CompoundTag;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/DefaultedList;ofSize(ILjava/lang/Object;)Lnet/minecraft/util/DefaultedList;"))
    private DefaultedList<ItemStack> createInventory2(int int_1, Object object_1){
        DefaultedList<ItemStack> ret =  InventoryListOptimized.ofSize(int_1,(ItemStack) object_1);
        ((InventoryListOptimized)ret).setSize(this.getInvSize());
        return ret;
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
    public void onInvOpen(PlayerEntity playerEntity_1) {
        if (!playerEntity_1.isSpectator())
            killOptimizer();
        viewerCount++;
    }

    public void onInvClose(PlayerEntity playerEntity_1) {
        viewerCount--;
        if (viewerCount < 0) {
            System.out.println("StorageMinecartEntityMixin: (Inventory-)viewerCount inconsistency detected, might affect performance of optimizedInventories!");
            viewerCount = 0;
        }
    }

    @Override
    public boolean mayHaveOptimizer() {
        return viewerCount <= 0;
    }
}
