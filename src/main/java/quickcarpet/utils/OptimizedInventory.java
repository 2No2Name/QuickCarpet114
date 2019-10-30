package quickcarpet.utils;

import net.minecraft.inventory.Inventory;

import javax.annotation.Nullable;

public interface OptimizedInventory extends Inventory {
    @Nullable
    InventoryOptimizer getOptimizer();

    void killOptimizer(); //For player actions (probably many uncontrolled actions, can be fixed if neccessary)
    boolean mayHaveOptimizer(); //True when no player is looking into the inventory
}
