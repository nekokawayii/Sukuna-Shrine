package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** 胀相：中立角色（不主动攻击，被打才还手），会使用赤血操术的「穿血」。 */
public class ChosoEntity extends PathfinderMob {
    private int shotAt;
    public ChosoEntity(EntityType<? extends ChosoEntity> type,Level level){super(type,level);setPersistenceRequired();xpReward=300;}

    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(2,new MeleeAttackGoal(this,1.15,true));
        goalSelector.addGoal(7,new WaterAvoidingRandomStrollGoal(this,0.8));
        goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,8f));
        goalSelector.addGoal(9,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes(){
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH,200.0)
            .add(Attributes.ATTACK_DAMAGE,5.0)
            .add(Attributes.MOVEMENT_SPEED,0.30)
            .add(Attributes.FOLLOW_RANGE,32.0)
            .add(Attributes.ARMOR,4.0);
    }

    /** 赤血操术技能表：胀相按顺序循环施放（名字与原作招式对应，效果为简化版）。 */
    private static final String[] SKILLS={"苅祓","穿血","血星磊","千本细针","血棘","超新星","血刃","赤鳞跃动"};
    private int skillIndex;

    @Override public void tick(){
        super.tick();
        if(level().isClientSide)return;
        LivingEntity target=getTarget();
        if(target==null||!target.isAlive()){skillIndex=0;return;}
        long now=level().getGameTime();
        if(now<shotAt)return;
        if(distanceToSqr(target)>32*32)return;
        shotAt=(int)now+70;
        cast(skillIndex++%SKILLS.length,target);
    }

    /** 一次赤血操术；按 index 轮换，确保"所有技能"都会用到。 */
    private void cast(int index,LivingEntity target){
        var world=(ServerLevel)level();
        Vec3 eye=getEyePosition(),to=target.getBoundingBox().getCenter(),dir=to.subtract(eye).normalize();
        switch(index){
            case 0 -> { // 苅祓：环形刃
                Network.send(world,new Network.Effect(412,getId(),position(),Vec3.ZERO,6,14,0));
                for(var t:near(6))damage(t,12f);
            }
            case 1 -> { // 穿血：直线
                Network.send(world,new Network.Effect(412,getId(),eye,dir,24,12,0));
                for(var t:near(24))damage(t,15f);
            }
            case 2 -> { // 血星磊：三颗血星坠落
                for(int i=0;i<3;i++){
                    Vec3 at=to.add((getRandom().nextDouble()-.5)*6,0,(getRandom().nextDouble()-.5)*6);
                    Network.send(world,new Network.Effect(412,getId(),at.add(0,8,0),new Vec3(0,-1,0),3,20,0));
                    for(var t:near(at,4))damage(t,14f);
                }
            }
            case 3 -> { // 千本细针：连打
                for(int i=0;i<5;i++){
                    Vec3 at=to.add((getRandom().nextDouble()-.5)*3,getRandom().nextDouble()*1.6,(getRandom().nextDouble()-.5)*3);
                    Network.send(world,new Network.Effect(412,getId(),eye,at.subtract(eye).normalize(),10,8,0));
                    hurt(target,4f);
                }
            }
            case 4 -> { // 血棘：扇形地面尖刺
                Vec3 flat=new Vec3(dir.x,0,dir.z).normalize();
                for(int lane=-2;lane<=2;lane++){
                    Vec3 ray=flat.yRot((float)(lane*Math.PI/12));
                    for(int i=1;i<=4;i++){
                        Vec3 at=position().add(ray.scale(i*2));
                        Network.send(world,new Network.Effect(412,getId(),at,Vec3.ZERO,1,10,0));
                        for(var t:near(at,2.4)){damage(t,8f);t.setDeltaMovement(t.getDeltaMovement().add(0,0.7,0));t.hurtMarked=true;}
                    }
                }
            }
            case 5 -> { // 超新星：自身周围爆发
                Network.send(world,new Network.Effect(412,getId(),position(),Vec3.ZERO,8,20,0));
                for(var t:near(8))damage(t,20f);
            }
            case 6 -> { // 血刃：近身斩击
                Network.send(world,new Network.Effect(412,getId(),to,dir,4,10,0));
                if(distanceToSqr(target)<=16)damage(target,18f);
            }
            default -> { // 赤鳞跃动：加速突进 + 一次撞击
                addEffect(new net.minecraft.world.effect.MobEffectInstance(
                    net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED,60,1,false,false));
                Network.send(world,new Network.Effect(412,getId(),position(),dir,6,10,0));
                getNavigation().moveTo(target,1.6);
                if(distanceToSqr(target)<=25)damage(target,10f);
            }
        }
    }

    private java.util.List<LivingEntity> near(double radius){return near(position(),radius);}
    private java.util.List<LivingEntity> near(Vec3 at,double radius){
        return level().getEntitiesOfClass(LivingEntity.class,new net.minecraft.world.phys.AABB(at,at).inflate(radius),
            e->e!=this&&e.isAlive()&&!(e instanceof ChosoEntity)&&!(e instanceof net.minecraft.world.entity.player.Player pl&&pl.isCreative()));
    }
    private void damage(LivingEntity target,float amount){hurt(target,amount);}
    private void hurt(LivingEntity target,float amount){
        AttackTypes.mark(AttackTypes.MAGIC);
        try{target.hurt(damageSources().indirectMagic(this,this),amount);}
        finally{AttackTypes.clear();}
    }
}
