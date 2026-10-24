package character.toji;

import character.CharacterService;
import character.CharacterPackets;

import cn.blockforge.generated.nnnnr1201forge2.*;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.*;
import net.minecraft.world.phys.Vec3;
import net.minecraft.sounds.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** 纯肉体技能：独立体术余额，不借用咒力，不触发咒术伤害；敌友仍走统一结算。 */
public final class TojiCombat {
    private static final UUID SPEED=UUID.fromString("480bb88f-333a-45aa-8bbb-001100220001"),HEALTH=UUID.fromString("480bb88f-333a-45aa-8bbb-001100220002"),ATTACK=UUID.fromString("480bb88f-333a-45aa-8bbb-001100220003"),RESIST=UUID.fromString("480bb88f-333a-45aa-8bbb-001100220004");
    public static double maximum(Player p) { return TojiRules.maximum(CharacterService.rank(p)); }
    public static double stamina(Player p) { var d=Curse.data(p);return TojiRules.current(d.contains("TojiStamina")?d.getDouble("TojiStamina"):maximum(p),CharacterService.rank(p)); }
    private static void modifier(Player p,Attribute attribute,UUID id,double value) {
        var a=p.getAttribute(attribute);if(a==null)return;var old=a.getModifier(id);
        if(old!=null&&Math.abs(old.getAmount()-value)<0.000001)return;
        a.removeModifier(id);if(value!=0)a.addTransientModifier(new AttributeModifier(id,"天宇暴君肉体",value,AttributeModifier.Operation.ADDITION));
    }
    public static void attributes(Player p) {
        boolean active=CharacterService.toji(p);int r=TojiRules.rank(CharacterService.rank(p));
        modifier(p,Attributes.MAX_HEALTH,HEALTH,active?20+10*r:0);
        modifier(p,Attributes.ATTACK_DAMAGE,ATTACK,active?4+2*r:0);
        modifier(p,Attributes.MOVEMENT_SPEED,SPEED,active?.025+.008*r:0);
        modifier(p,Attributes.KNOCKBACK_RESISTANCE,RESIST,active?.2+.1*r:0);
        if(p.getHealth()>p.getMaxHealth())p.setHealth(p.getMaxHealth());
    }
    public static void cast(ServerPlayer p,int kind) {
        if(!CharacterService.toji(p)||kind<0||kind>2||!p.isAlive()||p.isSpectator()||p.isPassenger()||p.isSleeping()||p.isUsingItem())return;
        if(PrisonSystem.locked(p)||ObserverRitual.active(p)||GojoEntity.mindLocked(p)||GuardianPlayerSkills.locked(p)||SixfoldFinale.controlled(p)||DomainClash.active(p)||Uraume.locked(p)||NetBranches.locked(p)||CombatExpansion.controlled(p)||DismantleMotion.active(p))return;
        if(Curse.active(p,"TojiCooldown")){p.displayClientMessage(Component.literal("体术尚未收招"),true);return;}
        int rank=TojiRules.rank(CharacterService.rank(p));
        if(!TojiRules.canSpend(stamina(p),rank,kind)){p.displayClientMessage(Component.literal("体术不足，停手稍候恢复"),true);return;}
        if(kind==2&&(!p.onGround()&&p.fallDistance>1.5f||Curse.data(p).getBoolean("TojiAirUsed"))){p.displayClientMessage(Component.literal("跃身需要落地后再用"),true);return;}
        Vec3 direction=p.getLookAngle().multiply(1,0,1).normalize();
        if(direction.lengthSqr()<.1)direction=new Vec3(-Math.sin(Math.toRadians(p.getYRot())),0,Math.cos(Math.toRadians(p.getYRot())));
        Vec3 start=p.position(),destination=start;
        if(kind==0){
            Vec3 step=direction.scale(.25);
            for(int i=0;i<(int)(TojiRules.dash(rank)*4);i++){
                Vec3 next=destination.add(step);var box=p.getBoundingBox().move(next.subtract(start));
                if(!p.serverLevel().hasChunkAt(net.minecraft.core.BlockPos.containing(next))||!p.level().getWorldBorder().isWithinBounds(box)||!p.level().noCollision(p,box))break;
                destination=next;
            }
            if(destination.distanceToSqr(start)<.04){p.displayClientMessage(Component.literal("前方被挡住，未消耗体术"),true);return;}
        }
        Curse.data(p).putDouble("TojiStamina",stamina(p)-TojiRules.cost(kind));
        Curse.status(p,"TojiCooldown",TojiRules.cooldown(kind));Curse.status(p,"TojiRecoveryDelay",30);
        p.swing(InteractionHand.MAIN_HAND,true);
        if(kind==0){
            p.teleportTo(destination.x,destination.y,destination.z);
            int count=Math.max(1,(int)(destination.distanceTo(start)*5));
            for(int i=0;i<=count;i++){Vec3 at=start.lerp(destination,i/(double)count);p.serverLevel().sendParticles(ParticleTypes.CLOUD,at.x,at.y+.7,at.z,2,.12,.2,.12,.015);}
            strike(p,rank,direction,false);
        }else if(kind==1){strike(p,rank,direction,true);}
        else {
            p.setDeltaMovement(direction.scale(.65+.1*rank).add(0,.65+.08*rank,0));p.hurtMarked=true;p.connection.send(new ClientboundSetEntityMotionPacket(p));
            Curse.data(p).putBoolean("TojiAirUsed",true);Curse.status(p,"TojiLeapLock",10);p.fallDistance=0;
            p.serverLevel().sendParticles(ParticleTypes.CLOUD,p.getX(),p.getY(),p.getZ(),18,.45,.1,.45,.04);
        }
        p.serverLevel().playSound(null,p.blockPosition(),kind==1?SoundEvents.PLAYER_ATTACK_STRONG:SoundEvents.PLAYER_ATTACK_SWEEP,SoundSource.PLAYERS,.9f,kind==1?.7f:1.3f);
        CharacterPackets.sync(p,false);
    }
    private static void strike(ServerPlayer p,int rank,Vec3 direction,boolean heavy) {
        double reach=TojiRules.reach(rank)+(heavy?.6:0);
        var targets=p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(reach),t->CombatSettings.canHit(p,t)&&p.hasLineOfSight(t)&&p.distanceToSqr(t)<=reach*reach);
        targets.sort(java.util.Comparator.comparingDouble(p::distanceToSqr));int hits=0;
        for(var target:targets){
            Vec3 delta=target.position().subtract(p.position()).multiply(1,0,1).normalize();
            if(delta.lengthSqr()>.01&&direction.dot(delta)<(heavy?.3:.55))continue;
            AttackTypes.mark(AttackTypes.UNARMED);
            float amount=(float)(p.getAttributeValue(Attributes.ATTACK_DAMAGE)+(heavy?8+3*rank:3+rank));
            if(CombatSettings.hurt(p,target,p.damageSources().playerAttack(p),amount)){
                target.knockback(heavy?1.1:.5,-direction.x,-direction.z);
                p.serverLevel().sendParticles(ParticleTypes.CRIT,target.getX(),target.getY()+target.getBbHeight()*.6,target.getZ(),heavy?20:10,.3,.3,.3,.15);
            }
            if(++hits>=(heavy?2+rank:1))break;
        }
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent e) {
        if(e.phase!=TickEvent.Phase.END||!(e.player instanceof ServerPlayer p)||p.tickCount%10!=0)return;
        attributes(p);if(!CharacterService.toji(p)||!p.isAlive())return;
        if(p.onGround()&&!Curse.active(p,"TojiLeapLock"))Curse.data(p).remove("TojiAirUsed");
        if(!Curse.active(p,"TojiRecoveryDelay"))Curse.data(p).putDouble("TojiStamina",Math.min(maximum(p),stamina(p)+TojiRules.recover(CharacterService.rank(p))*.5));
        CharacterPackets.sync(p,false);
    }
    @SubscribeEvent public static void fall(LivingFallEvent e) { if(e.getEntity() instanceof Player p&&CharacterService.toji(p))e.setDistance(Math.max(0,e.getDistance()-6-TojiRules.rank(CharacterService.rank(p))*2)); }
    private TojiCombat() {}
}
