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

    @Override public void tick(){
        super.tick();
        if(level().isClientSide)return;
        LivingEntity target=getTarget();
        if(target==null||!target.isAlive())return;
        long now=level().getGameTime();
        if(now<shotAt)return;
        if(distanceToSqr(target)>24*24)return;
        shotAt=(int)now+60;   // 每 3 秒一次「穿血」
        Vec3 from=getEyePosition(),to=target.getBoundingBox().getCenter();
        Network.send((ServerLevel)level(),new Network.Effect(412,getId(),from,to.subtract(from).normalize(),3,12,0));
        AttackTypes.mark(AttackTypes.MAGIC);
        try{target.hurt(damageSources().indirectMagic(this,this),8f);}
        finally{AttackTypes.clear();}
    }
}
