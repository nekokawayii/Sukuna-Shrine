package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;

/** 五位角色共用寻路和近战，术式统一交给 NpcCombatCasting。 */
public final class SorcererNpcEntity extends PathfinderMob implements character.npc.NpcTechniqueUser {
    private final String role;
    private static final net.minecraft.network.syncher.EntityDataAccessor<Integer> MAHITO_POSE=net.minecraft.network.syncher.SynchedEntityData.defineId(SorcererNpcEntity.class,net.minecraft.network.syncher.EntityDataSerializers.INT);
    private static final net.minecraft.network.syncher.EntityDataAccessor<Long> MAHITO_UNTIL=net.minecraft.network.syncher.SynchedEntityData.defineId(SorcererNpcEntity.class,net.minecraft.network.syncher.EntityDataSerializers.LONG);
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(MAHITO_POSE,0);entityData.define(MAHITO_UNTIL,0L);}
    public void mahitoPose(int action,int ticks){entityData.set(MAHITO_POSE,action);entityData.set(MAHITO_UNTIL,level().getGameTime()+ticks);}
    public int mahitoPose(){return entityData.get(MAHITO_UNTIL)>level().getGameTime()?entityData.get(MAHITO_POSE):0;}
    @Override public boolean doHurtTarget(Entity target){
        if("mahito".equals(role)&&target instanceof LivingEntity living){var actor=NpcBridge.actorFor(this);return actor!=null&&character.mahito.MahitoCombat.touch(actor,living);}
        return super.doHurtTarget(target);
    }
    @Override public boolean isAlliedTo(Entity target){return "mahito".equals(role)&&target instanceof character.mahito.MahitoPuppetEntity puppet&&getUUID().equals(puppet.ownerId())||super.isAlliedTo(target);}
    public SorcererNpcEntity(EntityType<? extends SorcererNpcEntity> type,Level level,String role) {
        super(type,level);this.role=role;setPersistenceRequired();xpReward=150;
        if(role.equals("nanami"))setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));
        if(role.equals("yuta"))setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Items.IRON_SWORD));
        if(role.equals("toji"))setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(Content.YUYUN.get()));
        setDropChance(EquipmentSlot.MAINHAND,0);
    }
    public String characterId(){return role;}
    @Override public character.npc.NpcLoadout npcTechniques(){return character.npc.NpcLoadout.all(role);}
    @Override protected void registerGoals(){
        goalSelector.addGoal(0,new FloatGoal(this));
        goalSelector.addGoal(2,new MeleeAttackGoal(this,1.15,true){
            @Override protected double getAttackReachSqr(LivingEntity target){return "mahito".equals(role)?Math.pow(getBbWidth()+target.getBbWidth()/2+3,2):super.getAttackReachSqr(target);}
        });
        goalSelector.addGoal(7,new WaterAvoidingRandomStrollGoal(this,.8));
        goalSelector.addGoal(8,new LookAtPlayerGoal(this,Player.class,12));
        goalSelector.addGoal(9,new RandomLookAroundGoal(this));
        targetSelector.addGoal(1,new HurtByTargetGoal(this));
        targetSelector.addGoal(2,new NearestAttackableTargetGoal<>(this,LivingEntity.class,10,true,false,
            t -> NpcBridge.canHit(this,t) && ("mahito".equals(role)
                ? t instanceof Player || t instanceof SorcererNpcEntity s&&!"mahito".equals(s.role)
                    ||t instanceof net.minecraft.world.entity.npc.AbstractVillager||t instanceof net.minecraft.world.entity.animal.Animal||t instanceof Monster&&t.getMaxHealth()<=40
                : t instanceof Monster || t instanceof SorcererNpcEntity s&&"mahito".equals(s.role))));
    }
    public static AttributeSupplier.Builder attributes(String role){
        double health=switch(role){case "nanami"->300;case "yuta"->700;case "yuki"->650;case "toji"->600;default->600;};
        return createMobAttributes().add(Attributes.MAX_HEALTH,health)
            .add(Attributes.ATTACK_DAMAGE,role.equals("toji")?14:8)
            .add(Attributes.ATTACK_SPEED,4).add(Attributes.MOVEMENT_SPEED,role.equals("toji")?.36:.29)
            .add(Attributes.FOLLOW_RANGE,40).add(Attributes.ARMOR,6).add(Attributes.KNOCKBACK_RESISTANCE,.25);
    }
}
