package me.dratii.tradr.modmenu;
import eu.midnightdust.lib.config.MidnightConfig;
import net.minecraft.util.Identifier;


public class ConfigScreen extends MidnightConfig {
    public static final String TEXT = "text";
    @Entry(category = TEXT, idMode = 0)
    public static Identifier TradeFor = Identifier.ofVanilla("emerald");
}