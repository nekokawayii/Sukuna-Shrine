package character.mainline;

/** 可脱离游戏运行的共享成长规则；技能号必须结合角色解释。 */
public final class MainlineRules {
    private static final java.util.Set<String> EXTRA_DOMAINS=java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** 扩展角色在公共初始化阶段登记一次，两端保持一致。 */
    public static void registerDomain(String role,int skill){
        if(role==null||role.isBlank()||skill<0)throw new IllegalArgumentException("领域角色和技能号无效");
        EXTRA_DOMAINS.add(role+"#"+skill);
    }
    public static boolean domainSkill(String role,int id){
        if("toji".equals(role))return false; // 连招 5/12 不是旧术式编号中的领域。
        if(EXTRA_DOMAINS.contains(role+"#"+id))return true;
        if(id<1000)return id==5||id==12||id==40||id==41||id==74||id==107;
        return switch(role){
            case "jogo" -> id==1003;
            case "yuta" -> id==1002;
            case "yuki" -> id==2004;
            case "domainking" -> id>=2000&&id<=2006&&id!=2005;
            case "mahito" -> id==2101;
            case "higuruma" -> id==2302;
            case "ryu" -> id==2704;
            case "naoya" -> id==2901;
            case "hakari" -> id==3006;
            default -> false;
        };
    }
    public static boolean below(float health,float maximum,float fraction){return Float.isFinite(health)&&Float.isFinite(maximum)&&maximum>0&&health>0&&health<maximum*fraction;}
    public static int nextCount(int count,int cap){return count>=cap?cap:Math.max(0,count)+1;}
    public static boolean inSimple(double dx,double dy,double dz){return Double.isFinite(dx)&&Double.isFinite(dy)&&Double.isFinite(dz)&&dx*dx+dz*dz<=100&&Math.abs(dy)<=3;}
    private MainlineRules() { }
}
