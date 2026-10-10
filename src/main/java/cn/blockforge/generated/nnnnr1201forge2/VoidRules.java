package cn.blockforge.generated.nnnnr1201forge2;

/** 战斗边界和时间独立于渲染，便于核对。 */
public final class VoidRules {
    public static final float HEALTH=150000;
    public static int phase(float health,float maximum){return health>maximum*0.60f?1:health>maximum*0.30f?2:3;}
    public static int[] skills(int phase){return phase==1?new int[]{1,2,3,4,5,6}:phase==2?new int[]{7,8,9,10,11,12,13,14}:new int[]{15,17,18,19,20,21};}
    public static int duration(int skill){return switch(skill){case 3,10->160;case 4,13,15,17,11->100;case 6->200;case 14->120;case 20->300;default->40;};}
    public static boolean rebirth(float health,float max,boolean used){return !used&&health>0&&health<max*0.10f;}
    public static final String[] NAMES={"","裂隙斩","裂隙传送","裂隙雨","虚空吞噬","裂隙锁链","虚空之眼","裂隙爆发","虚空分身","裂隙吞噬","虚空风暴","裂隙囚笼","虚空之触","裂隙共鸣","虚空漩涡","虚空终焉","虚空轮回","裂隙洪流","虚空之握","裂隙新星","虚空降临","虚空终斩"};
    public static final String[] TIPS={"","向侧面躲开地裂，踩入裂隙会禁疗","转身远离身后裂隙","持续移动，避开落地标记","远离核心与向内移动的裂隙","横向跑动避开追踪锁链","背对核心之眼","寻找裂隙之间的空地","击杀小型分身，减少地面裂隙","离开脚下漩涡；被吞噬后队友击碎灵魂锚救援","在裂隙间隙躲避，八秒后攻击虚弱核心","在圈内移动，等囚笼消失","斩断紫色触手节点，避开碎裂后的裂隙","远离正在震动的裂隙","向外跑，别被吸进核心","远离坍缩中心，结束后有五秒输出时间","轮回后虚弱十秒，集中攻击","向洪流两侧躲避","离开手掌标记；队友攻击手掌节点可打断","跳跃或位移越过贴地冲击波","持续移动十五秒，等待核心虚弱","离开巨大裂隙标记，再远离爆炸中心"};
}
