package cn.blockforge.generated.nnnnr1201forge2;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

/** 原模型、等级、碰撞尺寸统一由FORM同步，召唤归属单独存档。 */
public final class LowCurseEntity extends Monster {
    public static final int EMERGING=2,ATTACK=1,IDLE=0;
    private static final EntityDataAccessor<Integer> ACTION=SynchedEntityData.defineId(LowCurseEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> AGE=SynchedEntityData.defineId(LowCurseEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> FORM=SynchedEntityData.defineId(LowCurseEntity.class,EntityDataSerializers.INT);
    private UUID summoner;
    private boolean modelReady;
    public LowCurseEntity(EntityType<? extends Monster> type,Level level){
        super(type,level);modelReady=true;
        var key=ForgeRegistries.ENTITY_TYPES.getKey(type);String id=key==null?"":key.getPath();
        configure(id.startsWith("curse_")?CurseCatalog.index(id.substring(6)):CurseCatalog.choose(random,true));
    }
    public static AttributeSupplier.Builder createAttributes(){return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH,60).add(Attributes.ATTACK_DAMAGE,6).add(Attributes.MOVEMENT_SPEED,.27).add(Attributes.FOLLOW_RANGE,40).add(Attributes.ARMOR,2);}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(ACTION,0);entityData.define(AGE,0);entityData.define(FORM,0);}
    public int formIndex(){return entityData.get(FORM);}
    public CurseCatalog.Form form(){return CurseCatalog.form(formIndex());}
    public void configure(int index){
        entityData.set(FORM,Math.floorMod(index,CurseCatalog.FORMS.size()));var f=form();
        if(!level().isClientSide){getAttribute(Attributes.MAX_HEALTH).setBaseValue(f.health());getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(f.damage());setHealth(getMaxHealth());xpReward=5+(4-f.grade())*8;}
        refreshDimensions();
    }
    @Override public EntityDimensions getDimensions(Pose pose){
        // 父构造器定义同步字段之前会查询一次尺寸。
        if(!modelReady)return super.getDimensions(pose);
        var f=form();return EntityDimensions.scalable(f.width(),f.height());
    }
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){super.onSyncedDataUpdated(key);if(FORM.equals(key))refreshDimensions();}
    @Override protected Component getTypeName(){return Component.literal(form().gradeName()+"咒灵 · "+form().name());}
    @Override public boolean isAlliedTo(Entity target){return target instanceof GetoEntity||summoner!=null&&(summoner.equals(target.getUUID())||target instanceof LowCurseEntity c&&summoner.equals(c.summoner))||super.isAlliedTo(target);}
    public UUID summoner(){return summoner;}
    public void summonedBy(Mob owner){summoner=owner.getUUID();getPersistentData().putLong("CurseSummonExpires",level().getGameTime()+2400);}
    public boolean mayAttack(LivingEntity target){
        if(target==this||!target.isAlive()||target.isSpectator()||isAlliedTo(target)||target instanceof Player p&&p.isCreative())return false;
        if(summoner!=null&&(summoner.equals(target.getUUID())||target instanceof LowCurseEntity c&&summoner.equals(c.summoner)))return false;
        return target instanceof Player||CurseHostility.hunter(target);
    }
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(2,new MeleeAttackGoal(this,1.15,false){@Override public boolean canUse(){return !emerging()&&super.canUse();}@Override public boolean canContinueToUse(){return !emerging()&&super.canContinueToUse();}});
        goalSelector.addGoal(6,new WaterAvoidingRandomStrollGoal(this,.8));goalSelector.addGoal(7,new LookAtPlayerGoal(this,Player.class,24));goalSelector.addGoal(8,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,LivingEntity.class,10,true,false,this::mayAttack));
    }
    public int action(){return entityData.get(ACTION);}public int actionAge(){return entityData.get(AGE);}public boolean emerging(){return action()==EMERGING;}
    public void emergeFrom(Vec3 from){entityData.set(ACTION,EMERGING);entityData.set(AGE,0);}
    public void beginAttack(){entityData.set(ACTION,ATTACK);entityData.set(AGE,0);}
    @Override public void aiStep(){
        super.aiStep();if(level().isClientSide)return;
        long expires=getPersistentData().getLong("CurseSummonExpires");if(expires>0&&level().getGameTime()>=expires){discard();return;}
        if(getTarget()!=null&&!mayAttack(getTarget()))setTarget(null);
        if(action()!=IDLE){int age=actionAge()+1;entityData.set(AGE,age);if(emerging())getNavigation().stop();if(age>=(emerging()?20:18)){entityData.set(ACTION,IDLE);entityData.set(AGE,0);}}
    }
    @Override public boolean doHurtTarget(Entity target){if(target instanceof LivingEntity living&&!mayAttack(living))return false;boolean hit=super.doHurtTarget(target);if(hit)beginAttack();return hit;}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putInt("CurseAction",action());tag.putInt("CurseAge",actionAge());tag.putString("CurseForm",form().id());if(summoner!=null)tag.putUUID("CurseSummoner",summoner);}
    @Override public void readAdditionalSaveData(CompoundTag tag){
        super.readAdditionalSaveData(tag);entityData.set(ACTION,tag.getInt("CurseAction"));entityData.set(AGE,tag.getInt("CurseAge"));
        if(tag.contains("CurseForm")){entityData.set(FORM,CurseCatalog.index(tag.getString("CurseForm")));refreshDimensions();}
        else{float hp=getHealth();configure(formIndex());setHealth(Math.min(hp,getMaxHealth()));}
        summoner=tag.hasUUID("CurseSummoner")?tag.getUUID("CurseSummoner"):null;
    }
}
