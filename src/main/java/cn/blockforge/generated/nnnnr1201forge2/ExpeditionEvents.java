package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.core.*;
import net.minecraft.core.particles.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.*;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.registries.ForgeRegistries;

public final class ExpeditionEvents {
    public static boolean mindShield(LivingEntity p){return p.getPersistentData().getLong("ExpeditionMindShield")>p.level().getGameTime();}
    public static boolean discActive(Player p){var d=p.getPersistentData();return d.getLong("ExpeditionDiscUntil")>p.level().getGameTime()&&d.getString("ExpeditionDiscDim").equals(p.level().dimension().location().toString())&&p.distanceToSqr(Vec3.atCenterOf(BlockPos.of(d.getLong("ExpeditionDiscPos"))))<=25;}
    @SubscribeEvent public static void drops(LivingDropsEvent e){
        var mob=e.getEntity();if(!(mob.level() instanceof ServerLevel l))return;
        ServerPlayer p=CombatSettings.caster(e.getSource());if(p==null&&mob.getKillCredit() instanceof ServerPlayer credited)p=credited;
        if(p==null||p instanceof net.minecraftforge.common.util.FakePlayer)return;
        String id=ForgeRegistries.ENTITY_TYPES.getKey(mob.getType()).getPath(),role=mob.getPersistentData().getString("ExpeditionRole");
        if(mob instanceof LowCurseEntity||mob instanceof DaitianEntity||id.equals("broken_mahito")||id.equals("mahito_npc")){
            drop(e,"residue_"+(mob instanceof LowCurseEntity curse?(curse.form().grade()<=1?"high":curse.form().grade()==2?"medium":"low"):(mob.getMaxHealth()>=400?"high":mob.getMaxHealth()>=100?"medium":"low")),1+l.random.nextInt(3));
            if(l.random.nextInt(3)==0)drop(e,"deformed_bone",1);if(l.random.nextInt(4)==0)drop(e,"curse_flesh",1);
        }
        if(role.equals("renegade")){e.getDrops().removeIf(it->it.getItem().is(ExpeditionContent.item("overtime_tie")));drop(e,"seal_black",1);}
        if(mob instanceof DaitianEntity&&!role.isEmpty()){e.getDrops().removeIf(it->it.getItem().is(Content.FINGER.get()));if(role.equals("young_daitian")&&l.random.nextInt(20)==0)drop(e,Content.FINGER.get(),1);}
        switch(id){case "jogo_npc"->drop(e,"volcanic_core",1);case "mahito_npc","broken_mahito"->drop(e,"soul_molt",1);case "toji_npc"->drop(e,"heavenly_bone",1);case "naoya_npc"->drop(e,"frame_retina",1);case "uraume_npc"->drop(e,"frozen_crystal",1);case "nanami_npc"->{if(!role.equals("renegade"))drop(e,"overtime_tie",1);}case "ender_dragon","wither"->drop(e,"star_curse_core",1);}
        if(role.equals("kenjaku")){drop(e,"kenjaku_thread",3);drop(e,"nine_paintings_manuscript",1);}
        if(role.equals("ash_curse"))drop(e,"ash_clump",2);
        if(role.equals("mutant"))drop(e,"mutant_gland",1);
        if(role.equals("wraith")&&l.isNight())drop(e,"wraith_tear",1);
        if(mob.getPersistentData().getBoolean("CullingEnemy")){int points=role.startsWith("participant")?5:1;var d=ExpeditionProgress.data(p);d.putInt("CullingPoints",d.getInt("CullingPoints")+points);p.displayClientMessage(Component.literal("死灭回游积分 +"+points+"，现有 "+d.getInt("CullingPoints")),true);}
    }
    private static void drop(LivingDropsEvent e,String id,int count){drop(e,ExpeditionContent.item(id),count);}
    private static void drop(LivingDropsEvent e,Item item,int count){var m=e.getEntity();e.getDrops().add(new net.minecraft.world.entity.item.ItemEntity(m.level(),m.getX(),m.getY()+.5,m.getZ(),new ItemStack(item,count)));}
    @SubscribeEvent public static void mobTick(LivingEvent.LivingTickEvent e){
        if(!(e.getEntity() instanceof Mob m)||m.level().isClientSide||m.tickCount%20!=0)return;var d=m.getPersistentData();
        if(d.getLong("ExpeditionDespawnAt")>0&&m.level().getGameTime()>d.getLong("ExpeditionDespawnAt")){if(d.getBoolean("CullingEnemy"))m.level().getScoreboard().removePlayerFromTeam(m.getScoreboardName());m.discard();return;}
        String role=d.getString("ExpeditionRole");if(role.equals("renegade")||role.equals("toji_echo")||role.startsWith("participant")){var p=m.level().getNearestPlayer(m,32);if(p!=null&&!p.isCreative()&&!p.isSpectator())m.setTarget(p);}
    }
    @SubscribeEvent public static void playerTick(TickEvent.PlayerTickEvent e){
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p)||p.tickCount%20!=0||p.isSpectator())return;ServerLevel l=p.serverLevel();String biome=ExpeditionWorldgen.biome(l,p.blockPosition());
        if(biome.equals("scorched_volcanic_fields")&&!p.hasEffect(MobEffects.FIRE_RESISTANCE)&&!p.isCreative()){p.addEffect(new MobEffectInstance(ExpeditionContent.OVERHEAT.get(),60));p.hurt(l.damageSources().onFire(),1);}
        if(p.hasEffect(ExpeditionContent.RUNAWAY.get())){Curse.add(p,-Math.max(5,Curse.maximum(p)*.01));p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,40));}
        if(p.getItemBySlot(EquipmentSlot.CHEST).is(ExpeditionContent.item("forbidden_armor"))){p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,40,1));p.addEffect(new MobEffectInstance(MobEffects.HUNGER,40,1));}
        if(p.getItemBySlot(EquipmentSlot.FEET).is(ExpeditionContent.item("frame_boots")))p.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED,40,1));
        if(p.getInventory().countItem(ExpeditionContent.item("foresight_goggles"))>0)for(Mob m:l.getEntitiesOfClass(Mob.class,p.getBoundingBox().inflate(12),m->m.getTarget()==p)){m.addEffect(new MobEffectInstance(MobEffects.GLOWING,40));var actor=NpcBridge.existing(m);if(m.distanceToSqr(p)<16||m instanceof DaitianEntity dt&&dt.action()!=0||actor!=null&&(Skills.sealing(actor)||Curse.active(actor,"NpcChannelUntil")))p.displayClientMessage(Component.literal("预判护目镜："+m.getName().getString()+" 正在逼近或起手，注意闪避"),true);}
        if(mindShield(p)){p.getPersistentData().remove("GojoVoid");p.removeEffect(MobEffects.BLINDNESS);p.removeEffect(MobEffects.CONFUSION);}
        if(p.getPersistentData().getLong("ExpeditionOvertime")>l.getGameTime()){p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST,40,1));if(p.tickCount%60==0)p.hurt(l.damageSources().magic(),1);}
        if(discActive(p)){BlockPos c=BlockPos.of(p.getPersistentData().getLong("ExpeditionDiscPos"));for(int i=0;i<36;i++){double a=i*Math.PI/18;l.sendParticles(ParticleTypes.END_ROD,c.getX()+.5+Math.cos(a)*5,c.getY()+.15,c.getZ()+.5+Math.sin(a)*5,1,0,0,0,0);}}
        if(biome.equals("cursed_sakura_forest")){var fog=new DustParticleOptions(new org.joml.Vector3f(.9f,.28f,.48f),2.5f);l.sendParticles(p,fog,false,p.getX(),p.getY()+.8,p.getZ(),8,5,.8,5,.01);}
        if(p.tickCount%400==0){
            if(biome.equals("cursed_wasteland")){SkillSounds.play(p,"ten_shadows_chant",.22f);l.playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT,net.minecraft.sounds.SoundSource.AMBIENT,.5f,.65f);}
            if(biome.equals("cursed_sakura_forest")){SkillSounds.play(p,"iwate_wind",.2f);l.playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.GHAST_AMBIENT,net.minecraft.sounds.SoundSource.AMBIENT,.1f,1.7f);}
            if(biome.equals("ancient_battlefield")){l.playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.HORSE_AMBIENT,net.minecraft.sounds.SoundSource.AMBIENT,.3f,.7f);l.playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.ANVIL_LAND,net.minecraft.sounds.SoundSource.AMBIENT,.12f,1.6f);if(l.random.nextInt(5)==0)l.playSound(null,p.blockPosition(),net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_THUNDER,net.minecraft.sounds.SoundSource.WEATHER,.6f,.65f);}
        }
        if(!l.dimension().equals(Level.OVERWORLD)&&p.tickCount%600==0&&l.random.nextInt(5)==0&&ExpeditionWorldgen.BIOMES.contains(biome)&&!p.isCreative()&&l.getGameRules().getBoolean(GameRules.RULE_DOMOBSPAWNING)){
            if(l.getEntitiesOfClass(Mob.class,p.getBoundingBox().inflate(48),m->m.getPersistentData().getBoolean("ExpeditionWild")).size()<12){
                var at=p.blockPosition().offset(l.random.nextInt(49)-24,0,l.random.nextInt(49)-24);if(l.hasChunkAt(at)){at=l.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,at);String role=switch(biome){case "scorched_volcanic_fields"->"ash_curse";case "ancient_battlefield"->l.isNight()?"wraith":"low_curse";case "cursed_wasteland"->"special_curse";default->"mutant";};var m=ExpeditionMobs.spawn(l,at,role,p);if(m!=null){m.getPersistentData().putBoolean("ExpeditionWild",true);m.getPersistentData().putLong("ExpeditionDespawnAt",l.getGameTime()+3600);}}
            }
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void soulBlade(LivingAttackEvent e){
        if(!(e.getSource().getEntity() instanceof ServerPlayer p)||CombatSettings.scripted(e.getSource())||e.getSource().getDirectEntity()!=p||!e.getSource().is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK))return;
        ItemStack s=p.getMainHandItem();if(!s.is(ExpeditionContent.item("soul_splitter"))&&EnchantmentHelper.getItemEnchantmentLevel(ExpeditionContent.SOUL_EDGE.get(),s)==0)return;
        if(!CombatSettings.canHit(p,e.getEntity()))return;
        var key=net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DAMAGE_TYPE,new net.minecraft.resources.ResourceLocation("sukuna",s.getItem() instanceof SoulDevourerItem?"soul_devourer":"soul_edge"));
        var source=new net.minecraft.world.damagesource.DamageSource(p.level().registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(key),p);
        e.setCanceled(true);AttackTypes.mark(AttackTypes.SOUL);
        float before=e.getEntity().getHealth()+e.getEntity().getAbsorptionAmount();
        try{
            float amount=e.getAmount()*(s.getItem() instanceof SoulDevourerItem?SoulDevourerItem.multiplier(s):1);
            if(e.getEntity().hurt(source,amount)&&e.getEntity().getHealth()+e.getEntity().getAbsorptionAmount()<before){
                if(s.getItem() instanceof SoulDevourerItem)SoulDevourerItem.recordHit(s);
                s.hurtEnemy(e.getEntity(),p);p.setLastHurtMob(e.getEntity());
            }
        }finally{AttackTypes.consume();}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void damage(LivingDamageEvent e){
        if(e.getEntity() instanceof ServerPlayer p){
            if(e.getAmount()>=p.getHealth()&&discActive(p)){e.setAmount(0);p.getPersistentData().remove("ExpeditionDiscUntil");p.sendSystemMessage(Component.literal("简易领域阵盘破碎，抵挡了这次致命伤害。"));return;}
            if(e.getAmount()>=p.getMaxHealth()*.25f){for(BlockPos pos:BlockPos.betweenClosed(p.blockPosition().offset(-6,-3,-6),p.blockPosition().offset(6,3,6))){if(!p.level().hasChunkAt(pos))continue;if(p.level().getBlockEntity(pos) instanceof RitualBlockEntity be&&be.running()&&p.getUUID().equals(be.owner)&&!be.internalDamage)be.fail("施术者受到高额伤害");}}
        }
        if(e.getSource().getEntity() instanceof ServerPlayer p&&!CombatSettings.scripted(e.getSource())&&e.getSource().getDirectEntity()==p&&(e.getSource().is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK)||e.getSource().getMsgId().equals("soul_edge"))){var s=p.getMainHandItem();if(s.hasTag()&&s.getTag().getBoolean("ExpeditionPoison")){e.getEntity().addEffect(new MobEffectInstance(MobEffects.POISON,100));e.getEntity().addEffect(new MobEffectInstance(MobEffects.BLINDNESS,40));}if(s.is(ExpeditionContent.item("blood_blade")))p.heal(Math.min(2,e.getAmount()*.1f));if(s.is(ExpeditionContent.item("soul_splitter"))||EnchantmentHelper.getItemEnchantmentLevel(ExpeditionContent.SOUL_EDGE.get(),s)>0||s.hasTag()&&s.getTag().getBoolean("ExpeditionSoulEdge")){e.setAmount(e.getAmount()+4);e.getEntity().removeEffect(MobEffects.INVISIBILITY);e.getEntity().addEffect(new MobEffectInstance(MobEffects.GLOWING,100));}}
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void cleanupFaction(LivingDeathEvent e){if(!e.isCanceled()&&!e.getEntity().level().isClientSide&&e.getEntity().getPersistentData().getBoolean("CullingEnemy"))e.getEntity().level().getScoreboard().removePlayerFromTeam(e.getEntity().getScoreboardName());}
    @SubscribeEvent(priority=EventPriority.HIGH) public static void death(LivingDeathEvent e){
        if(!(e.getEntity() instanceof ServerPlayer p)||p.getInventory().countItem(ExpeditionContent.item("substitute_doll"))==0)return;
        BlockPos safe=safePosition(p.serverLevel(),p.blockPosition(),16);if(safe==null||PrisonSystem.locked(p))return;
        RitualRecipes.take(p,ExpeditionContent.item("substitute_doll"),1);e.setCanceled(true);p.setHealth(Math.max(1,p.getMaxHealth()*.3f));p.teleportTo(p.serverLevel(),safe.getX()+.5,safe.getY(),safe.getZ()+.5,p.getYRot(),p.getXRot());p.fallDistance=0;p.clearFire();p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,100,3));
    }
    public static BlockPos safePosition(ServerLevel l,BlockPos origin,int radius){
        for(int i=0;i<150;i++){BlockPos p=origin.offset(l.random.nextInt(radius*2+1)-radius,l.random.nextInt(13)-6,l.random.nextInt(radius*2+1)-radius);if(!l.hasChunkAt(p)||!l.getWorldBorder().isWithinBounds(p)||l.isOutsideBuildHeight(p)||l.isOutsideBuildHeight(p.above()))continue;
            var floor=l.getBlockState(p.below());if(!floor.isSolidRender(l,p.below())||floor.is(Blocks.MAGMA_BLOCK)||floor.getBlock() instanceof ExpeditionTrapBlock)continue;
            if(l.getBlockState(p).is(Blocks.FIRE)||l.getBlockState(p).is(Blocks.SOUL_FIRE)||l.getBlockState(p.above()).is(Blocks.FIRE)||l.getBlockState(p.above()).is(Blocks.SOUL_FIRE))continue;
            if(!l.getFluidState(p).isEmpty()||!l.getFluidState(p.above()).isEmpty())continue;
            if(l.noCollision(new AABB(p.getX()+.2,p.getY(),p.getZ()+.2,p.getX()+.8,p.getY()+1.8,p.getZ()+.8)))return p;
        }return null;
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void interact(PlayerInteractEvent.RightClickBlock e){
        if(!(e.getEntity() instanceof ServerPlayer p)||e.isCanceled()||e.getFace()==null||!p.mayUseItemAt(e.getPos(),e.getFace(),e.getItemStack()))return;var state=p.level().getBlockState(e.getPos());var stack=e.getItemStack();
        if(stack.is(Items.SHEARS)&&(state.is(ExpeditionContent.block("black_vines"))||state.is(ExpeditionContent.block("shimenawa")))){p.level().destroyBlock(e.getPos(),false);Journey.give(p,new ItemStack(ExpeditionContent.item("black_rope_fiber"),2));stack.hurtAndBreak(1,p,x->x.broadcastBreakEvent(e.getHand()));e.setCanceled(true);e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);}
        if(stack.is(Items.GLASS_BOTTLE)&&state.is(ExpeditionContent.block("eternal_flame"))){if(!p.isCreative())stack.shrink(1);Journey.give(p,new ItemStack(ExpeditionContent.item("eternal_fire")));e.setCanceled(true);e.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);}
    }
    public static float techniqueBonus(ServerPlayer p,String type){
        var d=ExpeditionProgress.data(p);boolean fire=AttackTypes.FIRE.equals(type)||AttackTypes.MAGIC_FURNACE.equals(type);
        if(fire&&d.getBoolean("ExpeditionScroll_furnace_scroll"))return 1.15f;
        if(AttackTypes.ICE.equals(type)&&d.getBoolean("ExpeditionScroll_ice_scroll"))return 1.15f;
        if(AttackTypes.BLOOD.equals(type)&&p.getInventory().countItem(ExpeditionContent.item("blood_sakura_petal"))>0)return 1.1f;
        return 1;
    }
    private ExpeditionEvents(){}
}
