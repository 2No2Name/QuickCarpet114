package quickcarpet.mixin;

import com.google.common.collect.Lists;
import com.mojang.datafixers.Dynamic;
import net.minecraft.entity.EntityType;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.EndCityFeature;
import net.minecraft.world.gen.feature.StructureFeature;
import org.spongepowered.asm.mixin.Mixin;
import quickcarpet.annotation.Feature;

import java.util.List;
import java.util.function.Function;

@Feature("shulkerSpawningInEndCities")
@Mixin(EndCityFeature.class)
public abstract class EndCityFeatureMixin extends StructureFeature<DefaultFeatureConfig> {
    private static final List<Biome.SpawnEntry> spawnList = Lists.newArrayList(new Biome.SpawnEntry(EntityType.SHULKER, 10, 4, 4));
    
    public EndCityFeatureMixin(Function<Dynamic<?>, ? extends DefaultFeatureConfig> function_1) {
        super(function_1);
    }
    
    @Override
    public List<Biome.SpawnEntry> getMonsterSpawns() {
        return spawnList;
    }
}
