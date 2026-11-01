package cn.blockforge.generated.nnnnr1201forge2;

import java.util.HashMap;
import java.util.Map;

/** 由《探索与祭坛玩法手册》生成的物品描述：只写是什么/从哪来，不写用途。 */
public final class AutoDescriptions {
    private static final Map<String,String> MAP = new HashMap<>();
    static {
        MAP.put("alloy_blueprint", "「高专原矿／合金」：Y=-31至-60深板岩矿；淬炼配方见下。");
        MAP.put("ash_clump", "「灰烬团」：挖火山灰烬、击杀灰烬咒灵。");
        MAP.put("authority_curse", "「诅咒废土」：灰紫天空、黑土、紫色发光裂缝、倒钩状枯树；高级咒灵较多；裂缝掉高浓度残秽。");
        MAP.put("black_rope_fiber", "「黑绳纤维」：剪刀右键黑藤或注连绳；破坏神社注连绳。");
        MAP.put("blood_sakura_petal", "「血樱花瓣」：血樱叶概率掉落。");
        MAP.put("eternal_fire", "「不灭之火」：玻璃瓶右键火山口不灭火种。");
        MAP.put("frame_retina", "「24帧视网膜」：直哉。");
        MAP.put("frozen_crystal", "「极寒冰晶」：里梅。");
        MAP.put("jujutsu_alloy", "「高专原矿／合金」：Y=-31至-60深板岩矿；淬炼配方见下。");
        MAP.put("nine_paintings_manuscript", "「九相图原稿」：实验室。");
        MAP.put("overtime_tie", "「加班领带」：七海。");
        MAP.put("raw_jujutsu_ore", "「高专原矿／合金」：Y=-31至-60深板岩矿；淬炼配方见下。");
        MAP.put("residue", "「低／中／高残秽」：玩家或玩家术式击杀咒灵；普通、精英、特级按生命档分级；废土裂缝掉高浓度。");
        MAP.put("soul_echo", "「灵魂残响」：灵魂通道/岩手附近的金色漂浮光点，5格内右键灵魂捕集瓶。");
        MAP.put("special_blueprint", "「特级咒具图纸」：城市宝箱、实验室、积分塔。");
        MAP.put("star_curse_core", "「星铁／星之咒核」：城市宝箱、积分兑换；9星铁合核心；末影龙/凋灵也掉核心。");
        MAP.put("star_iron_fragment", "「星铁／星之咒核」：城市宝箱、积分兑换；9星铁合核心；末影龙/凋灵也掉核心。");
        MAP.put("transfigured_axe_netherite", "「高专原矿／合金」：Y=-31至-60深板岩矿；淬炼配方见下。");
        MAP.put("transfigured_hoe_netherite", "「高专原矿／合金」：Y=-31至-60深板岩矿；淬炼配方见下。");
        MAP.put("transfigured_pickaxe_netherite", "「高专原矿／合金」：Y=-31至-60深板岩矿；淬炼配方见下。");
        MAP.put("transfigured_shovel_netherite", "「高专原矿／合金」：Y=-31至-60深板岩矿；淬炼配方见下。");
        MAP.put("wraith_tear", "「怨灵之泪」：夜间击杀战场怨灵。");
    }

    public static String get(String path, String fallback) {
        String text = MAP.get(path);
        return text != null ? text : fallback;
    }

    private AutoDescriptions() {}
}
