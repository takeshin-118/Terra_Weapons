package jp.shint.terracraft.jei;

import mezz.jei.api.IModPlugin;
import net.minecraft.util.Identifier;

/** An informational altar guide. It intentionally does not create a recipe or use for the permission token. */
public final class TerracraftJeiPlugin implements IModPlugin {
    private static final Identifier UID = Identifier.of("terracraft", "jei_plugin");
    public static final Identifier CATEGORY_ID = Identifier.of("terracraft", "terra_axe_rights");

    @Override public Identifier getPluginUid() { return UID; }

    public static final class RitualGuide { }
}
