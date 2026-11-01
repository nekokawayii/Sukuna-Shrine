package cn.blockforge.generated.nnnnr1201forge2;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.*;
import net.minecraft.core.particles.*;
import net.minecraft.sounds.*;

/** 仪式时间、持有者、已扣材料及成品全部存档；卸载暂停，重载继续，禁止重复领取。 */
public final class RitualBlockEntity extends BlockEntity {
    public static final int[][] FOUR={{3,0},{-3,0},{0,3},{0,-3}};
    public static final int[][] EIGHT={{3,0},{-3,0},{0,3},{0,-3},{3,3},{3,-3},{-3,3},{-3,-3}};
    public String recipeId="";
    public int elapsed,selection,strikes;
    public UUID owner;
    public ItemStack output=ItemStack.EMPTY;
    public boolean internalDamage;
    private int syncClock;
    private boolean volcanicFurnace;
    public RitualBlockEntity(BlockPos p,BlockState s){super(ExpeditionContent.RITUAL.get(),p,s);}
    public int tier(){return ((RitualBlock)getBlockState().getBlock()).tier;}
    public boolean running(){return !recipeId.isEmpty();}
    public RitualRecipes.Recipe recipe(){return RitualRecipes.get(recipeId);}
    public ItemStack display(){var r=recipe();return output.isEmpty()?(r==null?ItemStack.EMPTY:r.result()):output;}
    public void interact(ServerPlayer p){
        if(running()){var current=recipe();if(current==null){recipeId="";changed();p.sendSystemMessage(Component.literal("原仪式配方已不存在，已停止运行。"));return;}p.sendSystemMessage(Component.literal("仪式进度 "+elapsed+" / "+current.ticks()+" 刻；蛊毒缸卸载时暂停，其他仪式需要施术者留在阵内。"));return;}
        if(!output.isEmpty()){if(owner!=null&&!owner.equals(p.getUUID())){p.sendSystemMessage(Component.literal("这是另一位施术者的仪式产物。"));return;}ItemStack reward=output;output=ItemStack.EMPTY;changed();Journey.give(p,reward);return;}
        if(tier()==0&&p.getMainHandItem().is(ExpeditionContent.item("volcanic_core"))&&!volcanicFurnace){if(!p.isCreative())p.getMainHandItem().shrink(1);volcanicFurnace=true;changed();p.sendSystemMessage(Component.literal("火山核心已嵌入：手持可熔炼物潜行右键，无需燃料熔炼一件。"));return;}
        if(tier()==0&&volcanicFurnace&&p.isShiftKeyDown()&&!p.getMainHandItem().isEmpty()){
            var container=new net.minecraft.world.SimpleContainer(p.getMainHandItem().copy());var smelting=level.getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMELTING,container,level);
            if(smelting.isPresent()){ItemStack result=smelting.get().assemble(container,level.registryAccess());p.getMainHandItem().shrink(1);Journey.give(p,result);level.playSound(null,worldPosition,SoundEvents.FIRE_AMBIENT,SoundSource.BLOCKS,1,1);return;}
        }
        var recipes=RitualRecipes.ALL.stream().filter(r->r.tier()==tier()).toList();
        if(p.isShiftKeyDown()){selection=(selection+1)%recipes.size();changed();p.sendSystemMessage(Component.literal("已选择："+recipes.get(selection).description()));return;}
        var r=recipes.get(Math.floorMod(selection,recipes.size()));String fault=validate();
        if(fault!=null){p.sendSystemMessage(Component.literal(fault));return;}
        if(tier()>0&&!correctBiome()){p.sendSystemMessage(Component.literal(tier()==1?"蛊毒之浴必须建在樱花怨林或枯骨战场。":"天元降灵坛必须建在诅咒废土或枯骨战场。"));return;}
        if(tier()==2&&!level.canSeeSky(worldPosition.above())){p.sendSystemMessage(Component.literal("降灵核心上方必须露天，雷劫才能落下。"));return;}
        if(tier()==2&&!level.isNight()&&!level.isThundering()){p.sendSystemMessage(Component.literal("降灵只能在夜晚或雷雨中开始。"));return;}
        if(tier()==2&&p.getHealth()<=p.getMaxHealth()*.2f){p.sendSystemMessage(Component.literal("血量不足，无法支付20%最大生命。"));return;}
        if(!r.availableAt(p,worldPosition)){p.sendSystemMessage(Component.literal("材料或图纸不足；潜行右键换配方。\n"+r.description()+(r.blueprint()?"；需先右键研习特级咒具图纸":"")));return;}
        if(tier()==0&&Curse.energy(p)<20){p.sendSystemMessage(Component.literal("注入需要咒力，至少保留20点。"));return;}
        r.consumeAt(p,worldPosition);owner=p.getUUID();recipeId=r.id();elapsed=0;strikes=0;
        if(tier()==2){p.setHealth(Math.max(1,p.getHealth()-p.getMaxHealth()*.2f));((ServerLevel)level).setWeatherParameters(0,400,true,true);}
        changed();p.sendSystemMessage(Component.literal("仪式开始："+r.result().getHoverName().getString()+"。材料已投入；离阵、重创或破坏阵型会失败。"));
    }
    private boolean correctBiome(){String b=ExpeditionWorldgen.biome(level,worldPosition);return b.equals("ancient_battlefield")||b.equals(tier()==1?"cursed_sakura_forest":"cursed_wasteland");}
    public String validate(){
        if(tier()==0){
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)if(!level.getBlockState(worldPosition.offset(x,-1,z)).is(Blocks.OBSIDIAN))return "淬炼台下方需要3×3黑曜石底座。";
            for(int[] d:FOUR){if(!level.getBlockState(worldPosition.offset(d[0],0,d[1])).is(ExpeditionContent.block("simple_seal_pillar")))return "淬炼台东西南北各3格放一根简易封印柱。";if(!level.getBlockState(worldPosition.offset(d[0],-1,d[1])).is(Blocks.STONE_BRICKS))return "四根简易封印柱下方需要石砖。";}
            for(int x:new int[]{-1,1})for(int z:new int[]{-1,1})if(!level.getBlockState(worldPosition.offset(x,0,z)).is(Blocks.AMETHYST_BLOCK))return "淬炼台斜角的四格需要紫水晶块。";
        }else if(tier()==1){
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){if(x==0&&z==0)continue;if(!level.getBlockState(worldPosition.offset(x,0,z)).is(Blocks.CAULDRON)&&!level.getBlockState(worldPosition.offset(x,0,z)).is(Blocks.WATER_CAULDRON))return "蛊毒缸周围8格放炼药锅，组成3×3缸体。";if(!level.getBlockState(worldPosition.offset(x,-1,z)).is(ExpeditionContent.block("alloy_block")))return "3×3缸体下方需要高专合金块。";}
            if(!level.getBlockState(worldPosition.below()).is(ProgressionContent.TRANSFIGURED_FLESH.get()))return "蛊毒缸正下方放无为转变血肉块。";
            for(int x=-2;x<=2;x++)for(int z=-2;z<=2;z++)if(Math.abs(x)==2||Math.abs(z)==2){boolean corner=Math.abs(x)==2&&Math.abs(z)==2;String id=corner?"curse_crystal_block":"alloy_bars";if(!level.getBlockState(worldPosition.offset(x,0,z)).is(ExpeditionContent.block(id)))return "缸外一圈用高专合金栅栏围住，四角放咒物结晶块。";}
        }else{
            for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)if(!level.getBlockState(worldPosition.offset(x,-1,z)).is(ExpeditionContent.block("alloy_block")))return "降灵核心下方需要7×7高专合金块底座。";
            if(!level.getBlockState(worldPosition.below(2)).is(ExpeditionContent.block("spirit_stone")))return "降灵核心正下方两格需要灵脉石。";
            for(int[] d:EIGHT){for(int y=0;y<3;y++)if(!level.getBlockState(worldPosition.offset(d[0],y,d[1])).is(ExpeditionContent.block("special_seal_pillar")))return "半径3格的八个方位各建3格高特级封印柱。";if(!level.getBlockState(worldPosition.offset(d[0],2,d[1])).getValue(SealPillarBlock.INLAID))return "八根柱顶都需要持星铁碎片右键镶嵌。";}
        }
        return null;
    }
    public static void tick(Level level,BlockPos pos,BlockState state,RitualBlockEntity be){
        if(!be.running()||!(level instanceof ServerLevel sl))return;
        var r=be.recipe();if(r==null){be.recipeId="";be.changed();return;}
        ServerPlayer p=be.owner==null?null:sl.getServer().getPlayerList().getPlayer(be.owner);
        if(be.tier()!=1&&(p==null||!p.isAlive()||p.level()!=level||p.distanceToSqr(Vec3.atCenterOf(pos))>36)){be.fail("施术者离开阵眼");return;}
        if(be.elapsed%20==0){
            if(be.validate()!=null||be.tier()==2&&!level.canSeeSky(pos.above())){be.fail("阵型被破坏或阵眼被遮挡");return;}
            if(be.tier()==0&&!Curse.spend(p,20)){be.fail("咒力耗尽");return;}
            if(be.tier()==1)for(ServerPlayer other:sl.players())if(other.distanceToSqr(Vec3.atCenterOf(pos))<36&&!other.isCreative()&&!other.isSpectator())other.addEffect(new MobEffectInstance(MobEffects.POISON,100,1));
            if(be.elapsed%100==0)ExpeditionMobs.ritualWave(sl,pos,be.tier(),p);
            if(be.tier()==0&&be.elapsed%160==0&&p!=null)SkillSounds.play(p,"ten_shadows_chant",.45f);
            sl.sendParticles(be.tier()==1?ParticleTypes.SNEEZE:ParticleTypes.ENCHANT,pos.getX()+.5,pos.getY()+1.2,pos.getZ()+.5,12,1,.5,1,.02);
            if(be.elapsed%80==0)sl.playSound(null,pos,be.tier()==1?SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT:SoundEvents.ENCHANTMENT_TABLE_USE,SoundSource.BLOCKS,.8f,.55f);
        }
        ++be.elapsed;
        if(be.tier()==2&&(be.elapsed==60||be.elapsed==120||be.elapsed==180)&&p!=null){
            ++be.strikes;be.internalDamage=true;
            Network.send(sl,new Network.Effect(970,p.getId(),p.position(),Vec3.ZERO,1,20,sl.random.nextLong()));
            try{p.hurt(sl.damageSources().lightningBolt(),p.getMaxHealth()*.30f+4);}finally{be.internalDamage=false;}
            sl.playSound(null,pos,SoundEvents.LIGHTNING_BOLT_THUNDER,SoundSource.WEATHER,3,.7f);
            sl.sendParticles(ParticleTypes.SMOKE,p.getX(),p.getY()+1,p.getZ(),50,.5,1,.5,.05);
            if(!p.isAlive()){sl.sendParticles(ParticleTypes.ASH,p.getX(),p.getY()+1,p.getZ(),100,.8,1,.8,.1);be.fail("未能承受雷劫");return;}
        }
        if(be.elapsed>=r.ticks()){be.output=r.result();be.recipeId="";if(p!=null)p.sendSystemMessage(Component.literal("仪式完成，右键祭坛领取产物。"));}
        be.setChanged();if(++be.syncClock>=10){be.syncClock=0;be.changed();}
    }
    public void fail(String reason){if(!running())return;recipeId="";output=ItemStack.EMPTY;changed();if(level instanceof ServerLevel sl){ServerPlayer p=owner==null?null:sl.getServer().getPlayerList().getPlayer(owner);if(p!=null){p.addEffect(new MobEffectInstance(ExpeditionContent.RUNAWAY.get(),600));p.sendSystemMessage(Component.literal("仪式反噬："+reason+"，投入材料全部损毁。"));}sl.explode(null,worldPosition.getX()+.5,worldPosition.getY()+1,worldPosition.getZ()+.5,2.5f,Level.ExplosionInteraction.NONE);}}
    public void destroy(){if(running())fail("祭坛被摧毁");if(!output.isEmpty()){Block.popResource(level,worldPosition,output);output=ItemStack.EMPTY;}}
    private void changed(){setChanged();if(level!=null)level.sendBlockUpdated(worldPosition,getBlockState(),getBlockState(),3);}
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);n.putBoolean("VolcanicFurnace",volcanicFurnace);n.putString("Recipe",recipeId);n.putInt("Elapsed",elapsed);n.putInt("Selection",selection);n.putInt("Strikes",strikes);if(owner!=null)n.putUUID("Owner",owner);n.put("Output",output.save(new CompoundTag()));}
    @Override public void load(CompoundTag n){super.load(n);volcanicFurnace=n.getBoolean("VolcanicFurnace");recipeId=n.getString("Recipe");elapsed=n.getInt("Elapsed");selection=n.getInt("Selection");strikes=n.getInt("Strikes");owner=n.hasUUID("Owner")?n.getUUID("Owner"):null;output=ItemStack.of(n.getCompound("Output"));}
    @Override public CompoundTag getUpdateTag(){return saveWithoutMetadata();}
    @Override public ClientboundBlockEntityDataPacket getUpdatePacket(){return ClientboundBlockEntityDataPacket.create(this);}
}
