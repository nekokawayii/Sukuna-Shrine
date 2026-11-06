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
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingKnockBackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * 死灭洄游规则「第二批」的实际结算（r225）：绝对暴力 / 无限火力 / 绝对生存 / 机动风筝 / 资源爆率。
 * <p>与 {@link CullingRules}（数值管线）和 {@link CullingExtras}（第一批 8 条特殊规则）分开，
 * 只负责这批新规则的伤害、死亡、每刻与方块钩子。
 */
public final class CullingPerks {
    private static final String COMBAT_AT = "CullingCombatAt";
    private static boolean reapplying;
    private static final List<Object[]> WATER = new ArrayList<>();

    // ===== 规则数值读取 =====
    private static double sum(Player p, java.util.function.ToDoubleFunction<CullingRules.Mods> f) {
        double v = 0;
        for (CullingRules.Rule r : CullingRules.active(p)) v += f.applyAsDouble(r.mods());
        return v;
    }

    private static void tell(Player p, String text) {
        p.displayClientMessage(Component.literal(text).withStyle(ChatFormatting.LIGHT_PURPLE), true);
    }

    // ===== 伤害 =====
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void hurt(LivingHurtEvent e) {
        LivingEntity victim = e.getEntity();
        var src = e.getSource();
        LivingEntity attacker = src.getEntity() instanceof LivingEntity a ? a : null;
        boolean melee = attacker != null && src.getDirectEntity() == attacker;

        if (attacker instanceof ServerPlayer ap && CullingRules.inDimension(ap)) {
            double amount = e.getAmount();
            // 弱点洞悉：额外真实伤害
            double truePct = sum(ap, m -> m.truePct);
            if (truePct > 0 && !reapplying) {
                float extra = (float) (victim.getMaxHealth() * truePct);
                reapplying = true;
                try { victim.hurt(ap.damageSources().magic(), extra); } finally { reapplying = false; }
            }
            // 无下限切断：按目标护甲追加等效伤害
            double pen = sum(ap, m -> m.armorPen);
            if (pen > 0) {
                var armor = victim.getAttribute(Attributes.ARMOR);
                double a = armor == null ? 0 : armor.getValue();
                amount *= 1 + Math.min(pen, a / 30.0 * pen);
            }
            // 残血/满血增伤
            if (CullingRules.special(ap, "low_hp_damage") && ap.getHealth() < ap.getMaxHealth() * .3) amount *= 3;
            if (CullingRules.special(ap, "full_hp_damage") && ap.getHealth() >= ap.getMaxHealth() - .01) amount *= 1.5;
            // 连击大师：每 5 次命中后下一击必暴击
            if (CullingRules.special(ap, "combo_crit")) {
                CompoundTag d = Curse.data(ap);
                int n = d.getInt("CullingComboHits") + 1;
                if (n >= 5) { amount *= 2; n = 0; tell(ap, "连击大师：暴击！"); }
                d.putInt("CullingComboHits", n);
            }
            // 属性赋予：平A 附带随机元素伤害
            if (melee && CullingRules.special(ap, "element_melee") && !reapplying) {
                float extra = 6 + ap.getRandom().nextInt(6);
                reapplying = true;
                try { victim.hurt(ap.damageSources().magic(), extra); } finally { reapplying = false; }
            }
            // 降维打击：对怪额外 50% 灵魂判定伤害
            if (CullingRules.special(ap, "soul_strike") && victim instanceof Mob) amount *= 1.5;
            e.setAmount((float) amount);
            // 吸血
            double steal = sum(ap, m -> m.lifesteal);
            if (steal > 0) ap.heal((float) (e.getAmount() * steal));
            // 处刑宣告
            double exec = sum(ap, m -> m.execute);
            if (exec > 0 && victim instanceof Mob mob && mob.getMaxHealth() < 800
                && mob.getHealth() - e.getAmount() < mob.getMaxHealth() * .2 && ap.getRandom().nextDouble() < exec) {
                reapplying = true;
                try { mob.hurt(ap.damageSources().genericKill(), 100000f); } finally { reapplying = false; }
            }
            Curse.data(ap).putLong(COMBAT_AT, ap.level().getGameTime());
        }

        if (victim instanceof ServerPlayer vp && CullingRules.inDimension(vp)) {
            double amount = e.getAmount();
            // 伤害阈值
            double cap = sum(vp, m -> m.damageCap);
            if (cap > 0) amount = Math.min(amount, vp.getMaxHealth() * cap);
            // 逆境求生
            if (CullingRules.special(vp, "low_hp_defense") && vp.getHealth() < vp.getMaxHealth() * .3) amount *= .5;
            e.setAmount((float) amount);
            // 咒力结晶
            double mana = sum(vp, m -> m.manaOnHurt);
            if (mana > 0) Curse.addRaw(vp, amount * mana);
            // 反伤装甲
            double thorns = sum(vp, m -> m.thorns);
            if (thorns > 0 && melee && attacker != null) {
                reapplying = true;
                try { attacker.hurt(vp.damageSources().thorns(vp), (float) (amount * thorns)); } finally { reapplying = false; }
            }
            // 后发先至：受击向反方向弹射
            if (CullingRules.special(vp, "launch_back") && vp.level().getGameTime() - Curse.data(vp).getLong("CullingLaunchAt") > 40) {
                Curse.data(vp).putLong("CullingLaunchAt", vp.level().getGameTime());
                Vec3 away = attacker == null ? vp.getLookAngle().reverse() : vp.position().subtract(attacker.position());
                Vec3 to = vp.position().add(away.normalize().scale(5));
                if (vp.level().noCollision(vp, vp.getBoundingBox().move(to.subtract(vp.position())))) {
                    vp.teleportTo(to.x, to.y, to.z);
                    vp.fallDistance = 0;
                }
            }
            Curse.data(vp).putLong(COMBAT_AT, vp.level().getGameTime());
        }
    }

    // ===== 免死 / 击杀收益 =====
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void deathShield(LivingDeathEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !CullingRules.inDimension(p)) return;
        int shields = (int) sum(p, m -> m.deathShields);
        if (shields <= 0) return;
        CompoundTag d = Curse.data(p);
        if (d.getInt("CullingShieldUsed") >= shields) return;
        d.putInt("CullingShieldUsed", d.getInt("CullingShieldUsed") + 1);
        e.setCanceled(true);
        p.setHealth(p.getMaxHealth());
        Curse.addRaw(p, Curse.maximum(p));
        tell(p, "免死发动：剩余 " + (shields - d.getInt("CullingShieldUsed")) + " 层");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onKill(LivingDeathEvent e) {
        ServerPlayer killer = CombatSettings.caster(e.getSource());
        if (killer == null && e.getEntity().getKillCredit() instanceof ServerPlayer credited) killer = credited;
        if (killer == null || !CullingRules.inDimension(killer)) return;
        // 杀敌回血
        double heal = sum(killer, m -> m.killHeal);
        if (heal > 0) killer.heal((float) (killer.getMaxHealth() * heal));
        // 时间窃贼 / 超频运转：击杀减冷却
        int reduce = (int) sum(killer, m -> m.cooldownOnKill);
        if (CullingRules.special(killer, "max_hp_cost")) {
            var max = killer.getAttribute(Attributes.MAX_HEALTH);
            if (max != null) { max.setBaseValue(Math.min(200, max.getBaseValue() + 1)); killer.heal(1f); }
        }
        if (reduce > 0) {
            CompoundTag d = Curse.data(killer);
            long now = killer.level().getGameTime();
            for (String key : new ArrayList<>(d.getAllKeys())) {
                if (!key.startsWith("CharacterSkillCooldown_") && !key.equals("DomainCooldown")) continue;
                long until = d.getLong(key);
                if (until <= now) continue;
                d.putLong(key, Math.max(now, until - reduce));
            }
        }
        // 精准采集：掉落稀有核心
        if (CullingRules.special(killer, "rare_core") && e.getEntity().level() instanceof ServerLevel level) {
            String core = e.getEntity() instanceof Mob mob && mob.getMaxHealth() >= 400 ? "star_curse_core" : "curse_concentrate";
            var item = ExpeditionContent.item(core);
            if (item != null) e.getEntity().spawnAtLocation(new ItemStack(item));
        }
    }

    // ===== 每刻 =====
    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || !(e.player instanceof ServerPlayer p)) return;
        if (!CullingRules.inDimension(p)) return;
        CompoundTag d = Curse.data(p);
        long now = p.level().getGameTime();

        // 咒力涌泉：额外回复（按最大值的比例）
        double recovery = sum(p, m -> m.recovery) - 1;
        if (recovery > 0 && p.tickCount % 20 == 0) Curse.addRaw(p, Curse.maximum(p) * .01 * recovery);

        // 免疫减速 / 毒抗
        if (CullingRules.special(p, "slow_immune")) p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        if (CullingRules.special(p, "poison_immune")) { p.removeEffect(MobEffects.POISON); p.removeEffect(MobEffects.WITHER); }

        // 绝对隐蔽：脱战 3 秒隐身，直到出手
        if (CullingRules.special(p, "stealth")) {
            long last = d.getLong(COMBAT_AT);
            if (now - last >= 60 && p.tickCount % 40 == 0)
                p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 60, 0, false, false));
        }

        // 飞行权限
        if (CullingRules.special(p, "fly")) {
            if (!p.getAbilities().mayfly) { p.getAbilities().mayfly = true; p.onUpdateAbilities(); }
        }

        // 水上行走：脚下是流体时铺一层临时冰
        if (CullingRules.special(p, "water_walk") && p.tickCount % 4 == 0) {
            BlockPos under = p.blockPosition().below();
            if (p.level() instanceof ServerLevel level && !level.getFluidState(under).isEmpty()) {
                var state = level.getBlockState(under);
                level.setBlockAndUpdate(under, Blocks.FROSTED_ICE.defaultBlockState());
                WATER.add(new Object[]{level, under, state, now + 60});
            }
        }

        // 蜘蛛侠：贴墙且离地时缓慢上移
        if (CullingRules.special(p, "wall_climb") && !p.onGround() && p.horizontalCollision) {
            Vec3 v = p.getDeltaMovement();
            p.setDeltaMovement(v.x, Math.min(.28, v.y + .12), v.z);
            p.fallDistance = 0;
        }

        // 时间停滞：附近怪物变慢
        if (CullingRules.special(p, "mob_slow") && p.tickCount % 20 == 0)
            for (Mob mob : p.serverLevel().getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(16), Mob::isAlive))
                mob.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, false));

        // 护甲永固：把耐久按基线锁住
        if (CullingRules.special(p, "armor_unbreak")) {
            for (var slot : new net.minecraft.world.entity.EquipmentSlot[]{
                net.minecraft.world.entity.EquipmentSlot.HEAD, net.minecraft.world.entity.EquipmentSlot.CHEST,
                net.minecraft.world.entity.EquipmentSlot.LEGS, net.minecraft.world.entity.EquipmentSlot.FEET}) {
                ItemStack stack = p.getItemBySlot(slot);
                if (stack.isEmpty() || !stack.isDamageableItem()) continue;
                String key = "CullingArmorDmg_" + slot.getName();
                if (!d.contains(key)) d.putInt(key, stack.getDamageValue());
                else if (stack.getDamageValue() > d.getInt(key)) stack.setDamageValue(d.getInt(key));
            }
        }
        if (p.tickCount % 20 == 0) revertWater();
    }

    private static void revertWater() {
        if (WATER.isEmpty()) return;
        long now = -1;
        for (var it = WATER.iterator(); it.hasNext(); ) {
            Object[] w = it.next();
            ServerLevel level = (ServerLevel) w[0];
            BlockPos pos = (BlockPos) w[1];
            if (now < 0) now = level.getGameTime();
            if (level.getGameTime() < (long) (Long) w[3]) continue;
            it.remove();
            if (level.getBlockState(pos).is(Blocks.FROSTED_ICE)) level.setBlockAndUpdate(pos, (net.minecraft.world.level.block.state.BlockState) w[2]);
        }
    }

    // ===== 击退免疫 =====
    @SubscribeEvent
    public static void knockback(LivingKnockBackEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !CullingRules.inDimension(p)) return;
        if (CullingRules.special(p, "no_knockback")) e.setCanceled(true);
    }

    // ===== 方块：点石成金 / 拾荒者 =====
    @SubscribeEvent
    public static void breakBlock(BlockEvent.BreakEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer p) || !CullingRules.inDimension(p)) return;
        if (!(e.getLevel() instanceof ServerLevel level)) return;
        BlockPos pos = e.getPos();
        var state = level.getBlockState(pos);
        var rnd = p.getRandom();
        if (CullingRules.special(p, "stone_gold") && state.is(net.minecraft.tags.BlockTags.STONE_ORE_REPLACEABLES)
            && rnd.nextInt(10) == 0) {
            var drop = rnd.nextBoolean() ? ExpeditionContent.item("star_iron_fragment") : net.minecraft.world.item.Items.AMETHYST_SHARD;
            if (drop != null) net.minecraft.world.level.block.Block.popResource(level, pos, new ItemStack(drop));
        }
        if (CullingRules.special(p, "ruin_scrap") && rnd.nextInt(4) == 0) {
            var drop = ExpeditionContent.item("residue_medium");
            if (drop != null) net.minecraft.world.level.block.Block.popResource(level, pos, new ItemStack(drop));
        }
    }

    // ===== 死亡不掉落（虚空背包） =====
    @SubscribeEvent
    public static void keepInventory(PlayerEvent.Clone e) {
        if (!e.isWasDeath()) return;
        Player from = e.getOriginal(), to = e.getEntity();
        if (!(from instanceof ServerPlayer oldP) || !(to instanceof ServerPlayer p)) return;
        if (!CullingRules.inDimension(oldP) || !CullingRules.special(oldP, "keep_inventory")) return;
        var from2 = oldP.getInventory();
        for (int i = 0; i < from2.getContainerSize(); i++) p.getInventory().setItem(i, from2.getItem(i).copy());
        p.containerMenu.broadcastChanges();
        tell(p, "虚空背包：装备与物品已保留");
    }

    private CullingPerks() { }
}
