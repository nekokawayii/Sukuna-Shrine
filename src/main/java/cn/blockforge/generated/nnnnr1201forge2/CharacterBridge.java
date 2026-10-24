package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.server.level.ServerPlayer;

/** 切换角色时只清理本人施放的战斗对象，不清除全服状态、不重置通关记录。 */
public final class CharacterBridge {
    public static void stop(ServerPlayer p) {
        AuthoritySkills.cancel(p);MeleeCombo.cancel(p);FreehandDrawing.cancel(p);
        Network.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->p),new Network.TestReset());
        GuardianPlayerSkills.testReset(p);DeathStar.cancel(p);Shinbuke.cancelCharge(p);
        ShadowSystem.recall(p,-1);
        MahoragaAdaptStore.recallAll(p);
        if(Authority.unlocked(p))Legion.storeAll(p);
        Curse.data(p).putBoolean("Fist",false);Curse.data(p).remove("Rotation");
        Curse.data(p).putBoolean("TenShadowsMahoragaPartial",false);
    }
    private CharacterBridge() {}
}
