package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;
import net.minecraft.server.level.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

/** 服务端保存付费线段，拒绝越界、重复会话和未付费发射。 */
public final class FreehandDrawing {
    public static final int BEGIN=0,SEGMENTS=1,PAUSE=2,RESUME=3,FIRE=4,CANCEL=5;
    private static final class Drawing {
        final ServerPlayer p;final ServerLevel level;final UUID id;final int width,height;final List<SlashFormRules.Segment> lines=new ArrayList<>();
        double pixels;boolean holding=true,ready;long released,last;
        Drawing(ServerPlayer p,Network.DrawInput m){this.p=p;level=p.serverLevel();id=m.id();width=m.width();height=m.height();last=level.getGameTime();}
    }
    private static final Map<UUID,Drawing> drawings=new HashMap<>();
    private static boolean allowed(ServerPlayer p){return Journey.unlocked(p)&&p.isAlive()&&!p.isRemoved()&&!p.isSpectator()&&Curse.canUseTechnique(p)&&!SoulCombat.suppressed(p)&&!DismantleMotion.active(p)&&!EternalLove.settling(MaidBridge.body(p))&&!Skills.sealing(p);}
    private static void reply(ServerPlayer p,UUID id,int state,double pixels){Network.CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new Network.DrawState(id,state,pixels));}
    public static void input(ServerPlayer p,Network.DrawInput m){
        if(m.operation()==CANCEL){Drawing d=drawings.get(p.getUUID());if(d!=null&&d.id.equals(m.id()))cancel(p);return;}
        if(!allowed(p)){cancel(p);reply(p,m.id(),0,0);CastFeedback.unavailable(p,0);return;}
        if(m.operation()==BEGIN){
            if(m.width()<64||m.width()>16384||m.height()<64||m.height()>16384||drawings.containsKey(p.getUUID()))return;
            if(CastFeedback.cooldown(p,"FreehandCooldown","纵心")){reply(p,m.id(),0,0);return;}
            Drawing d=new Drawing(p,m);drawings.put(p.getUUID(),d);reply(p,d.id,1,0);return;
        }
        Drawing d=drawings.get(p.getUUID());if(d==null||!d.id.equals(m.id())||d.level!=p.level())return;
        long now=p.level().getGameTime();d.last=now;
        switch(m.operation()){
            case SEGMENTS->{
                if(!d.holding||d.ready)return;
                double cost=0,budget=Curse.energy(p);
                for(var s:m.segments()){
                    if(!s.valid()||Math.abs(s.x1())>d.width/2.0||Math.abs(s.x2())>d.width/2.0||Math.abs(s.y1())>d.height/2.0||Math.abs(s.y2())>d.height/2.0){cancel(p);return;}
                    double n=s.length();if(d.lines.size()>=SlashFormRules.MAX_SEGMENTS||d.pixels+n>SlashFormRules.MAX_PIXELS+0.001){d.ready=true;d.holding=false;break;}
                    // 每一个屏幕像素扣一实际咒力，不受GUI缩放或整合包倍率影响。
                    if(cost+n>budget+0.0001){d.ready=true;d.holding=false;break;}d.lines.add(s);d.pixels+=n;cost+=n;
                    if(d.pixels>=SlashFormRules.MAX_PIXELS-0.001){d.ready=true;d.holding=false;break;}
                }
                if(cost>0&&!Curse.spendEffective(p,cost)){cancel(p);return;}
                reply(p,d.id,d.ready?3:1,d.pixels);
            }
            case PAUSE->{if(d.holding){d.holding=false;d.released=now;}reply(p,d.id,d.ready?3:2,d.pixels);}
            case RESUME->{if(!d.ready&&!d.holding&&now-d.released<SlashFormRules.RESUME_TICKS){d.holding=true;reply(p,d.id,1,d.pixels);}}
            case FIRE->{if(!d.holding&&(d.ready||now-d.released>=SlashFormRules.RESUME_TICKS)){if(!d.lines.isEmpty()){drawings.remove(p.getUUID());TechniqueInput.withIntent(p,new TechniqueInput.Input(SlashFormRules.FREEHAND,0,m.forward(),m.backward(),m.jump(),m.crouch()),()->SlashForms.freehand(p,List.copyOf(d.lines),d.height,d.pixels));Curse.status(p,"FreehandCooldown",10);}drawings.remove(p.getUUID());reply(p,d.id,0,d.pixels);}}
            default->{}
        }
    }
    @SubscribeEvent public static void tick(TickEvent.ServerTickEvent e){if(e.phase!=TickEvent.Phase.END)return;for(Drawing d:List.copyOf(drawings.values())){
        long now=d.level.getGameTime();if(!allowed(d.p)||d.p.level()!=d.level||now-d.last>2400){cancel(d.p);continue;}
        if(!d.holding&&!d.ready&&now-d.released>=SlashFormRules.RESUME_TICKS){d.ready=true;reply(d.p,d.id,3,d.pixels);}
    }}
    public static void cancel(ServerPlayer p){Drawing d=drawings.remove(p.getUUID());if(d!=null)reply(p,d.id,0,d.pixels);}
    public static void clear(){drawings.clear();}
    private FreehandDrawing(){}
}
