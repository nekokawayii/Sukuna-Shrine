package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import com.github.tartaricacid.touhoulittlemaid.api.*;
import com.github.tartaricacid.touhoulittlemaid.api.task.IAttackTask;
import com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid;
import com.github.tartaricacid.touhoulittlemaid.entity.task.TaskManager;
import com.mojang.datafixers.util.Pair;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.behavior.BehaviorControl;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.*;

/** 由车万女仆的扩展发现机制加载；未安装时主模组不链接此类。 */
@LittleMaidExtension
public final class MaidExtension implements ILittleMaid {
    private static final ResourceLocation ID=new ResourceLocation("sukuna","malevolent_shrine");
    // 仅供女仆内部选招，不占用玩家轮盘技能编号。
    private static final int MAID_BACKFLIP=101,MAID_KICK=102;
    private static int motionKind(int skill){return skill==MAID_BACKFLIP?DismantleMotionRules.BACKFLIP:skill==MAID_KICK?DismantleMotionRules.FLYING_KICK:skill==DismantleMotionRules.RING_SKILL?DismantleMotionRules.SPIN:skill>=103&&skill<=106?skill-99:0;}
    private static final com.github.tartaricacid.touhoulittlemaid.api.entity.data.TaskDataKey<Boolean> MEMORY=new com.github.tartaricacid.touhoulittlemaid.api.entity.data.TaskDataKey<>() {
        @Override public ResourceLocation getKey(){return new ResourceLocation("sukuna","shrine_memory");}
        @Override public net.minecraft.nbt.CompoundTag writeSaveData(Boolean value){var tag=new net.minecraft.nbt.CompoundTag();tag.putBoolean("Unlocked",Boolean.TRUE.equals(value));return tag;}
        @Override public Boolean readSaveData(net.minecraft.nbt.CompoundTag tag){return tag.getBoolean("Unlocked");}
    };
    private static final ResourceLocation ICE_ID=new ResourceLocation("sukuna","ice_formation");
    private static final com.github.tartaricacid.touhoulittlemaid.api.entity.data.TaskDataKey<Boolean> ICE_MEMORY=new com.github.tartaricacid.touhoulittlemaid.api.entity.data.TaskDataKey<>(){
        @Override public ResourceLocation getKey(){return new ResourceLocation("sukuna","ice_memory");}
        @Override public net.minecraft.nbt.CompoundTag writeSaveData(Boolean value){var tag=new net.minecraft.nbt.CompoundTag();tag.putBoolean("Unlocked",Boolean.TRUE.equals(value));return tag;}
        @Override public Boolean readSaveData(net.minecraft.nbt.CompoundTag tag){return tag.getBoolean("Unlocked");}
    };
    public MaidExtension(){MinecraftForge.EVENT_BUS.register(this);}
    @Override public void registerTaskData(com.github.tartaricacid.touhoulittlemaid.entity.data.TaskDataRegister register){register.register(MEMORY);register.register(ICE_MEMORY);}
    @Override public void addMaidTask(TaskManager manager){manager.add(new ShrineTask());manager.add(new IceTask());}
    @Override public void bindMaidBauble(com.github.tartaricacid.touhoulittlemaid.item.bauble.BaubleManager manager){
        manager.bind(EternalLove.TRUE_FALSE,new com.github.tartaricacid.touhoulittlemaid.api.bauble.IMaidBauble(){});
    }
    private static void syncMemory(EntityMaid maid){
        boolean unlocked=maid.getPersistentData().getCompound("SukunaProgress").getBoolean("Memory");
        if(!Objects.equals(maid.getData(MEMORY),unlocked))maid.setAndSyncData(MEMORY,unlocked);
        boolean ice=maid.getPersistentData().getCompound("SukunaProgress").getBoolean("UraumeMemory");if(!Objects.equals(maid.getData(ICE_MEMORY),ice))maid.setAndSyncData(ICE_MEMORY,ice);
    }
    public static final class ShrineTask implements IAttackTask {
        @Override public ResourceLocation getUid(){return ID;}
        @Override public ItemStack getIcon(){return new ItemStack(Content.FINGER.get());}
        @Override public net.minecraft.network.chat.MutableComponent getName(){return Component.literal("伏魔御厨子");}
        @Override public SoundEvent getAmbientSound(EntityMaid maid){return null;}
        @Override public List<Pair<Integer,BehaviorControl<? super EntityMaid>>> createBrainTasks(EntityMaid maid){return List.of();}
        @Override public boolean isWeapon(EntityMaid maid,ItemStack stack){return true;}
        @Override public boolean canAttack(EntityMaid maid,LivingEntity target){return !maid.level().isClientSide&&CombatSettings.canHit(MaidBridge.actor(maid),target);}
        @Override public boolean isEnable(EntityMaid maid){return maid.level().isClientSide?Boolean.TRUE.equals(maid.getData(MEMORY)):maid.getPersistentData().getCompound("SukunaProgress").getBoolean("Memory");}
        @Override public boolean enableLookAndRandomWalk(EntityMaid maid){return maid.getTarget()==null;}
        @Override public List<String> getDescription(EntityMaid maid){return List.of("task.sukuna.malevolent_shrine.desc");}
    }
    public static final class IceTask implements IAttackTask {
        @Override public ResourceLocation getUid(){return ICE_ID;}
        @Override public ItemStack getIcon(){return new ItemStack(IceContent.MEMORY.get());}
        @Override public net.minecraft.network.chat.MutableComponent getName(){return Component.literal("冰凝咒法");}
        @Override public SoundEvent getAmbientSound(EntityMaid maid){return null;}
        @Override public List<Pair<Integer,BehaviorControl<? super EntityMaid>>> createBrainTasks(EntityMaid maid){return List.of();}
        @Override public boolean isWeapon(EntityMaid maid,ItemStack stack){return true;}
        @Override public boolean canAttack(EntityMaid maid,LivingEntity target){return !maid.level().isClientSide&&CombatSettings.canHit(MaidBridge.actor(maid),target);}
        @Override public boolean isEnable(EntityMaid maid){return maid.level().isClientSide?Boolean.TRUE.equals(maid.getData(ICE_MEMORY)):maid.getPersistentData().getCompound("SukunaProgress").getBoolean("UraumeMemory");}
        @Override public boolean enableLookAndRandomWalk(EntityMaid maid){return maid.getTarget()==null;}
        @Override public List<String> getDescription(EntityMaid maid){return List.of("task.sukuna.ice_formation.desc");}
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public void feed(PlayerInteractEvent.EntityInteract event){
        if(!(event.getTarget() instanceof EntityMaid maid)||!event.getEntity().isShiftKeyDown()||!maid.isOwnedBy(event.getEntity()))return;
        event.setCanceled(true);event.setCancellationResult(InteractionResult.SUCCESS);
        if(!(event.getEntity() instanceof ServerPlayer owner))return;
        ItemStack stack=event.getItemStack();MaidBridge.Actor actor=MaidBridge.actor(maid);
        owner.sendSystemMessage(Component.literal("女仆当前咒力："+(int)Curse.energy(actor)+"/"+(int)Curse.maximum(actor)+"；"+(MaidBridge.recovering(actor)?"肉搏恢复中，超过25%恢复施法":"施法就绪")+"；成长："+Journey.guide(actor)));
        if(stack.is(IceContent.MEMORY.get())){
            if(Uraume.consumeMemory(actor)){if(!owner.isCreative())stack.shrink(1);owner.sendSystemMessage(Component.literal("女仆已选择里梅：请在女仆任务中选择冰凝咒法。"));}
            else owner.displayClientMessage(Component.literal(Journey.unlocked(actor)?"女仆已选择宿傩，不能再吃里梅记忆":"女仆已经吃过里梅记忆"),true);
            syncMemory(maid);return;
        }
        if(stack.is(IceContent.FISH.get())){
            if(Uraume.feedFish(actor)){if(!owner.isCreative())stack.shrink(1);owner.sendSystemMessage(Component.literal("女仆冰鱼："+Uraume.fish(actor)+" 条，冰伤 +"+Uraume.fish(actor)+"%，咒力上限 +"+BalanceRules.number(EnergyRules.ICE_FISH_BONUS)+"（现为 "+BalanceRules.number(Curse.maximum(actor))+"）"));}
            else owner.displayClientMessage(Component.literal("只有选择里梅的女仆能吃冰鱼成长"),true);return;
        }
        if(stack.is(ProgressionContent.MEMORY.get())){
            if(MaidBridge.consumeMemory(actor)){if(!owner.isCreative())stack.shrink(1);owner.sendSystemMessage(Component.literal("女仆已吃下宿傩记忆，伏魔御厨子任务开启。"));Curse.sync(actor);}
            else owner.displayClientMessage(Component.literal(Uraume.unlocked(actor)?"女仆已选择里梅，不能再吃宿傩记忆":"这位女仆已经吃下过宿傩记忆"),true);
            syncMemory(maid);
            return;
        }
        if(stack.isEmpty()||!StageFood.feedItem(stack))return;
        if(!StageFood.feed(actor,stack)){owner.displayClientMessage(Component.literal("这位女仆尚未达到对应阶段，或已吸收过该成长物品"),true);return;}
        MaidBridge.apply(actor);MaidBridge.rememberCondensed(actor);if(!owner.isCreative())stack.shrink(1);
        owner.sendSystemMessage(Component.literal("女仆成长："+Journey.guide(actor)));
    }
    @SubscribeEvent public void tick(LivingEvent.LivingTickEvent event){
        if(!(event.getEntity() instanceof EntityMaid maid)||maid.level().isClientSide||!maid.isAlive())return;
        syncMemory(maid);
        if(EternalLove.settling(maid)||NetBranches.locked(maid)||Uraume.locked(maid)||CombatExpansion.controlled(maid))return;
        var progress=maid.getPersistentData().getCompound("SukunaProgress");
        if(!progress.getBoolean("Memory")&&!progress.getBoolean("UraumeMemory"))return;
        MaidBridge.Actor actor=MaidBridge.actor(maid);
        if(Uraume.unlocked(actor)){iceTick(maid,actor);return;}
        Curse.data(actor).putBoolean("MemoryGiven",true);
        if(maid.tickCount%ShrineRules.ACTION_INTERVAL==0){absorbOrb(maid,actor);MaidBridge.rememberCondensed(actor);}
        boolean active=maid.getTask().getUid().equals(ID)&&!maid.isMaidInSittingPose();
        if(!active){CombatExpansion.cancel(actor);DismantleMotion.cancel(actor);if(Skills.domainActive(actor))Skills.closeDomain(actor);Skills.cancelSustained(actor);GuardianPlayerSkills.cancel(actor);actor.aim=null;return;}
        if(CombatExpansion.automatic(actor)){stopMoving(maid);return;}
        if(DismantleMotion.active(actor)){stopMoving(maid);maid.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);return;}
        if(DomainClash.active(actor)){stopMoving(maid);DomainClash.hold(maid);return;}
        if(maid.tickCount%ShrineRules.ACTION_INTERVAL!=0)return;
        actor.syncPosition();MaidBridge.apply(actor);
        boolean recovering=ShrineRules.recovering(MaidBridge.recovering(actor),Curse.energy(actor),Curse.maximum(actor));
        Curse.data(actor).putBoolean("MaidRecovering",recovering);
        Curse.data(actor).putBoolean("Fist",!recovering&&Curse.fingers(actor)>=5);
        LivingEntity target=maid.getTarget();
        if(target==null||!CombatSettings.canHit(actor,target)||maid.distanceToSqr(target)>4096||!maid.hasLineOfSight(target)){
            target=maid.level().getEntitiesOfClass(LivingEntity.class,maid.getBoundingBox().inflate(48),e->CombatSettings.canHit(actor,e)&&maid.hasLineOfSight(e)).stream().min(Comparator.<LivingEntity>comparingInt(e->MaidBridge.modCreature(e)?0:1).thenComparingDouble(maid::distanceToSqr)).orElse(null);
        }
        actor.aim=target;
        if(target==null)maid.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
        else{maid.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET,target);maid.getBrain().setMemory(MemoryModuleType.LOOK_TARGET,new EntityTracker(target,true));maid.getLookControl().setLookAt(target,90,90);actor.syncPosition();}
        if(recovering){melee(maid,actor,target);return;}
        if(Curse.burnout(actor)&&Curse.fingers(actor)>=15&&Curse.energy(actor)>=ShrineRules.repairCost(Curse.maximum(actor))){stopMoving(maid);Curse.repairBurnout(actor);return;}
        if(ShrineRules.needsReverse(maid.getHealth(),maid.getMaxHealth())&&Curse.reversePulse(actor)){stopMoving(maid);return;}
        if(GuardianPlayerSkills.casting(actor)||NetBranches.casting(actor)){stopMoving(maid);return;}
        if(Curse.data(actor).getBoolean("MultiArmed")&&target!=null){melee(maid,actor,target);return;}
        List<Integer> skills=new ArrayList<>();
        if(target!=null&&!SoulCombat.suppressed(actor)&&Curse.canUseTechnique(actor)){
            for(int i=0;i<6;i++)if(Skills.available(actor,i)&&!(i==2&&Skills.cleaveActive(actor))&&!(i==5&&(Skills.domainActive(actor)||GuardianPlayerSkills.sustaining(actor))))skills.add(i);
            if(Skills.peelAvailable(actor))skills.add(6);
            for(int form:new int[]{SlashFormRules.SHAPE,SlashFormRules.CROSS_FLIGHT,SlashFormRules.WORLD})if(SlashForms.available(actor,form))skills.add(form);
            for(int branch:new int[]{CombatExpansionRules.LINGER,CombatExpansionRules.PURPLE_FURNACE,CombatExpansionRules.DRAGON_STAR})if(CombatExpansion.available(actor,branch))skills.add(branch);
            for(int motion:new int[]{MAID_BACKFLIP,MAID_KICK,DismantleMotionRules.RING_SKILL,103,104,105,106})if(DismantleMotion.available(actor,motionKind(motion)))skills.add(motion);
            if(NetBranches.available(actor,NetBranchRules.CROSS_SKILL)&&!NetBranches.locked(target))skills.add(NetBranchRules.CROSS_SKILL);
            if(NetBranches.available(actor,NetBranchRules.QI_SKILL))skills.add(NetBranchRules.QI_SKILL);
            for(int i=BranchRules.WEB;i<=BranchRules.WALL;i++)if(SukunaBranches.available(actor,i))skills.add(i);
            if(GuardianCombat.awakened(actor))for(int i=0;i<GuardianPlayerSkills.COUNT;i++)if(i!=7&&!(i==5&&(Skills.domainActive(actor)||GuardianPlayerSkills.sustaining(actor)))&&GuardianPlayerSkills.available(actor,i))skills.add(7+i);
        }
        if(ReverseOutput.maidReady(actor))skills.add(14);
        if(skills.isEmpty()){
            if(target!=null){Curse.data(actor).putBoolean("MaidRecovering",true);Curse.data(actor).putBoolean("Fist",false);Skills.cancelSustained(actor);melee(maid,actor,target);}
            return;
        }
        stopMoving(maid);int skill=chooseSkill(actor,skills);
        if(CombatExpansionRules.branch(skill))CombatExpansion.castAutomatic(actor,skill);
        else if(motionKind(skill)!=0)DismantleMotion.start(actor,motionKind(skill));
        else if(skill==SlashFormRules.SHAPE||skill==SlashFormRules.CROSS_FLIGHT||skill==SlashFormRules.WORLD){
            boolean previous=SlashForms.qi(actor);
            try{Curse.data(actor).putBoolean("DismantleQi",true);SlashForms.cast(actor,skill);}
            finally{Curse.data(actor).putBoolean("DismantleQi",previous);}
        }
        else if(skill>=BranchRules.WEB&&skill<=BranchRules.WALL)SukunaBranches.cast(actor,skill);else if(NetBranchRules.branch(skill))NetBranches.cast(actor,skill);else if(skill<6)Skills.cast(actor,skill);else if(skill==6)Skills.peel(actor);else if(skill==14)ReverseOutput.cast(actor);else GuardianPlayerSkills.cast(actor,skill-7);
    }
    private static void iceTick(EntityMaid maid,MaidBridge.Actor actor){
        if(!maid.getTask().getUid().equals(ICE_ID)||maid.isMaidInSittingPose()){Uraume.cancel(actor);actor.aim=null;return;}
        if(DomainClash.active(actor)){stopMoving(maid);return;}
        if(maid.tickCount%ShrineRules.ACTION_INTERVAL!=0)return;
        absorbOrb(maid,actor);actor.syncPosition();MaidBridge.apply(actor);
        boolean recovering=ShrineRules.recovering(MaidBridge.recovering(actor),Curse.energy(actor),Curse.maximum(actor));Curse.data(actor).putBoolean("MaidRecovering",recovering);Curse.data(actor).putBoolean("Fist",false);
        LivingEntity target=maid.getTarget();
        if(target==null||!CombatSettings.canHit(actor,target)||maid.distanceToSqr(target)>4096||!maid.hasLineOfSight(target))target=maid.level().getEntitiesOfClass(LivingEntity.class,maid.getBoundingBox().inflate(48),e->CombatSettings.canHit(actor,e)&&maid.hasLineOfSight(e)).stream().min(Comparator.comparingDouble(maid::distanceToSqr)).orElse(null);
        actor.aim=target;if(target==null){maid.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);return;}
        maid.getBrain().setMemory(MemoryModuleType.ATTACK_TARGET,target);maid.getBrain().setMemory(MemoryModuleType.LOOK_TARGET,new EntityTracker(target,true));maid.getLookControl().setLookAt(target,90,90);actor.syncPosition();
        if(recovering){melee(maid,actor,target);return;}
        if(Uraume.breathing(actor)){stopMoving(maid);return;}
        int skill=target.distanceToSqr(maid)<=Uraume.length(actor)*Uraume.length(actor)&&Uraume.available(actor,BranchRules.FROST)?BranchRules.FROST:BranchRules.ICEFALL;
        if(Uraume.available(actor,skill)){stopMoving(maid);Uraume.cast(actor,skill);}else{if(!Curse.canSpend(actor,Math.min(Uraume.cost(BranchRules.FROST),Uraume.cost(BranchRules.ICEFALL))))Curse.data(actor).putBoolean("MaidRecovering",true);melee(maid,actor,target);}
    }
    private static void stopMoving(EntityMaid maid){maid.getBrain().eraseMemory(MemoryModuleType.WALK_TARGET);maid.getNavigation().stop();}
    private static void melee(EntityMaid maid,MaidBridge.Actor actor,LivingEntity target){
        if(target==null){stopMoving(maid);return;}
        maid.getNavigation().moveTo(target,1.0);
        double reach=maid.getBbWidth()*2;
        if(maid.distanceToSqr(target)<=reach*reach+target.getBbWidth()&&maid.hasLineOfSight(target)){
            maid.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
            CombatSettings.hurt(actor,target,maid.damageSources().mobAttack(maid),(float)maid.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE));
        }
    }
    private static void absorbOrb(EntityMaid maid,MaidBridge.Actor actor){
        if(!ShrineRules.needsOrb(Curse.energy(actor),Curse.maximum(actor)))return;
        var inventory=maid.getAvailableBackpackInv();
        for(int i=0;i<inventory.getSlots();i++)if(inventory.getStackInSlot(i).is(Content.ORB.get())){
            ItemStack taken=inventory.extractItem(i,1,false);if(!taken.isEmpty()){Curse.addRaw(actor,150);return;}
        }
    }
    /** 低消耗术式权重更高，高消耗术式仍然有机会释放。 */
    private static int chooseSkill(MaidBridge.Actor actor,List<Integer> skills){
        double total=0;for(int skill:skills)total+=ShrineRules.skillWeight(skillCost(skill));
        double roll=actor.getRandom().nextDouble()*total;
        for(int skill:skills){roll-=ShrineRules.skillWeight(skillCost(skill));if(roll<=0)return skill;}
        return skills.get(skills.size()-1);
    }
    private static int skillCost(int skill){if(CombatExpansionRules.branch(skill))return skill==CombatExpansionRules.DRAGON_STAR?2000:skill==CombatExpansionRules.PURPLE_FURNACE?Curse.COST[3]:Curse.COST[0];if(motionKind(skill)!=0)return Curse.COST[0];if(skill==SlashFormRules.SHAPE)return 120;if(skill==SlashFormRules.CROSS_FLIGHT)return 180;if(skill==SlashFormRules.WORLD)return 2000;if(skill==BranchRules.WEB)return 180;if(skill==BranchRules.MULTI)return 120;if(skill==BranchRules.WALL)return BranchRules.wallCost(4);return NetBranchRules.branch(skill)?NetBranchRules.cost(skill):skill<=6?Curse.COST[skill]:GuardianPlayerSkills.COST[skill-7];}
}
