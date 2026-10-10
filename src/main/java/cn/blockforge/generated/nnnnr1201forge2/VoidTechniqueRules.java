package cn.blockforge.generated.nnnnr1201forge2;

/** 虚空术式的时间、倍率和数量边界。伤害数值以生命点为单位。 */
public final class VoidTechniqueRules {
    public static final int RIFT_TICKS=100, DOMAIN_TICKS=400, FUSION_TICKS=60;
    public static final int RESONANCE_COUNT=5, MAX_RIFTS=256;
    public static final double FIELD_RADIUS=200, FIRE_SCALE=5;
    public static final float FIRE_DAMAGE=40;
    /** 解命中时的即时伤害；裂缝只是后续角度和引爆标记，不再是唯一伤害来源。 */
    public static final float DISMANTLE_DAMAGE=18;
    /** 捌一次接触会生成的斩击数量。 */
    public static final int CLEAVE_SLASHES=3;
    public static boolean pierces(double casterDamage,double targetDamage){return casterDamage>targetDamage;}
    public static boolean resonate(int before,int after){return before<RESONANCE_COUNT&&after>=RESONANCE_COUNT;}
    public static float swallow(float maxHealth){return maxHealth*0.5f;}
    public static float fusion(int rifts){return 80+Math.max(0,rifts)*24f;}
    public static float closure(int rifts){return Math.max(0,rifts)*12f;}
    private VoidTechniqueRules(){}
}
