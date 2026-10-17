package cn.blockforge.generated.nnnnr1201forge2;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 真人发射的可斩断肢体：0.8秒红线预警后，以中等速度追踪目标。 */
public final class MahitoLimbEntity extends Monster {
    private static final EntityDataAccessor<Integer> WARNING=SynchedEntityData.defineId(MahitoLimbEntity.class,EntityDataSerializers.INT);
    private UUID owner,target;
    private int age;
    private float castBonus;
    private Vec3 flight=Vec3.ZERO;
    public MahitoLimbEntity(EntityType<? extends Monster> type,Level level){super(type,level);noPhysics=true;setNoGravity(true);xpReward=0;}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(WARNING,16);}
    @Override protected void registerGoals(){}
    public void setup(BrokenMahitoEntity owner,LivingEntity target){this.owner=owner.getUUID();this.target=target.getUUID();castBonus=owner.castBonus();setHealth(getMaxHealth());}
    private ServerLevel server(){return (ServerLevel)level();}
    private BrokenMahitoEntity owner(){return owner!=null&&server().getEntity(owner) instanceof BrokenMahitoEntity b?b:null;}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override public boolean isPushable(){return false;}
    @Override public boolean causeFallDamage(float distance,float multiplier,DamageSource source){return false;}
    @Override public void tick(){
        super.tick();if(level().isClientSide)return;
        BrokenMahitoEntity boss=owner();Entity entity=target==null?null:server().getEntity(target);
        if(boss==null||!boss.isAlive()||!(entity instanceof LivingEntity player)||!BossTargets.valid(boss,player)||++age>100){discard();return;}
        Vec3 desired=player.getEyePosition().subtract(getEyePosition()).normalize();
        if(age<=16){setDeltaMovement(Vec3.ZERO);flight=desired;Network.send(server(),new Network.Effect(134,getId(),position(),position().add(flight.scale(32)),0.07f,3,age));return;}
        if(flight.lengthSqr()<0.01)flight=desired;
        Vec3 start=position(),end=start.add(flight.scale(0.72));
        var wall=level().clip(new net.minecraft.world.level.ClipContext(start,end,net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,this));
        if(wall.getType()!=net.minecraft.world.phys.HitResult.Type.MISS){discard();return;}
        setDeltaMovement(Vec3.ZERO);setPos(end);Network.send(server(),new Network.Effect(132,getId(),position(),flight,0.55f,3,age));
        if(player.getBoundingBox().inflate(0.3).clip(start,end).isPresent()){player.hurt(damageSources().indirectMagic(this,boss),BalanceRules.damage(40+castBonus,BalanceRules.Category.BOSS_ATTACK));Network.send(server(),new Network.Effect(133,getId(),player.getBoundingBox().getCenter(),flight,1.2f,12,player.getId()));discard();}
    }
    @Override public boolean hurt(DamageSource source,float amount){
        if(level().isClientSide)return false;
        boolean slash=CombatSettings.caster(source)!=null&&(source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)||source.is(net.minecraft.world.damagesource.DamageTypes.INDIRECT_MAGIC)||source.is(net.minecraft.world.damagesource.DamageTypes.SONIC_BOOM));
        if(!slash)return false;
        boolean hit=super.hurt(source,Math.max(amount,getMaxHealth()));
        if(hit&&!isAlive()){BrokenMahitoEntity boss=owner();if(boss!=null)boss.limbSevered();}
        return hit;
    }
    @Override public void die(DamageSource source){BrokenMahitoEntity boss=owner();if(boss!=null)boss.limbSevered();super.die(source);}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);if(owner!=null)tag.putUUID("Owner",owner);if(target!=null)tag.putUUID("Target",target);tag.putInt("Age",age);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);if(tag.hasUUID("Owner"))owner=tag.getUUID("Owner");if(tag.hasUUID("Target"))target=tag.getUUID("Target");age=tag.getInt("Age");}
    public static net.minecraft.world.entity.ai.attributes.AttributeSupplier createAttributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,1).add(Attributes.MOVEMENT_SPEED,0).add(Attributes.FOLLOW_RANGE,64).build();}
}
