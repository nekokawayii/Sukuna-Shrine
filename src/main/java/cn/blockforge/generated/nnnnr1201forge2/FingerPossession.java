package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 吞手指的宿傩夺舍（r326）：除虎杖/宿傩外，每根 70% 被夺舍、30% 咒力翻倍但反胃+黑暗 15 秒。 */
public final class FingerPossession {
    private static final int CAMERA_LOCK = 798;

    /** 返回 true = 被夺舍（调用方不再走正常成长）。 */
    public static boolean tryPossess(ServerPlayer p) {
        var definition = character.CharacterService.selected(p);
        String id = definition == null ? "sukuna" : definition.id();
        if ("yuji".equals(id) || "sukuna".equals(id)) return false;
        if (p.getRandom().nextInt(100) >= 70) { // 30%：咒力翻倍 + 反胃/黑暗 15 秒
            Curse.addRaw(p, (int) Math.min(1_000_000, Curse.maximum(p)));
            p.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 300, 0, false, true));
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 300, 0, false, true));
            p.displayClientMessage(Component.literal("诅咒之力灌入体内：咒力猛然翻倍，但胃里翻江倒海、眼前一片漆黑。"), true);
            return false;
        }
        if (!(p.level() instanceof ServerLevel level)) return false;
        Vec3 at = p.position();
        var npc = Content.SUKUNA_NPC.get().create(level);
        if (npc != null) {
            npc.moveTo(at.x, at.y, at.z, p.getYRot(), 0);
            npc.setPersistenceRequired();
            npc.finalizeSpawn(level, level.getCurrentDifficultyAt(p.blockPosition()), MobSpawnType.EVENT, null, null);
            level.addFreshEntity(npc);
        }
        p.getPersistentData().putBoolean("SukunaPossessed", true);
        p.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
        Network.send(level, new Network.Effect(CAMERA_LOCK, p.getId(), at, Vec3.ZERO, 1f, 200, 0));
        Component suicide = Component.literal("[ 自杀 ]")
            .withStyle(Style.EMPTY.withColor(0xFFFF4A4A).withBold(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/kill @s"))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal("放弃这具身体，于出生点重生（宿傩会留下）"))));
        p.sendSystemMessage(Component.literal("手指在喉间化作一声低笑——「这具身体，归我了。」").withStyle(Style.EMPTY.withColor(0xFF8A00)));
        p.sendSystemMessage(suicide);
        return true;
    }

    @SubscribeEvent
    public static void respawn(PlayerEvent.PlayerRespawnEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        if (!p.getPersistentData().getBoolean("SukunaPossessed")) return;
        p.getPersistentData().remove("SukunaPossessed");
        p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
    }

    private FingerPossession() { }
}
