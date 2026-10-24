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
    private record Key(String character,int skill) {}
    private static final java.util.Map<Key,Effect> EFFECTS=new java.util.HashMap<>();
    public static void registerEffect(String character,int skill,Effect effect) {
        var d=CharacterRegistry.get(character);
        if(d==null||d.config().skill(skill)==null||effect==null)throw new IllegalArgumentException("请先登记角色及自定义技能，再登记专属效果");
        if(EFFECTS.putIfAbsent(new Key(character,skill),effect)!=null)throw new IllegalArgumentException("重复的角色技能效果");
    }
    public static void cast(ServerPlayer p,int id) {
        var definition=CharacterService.selected(p);
        if(definition==null||!CharacterService.allowsSkill(p,id))return;
        var skill=definition.config().skill(id);if(skill==null)return;
        if(!p.isAlive()||p.isSpectator()||p.isPassenger()||p.isSleeping()||p.isUsingItem()||!Curse.canUseTechnique(p)
                ||PrisonSystem.locked(p)||ObserverRitual.active(p)||TenShadows.locked(p)||DeathStar.charging(p)||DeathStar.anemic(p)
                ||Shinbuke.holding(p)||GojoEntity.mindLocked(p)||GuardianPlayerSkills.locked(p)||SoulCombat.suppressed(p)
                ||Skills.sealing(p)||Skills.sustaining(p)||GuardianPlayerSkills.sustaining(p)||FurnaceMax.active(p)
                ||DismantleMotion.active(p)||CombatExpansion.chanting(p))return;
        String cooldown="CharacterSkillCooldown_"+id;
        if(Curse.active(p,cooldown)||Curse.active(p,"CharacterGlobalCooldown")){
            p.displayClientMessage(Component.literal("技能冷却中，请稍后再试"),true);return;
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
