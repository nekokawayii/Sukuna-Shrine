package cn.blockforge.generated.nnnnr1201forge2;

import java.util.*;

/** 服务端持有战斗状态；桌面坐标以游戏内容区宽高归一化，可越过 0..1。 */
public final class ObserverBattleRules {
    public static final int SHIELD=15,HITS_TO_WIN=20,INTRO=260;
    public static final int INTRO_PHASE=0,FIGHT=1,SHATTER=2,FAILED=3;
    public static final int CURSOR=0,WEAK=1,KEY=2,RESET=3,ABORT=4,DESKTOP=5,CONTACT=6,WINDOW_MOVE=7;
    public static final int SLASH=0,WORLD=1,DOMAIN=2,IMPACT=3,MELEE=4,SUPPORT=5;
    public static final int MELEE_REACTION=7,QTE_MIN=4,QTE_MAX=10;
    public static final float MELEE_RADIUS=.115f;
    public record Blade(float x,float y,int life,int style,int id,float vx,float vy) {
        public Blade(float x,float y,int life,int style){this(x,y,life,style,0,0,0);}
    }
    public record Move(String name,int style,int cooldown) {}
    public int age,phase,damage,hits,weakId,weakPart,weakUntil,attack,attackUntil,keysDone,staggerUntil,castSerial;
    public int[] keys={65,66,67,68};
    public String move="断线";
    public final List<Blade> blades=new ArrayList<>();
    private final Random random;
    private final List<Move> moves;
    private final Map<Move,Integer> cooldowns=new HashMap<>(),lastCast=new HashMap<>();
    private int nextAttack=INTRO+50,nextWeak=INTRO+65,lastHurt=-30,lastCursor=-1,nextImpact=INTRO+220;
    private int nextWorld=INTRO+240,nextDomain=INTRO+420,nextMelee=INTRO+140,nextBlade,lastWindow=-1,lastDesktop=-100;
    public float meleeX,meleeY;
    private float cursorX=.5f,cursorY=.5f,shake,windowTravel;
    private boolean desktop;
    public ObserverBattleRules(long seed,List<Move> moves){
        random=new Random(seed);this.moves=new ArrayList<>(moves);Collections.shuffle(this.moves,random);
        if(moves.stream().noneMatch(m->m.style==WORLD))nextWorld=Integer.MAX_VALUE;
        if(moves.stream().noneMatch(m->m.style==DOMAIN))nextDomain=Integer.MAX_VALUE;
        if(moves.stream().noneMatch(m->m.style==MELEE))nextMelee=Integer.MAX_VALUE;
    }
    public boolean windowed(){return hits>5;}
    public boolean desktop(){return desktop&&windowed()&&age-lastDesktop<60;}
    public void desktop(boolean on){desktop=on;lastDesktop=age;}
    public int percent(){return Math.max(0,100-hits*5);}
    public int shield(){return Math.max(0,SHIELD-damage);}
    public boolean weak(){return phase==FIGHT&&age<weakUntil&&age>=staggerUntil&&attackUntil==0;}
    public void cursor(float x,float y){
        if(!Float.isFinite(x)||!Float.isFinite(y)||lastCursor==age)return;
        float low=desktop()?-8:0,high=desktop()?8:1;
        x=Math.max(low,Math.min(high,x));y=Math.max(low,Math.min(high,y));
        if(!windowed()&&attack==WORLD&&attackUntil>age)shake+=Math.min(.35f,Math.abs(x-cursorX)+Math.abs(y-cursorY));
        cursorX=x;cursorY=y;lastCursor=age;
    }
    /** 只接受本刻窗口位移；自动抖动不由客户端上报。 */
    public void windowMove(float dx,float dy){
        if(!windowed()||phase!=FIGHT||lastWindow==age||!Float.isFinite(dx)||!Float.isFinite(dy))return;
        lastWindow=age;
        if(attack==WORLD&&attackUntil>age)windowTravel+=Math.min(.12f,(float)Math.hypot(dx,dy));
    }
    /** 鼠标与已经显示的原生弹幕窗相碰。令牌、寿命、范围和无敌间隔均再检查。 */
    public int contact(int id,float x,float y){
        if(!desktop()||phase!=FIGHT||attackUntil>age||age<staggerUntil||!Float.isFinite(x)||!Float.isFinite(y))return 0;
        for(int i=0;i<blades.size();i++){
            Blade b=blades.get(i);
            if(b.id==id&&b.life>0&&touches(b,x,y)){if(b.style!=11&&b.style!=2)blades.remove(i);return hurt();}
        }
        // 虎杖的桌面窗口同样有接触伤害；仍与弹幕共享受击无敌间隔。
        if(id==-1&&windowed()&&Math.abs(x-1.10f)<.16&&Math.abs(y-.55f)<.22)return hurt();
        return 0;
    }
    public int strike(int token){
        if(!weak()||token!=weakId)return 0;
        hits++;weakUntil=0;nextWeak=age+45;
        if(hits==HITS_TO_WIN){phase=SHATTER;blades.clear();attackUntil=0;}
        return windowed()&&hits%3==0?3:2;
    }
    public int key(int key){
        if(phase!=FIGHT||attack!=DOMAIN||attackUntil<=age||keysDone>=keys.length)return 0;
        if(key<65||key>90)return 0;
        if(key!=keys[keysDone]){keysDone=0;return 0;}
        if(++keysDone==keys.length){attackUntil=0;nextAttack=age+45;return 5;}return 0;
    }
    public static int[] randomKeys(Random random){int[] out=new int[QTE_MIN+random.nextInt(QTE_MAX-QTE_MIN+1)];for(int i=0;i<out.length;i++)out[i]=65+random.nextInt(26);return out;}
    public int tick(){
        age++;
        if(phase==INTRO_PHASE){if(age>=INTRO)phase=FIGHT;return 0;}
        if(phase!=FIGHT)return 0;
        int event=0;
        if(attackUntil>0&&age>=attackUntil){
            if(attack==MELEE)event=Math.hypot(cursorX-meleeX,cursorY-meleeY)>MELEE_RADIUS?6:hurt();
            else if(attack==WORLD&&(windowed()?windowTravel>=.30f:shake>=1.6f||windowTravel>=.30f))event=4;
            else if(attack==WORLD||attack==DOMAIN)event=hurt();
            attackUntil=0;nextAttack=age+35;
        }
        if(phase!=FIGHT)return event;
        if(attackUntil==0&&age>=staggerUntil){
            for(int i=blades.size()-1;i>=0;i--){
                Blade b=blades.get(i);if(b.life<=0){blades.remove(i);continue;}
                double dx=cursorX-b.x,dy=cursorY-b.y,d=Math.hypot(dx,dy);
                if(!desktop()&&(b.style==11?touches(b,cursorX,cursorY):d<.033)){if(b.style!=11)blades.remove(i);int h=hurt();if(h!=0)event=h;if(phase!=FIGHT)return event;continue;}
                // 桌面攻击保留惯性，才能真正越过边界，而不是在鼠标附近停止。
                float vx=b.vx,vy=b.vy;double speed=.012;
                if(b.style==2||b.style==6&&b.life>65){vx=vy=0;}
                else if(b.style!=3&&d>.0001){
                    if(b.style==11)speed=.017;
                    float tx=(float)(dx/d*speed),ty=(float)(dy/d*speed);float steer=b.style==11?.12f:desktop()?.035f:.30f;vx=vx*(1-steer)+tx*steer;vy=vy*(1-steer)+ty*steer;
                }
                blades.set(i,new Blade(b.x+vx,b.y+vy,b.life-1,b.style,b.id,vx,vy));
            }
            if(age>=nextWeak){weakId++;weakPart=random.nextInt(3);weakUntil=age+55;nextWeak=age+95;}
            if(age>=nextAttack){
                int preferred=age>=nextDomain?DOMAIN:age>=nextWorld?WORLD:age>=nextMelee?MELEE:SLASH;
                List<Move> ready=moves.stream().filter(m->(m.style==preferred||preferred==SLASH&&m.style==SUPPORT)&&age>=cooldowns.getOrDefault(m,0)).toList();
                if(ready.isEmpty())ready=moves.stream().filter(m->(m.style==SLASH||m.style==SUPPORT)&&age>=cooldowns.getOrDefault(m,0)).toList();
                if(ready.isEmpty()){nextAttack=age+1;return event;}
                // 同类招式优先用最久没用过的，防止长招式表永远抽不到末尾。
                Move m=ready.stream().min(Comparator.comparingInt(a->lastCast.getOrDefault(a,-1))).orElseThrow();
                move=m.name;attack=m.style;castSerial++;lastCast.put(m,castSerial);
                if(attack==DOMAIN)nextDomain=age+480;
                if(attack==WORLD)nextWorld=age+320;
                if(attack==MELEE){blades.clear();weakUntil=0;attackUntil=age+MELEE_REACTION;meleeX=cursorX;meleeY=cursorY;nextMelee=age+120+random.nextInt(81);}
                else if(attack==WORLD||attack==DOMAIN){
                    blades.clear();weakUntil=0;keys=randomKeys(random);attackUntil=age+(attack==WORLD?65:40+keys.length*12);shake=windowTravel=0;keysDone=0;
                }else if(attack==SUPPORT){
                    blades.clear();weakUntil=0;attackUntil=age+24;nextWeak=age+30;
                }else{
                    int style=visual(move),count=style==3||style==4||style==5?4:style==1||style==8?3:style==7?2:1;
                    for(int i=0;i<count&&blades.size()<6;i++){
                        float x=.5f,y=.45f;double angle=count>1?i*Math.PI*2/count:Math.atan2(cursorY-y,cursorX-x);
                        if(style==2&&desktop()){int side=random.nextInt(4);x=side==0?-.22f:side==1?1.22f:.2f+random.nextFloat()*.6f;y=side==2?-.22f:side==3?1.22f:.2f+random.nextFloat()*.6f;}
                        float speed=style==11?.017f:.022f;
                        blades.add(new Blade(x,y,desktop()?180:110,style,++nextBlade,style==2?0:(float)Math.cos(angle)*speed,style==2?0:(float)Math.sin(angle)*speed));
                    }
                }
                cooldowns.put(m,age+Math.max(1,(int)Math.ceil(m.cooldown*.1)));nextAttack=age+30;
            }
        }
        if(windowed()&&age>=nextImpact&&attackUntil==0){staggerUntil=age+18;nextImpact=age+160;}
        return event;
    }
    /** 同一套图形同时用于游戏内和原生窗口，不只是随机换颜色。 */
    public static int visual(String name){
        if(name.contains("百敛穿血"))return 11;
        if(name.contains("滞留"))return 6;
        if(name.contains("苍"))return 2;
        if(name.contains("赫"))return 3;
        if(name.contains("茈")||name.contains("紫"))return 7;
        if(name.contains("炎")||name.contains("灶")||name.contains("烬")||name.contains("焚"))return 1;
        if(name.contains("霜")||name.contains("瀑"))return 8;
        if(name.contains("血")||name.contains("超新星"))return 9;
        if(name.contains("网")||name.contains("墙")||name.contains("十字"))return 4;
        if(name.contains("星")||name.contains("形状")||name.contains("旋转"))return 5;
        if(name.contains("剥离"))return 10;
        return 0;
    }
    public static boolean touches(Blade b,float x,float y){
        if(b.style!=11)return Math.abs(x-b.x)<.065&&Math.abs(y-b.y)<.065;
        double length=Math.max(.00001,Math.hypot(b.vx,b.vy)),dx=b.vx/length*.38,dy=b.vy/length*.38;
        double t=Math.max(0,Math.min(1,((b.x-x)*dx+(b.y-y)*dy)/(dx*dx+dy*dy)));
        return Math.hypot(x-(b.x-dx*t),y-(b.y-dy*t))<.022;
    }
    private int hurt(){
        if(age-lastHurt<15||phase!=FIGHT)return 0;
        lastHurt=age;damage++;
        if(damage>=SHIELD){phase=FAILED;blades.clear();weakUntil=0;attackUntil=0;}return 1;
    }
    public float dodgeProgress(){return Math.min(1,windowed()?windowTravel/.30f:Math.max(shake/1.6f,windowTravel/.30f));}
}
