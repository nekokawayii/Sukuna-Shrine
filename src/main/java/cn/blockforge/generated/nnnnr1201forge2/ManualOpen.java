package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

/** 帕秋莉打开：全程反射，未安装时返回 false。 */
public final class ManualOpen {
    public static boolean openPatchouli(ServerPlayer p) {
        if (!ModList.get().isLoaded("patchouli")) return false;
        try {
            Class<?> api = Class.forName("vazkii.patchouli.api.PatchouliAPI");
            Object inst = api.getMethod("get").invoke(null);
            inst.getClass().getMethod("openBookGUI", net.minecraft.world.entity.player.Player.class,
                net.minecraft.resources.ResourceLocation.class)
                .invoke(inst, p, new net.minecraft.resources.ResourceLocation("jujutsu_kaisen_cursed_paths", "guide"));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private ManualOpen() {}
}
