package character.mahito;

import cn.blockforge.generated.nnnnr1201forge2.*;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** 改造人偶：八只上限、有限寿命、按主人敌友判定追击，抱缚后自爆但不破坏地形。 */
public final class MahitoPuppetEntity extends Monster {
    private UUID owner;
    private int age,grip;
    public MahitoPuppetEntity(EntityType<? extends Monster> type,Level level){super(type,level);xpReward=0;}
    public static AttributeSupplier.Builder attributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,36).add(Attributes.ATTACK_DAMAGE,4).add(Attributes.MOVEMENT_SPEED,.3).add(Attributes.FOLLOW_RANGE,24);}
    public UUID ownerId(){return owner;}
    public void setOwner(UUID id){owner=id;}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));goalSelector.addGoal(2,new MeleeAttackGoal(this,1.1,false));}
    @Override public boolean isAlliedTo(Entity e){return e.getUUID().equals(owner)||e instanceof MahitoPuppetEntity p&&owner!=null&&owner.equals(p.owner)||super.isAlliedTo(e);}
    @Override public boolean doHurtTarget(Entity e){return false;}
    @Override public void tick(){
        super.tick();if(!(level() instanceof ServerLevel level))return;
        var master=owner==null?null:level.getEntity(owner);
        if(!(master instanceof LivingEntity living)||!living.isAlive()||++age>1200){discard();return;}
        var caster=NpcBridge.actorFor(living);if(caster==null){discard();return;}
        if(age%10==0){
            LivingEntity target=living instanceof Mob mob?mob.getTarget():null;
            if(target!=null&&CombatSettings.canHit(caster,target))setTarget(target);
            else if(getTarget()!=null&&!CombatSettings.canHit(caster,getTarget()))setTarget(null);
            if(getTarget()==null)getNavigation().moveTo(living,1);
        }
        LivingEntity target=getTarget();if(target==null||!target.isAlive()||!CombatSettings.canHit(caster,target)){grip=0;return;}
        if(distanceToSqr(target)<3&&hasLineOfSight(target)){
            getNavigation().stop();MahitoCombat.root(target,5);grip++;
            if(grip%5==0)level.sendParticles(ParticleTypes.DAMAGE_INDICATOR,getX(),getY()+1,getZ(),3,.4,.5,.4,.01);
        }else grip=Math.max(0,grip-1);
        if(grip>=30||getHealth()<getMaxHealth()*.25f&&distanceToSqr(target)<16){
            for(LivingEntity enemy:level.getEntitiesOfClass(LivingEntity.class,getBoundingBox().inflate(3),e->e!=this&&!isAlliedTo(e)&&CombatSettings.canHit(caster,e)))MahitoCombat.soulDamage(caster,enemy,8);
            Network.send(level,new Network.Effect(MahitoCombat.PUPPET,getId(),position(),net.minecraft.world.phys.Vec3.ZERO,1.5f,18,age));
            level.playSound(null,blockPosition(),net.minecraft.sounds.SoundEvents.SLIME_DEATH,net.minecraft.sounds.SoundSource.HOSTILE,1.5f,.6f);discard();
        }
    }
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);if(owner!=null)tag.putUUID("MahitoMaster",owner);tag.putInt("PuppetAge",age);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);owner=tag.hasUUID("MahitoMaster")?tag.getUUID("MahitoMaster"):null;age=tag.getInt("PuppetAge");}
}
