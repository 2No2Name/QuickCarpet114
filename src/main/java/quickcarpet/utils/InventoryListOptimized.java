package quickcarpet.utils;

import net.minecraft.item.ItemStack;
import net.minecraft.util.DefaultedList;
import org.apache.commons.lang3.Validate;
import quickcarpet.settings.Settings;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;

public class InventoryListOptimized<E> extends DefaultedList<E>
{
    private InventoryOptimizer optimizer = null;

    public InventoryOptimizer getCreateOrRemoveOptimizer(){
        if (!Settings.optimizedInventories) return this.optimizer = null;

        if (this.optimizer == null){
            this.optimizer = new InventoryOptimizer(this);
        }
        return this.optimizer;
    }

    public InventoryOptimizer getOrRemoveOptimizer(){
        if (!Settings.optimizedInventories) return this.optimizer = null;
        return optimizer;
    }

    public void killOptimizer(){
        this.optimizer = null;
    }

    public static <E> DefaultedList<E> of() {
        return new InventoryListOptimized<>();
    }

    public static <E> DefaultedList<E> ofSize(int int_1, E object_1) {
        Validate.notNull(object_1);
        Object[] objects_1 = new Object[int_1];
        Arrays.fill(objects_1, object_1);
        return new InventoryListOptimized<>((List<E>) Arrays.asList(objects_1), object_1);
    }

    @SafeVarargs
    public static <E> DefaultedList<E> copyOf(E object_1, E... objects_1) {
        return new InventoryListOptimized<>(Arrays.asList(objects_1), object_1);
    }

    private InventoryListOptimized() {
        super();
    }

    public InventoryListOptimized(List<E> list_1, @Nullable E object_1) {
        super(list_1,object_1);
    }

    public E set(int int_1, E object_1) {
        E ret =  super.set(int_1, object_1);
        if(Settings.optimizedInventories){
            InventoryOptimizer opt = this.getOrRemoveOptimizer();
            if (opt != null) opt.update(int_1, (ItemStack) ret);
        }
        else this.optimizer = null;
        return ret;
    }

    public void add(int int_1, E object_1) {
        if(Settings.optimizedInventories)
            throw new UnsupportedOperationException("Won't resize optimized inventory!");
        else
            super.add(int_1,object_1);
    }

    public E remove(int int_1) {
        if(Settings.optimizedInventories)
            throw new UnsupportedOperationException("Won't resize optimized inventory!");
        else
            return super.remove(int_1);
    }

    public void clear() {
        this.killOptimizer(); //idk if this call is neccessary. (clear is usually called when closing the world)
        super.clear();
    }


    private int sizeOverride = -1;

    public void setSize(int size){
        sizeOverride = size;
    }

    @Override
    public int size() {
        if(sizeOverride >= 0 && Settings.optimizedInventories)
            return sizeOverride;
        return super.size();
    }
}
