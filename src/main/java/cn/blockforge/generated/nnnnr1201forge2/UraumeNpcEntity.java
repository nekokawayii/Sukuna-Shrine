package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 里梅（NPC）：敌对，使用冰凝咒法「霜凪」与「直瀑」。 */
public class UraumeNpcEntity extends Monster {
    private int shotAt;
    private int skillIndex;

    public UraumeNpcEntity(EntityType<? extends UraumeNpcEntity> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        xpReward = 500;
    }

    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15, true));
        goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 16f));
        goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true,
            e -> e instanceof Player p  && !p.isSpectator()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, NpcBalance.health("sukuna:uraume_npc"))
            .add(Attributes.ATTACK_DAMAGE, 8.0)
            .add(Attributes.MOVEMENT_SPEED, 0.30)
            .add(Attributes.FOLLOW_RANGE, 40.0)
            .add(Attributes.ARMOR, 6.0);
    }

    @Override public void tick() {
        super.tick();
        if (NpcBridge.supports(this)) return; // 霜凪与直瀑使用玩家实现。
        if (level().isClientSide) return;
        if (!(level() instanceof ServerLevel world)) return;
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive()) return;
        if (distanceToSqr(target) > 32 * 32) return;
        long now = world.getGameTime();
        if (now < shotAt) return;
        // 大冰河时代之后的 30 秒严重冻伤：不能放技能
        if (getPersistentData().getLong("FrostbiteUntil") > now) return;
        // 被动·宿傩的眷属：附近有宿傩线玩家/宿傩 NPC 时，回蓝与出招速度翻倍
        boolean kin = level().getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(16),
            e -> e != this && e.isAlive() && (e instanceof SukunaNpcEntity
                || e instanceof Player p && character.CharacterService.selected(p) != null
                    && "sukuna".equals(character.CharacterService.selected(p).id()))).size() > 0;
        shotAt = (int) now + (kin ? 25 : 50);
        if (!NpcCurse.spend(this, 40)) { shotAt = (int) now + 20; return; }
        boolean domain = character.npc.NpcDomain.inIceDomain(this);
        Vec3 eye = getEyePosition();
        Vec3 to = target.getBoundingBox().getCenter();
        Vec3 dir = to.subtract(eye).normalize();
        int pick = skillIndex++ % 7;
        if (pick == 6) {
            // r292：NPC 里梅也会展开「霜天宫殿」（与玩家同步的领域）
            if (!character.npc.NpcDomain.active(this)) {
                character.npc.NpcDomain.open(this, character.npc.NpcDomain.RADIUS,
                    character.npc.NpcDomain.Style.ICE);
            }
            return;
        }
        if (pick == 2) { UraumeIce.handPrison(this, target); return; }
        if (pick == 3) { UraumeIce.storm(this); return; }
        if (pick == 4) { UraumeIce.dragon(this); return; }
        if (pick == 5 && distanceToSqr(target) < 30 * 30) { UraumeIce.iceAge(this); return; }
        if (pick == 0) {
            // 冰凝咒法 · 霜凪：前方锥形寒气
            Network.send(world, new Network.Effect(BranchRules.FX_FROST, getId(), eye, dir, 14, 10, 0));
            for (var t : world.getEntitiesOfClass(LivingEntity.class, getBoundingBox().inflate(14),
                    e -> e != this && e.isAlive() && e instanceof Player p
                        && (domain || e.position().subtract(position()).normalize().dot(dir) > 0.55))) {
                NpcCombat.hurt(this, t, domain ? 24f : 12f);
                t.setTicksFrozen(Math.min(600, t.getTicksFrozen() + 200));
                UraumeIce.addFrost(t, 1);
            }
        } else {
            // 冰凝咒法 · 直瀑：目标处冰柱
            Network.send(world, new Network.Effect(BranchRules.FX_ICE, getId(), to.add(0, 10, 0), new Vec3(0, -1, 0), 3, 20, 0));
            for (var t : world.getEntitiesOfClass(LivingEntity.class, new net.minecraft.world.phys.AABB(to, to).inflate(3.5),
                    e -> e != this && e.isAlive() && e instanceof Player p)) {
                NpcCombat.hurt(this, t, domain ? 32f : 16f);
                t.setTicksFrozen(Math.min(600, t.getTicksFrozen() + 120));
                UraumeIce.addFrost(t, 1);
            }
        }
        world.playSound(null, blockPosition(), (skillIndex & 1) == 1
            ? net.minecraft.sounds.SoundEvents.GLASS_BREAK : net.minecraft.sounds.SoundEvents.POWDER_SNOW_BREAK,
            net.minecraft.sounds.SoundSource.HOSTILE, 1.4f, 0.8f);
    }
}
