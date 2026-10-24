package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** 降灵后的中立伏黑惠；战斗、残血吟唱、倒地受肉均使用同一实体并持久化。 */
public final class MegumiEntity extends PathfinderMob {
    private static final EntityDataAccessor<Integer> PHASE=SynchedEntityData.defineId(MegumiEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CHANT=SynchedEntityData.defineId(MegumiEntity.class,EntityDataSerializers.INT);
    private final ServerBossEvent bar=new ServerBossEvent(Component.literal("伏黑惠"),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);
    private UUID challenger,mahoraga;
    private int meleeCooldown,summonCooldown,sequence,executionTicks;
    private boolean legacy,guardsLoaded;
    // 令牌随伏黑惠保存；卸载的式神仍占名额，换下来的旧实体重载后自动消散。
    private final CompoundTag guards=new CompoundTag();
    private void pruneGuards(){for(String key:new ArrayList<>(guards.getAllKeys()))if(guards.getCompound(key).getLong("End")<=level().getGameTime())guards.remove(key);}
    private int guardMask(){int mask=0;for(String key:guards.getAllKeys()){int k=guards.getCompound(key).getInt("Kind");if(TenShadowRules.valid(k))mask|=1<<k;}return mask;}
    private void migrateGuards(){
        if(guardsLoaded||!(level() instanceof ServerLevel l))return;guardsLoaded=true;
        for(Entity entity:l.getAllEntities())if(entity instanceof ShadowEntity s&&getUUID().equals(s.ownerId())&&s.isAlive()&&s.action()!=ShadowEntity.DISSOLVE){
            if(TenShadowRules.megumiCanAdd(guardMask(),s.kind()))enrollGuard(s);else s.dissolve();
        }
    }
    private void enrollGuard(ShadowEntity s){CompoundTag row=new CompoundTag();row.putInt("Kind",s.kind());row.putLong("End",s.expires());row.putLong("Start",level().getGameTime());guards.put(s.getUUID().toString(),row);}
    public boolean guardAllowed(ShadowEntity s){migrateGuards();pruneGuards();return guards.contains(s.getUUID().toString());}
    public void forgetGuard(ShadowEntity s){guards.remove(s.getUUID().toString());}
    private void recallKind(int kind){
        for(String key:new ArrayList<>(guards.getAllKeys()))if(guards.getCompound(key).getInt("Kind")==kind)guards.remove(key);
        if(level() instanceof ServerLevel l)for(Entity entity:l.getAllEntities())if(entity instanceof ShadowEntity s&&getUUID().equals(s.ownerId())&&s.kind()==kind)s.dissolve();
    }
    public MegumiEntity(EntityType<? extends PathfinderMob> type,Level level){super(type,level);setPersistenceRequired();}
    @Override protected void registerGoals(){goalSelector.addGoal(0,new FloatGoal(this));}
    @Override protected void defineSynchedData(){super.defineSynchedData();entityData.define(PHASE,0);entityData.define(CHANT,0);}
    public int phase(){return entityData.get(PHASE);}
    public boolean downed(){return phase()==3;}
    public boolean chanting(){return phase()==2||phase()==4;}
    public int chantTicks(){return entityData.get(CHANT);}
    public void configure(ServerPlayer summoner){challenger=summoner.getUUID();getAttribute(Attributes.MAX_HEALTH).setBaseValue(TenShadowRules.megumiHealth(BalanceRules.modpackMode()));getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(BalanceRules.modpackMode()?240:12);setHealth(getMaxHealth());setCustomName(Component.literal("伏黑惠"));setNoAi(false);setInvulnerable(false);}
    public ServerPlayer challenger(){return challenger==null||!(level() instanceof ServerLevel l)?null:l.getServer().getPlayerList().getPlayer(challenger);}
    public boolean enemy(LivingEntity target){ServerPlayer p=challenger();return target!=null&&target.isAlive()&&!downed()&&(target==p||target instanceof ShadowEntity s&&p!=null&&s.ownedBy(p)||target.getLastHurtMob()==this);}
    private void start(ServerPlayer p){if(phase()!=0)return;challenger=p.getUUID();entityData.set(PHASE,1);setTarget(p);summonCooldown=20;p.sendSystemMessage(Component.literal("伏黑惠应战 · 生命降到5%时恢复至10%，随后吟唱召唤魔虚罗！"));}
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){
        if(hand!=InteractionHand.MAIN_HAND)return InteractionResult.PASS;
        if(player instanceof ServerPlayer p){
            if(downed()){
                if(challenger!=null&&!challenger.equals(p.getUUID())){p.displayClientMessage(Component.literal("这具肉身属于本次挑战者"),true);return InteractionResult.CONSUME;}
                if(!TenShadows.incarnated(p)){TenShadows.incarnate(p);if(TenShadows.incarnated(p))discard();}else p.displayClientMessage(Component.literal("你已经完成伏黑惠受肉"),true);
            }else if(phase()==0)start(p);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }
    @Override public boolean hurt(DamageSource source,float amount){
        if(downed())return false;
        if(source.getEntity() instanceof ShadowEntity s&&getUUID().equals(s.ownerId()))return false;
        ServerPlayer p=CombatSettings.caster(source);if(p==null&&source.getEntity() instanceof ShadowEntity s&&s.owner() instanceof ServerPlayer owner)p=owner;
        if(phase()==0&&p!=null)start(p);
        boolean hit=super.hurt(source,amount);
        if(!level().isClientSide&&TenShadowRules.megumiChant(phase(),getHealth(),getMaxHealth()))beginChant();
        return hit;
    }
    private void beginChant(){
        if(phase()>=2)return;
        setHealth(TenShadowRules.megumiRecovery(getMaxHealth()));entityData.set(PHASE,2);entityData.set(CHANT,TenShadows.CHANT_TICKS);setTarget(null);navigation.stop();invulnerableTime=0;hurtTime=0;
        ServerPlayer p=challenger();if(p!=null){SkillSounds.play(p,"ten_shadows_chant");p.sendSystemMessage(Component.literal("伏黑惠生命降至5% · 恢复至10%，魔虚罗吟唱开始，约9秒后完成！"));}
    }
    @Override public void die(DamageSource source){
        if(level().isClientSide)return;
        if(source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)){super.die(source);return;}
        if(TenShadowRules.lethalMegumiPhase(phase())==2)beginChant();else down();
    }
    private void down(){setHealth(1);entityData.set(PHASE,3);entityData.set(CHANT,0);setNoAi(true);setInvulnerable(true);setTarget(null);navigation.stop();setDeltaMovement(Vec3.ZERO);bar.removeAllPlayers();clearSummons();ServerPlayer p=challenger();if(p!=null)p.sendSystemMessage(Component.literal("伏黑惠已经倒地 · 空手右键肉身完成受肉（需要先吃下宿傩记忆）"));}
    private void clearSummons(){guardsLoaded=true;for(String key:new ArrayList<>(guards.getAllKeys()))guards.remove(key);if(level() instanceof ServerLevel l)for(Entity e:l.getAllEntities())if(e instanceof ShadowEntity s&&getUUID().equals(s.ownerId()))s.dissolve();}
    @Override public void tick(){
        super.tick();if(level().isClientSide)return;
        if(legacy){discard();return;}
        if(downed()){setDeltaMovement(Vec3.ZERO);return;}
        if(phase()==0)return;
        if(TenShadowRules.megumiChant(phase(),getHealth(),getMaxHealth()))beginChant();
        ServerPlayer player=challenger();
        if(tickCount%20==0){
            for(ServerPlayer listener:((ServerLevel)level()).players())if(listener.distanceToSqr(this)<80*80)bar.addPlayer(listener);else bar.removePlayer(listener);
            bar.setName(Component.literal("伏黑惠 · "+(int)getHealth()+" / "+(int)getMaxHealth()+(chanting()?" · 召唤魔虚罗 "+((chantTicks()+19)/20)+"秒":"")));
        }
        bar.setProgress(Math.max(0,Math.min(1,getHealth()/getMaxHealth())));
        if(chanting()){
            navigation.stop();setDeltaMovement(Vec3.ZERO);
            if(phase()==2){int left=TenShadowRules.chantAfter(chantTicks());entityData.set(CHANT,left);if(left==0)summonMahoraga(player);}
            else if(++executionTicks>=24){
                Entity entity=mahoraga==null?null:((ServerLevel)level()).getEntity(mahoraga);
                if(entity instanceof MahoragaEntity m){m.swing(InteractionHand.MAIN_HAND);ShadowSystem.fx((ServerLevel)level(),1,m.getEyePosition(),getBoundingBox().getCenter(),.2f,15);down();if(player!=null)m.setTarget(player);}else{entityData.set(PHASE,2);entityData.set(CHANT,20);}
            }return;
        }
        if(player==null||!player.isAlive()||player.level()!=level()){setTarget(null);navigation.stop();return;}
        setTarget(player);getLookControl().setLookAt(player,45,45);
        if(meleeCooldown>0)meleeCooldown--;if(summonCooldown>0)summonCooldown--;
        if(distanceToSqr(player)<6.25&&hasLineOfSight(player)&&meleeCooldown<=0){swing(InteractionHand.MAIN_HAND);doHurtTarget(player);meleeCooldown=22;}
        else if(tickCount%10==0)navigation.moveTo(player,1.1);
        if(summonCooldown<=0){summonGuard();summonCooldown=guardMask()==0?40:Integer.bitCount(guardMask())<2?80:240;}
    }
    private void summonGuard(){
        migrateGuards();pruneGuards();int mask=guardMask();
        int[] order={0,1,3,4,5,6,7,9,10,8};int kind;
        do{kind=order[Math.floorMod(sequence++,order.length)];}while((mask&(1<<kind))!=0);
        if(!TenShadowRules.megumiCanAdd(mask,kind)){
            String oldest=guards.getAllKeys().stream().min(Comparator.comparingLong(key->guards.getCompound(key).getLong("Start"))).orElse(null);
            if(oldest!=null)recallKind(guards.getCompound(oldest).getInt("Kind"));
        }
        if(!TenShadowRules.megumiCanAdd(guardMask(),kind))return;
        int n=kind==TenShadowRules.RABBIT?5:1;
        for(int i=0;i<n;i++){ShadowEntity e=ShadowContent.SHIKIGAMI.get().create(level());if(e==null)return;e.configure(this,kind,false);Vec3 at=ShadowSystem.findSpace((ServerLevel)level(),e,position(),i);if(at!=null){e.moveTo(at.x,at.y,at.z,getYRot(),0);if(level().addFreshEntity(e)){enrollGuard(e);e.setTarget(challenger());e.emergeFrom(position());}}}
    }
    private void summonMahoraga(ServerPlayer player){
        ServerLevel l=(ServerLevel)level();MahoragaEntity boss=VoidContent.MAHORAGA.get().create(l);if(boss==null)return;
        for(int i=0;i<16;i++){
            double angle=i*Math.PI/8;Vec3 at=position().add(Math.cos(angle)*(4+i/8*2),0,Math.sin(angle)*(4+i/8*2));boss.moveTo(at.x,at.y,at.z,getYRot(),0);
            if(!l.noCollision(boss))continue;
            if(l.addFreshEntity(boss)){mahoraga=boss.getUUID();if(player!=null)boss.getPersistentData().putUUID("MegumiHunt",player.getUUID());boss.setTarget(this);entityData.set(PHASE,4);executionTicks=0;clearSummons();ShadowSystem.fx(l,0,position(),position(),3,45);if(player!=null)player.sendSystemMessage(Component.literal("吟唱完成 · 魔虚罗降临，先处决伏黑惠，再追杀挑战者！"));return;}
        }
        entityData.set(CHANT,20);if(player!=null)player.displayClientMessage(Component.literal("魔虚罗正在寻找出场空间，仍可击败伏黑惠阻止召唤"),true);
    }
    @Override public void remove(Entity.RemovalReason reason){bar.removeAllPlayers();super.remove(reason);}
    @Override public void startSeenByPlayer(ServerPlayer p){super.startSeenByPlayer(p);if(phase()>0&&!downed())bar.addPlayer(p);}
    @Override public void stopSeenByPlayer(ServerPlayer p){super.stopSeenByPlayer(p);bar.removePlayer(p);}
    @Override public boolean removeWhenFarAway(double distance){return false;}
    @Override public boolean isPushable(){return !downed()&&!chanting();}
    @Override public void addAdditionalSaveData(CompoundTag tag){super.addAdditionalSaveData(tag);tag.putBoolean("MegumiSeance",true);tag.putInt("MegumiPhase",phase());tag.putInt("MegumiChant",chantTicks());tag.putInt("MegumiSequence",sequence);if(guardsLoaded)tag.put("MegumiGuards",guards.copy());if(challenger!=null)tag.putUUID("MegumiChallenger",challenger);if(mahoraga!=null)tag.putUUID("MegumiMahoraga",mahoraga);}
    @Override public void readAdditionalSaveData(CompoundTag tag){super.readAdditionalSaveData(tag);legacy=!tag.getBoolean("MegumiSeance");entityData.set(PHASE,Math.max(0,Math.min(4,tag.getInt("MegumiPhase"))));entityData.set(CHANT,tag.getInt("MegumiChant"));sequence=tag.getInt("MegumiSequence");guardsLoaded=tag.contains("MegumiGuards");for(String key:new ArrayList<>(guards.getAllKeys()))guards.remove(key);guards.merge(tag.getCompound("MegumiGuards"));challenger=tag.hasUUID("MegumiChallenger")?tag.getUUID("MegumiChallenger"):null;mahoraga=tag.hasUUID("MegumiMahoraga")?tag.getUUID("MegumiMahoraga"):null;setNoAi(downed());setInvulnerable(downed());}
}
