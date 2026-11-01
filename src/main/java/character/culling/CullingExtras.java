package character.culling;

import cn.blockforge.generated.nnnnr1201forge2.*;
import java.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobEffectEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 死灭洄游 r219：把 r218 里只登记未实现的 8 条规则补齐。
 *
 * <ul>
 *   <li>术式公开：敌方出手先亮 0.2 秒红光，伤害延后 4 刻结算；</li>
 *   <li>量子纠缠：界内飞行弹道速度 ×0.5；</li>
 *   <li>镜面反射：弹道撞方块后折射一次（每个弹道只折一次）；</li>
 *   <li>时间扭曲：药水/状态时长 ×4；</li>
 *   <li>剥夺感官：附近怪物失明，玩家屏幕加暗角；</li>
 *   <li>活体迷宫：附近方块临时变成改造血肉，2 秒后还原，靠太近会被挤压；</li>
 *   <li>契约赌局：界内刷出「羂索的幻影」（半透明夏油杰），右键用一半积分回满；</li>
 *   <li>亡命之徒：隐藏血条（客户端遮罩）；</li>
 *   <li>最终清算：免死一次。</li>
 * </ul>
 */
public final class CullingExtras {
    public static final int UI_HIDE_HEALTH = 1, UI_FOG = 2;

    // ========== 术式公开：延迟结算 ==========
    private record Delayed(ServerPlayer victim, net.minecraft.world.damagesource.DamageSource source, float amount, long at) { }
    private static final List<Delayed> DELAYED = new ArrayList<>();
    private static boolean reapplying;

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void telegraph(LivingHurtEvent e) {
        if (reapplying || e.isCanceled() || e.getAmount() <= 0) return;
        if (!(e.getEntity() instanceof ServerPlayer vp) || !CullingRules.inDimension(vp)) return;
        if (!CullingRules.special(vp, "art_open")) return;
        Mob source = e.getSource().getDirectEntity() instanceof Mob m ? m
            : e.getSource().getEntity() instanceof Mob m ? m : null;
        if (source == null) return;
        e.setCanceled(true);
        DELAYED.add(new Delayed(vp, e.getSource(), e.getAmount(), vp.level().getGameTime() + 4));
        if (vp.level() instanceof ServerLevel level)
            level.sendParticles(new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(1f, .1f, .1f), 1.6f),
                source.getX(), source.getEyeY() + .4, source.getZ(), 14, .3, .3, .3, .02);
    }

    @SubscribeEvent
    public static void delayed(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || DELAYED.isEmpty()) return;
        long now = e.getServer().overworld().getGameTime();
        for (var it = DELAYED.iterator(); it.hasNext(); ) {
            Delayed d = it.next();
            if (now < d.at()) continue;
            it.remove();
            if (!d.victim().isAlive()) continue;
            reapplying = true;
            try { d.victim().hurt(d.source(), d.amount()); } finally { reapplying = false; }
        }
    }

    // ========== 量子纠缠 ==========
    @SubscribeEvent
    public static void projectileJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof net.minecraft.world.entity.projectile.Projectile proj)) return;
        if (!proj.level().dimension().equals(CullingGame.DIM)) return;
        if (!anyRule(proj.level(), "quantum")) return;
        proj.setDeltaMovement(proj.getDeltaMovement().scale(.5));
    }

    // ========== 镜面反射 ==========
    @SubscribeEvent
    public static void impact(ProjectileImpactEvent e) {
        var proj = e.getProjectile();
        if (proj.level().isClientSide() || !proj.level().dimension().equals(CullingGame.DIM)) return;
        if (!anyRule(proj.level(), "mirror_shot")) return;
        if (proj.getPersistentData().getBoolean("CullingMirror")) return;
        if (!(e.getRayTraceResult() instanceof BlockHitResult hit)) return;
        Vec3 normal = Vec3.atLowerCornerOf(hit.getDirection().getNormal());
        Vec3 v = proj.getDeltaMovement();
        if (v.lengthSqr() < 1e-4) return;
        proj.getPersistentData().putBoolean("CullingMirror", true);
        proj.setPos(hit.getLocation().add(normal.scale(.35)));
        proj.setDeltaMovement(v.subtract(normal.scale(2 * v.dot(normal))).scale(.85));
        e.setCanceled(true);
    }

    // ========== 反转剥夺：治疗减半 ==========
    @SubscribeEvent
    public static void healed(net.minecraftforge.event.entity.living.LivingHealEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !CullingRules.inDimension(p)) return;
        double mul = CullingRules.healMul(p);
        if (mul < 1) e.setAmount((float) (e.getAmount() * mul));
    }

    // ========== 万有引力 / 低空禁飞 ==========
    private static void noFly(ServerPlayer p) {
        var abilities = p.getAbilities();
        if (abilities.flying || abilities.mayfly) {
            abilities.flying = false;
            abilities.mayfly = false;
            p.onUpdateAbilities();
        }
        if (p.tickCount % 20 == 0) {
            p.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 40, 0, false, false));
            for (Mob mob : p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(48),
                m -> m.isAlive() && m.isNoGravity())) mob.setNoGravity(false);
        }
    }

    // ========== 时间扭曲 ==========
    private static boolean scaling;
    @SubscribeEvent
    public static void effectAdded(MobEffectEvent.Added e) {
        LivingEntity living = e.getEntity();
        if (scaling || living == null) return;
        if (!living.level().dimension().equals(CullingGame.DIM)) return;
        boolean on = living instanceof ServerPlayer p ? CullingRules.special(p, "time_warp") : anyRule(living.level(), "time_warp");
        if (!on) return;
        var instance = e.getEffectInstance();
        if (instance == null || instance.getDuration() >= 20000) return;
        scaling = true;
        try {
            living.addEffect(new MobEffectInstance(instance.getEffect(), instance.getDuration() * 4,
                instance.getAmplifier(), instance.isAmbient(), instance.isVisible(), instance.showIcon()));
        } finally { scaling = false; }
    }

    // ========== 活体迷宫 ==========
    private record Morph(ServerLevel level, BlockPos pos, net.minecraft.world.level.block.state.BlockState state, long revertAt) { }
    private static final List<Morph> MORPHS = new ArrayList<>();

    private static void morph(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level)) return;
        var rnd = level.random;
        for (int i = 0; i < 8; i++) {
            BlockPos pos = p.blockPosition().offset(rnd.nextInt(17) - 8, rnd.nextInt(7) - 3, rnd.nextInt(17) - 8);
            var state = level.getBlockState(pos);
            if (state.isAir() || !state.getFluidState().isEmpty() || state.hasBlockEntity()
                || state.getDestroySpeed(level, pos) < 0) continue;
            MORPHS.add(new Morph(level, pos, state, level.getGameTime() + 120));
            level.setBlockAndUpdate(pos, ProgressionContent.TRANSFIGURED_FLESH.get().defaultBlockState());
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void revert(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || MORPHS.isEmpty()) return;
        long now = e.getServer().overworld().getGameTime();
        for (var it = MORPHS.iterator(); it.hasNext(); ) {
            Morph m = it.next();
            if (now < m.revertAt()) continue;
            it.remove();
            if (m.level().getBlockState(m.pos()).is(ProgressionContent.TRANSFIGURED_FLESH.get()))
                m.level().setBlockAndUpdate(m.pos(), m.state());
        }
    }

    // ========== 契约赌局：羂索的幻影 ==========
    private static void ensurePhantom(ServerPlayer p) {
        if (!(p.level() instanceof ServerLevel level)) return;
        if (!level.getEntitiesOfClass(GetoEntity.class, p.getBoundingBox().inflate(64),
            g -> g.getPersistentData().getBoolean("CullingPhantom")).isEmpty()) return;
        GetoEntity phantom = Content.GETO.get().create(level);
        if (phantom == null) return;
        var rnd = level.random;
        for (int tries = 0; tries < 24; tries++) {
            BlockPos at = p.blockPosition().offset(rnd.nextInt(13) - 6, 0, rnd.nextInt(13) - 6);
            if (!level.hasChunkAt(at)) continue;
            if (!level.getBlockState(at).isAir() || !level.getBlockState(at.above()).isAir()) continue;
            phantom.moveTo(at.getX() + .5, at.getY(), at.getZ() + .5, 0, 0);
            phantom.getPersistentData().putBoolean("CullingPhantom", true);
            phantom.setNoAi(true);
            phantom.setInvulnerable(true);
            phantom.setSilent(true);
            phantom.setPersistenceRequired();
            phantom.setCustomName(Component.literal("羂索的幻影 · 右键用一半积分换满血"));
            phantom.setCustomNameVisible(true);
            level.addFreshEntity(phantom);
            p.sendSystemMessage(Component.literal("契约赌局：羂索的幻影出现了（右键交易）。").withStyle(ChatFormatting.DARK_PURPLE));
            return;
        }
    }

    @SubscribeEvent
    public static void trade(PlayerInteractEvent.EntityInteract e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !(e.getTarget() instanceof GetoEntity geto)) return;
        if (!geto.getPersistentData().getBoolean("CullingPhantom")) return;
        if (!CullingRules.special(p, "kenjaku")) return;
        e.setCanceled(true);
        int points = CullingRules.points(p);
        if (points < 2) { p.sendSystemMessage(Component.literal("契约赌局：积分太少，羂索看不上一眼。").withStyle(ChatFormatting.GRAY)); return; }
        int cost = Math.max(1, points / 2);
        ExpeditionProgress.data(p).putInt("CullingPoints", points - cost);
        p.heal(p.getMaxHealth());
        Curse.addRaw(p, Curse.maximum(p));
        p.displayClientMessage(Component.literal("契约赌局：付出 " + cost + " 分，血与咒力回满").withStyle(ChatFormatting.LIGHT_PURPLE), false);
    }

    // ========== 最终清算：免死一次 ==========
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void bless(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !CullingRules.inDimension(p)) return;
        if (!CullingRules.special(p, "final_clear")) return;
        CompoundTag d = Curse.data(p);
        if (d.getBoolean("CullingBlessUsed")) return;
        d.putBoolean("CullingBlessUsed", true);
        e.setCanceled(true);
        p.setHealth(p.getMaxHealth());
        Curse.addRaw(p, Curse.maximum(p));
        p.displayClientMessage(Component.literal("天元庇护：免死一次").withStyle(ChatFormatting.GOLD), false);
    }

    // ========== 每刻：剥夺感官 / 活体迷宫 / 幻影 / UI 标志 ==========
    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        if (!CullingRules.inDimension(p)) return;
        if (CullingRules.special(p, "fog") && p.tickCount % 20 == 0)
            for (Mob mob : p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(24), Mob::isAlive))
                mob.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0, false, false));
        boolean maze = CullingRules.special(p, "live_maze") || CullingRules.special(p, "live_maze2");
        if (maze && p.tickCount % 40 == 0) morph(p);
        if (maze && p.tickCount % 20 == 0) {
            // 站得离血肉太近就被挤压：轻微伤害 + 推离
            BlockPos around = p.blockPosition();
            for (BlockPos pos : BlockPos.betweenClosed(around.offset(-1, -1, -1), around.offset(1, 1, 1))) {
                if (p.level().getBlockState(pos).is(ProgressionContent.TRANSFIGURED_FLESH.get())) {
                    p.hurt(p.damageSources().magic(), 2f);
                    Vec3 push = p.position().subtract(Vec3.atCenterOf(pos)).normalize().scale(.35);
                    p.push(push.x, .2, push.z);
                    p.hurtMarked = true;
                    break;
                }
            }
        }
        if (CullingRules.special(p, "kenjaku") && p.tickCount % 200 == 0) ensurePhantom(p);
        if (CullingRules.flag(p, m -> m.noFly)) noFly(p);
        if (p.tickCount % 100 == 0) sync(p);
    }

    /** 把客户端 HUD 需要的标志位同步过去。 */
    public static void sync(ServerPlayer p) {
        int mask = 0;
        if (CullingRules.noHealthBar(p)) mask |= UI_HIDE_HEALTH;
        if (CullingRules.special(p, "fog")) mask |= UI_FOG;
        if (mask == Curse.data(p).getInt("CullingUiMask")) return;
        Curse.data(p).putInt("CullingUiMask", mask);
        Network.sendTo(p, new Network.Effect(621, p.getId(), Vec3.ZERO, Vec3.ZERO, mask, 0, 0));
    }

    public static void onLeave(ServerPlayer p) {
        Curse.data(p).putInt("CullingUiMask", 0);
        Curse.data(p).putBoolean("CullingBlessUsed", false);
        Network.sendTo(p, new Network.Effect(621, p.getId(), Vec3.ZERO, Vec3.ZERO, 0, 0, 0));
    }

    private static boolean anyRule(net.minecraft.world.level.Level level, String id) {
        if (!(level instanceof ServerLevel server)) return false;
        for (Player p : server.players()) if (CullingRules.special(p, id)) return true;
        return false;
    }

    private CullingExtras() { }
}
