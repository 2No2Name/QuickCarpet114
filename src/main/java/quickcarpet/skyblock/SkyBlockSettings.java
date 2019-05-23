package quickcarpet.skyblock;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import quickcarpet.settings.ChangeListener;
import quickcarpet.settings.ParsedRule;
import quickcarpet.settings.Rule;

import static quickcarpet.settings.RuleCategory.EXPERIMENTAL;
import static quickcarpet.settings.RuleCategory.FEATURE;

public class SkyBlockSettings {
    @Rule(desc = "Better potions", category = {EXPERIMENTAL, FEATURE}, onChange = BetterPotionListener.class)
    public static boolean betterPotions = false;

    @Rule(desc = "Add trades to the wandering trader for Skyblock", category = {EXPERIMENTAL, FEATURE}, onChange = WanderingTraderSkyblockTradesChange.class)
    public static boolean wanderingTraderSkyblockTrades = false;

    private static class WanderingTraderSkyblockTradesChange implements ChangeListener<Boolean> {
        @Override
        public void onChange(ParsedRule<Boolean> rule) {
            if (wanderingTraderSkyblockTrades) {
                Trades.mergeWanderingTraderOffers(Trades.getSkyblockWanderingTraderOffers());
            } else {
                Trades.mergeWanderingTraderOffers(new Int2ObjectOpenHashMap<>());
            }
        }
    }

    @Rule(
        desc = "Block light detector.", extra = {
            "Right click a daylight sensor with a light source to toggle.",
            "No visual indicator besides redstone power is given."
        }, category = {FEATURE, EXPERIMENTAL}
    )
    public static boolean blockLightDetector = false;
}
