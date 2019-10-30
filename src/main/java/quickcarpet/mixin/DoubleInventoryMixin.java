package quickcarpet.mixin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.DoubleInventory;
import net.minecraft.inventory.Inventory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import quickcarpet.annotation.Feature;
import quickcarpet.settings.Settings;
import quickcarpet.utils.DoubleInventoryOptimizer;
import quickcarpet.utils.InventoryOptimizer;
import quickcarpet.utils.OptimizedInventory;

import javax.annotation.Nullable;

@Feature("optimizedInventories")
@Mixin(DoubleInventory.class)
public abstract class DoubleInventoryMixin implements OptimizedInventory {
    @Shadow @Final private Inventory first;
    @Shadow @Final private Inventory second;

    private DoubleInventoryOptimizer optimizer; //Make sure this is only used when both of its halfs have optimizers

    private DoubleInventoryOptimizer getCreateOrRemoveOptimizer(){
        if (!Settings.optimizedInventories || !mayHaveOptimizer()) { //Remove first's and second's optimizers
            if(this.first == null){
                System.out.println("Double Inventory with empty first half!");
            }else if (this.first instanceof OptimizedInventory){
                ((OptimizedInventory) this.first).getOptimizer();
            }
            if(this.second == null){
                System.out.println("Double Inventory with empty second half!");
            }else if (this.second instanceof OptimizedInventory){
                ((OptimizedInventory) this.second).getOptimizer();
            }
            return this.optimizer = null;
        }

        if (this.optimizer == null && first instanceof OptimizedInventory && second instanceof OptimizedInventory){
            if(((OptimizedInventory) first).getOptimizer() == null || ((OptimizedInventory) second).getOptimizer() == null){
                System.out.println("Bad initialisation of OptimizedInventory's stacklist! Skipping optmizations!");
                return null;
            }
            this.optimizer = new DoubleInventoryOptimizer((OptimizedInventory)first,(OptimizedInventory)second);
        }
        return this.optimizer;
    }

    @Override
    @Nullable
    public InventoryOptimizer getOptimizer() {
        return mayHaveOptimizer() && first instanceof OptimizedInventory && second instanceof OptimizedInventory ? getCreateOrRemoveOptimizer() : null;
    }

    @Override
    public void killOptimizer() {
        if(this.first == null){
            System.out.println("Double Inventory with empty first half!");
        }else if (this.first instanceof OptimizedInventory){
            ((OptimizedInventory) this.first).killOptimizer();
        }
        if(this.second == null){
            System.out.println("Double Inventory with empty second half!");
        }else if (this.second instanceof OptimizedInventory){
            ((OptimizedInventory) this.second).killOptimizer();
        }
        this.optimizer = null;
    }

    @Override
    public boolean mayHaveOptimizer() {
        return this.first instanceof OptimizedInventory && ((OptimizedInventory) this.first).mayHaveOptimizer()
                && this.second instanceof OptimizedInventory && ((OptimizedInventory) this.second).mayHaveOptimizer();
    }

    @Inject(method = "onInvOpen(Lnet/minecraft/entity/player/PlayerEntity;)V", at = @At(value = "HEAD"))
    private void inventoryPanic(PlayerEntity playerEntity_1, CallbackInfo ci) {
        if (!playerEntity_1.isSpectator())
            killOptimizer();
    }

}
