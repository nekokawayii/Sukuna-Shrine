package character;

import cn.blockforge.generated.nnnnr1201forge2.*;
import java.util.Comparator;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import org.joml.Vector3f;

/** 自定义即时技能的唯一服务端入口：角色资格、章节、控制、消耗、冷却、敌友和挡墙在此统一验证。 */
public final class CharacterCasting {
    /** 高级扩展：角色专属效果仍经过本类的资格、扣费与冷却检查。目标与伤害必须继续用统一敌友接口。 */
    @FunctionalInterface public interface Effect { void apply(ServerPlayer player,CharacterSkill skill,int rank); }
    /** 角色可以改自己的冷却倍率（例：漏壶在盖棺铁围山里减半）。 */
    @FunctionalInterface public interface CooldownScale { double scale(ServerPlayer player); }
    private static final java.util.Map<String,CooldownScale> COOLDOWN_SCALES=new java.util.HashMap<>();
    public static void registerCooldownScale(String character,CooldownScale scale) { if(scale!=null)COOLDOWN_SCALES.put(character,scale); }

    /** 允许在「无量空处」压制下仍然能施放的角色技能（漏壶的盖棺铁围山要在对方领域里展开打对拼）。 */
    private static final java.util.Set<String> MIND_LOCK_CASTABLE=new java.util.HashSet<>();
    public static void allowWhileMindLocked(String character,int skill) { MIND_LOCK_CASTABLE.add(character+"#"+skill); }
    private static boolean mindLockAllowed(CharacterDefinition d,int skill) {
        return d!=null&&MIND_LOCK_CASTABLE.contains(d.id()+"#"+skill);
    }
    /**
     * NPC「借用玩家技能」的统一入口：把生物包装成施术者，交给注册的 provider 走**同一套技能分派**。
     * 玩家仍然走 {@link #cast(ServerPlayer,int)}；生物走 provider（内部按角色 + 技能号查同一份数值表）。
     */
    @FunctionalInterface public interface NpcProvider { boolean cast(net.minecraft.world.entity.LivingEntity caster,int skill); }
    private static final java.util.Map<Class<?>,NpcProvider> NPC_PROVIDERS=new java.util.HashMap<>();
    public static void registerNpcProvider(Class<? extends net.minecraft.world.entity.LivingEntity> type,NpcProvider provider){
        if(provider!=null)NPC_PROVIDERS.put(type,provider);
    }
    /** 把任意实体包装成施术者（玩家/生物通用）。 */
    public static SkillCaster caster(net.minecraft.world.entity.LivingEntity entity){return SkillCaster.of(entity);}

    /** 生物的施法入口：找不到对应 provider 时返回 false（不会悄悄扣费）。 */
    public static boolean castFor(net.minecraft.world.entity.LivingEntity caster,int skill){
        if(caster instanceof ServerPlayer p){cast(p,skill);return true;}
        for(var e:NPC_PROVIDERS.entrySet())if(e.getKey().isInstance(caster))return e.getValue().cast(caster,skill);
        return false;
    }

    /** 以指定角色身份施放其自定义技能（乙骨复制来的漏壶术式走这里）。 */
    public static boolean castAs(ServerPlayer p,String character,int id,int rank){
        var def=CharacterRegistry.get(character);
        if(def==null)return false;
        CharacterSkill skill=def.config().skill(id);
        var effect=EFFECTS.get(new Key(character,id));
        if(skill==null||effect==null)return false;
        effect.apply(p,skill,rank);
        return true;
    }

    private record Key(String character,int skill) {}
    private static final java.util.Map<Key,Effect> EFFECTS=new java.util.HashMap<>();
    public static void registerEffect(String character,int skill,Effect effect) {
        var d=CharacterRegistry.get(character);
        if(d==null||d.config().skill(skill)==null||effect==null)throw new IllegalArgumentException("请先登记角色及自定义技能，再登记专属效果");
        if(EFFECTS.putIfAbsent(new Key(character,skill),effect)!=null)throw new IllegalArgumentException("重复的角色技能效果");
    }
    public static void cast(ServerPlayer p,int id) {
        var definition=CharacterService.selected(p);
        if(definition==null)return;
        if(!CharacterService.allowsSkill(p,id)){
            p.sendSystemMessage(Component.literal("无法施放：技能"+id+" 未解锁（"+definition.name()+" 阶段"+CharacterService.rank(p)+"，需求"+definition.config().unlockChapter(id)+"，已注册"+(definition.config().skill(id)==null?"否":"是")+"）"));
            return;
        }
        // 乙骨忧太：释放掠夺来的旧术式。必须先召唤里香；不限次数，但冷却为原术式的两倍。
        if(definition.config().skill(id)==null&&character.yuta.YutaCopies.has(p,id)){
            if(!RikaRules.active(p)){p.displayClientMessage(Component.literal("未召唤里香，无法使用复制术式"),true);return;}
            String copyKey="YutaCopyCooldown_"+id;
            if(Curse.active(p,copyKey)){p.displayClientMessage(Component.literal(character.yuta.YutaCopies.nameOf(id)+" 冷却中"),true);return;}
            // AuthoritySkills 的分派里五条只认 100–108，而掠夺来的是 34–40（苍/赫/…），
            // 直接传会在里面静默掉，所以先按 WHEEL 换算成 100+ 的内部偏移编号。
            int target=id;
            if(id>=34&&id<34+cn.blockforge.generated.nnnnr1201forge2.GojoPlayer.SLOTS)
                target=100+cn.blockforge.generated.nnnnr1201forge2.GojoPlayer.WHEEL[id-34];
            // 借用放行：各家族的 unlocked 会检查「是否开启了该家族」，乙骨永远不满足，
            // 会在 AuthoritySkills.cast 里被拒（例如「尚未睁开六眼」）。转调期间临时放行。
            // 借用施法统一扣咒力（只加在剑上会漏掉「按 R 从轮盘放」这条路）。
            int castCost = 300;
            if (!Curse.spend(p, castCost)) {
                p.displayClientMessage(Component.literal("咒力不足（需要 " + castCost + "）"), true);
                return;
            }
            character.yuta.CopyBorrow.begin(p);
            try {
                if (character.yuta.YutaCopies.isCustom(id)) {
                    // 漏壶这类自定义技能：转调到它自己的实现，否则旧版分派里根本不存在。
                    castAs(p, character.yuta.YutaCopies.ownerOf(id), id, CharacterService.rank(p));
                } else {
                    AuthoritySkills.cast(p,target);
                }
            }
            finally { character.yuta.CopyBorrow.end(p); }
            // 冷却翻倍 + 开 3 秒爆发窗口（倍率 1.5 + 0.5×章节）。
            Curse.status(p,copyKey,character.yuta.YutaCopies.copiedCooldown(id));
            character.yuta.YutaBoost.activate(p,CharacterService.rank(p));
            return;
        }
        var skill=definition.config().skill(id);if(skill==null)return;
        if(!p.isAlive()||p.isSpectator()||p.isPassenger()||p.isSleeping()||p.isUsingItem()||!Curse.canUseTechnique(p)
                ||PrisonSystem.locked(p)||ObserverRitual.active(p)||TenShadows.locked(p)||DeathStar.charging(p)||DeathStar.anemic(p)
                ||Shinbuke.holding(p)||(GojoEntity.mindLocked(p)&&!mindLockAllowed(definition,id))||GuardianPlayerSkills.locked(p)||SoulCombat.suppressed(p)
                ||Skills.sealing(p)||Skills.sustaining(p)||GuardianPlayerSkills.sustaining(p)||FurnaceMax.active(p)
                ||DismantleMotion.active(p)||CombatExpansion.chanting(p))return;
        // 乙骨领域：展开中再次按下 = 主动收起（必须放在冷却判断之前，否则第二次按会被冷却吃掉）。
        if("yuta".equals(definition.id())&&id==character.yuta.YutaDomain.SKILL&&character.yuta.YutaDomain.active(p)){
            character.yuta.YutaDomain.close(p,"主动解除");
            return;
        }
        String cooldown="CharacterSkillCooldown_"+id;
        long nowTick=p.level().getGameTime();
        long remainSkill=Curse.data(p).getLong(cooldown)-nowTick;
        long remainGlobal=Curse.data(p).getLong("CharacterGlobalCooldown")-nowTick;
        if(remainSkill>0||remainGlobal>0){
            // 冷却明示：技能名 + 剩余秒数（聊天栏，避免只闪一下动作栏看不到）
            String text=remainSkill>=remainGlobal
                ? skill.name()+" 冷却中 · "+String.format(java.util.Locale.ROOT,"%.1f",remainSkill/20.0)+" 秒"
                : "公共施法间隔 · "+String.format(java.util.Locale.ROOT,"%.1f",remainGlobal/20.0)+" 秒后再试";
            p.sendSystemMessage(Component.literal(text));
            p.displayClientMessage(Component.literal(text),true);
            return;
        }
        if(!Curse.spend(p,skill.cost()))return;
        double cdScale=COOLDOWN_SCALES.getOrDefault(definition.id(),pl->1.0).scale(p);
        Curse.status(p,cooldown,(int)Math.max(1,Math.round(skill.cooldown()*cdScale)));Curse.status(p,"CharacterGlobalCooldown",4);
        MeleeCombo.cancel(p);
        int rank=CharacterService.rank(p); // 先固定本次阶段，击杀升级不会在同次施法中再增伤。
        var effect=EFFECTS.get(new Key(definition.id(),id));
        if(effect!=null){effect.apply(p,skill,rank);return;}
        Vec3 start=p.getEyePosition(),look=p.getLookAngle(),end=start.add(look.scale(skill.range()));
        var wall=p.level().clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        if(wall.getType()!=HitResult.Type.MISS)end=wall.getLocation();
        final Vec3 rayEnd=end;
        AABB search=skill.shape()==CharacterSkill.Shape.RAY?new AABB(start,end).inflate(skill.radius()+1):p.getBoundingBox().inflate(skill.range());
        var targets=p.level().getEntitiesOfClass(LivingEntity.class,search,t->CombatSettings.canHit(p,t)&&p.hasLineOfSight(t)&&inside(skill,p,start,look,rayEnd,t));
        targets.sort(Comparator.comparingDouble(p::distanceToSqr));
        float damage=CombatSettings.spellDamage(p,skill.damageAt(rank));int hits=0;
        for(var target:targets){
            AttackTypes.mark(AttackTypes.MAGIC);
            if(CombatSettings.hurt(p,target,p.damageSources().indirectMagic(p,p),damage)){
                if(target.isAlive()&&skill.slowTicks()>0&&CombatSettings.canHit(p,target))target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,skill.slowTicks(),skill.slowAmplifier()));
            }
            if(++hits>=skill.targetsAt(rank))break;
        }
        particles(p,skill,start,look,end);
    }
    private static boolean inside(CharacterSkill skill,ServerPlayer p,Vec3 start,Vec3 look,Vec3 end,LivingEntity target) {
        if(skill.shape()==CharacterSkill.Shape.RAY){
            var box=target.getBoundingBox().inflate(skill.radius());return box.contains(start)||box.clip(start,end).isPresent();
        }
        Vec3 delta=target.getBoundingBox().getCenter().subtract(p.getBoundingBox().getCenter());
        if(delta.lengthSqr()>skill.range()*skill.range())return false;
        return skill.shape()==CharacterSkill.Shape.BURST||delta.lengthSqr()<.01||look.dot(delta.normalize())>=Math.cos(Math.toRadians(skill.halfAngle()));
    }
    private static void particles(ServerPlayer p,CharacterSkill skill,Vec3 start,Vec3 look,Vec3 end) {
        int color=skill.color();var dust=new DustParticleOptions(new Vector3f(((color>>16)&255)/255f,((color>>8)&255)/255f,(color&255)/255f),1.2f);
        // 每次最多65个采样点，不注册额外贴图；颜色与技能配置一致。
        if(skill.shape()==CharacterSkill.Shape.RAY){
            int n=Math.max(1,(int)Math.ceil(start.distanceTo(end)*2));
            for(int i=0;i<=n;i++)point(p,dust,start.lerp(end,i/(double)n));
        }else{
            Vec3 center=p.getBoundingBox().getCenter();double half=Math.toRadians(skill.halfAngle());
            Vec3 side=look.cross(new Vec3(0,1,0)).normalize();if(side.lengthSqr()<.01)side=new Vec3(1,0,0);
            for(int i=0;i<48;i++){
                double a=skill.shape()==CharacterSkill.Shape.BURST?i*Math.PI*2/48:-half+2*half*i/47;
                Vec3 direction=skill.shape()==CharacterSkill.Shape.BURST?new Vec3(Math.cos(a),0,Math.sin(a)):look.scale(Math.cos(a)).add(side.scale(Math.sin(a)));
                Vec3 at=center.add(direction.scale(skill.range()));
                var wall=p.level().clip(new ClipContext(center,at,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
                point(p,dust,wall.getType()==HitResult.Type.MISS?at:wall.getLocation());
            }
        }
    }
    private static void point(ServerPlayer p,DustParticleOptions dust,Vec3 at) { p.serverLevel().sendParticles(dust,at.x,at.y,at.z,1,.02,.02,.02,0); }
    private CharacterCasting() {}
}
