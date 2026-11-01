package character;

import cn.blockforge.generated.nnnnr1201forge2.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

/** 给后续角色使用的公共接口；只在服务端施法，不借用在线玩家。 */
public final class TechniqueApi {
    @FunctionalInterface public interface Effect {
        void cast(SkillCaster caster,CharacterSkill skill,int rank);
    }
    private static final java.util.Map<String,java.util.List<java.util.function.Consumer<SkillCaster>>> TICKS=new java.util.HashMap<>();
    /** 持续技能/被动的共同维护；每名真实玩家和每个 NPC 上下文每刻调用一次。 */
    public static void registerTick(String role,java.util.function.Consumer<SkillCaster> ticker){
        if(CharacterRegistry.get(role)==null||ticker==null)throw new IllegalArgumentException("请先登记角色再登记维护函数");
        TICKS.computeIfAbsent(role,k->new java.util.ArrayList<>()).add(ticker);
    }
    public static void tick(ServerPlayer actor){
        var definition=CharacterService.selected(actor);if(definition==null)return;
        for(var ticker:TICKS.getOrDefault(definition.id(),java.util.List.of()))ticker.accept(SkillCaster.of(actor));
    }
    @net.minecraftforge.eventbus.api.SubscribeEvent
    public static void playerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event){
        if(event.phase==net.minecraftforge.event.TickEvent.Phase.END&&event.player instanceof ServerPlayer p
            &&!(p instanceof net.minecraftforge.common.util.FakePlayer))tick(p);
    }
    public static void registerSkill(String role,int id,Effect effect){
        CharacterCasting.registerEffect(role,id,(actor,skill,rank)->effect.cast(SkillCaster.of(actor),skill,rank));
    }
    public static void registerDomain(String role,int id,Effect effect){
        registerSkill(role,id,effect);
        character.mainline.MainlineRules.registerDomain(role,id);
        CharacterCasting.allowWhileMindLocked(role,id);
    }
    /** 给原版、已有 NPC 或第三方生物挂上已注册的角色；只替换术式，不改变外观。 */
    public static boolean bind(Mob mob,String role){
        if(mob==null||mob.level().isClientSide||CharacterRegistry.get(role)==null)return false;
        var previous=NpcBridge.existing(mob);
        if(previous!=null&&!previous.characterId.equals(role)&&DomainClash.hasDomain(previous))return false;
        mob.getPersistentData().putString("NpcTechnique",role);
        return role.equals(NpcBridge.character(mob));
    }
    public static boolean cast(Mob mob,LivingEntity target,int skill){
        return character.npc.NpcCombatCasting.cast(mob,target,skill);
    }
    /** 旧玩家方法的兼容入口；新效果优先使用 SkillCaster.body/spend/hurt。 */
    public static ServerPlayer actor(LivingEntity body){
        return body instanceof ServerPlayer p?p:NpcBridge.actorFor(body);
    }
    public static void opened(LivingEntity owner,Vec3 center,double radius,DomainClash.Lifecycle lifecycle){
        if(lifecycle==null)throw new IllegalArgumentException("扩展领域必须提供生命周期");
        DomainClash.opened(owner,center,radius,lifecycle);
    }
    public static void opened(LivingEntity owner,Vec3 center,double radius,DomainClash.Lifecycle lifecycle,
                              character.domain.DomainPresentation.Style style){
        if(lifecycle==null)throw new IllegalArgumentException("扩展领域必须提供生命周期");
        DomainClash.opened(owner,center,radius,lifecycle,style);
    }
    public static boolean canAffect(SkillCaster caster,LivingEntity target,boolean domain){
        if(caster==null||target==null)return false;
        ServerPlayer actor=actor(caster.body());
        return actor!=null&&!DomainClash.stunned(caster.body())&&!DomainClash.active(caster.body())&&!DomainClash.active(target)
            &&CombatSettings.canHit(actor,target)&&(!domain||!character.mainline.Mainline.simpleProtected(target));
    }
    private TechniqueApi() { }
}
