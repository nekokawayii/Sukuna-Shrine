package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** 调伏仪式的入口与查询：召唤、失败判定文案、法轮的进度播报。 */
public final class MahoragaRitual {
    /** 半径内已经有魔虚罗在活动时不再重复召唤。 */
    private static final double EXISTING_RANGE=200;
    public static boolean summon(ServerPlayer owner){
        ServerLevel level=owner.serverLevel();
        List<MahoragaEntity> existing=level.getEntitiesOfClass(MahoragaEntity.class,owner.getBoundingBox().inflate(EXISTING_RANGE),LivingEntity::isAlive);
        if(!existing.isEmpty()){
            owner.displayClientMessage(Component.literal("附近已经有魔虚罗在活动，先解决它再开新的仪式。"),true);
            return false;
        }
        Vec3 at=owner.position();
        MahoragaEntity boss=VoidContent.MAHORAGA.get().create(level);
        if(boss==null)return false;
        boss.moveTo(at.x,at.y,at.z,owner.getYRot()+180,0);
        level.addFreshEntity(boss);
        boss.beginRitual(owner);
        return true;
    }
    /** 法轮状态播报：玩家据此判断还剩几种攻击类型可用。 */
    public static void report(ServerPlayer p){
        List<MahoragaEntity> found=p.serverLevel().getEntitiesOfClass(MahoragaEntity.class,p.getBoundingBox().inflate(256),LivingEntity::isAlive);
        if(found.isEmpty()){p.displayClientMessage(Component.literal("附近没有魔虚罗。"),true);return;}
        MahoragaEntity boss=found.get(0);
        p.sendSystemMessage(Component.literal("魔虚罗 · 头顶法轮 "+boss.wheelCells()+"/"+MahoragaEntity.CELLS
            +" 格（每 "+MahoragaEntity.HITS_PER_TURN+" 次受击转 1 格，转满 "+MahoragaEntity.CLEAR_HITS+" 次完全适应）"
            +(boss.wheelType()>=0?" · 正在适应「"+AttackTypes.name(boss.wheelType())+"」":"")+" · 生命 "+BalanceRules.number(boss.getHealth())));
        // 每条细分各自一条进度：解 / 捌 / 解·网分支 / 苍 / 赫 / 茈 …
        StringBuilder detail=new StringBuilder("适应进度：");
        for(int i=0;i<AttackTypes.SLOTS;i++){
            int hits=boss.hits(AttackTypes.ALL[i]);
            detail.append(AttackTypes.NAMES[i]).append(hits>=MahoragaEntity.CLEAR_HITS?"×":hits+"/"+MahoragaEntity.CLEAR_HITS).append(' ');
        }
        p.sendSystemMessage(Component.literal(detail.toString().trim()));
        int left=0;
        for(int i=0;i<AttackTypes.SLOTS;i++)if(boss.hits(AttackTypes.ALL[i])<MahoragaEntity.CLEAR_HITS)left++;
        p.sendSystemMessage(Component.literal("尚未被完全适应的招式："+left+" / "+AttackTypes.SLOTS+"（每条独立计数，打满一条只免疫那一条）"));
    }
    private MahoragaRitual(){}
}
