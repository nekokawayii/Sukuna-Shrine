package cn.blockforge.generated.nnnnr1201forge2;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.effect.*;

public final class ExpeditionItem extends Item {
    public final String id;
    public ExpeditionItem(String id){super(new Item.Properties().stacksTo(id.endsWith("blueprint")||id.endsWith("map")||id.equals("culling_key")?1:64));this.id=id;}
    @Override public void appendHoverText(ItemStack s,Level l,List<Component> lines,TooltipFlag f){lines.add(Component.translatable("hint.sukuna."+id));}
    @Override public InteractionResult useOn(UseOnContext c){
        if(id.equals("simple_domain_disc")&&c.getPlayer() instanceof ServerPlayer p){
            if(p.getCooldowns().isOnCooldown(this)||!p.mayUseItemAt(c.getClickedPos(),c.getClickedFace(),c.getItemInHand()))return InteractionResult.FAIL;
            var at=c.getClickedPos().relative(c.getClickedFace());
            p.getPersistentData().putLong("ExpeditionDiscUntil",p.level().getGameTime()+200);
            p.getPersistentData().putLong("ExpeditionDiscPos",at.asLong());
            p.getPersistentData().putString("ExpeditionDiscDim",p.level().dimension().location().toString());
            consume(p,c.getItemInHand(),20);p.sendSystemMessage(Component.literal("简易领域阵盘展开：10秒内，留在阵盘5格内可抵挡一次致命攻击。"));return InteractionResult.CONSUME;
        }
        return super.useOn(c);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        ItemStack s=player.getItemInHand(hand);if(!(player instanceof ServerPlayer p))return InteractionResultHolder.pass(s);
        if(!p.isAlive()||p.isSpectator()||p.getCooldowns().isOnCooldown(this))return InteractionResultHolder.fail(s);
        switch(id){
            case "soul_capture_bottle"->{if(!SoulEchoes.capture(p)){p.sendSystemMessage(Component.literal("附近5格内没有可捕集的金色灵魂光点。"));return InteractionResultHolder.fail(s);}consume(p,s,20);Journey.give(p,new ItemStack(ExpeditionContent.item("soul_echo")));}
            case "incarnation_reagent"->{ItemStack tool=p.getOffhandItem();if(!tool.isDamageableItem()){p.sendSystemMessage(Component.literal("把需要修复的武器或防具放副手。"));return InteractionResultHolder.fail(s);}tool.setDamageValue(0);consume(p,s,40);}
            case "culling_key"->{CullingGame.enter(p);p.getCooldowns().addCooldown(this,40);return InteractionResultHolder.success(s);}
            case "kenjaku_invitation"->{
                int pulled=0;
                for(ServerPlayer other:p.serverLevel().getEntitiesOfClass(ServerPlayer.class,p.getBoundingBox().inflate(16),
                        o->o!=p&&!o.isSpectator()&&!o.isCreative())){
                    CullingGame.enter(other);pulled++;
                }
                CullingGame.enter(p);
                p.sendSystemMessage(Component.literal("羂索的邀请函展开成一个黑色立方体——附近的 "+pulled+" 名玩家一起被拉入了死灭回游。"));
                consume(p,s,40);
                p.getCooldowns().addCooldown(this,40);
                return InteractionResultHolder.success(s);
            }
            case "reverse_injection"->{p.heal(p.getMaxHealth()*.5f);consume(p,s,40);}
            case "curse_concentrate"->{Curse.add(p,Curse.maximum(p));Curse.sync(p);p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS,600,1));consume(p,s,40);}
            case "blood_sakura_wine"->{p.heal(p.getMaxHealth()*.6f);p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS,100));consume(p,s,40);}
            case "mental_resistance"->{p.getPersistentData().putLong("ExpeditionMindShield",level.getGameTime()+600);p.removeEffect(MobEffects.BLINDNESS);p.removeEffect(MobEffects.CONFUSION);consume(p,s,40);}
            case "cold_resistance"->{p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE,9600));consume(p,s,40);}
            case "special_blueprint"->{ExpeditionProgress.data(p).putBoolean("ExpeditionSpecialBlueprint",true);p.sendSystemMessage(Component.literal("已掌握特级咒具图纸：可在天元降灵坛打造天逆鉾、释魂刀和游云。"));consume(p,s,20);}
            case "alloy_blueprint"->{ExpeditionProgress.data(p).putBoolean("ExpeditionAlloyBlueprint",true);p.sendSystemMessage(Component.literal("高专合金：在淬炼台投入高专原矿×2、铁锭×2、金锭×1、低浓度残秽×3。"));return InteractionResultHolder.success(s);}
            case "soul_edge","poison_coating"->{ItemStack weapon=p.getOffhandItem();if(!(weapon.getItem() instanceof SwordItem)){p.sendSystemMessage(Component.literal("请把要强化的剑放在副手，再右键材料。"));return InteractionResultHolder.fail(s);}if(id.equals("soul_edge"))weapon.enchant(ExpeditionContent.SOUL_EDGE.get(),1);else weapon.getOrCreateTag().putBoolean("ExpeditionPoison",true);consume(p,s,20);}
            case "overtime_binding"->{p.getPersistentData().putLong("ExpeditionOvertime",level.getGameTime()+1200);consume(p,s,20);}
            case "furnace_scroll","ice_scroll","ultimate_scroll"->{
                if(id.equals("ice_scroll")&&!Uraume.unlocked(p)&&!Uraume.consumeMemory(p)){p.sendSystemMessage(Component.literal("冰凝卷轴只回应未选择冲突术式的玩家，或里梅角色。"));return InteractionResultHolder.fail(s);}
                ExpeditionProgress.data(p).putBoolean("ExpeditionScroll_"+id,true);ExpeditionProgress.sync(p);
                if(id.equals("ultimate_scroll")){for(int i:new int[]{character.mainline.Mainline.SORCERER,character.mainline.Mainline.BARRIER,character.mainline.Mainline.SIMPLE,character.mainline.Mainline.DOMAIN})character.mainline.Mainline.unlock(p,i);}
                p.sendSystemMessage(Component.literal(id.equals("furnace_scroll")?"已研习灶开：宿傩可在未展开领域时施放灶，火系伤害提高15%。":id.equals("ice_scroll")?"冰凝咒法已研习，冰伤提高15%。":"极之番已研习：解锁领域资格与宿傩龙之昴星，仍使用当前角色自己的术式。"));consume(p,s,20);}
            case "curtain_talisman"->{p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,200,2));p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,200));consume(p,s,200);}
            case "inferno_bomb"->{var center=p.getEyePosition().add(p.getLookAngle().scale(6));for(var e:level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new net.minecraft.world.phys.AABB(center,center).inflate(5),e->e!=p&&CombatSettings.canHit(p,e))){AttackTypes.mark(AttackTypes.FIRE);if(CombatSettings.hurt(p,e,p.damageSources().playerAttack(p),24))e.setSecondsOnFire(15);}p.serverLevel().sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,center.x,center.y,center.z,120,2,2,2,.1);consume(p,s,40);}
            case "tengen_map"->{p.sendSystemMessage(Component.literal("天元结界图：诅咒废土或枯骨战场，夜间或雷雨中，以7×7高专合金底座、中心降灵核心、八根三格高特级封印柱构阵；柱顶右键镶嵌星铁。"));return InteractionResultHolder.success(s);}
            default->{return InteractionResultHolder.pass(s);}
        }
        return InteractionResultHolder.success(s);
    }
    private void consume(ServerPlayer p,ItemStack s,int cooldown){if(!p.isCreative())s.shrink(1);p.getCooldowns().addCooldown(this,cooldown);}
}
