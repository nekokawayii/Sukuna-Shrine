package cn.blockforge.generated.nnnnr1201forge2;

/** 轮盘绘制与鼠标命中共用角度计算；未解锁的灵魂剥离不会占用轮盘扇区。 */
public final class WheelSelection {
    private int selected;private boolean confirmed;private final int count;
    public WheelSelection(int initial){this(initial,SKILL_COUNT);}
    public WheelSelection(int initial,int count){this.count=count;selected=initial>=0&&initial<count?initial:0;}
    public int selected(){return selected;}
    public int confirm(){confirmed=true;return selected;}
    public void move(double dx,double dy,double inner,double outer){double r=dx*dx+dy*dy;if(confirmed||r<inner*inner||r>outer*outer)return;selected=sector(dx,dy,count);}
    public static int sector(double dx,double dy){return sector(dx,dy,SKILL_COUNT);}
    public static int sector(double dx,double dy,int count){double full=Math.PI*2,step=full/count;double angle=(Math.atan2(dy,dx)+Math.PI/2+step/2+full)%full;return (int)(angle/step)%count;}
    public static final int SKILL_COUNT=7;
}
