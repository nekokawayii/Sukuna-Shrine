package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;

/** 日车宽见新招粒子（r344）：PhotonBridge 贴图粒子，金色法庭风。 */
@Mod.EventBusSubscriber(modid = "sukuna", value = Dist.CLIENT)
public final class HigurumaFxClient {
    private static final int GOLD = 0xFFE8C86A, PALE = 0xFFFFF0C0, WOOD = 0xFF8A6A3A;

    public static void receive(Network.Effect e) {
        Vec3 at = e.position();
        float size = Math.max(.3f, e.size());
        switch (e.kind()) {
            case character.higuruma.HigurumaExtras.FX_BONE ->
                PhotonBridge.cloud(at, GOLD, 1.2, .5, 14, 12);
            case character.higuruma.HigurumaExtras.FX_VERDICT -> {
                PhotonBridge.cloud(at, GOLD, 2.4, .9, 26, 16);
                PhotonBridge.smoke(at.add(0, .2, 0), PALE, size, .7, 10, 16);
            }
            case character.higuruma.HigurumaExtras.FX_FREEZE -> { // 金链：两串绕目标的粒子
                for (int i = 0; i < 10; i++) {
                    double a = i * Math.PI / 5;
                    PhotonBridge.spawn(at.add(Math.cos(a) * 1.1, i * .22, Math.sin(a) * 1.1), GOLD, .4, 3, 14);
                }
            }
            case character.higuruma.HigurumaExtras.FX_COFFIN -> { // 长椅破土
                for (int i = 0; i < 8; i++) {
                    double a = i * Math.PI / 4;
                    PhotonBridge.smoke(at.add(Math.cos(a) * size * .5, .1, Math.sin(a) * size * .5), WOOD, .8, .5, 5, 14);
                }
            }
            case character.higuruma.HigurumaExtras.FX_LIGHT -> { // 天降金柱
                for (int i = 0; i < 8; i++) PhotonBridge.spawn(at.add(0, i * .9, 0), PALE, .8, 5, 14);
                PhotonBridge.cloud(at, GOLD, 1.6, .7, 18, 14);
            }
            default -> { }
        }
    }

    private HigurumaFxClient() { }
}
