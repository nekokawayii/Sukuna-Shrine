package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.*;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.*;

/** 所有特殊怪均复用已有实体，保留玩家同款术式接口。生成标记写入实体存档。 */
public final class ExpeditionMobs {
    public static Mob spawn(ServerLevel l,BlockPos pos,String role,ServerPlayer target){
        if(l.getDifficulty()==Difficulty.PEACEFUL||!l.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING))return null;
        EntityType<? extends Mob> type=switch(role){case "young_daitian","special_curse","failed_vessel"->Content.DAITIAN.get();case "renegade"->Content.NANAMI_NPC.get();case "kenjaku"->Content.GETO.get();case "naoya"->Content.NAOYA_NPC.get();case "toji_echo"->Content.TOJI_NPC.get();case "participant_ryu"->Content.RYU_NPC.get();case "participant_kashimo"->Content.KASHIMO_NPC.get();case "participant_mahito"->Content.MAHITO_NPC.get();default->Content.LOW_CURSE.get();};
        Mob m=type==Content.LOW_CURSE.get()?CurseRoster.create(l,role.equals("ash_curse")?CurseCatalog.index("ashen_wraith"):role.equals("wraith")?CurseCatalog.index("bloody_ghost"):CurseCatalog.choose(l.random,role.equals("low_curse"))):type.create(l);if(m==null)return null;
        m.moveTo(pos.getX()+.5,pos.getY(),pos.getZ()+.5,0,0);
        if(!l.noCollision(m)||!l.getFluidState(pos).isEmpty()||!l.getBlockState(pos.below()).isSolidRender(l,pos.below()))return null;
        m.getPersistentData().putString("ExpeditionRole",role);
        String name=switch(role){case "young_daitian"->"咒胎戴天 · 未成熟";case "renegade"->"高专叛逃术师";case "special_curse"->"特级咒灵";case "failed_vessel"->"受肉失败的特级怪物";case "kenjaku"->"羂索的分身";case "toji_echo"->"伏黑甚尔残影";case "ash_curse"->"灰烬咒灵";case "wraith"->"战场怨灵";case "mutant"->"变异咒灵";default->role.startsWith("participant")?"死灭回游参赛者":"低级咒灵";};
        if(!(m instanceof LowCurseEntity))m.setCustomName(Component.literal(name));
        double hp=switch(role){case "young_daitian"->70;case "low_curse","ash_curse","wraith"->32;case "mutant"->90;case "special_curse"->400;case "failed_vessel"->900;default->m.getMaxHealth();};
        if(!(m instanceof LowCurseEntity)){if(m.getAttribute(Attributes.MAX_HEALTH)!=null)m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(hp);m.setHealth(m.getMaxHealth());}
        if(role.equals("young_daitian"))if(m.getAttribute(Attributes.ATTACK_DAMAGE)!=null)m.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4);
        if(target!=null)m.setTarget(target);
        m.setPersistenceRequired();return l.addFreshEntity(m)?m:null;
    }
    public static void ritualWave(ServerLevel l,BlockPos center,int tier,ServerPlayer target){
        if(!l.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBSPAWNING))return;
        int available=24-l.getEntitiesOfClass(Mob.class,new AABB(center).inflate(50),m->m.getPersistentData().getBoolean("ExpeditionRitualMob")).size();
        if(available<=0)return;
        if(target!=null&&(target.level()!=l||!target.isAlive()))target=null;
        var r=l.random;
        for(int i=0;i<Math.min(2+tier,available);i++){double a=r.nextDouble()*Math.PI*2;int d=12+r.nextInt(33);BlockPos x=center.offset((int)(Math.cos(a)*d),0,(int)(Math.sin(a)*d));if(!l.hasChunkAt(x))continue;
            BlockPos p=l.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,x);
            if(Math.abs(p.getY()-center.getY())>50){p=ExpeditionEvents.safePosition(l,x,3);if(p==null)continue;}
            if(p.distSqr(center)>2500)continue;
            Mob m=spawn(l,p,tier==2&&r.nextInt(5)==0?"special_curse":"low_curse",target);if(m!=null){m.getPersistentData().putBoolean("ExpeditionRitualMob",true);m.getPersistentData().putLong("ExpeditionDespawnAt",l.getGameTime()+2400);}
        }
    }
    private ExpeditionMobs(){}
}
