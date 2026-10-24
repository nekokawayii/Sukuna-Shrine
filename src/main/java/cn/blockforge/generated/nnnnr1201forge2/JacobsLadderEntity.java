package cn.blockforge.generated.nnnnr1201forge2;

import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.*;
import net.minecraft.resources.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.network.NetworkHooks;

public final class JacobsLadderEntity extends Entity {
    public static final ResourceKey<DamageType> HOLY=ResourceKey.create(Registries.DAMAGE_TYPE,new ResourceLocation("sukuna","holy_light"));
    private static final EntityDataAccessor<Integer> AGE=SynchedEntityData.defineId(JacobsLadderEntity.class,EntityDataSerializers.INT);
    private UUID owner;
    public JacobsLadderEntity(EntityType<? extends JacobsLadderEntity> type,Level level){super(type,level);setNoGravity(true);noPhysics=true;noCulling=true;}
    public void configure(UUID id){owner=id;}
    public UUID ownerId(){return owner;}
    public int age(){return entityData.get(AGE);}
    public AABB area(){return new AABB(getX()-5,getY()-1,getZ()-5,getX()+5,getY()+PrisonRules.LIGHT_HEIGHT,getZ()+5);}
    private boolean inside(Entity e){double x=e.getX()-getX(),z=e.getZ()-getZ();return x*x+z*z<=25;}
    @Override protected void defineSynchedData(){entityData.define(AGE,0);}
    @Override public void tick(){
        super.tick();if(!(level() instanceof ServerLevel l))return;
        int age=age()+1;entityData.set(AGE,age);
        if(PrisonRules.lightPulse(age)){
            Entity caster=owner==null?null:l.getEntity(owner);DamageSource source=new DamageSource(l.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(HOLY),this,caster);
            float amount=BalanceRules.modpackMode()?80:4;
            var player=CombatSafety.owner(caster);
            if(player!=null)for(LivingEntity target:l.getEntitiesOfClass(LivingEntity.class,area(),e->inside(e)&&CombatSafety.canAffect(player,e)))CombatSafety.hurt(player,target,source,amount);
            l.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,getX(),getY()+4,getZ(),30,3,4,3,.025);
        }
        if(age>=PrisonRules.LIGHT_TICKS){
            for(PrisonRealmEntity realm:l.getEntitiesOfClass(PrisonRealmEntity.class,area(),this::inside)){PrisonSystem.release(realm);realm.discard();}
            discard();
        }
    }
    @Override protected void addAdditionalSaveData(CompoundTag t){if(owner!=null)t.putUUID("Owner",owner);t.putInt("Age",age());}
    @Override protected void readAdditionalSaveData(CompoundTag t){owner=t.hasUUID("Owner")?t.getUUID("Owner"):null;entityData.set(AGE,t.getInt("Age"));}
    @Override public Packet<ClientGamePacketListener> getAddEntityPacket(){return NetworkHooks.getEntitySpawningPacket(this);}
}
