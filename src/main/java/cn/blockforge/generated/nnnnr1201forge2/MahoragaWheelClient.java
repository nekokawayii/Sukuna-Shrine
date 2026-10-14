package cn.blockforge.generated.nnnnr1201forge2;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** 客户端记录"谁戴着法轮、法轮转到第几格"，并在转格时播放**自绘的**转动特效。
 *
 *  <p>服务端在每次转格（{@link MahoragaVisuals#WHEEL_ROTATE}）与每秒心跳
 *  （{@link MahoragaVisuals#WHEEL_BEAT}）时把（召唤者实体 id、已转格数、当前类型）发过来；
 *  这里只维护状态并转发给 {@link MahoragaWheelRenderer} 绘制，不发包、不参与判定。</p> */
public final class MahoragaWheelClient {
    /** 一圈转格的过渡时长（客户端刻）：受击转格后法轮平滑转到新角度，而不是瞬间跳过去。 */
    private static final int SPIN_TICKS=20;
    /** 转动光环的存活刻数（重做的特效，自绘顶点环）。 */
    public static final int RING_TICKS=16;
    /** 一条记录：owner 是戴法轮的玩家实体 id。 */
    public static final class Entry {
        public final int owner;
        public int cells;       // 目标格数 0~4（4 = 完全适应）
        public float shown;     // 当前显示的格数（带过渡，给动画时间用）
        public float spin=1f;   // 最近的转格过渡进度 0~1
        public int type=-1;     // 正在适应的攻击类型
        public int seen;        // 最近一次收到心跳的客户端刻
        public int ring;        // 转动光环剩余刻数（>0 时画环）
        public float flash=1f;  // 本次光环的亮度（转格 1.0，普通受力 0.55）
        Entry(int owner){this.owner=owner;}
        /** 转动光环的进度 0~1。 */
        public float ringProgress(){return 1f-Math.max(0,ring)/(float)RING_TICKS;}
    }
    private static final Map<Integer,Entry> entries=new ConcurrentHashMap<>();
    private static int time;
    /** 每次转格（法轮动一下）：更新格数并点火转动光环。 */
    public static void rotate(Network.Effect e){
        Entry entry=entries.computeIfAbsent(e.owner(),Entry::new);
        int packed=Math.max(0,(int)e.size());
        int cells=Math.min(MahoragaEntity.CELLS,packed/16);
        boolean turned=cells!=entry.cells;
        if(turned){entry.cells=cells;entry.spin=0f;} // 转格：重新起一段过渡
        entry.type=(int)e.seed();
        entry.seen=time;
        // 重做的特效：每次收到"法轮被推动"就点亮一圈扩散金环（转格时更亮更长）
        entry.ring=Math.max(entry.ring,turned?RING_TICKS:RING_TICKS/2);
        entry.flash=turned?1f:0.55f;
        System.out.println("[SUKUNA-WHEEL] rotate owner="+e.owner()+" cells="+cells+" turned="+turned+" ring="+entry.ring);
    }
    /** 心跳：只续租，不动动画。 */
    public static void beat(Network.Effect e){
        Entry entry=entries.computeIfAbsent(e.owner(),Entry::new);
        entry.seen=time;
        if(entry.type<0)entry.type=(int)e.seed();
    }
    /** 本地触发一次转动光环（例如玩家自己戴上法轮时）。 */
    public static void spin(int owner){entries.computeIfAbsent(owner,Entry::new).ring=RING_TICKS;}
    public static void stop(int owner){entries.remove(owner);}
    public static void clear(){entries.clear();time=0;}
    public static void tick(){
        time++;
        entries.values().removeIf(e->time-e.seen>60); // 3 秒收不到心跳就当仪式结束，法轮自己消失
        for(Entry entry:entries.values()){
            if(entry.ring>0)entry.ring--;
            if(entry.spin<1f)entry.spin=Math.min(1f,entry.spin+1f/SPIN_TICKS);
            float target=Math.min(1f,entry.cells/(float)MahoragaEntity.CELLS);
            entry.shown+=(target-entry.shown)*0.22f;
            if(Math.abs(target-entry.shown)<0.002f)entry.shown=target;
        }
    }
    public static Iterable<Entry> entries(){return entries.values();}
    private MahoragaWheelClient(){}
}
