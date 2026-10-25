package cn.blockforge.generated.nnnnr1201forge2;

/** 网络传输使用明确名称，不再将技能下标和按键操作混在同一个整数里。 */
public enum SkillAction {
    DISMANTLE, SLASH_NET, CLEAVE, FLAME_BOW, FURNACE, DOMAIN, SOUL_PEEL,
    CHARGE_START, CHARGE_RELEASE, ROTATE, CANCEL_CHARGE, CLEAVE_START, CLEAVE_RELEASE, TOGGLE_FIST,
    GUARDIAN_PUNCH, GUARDIAN_DIVERGENT, GUARDIAN_BLOOD, GUARDIAN_BOOST, GUARDIAN_GRID,
    GUARDIAN_DOMAIN, GUARDIAN_COMBO, GUARDIAN_REVERSE, GUARDIAN_GUARD, GUARDIAN_BEAM,
    REVERSE, REPAIR_BURNOUT, DOMAIN_AMPLIFICATION, CROSS_SLASH,
    GOJO_BLUE, GOJO_RED, GOJO_RED_MAX, GOJO_PURPLE, GOJO_UNLIMITED, GOJO_TELEPORT, GOJO_MELEE, GOJO_DOMAIN, GOJO_BLUE_MAX, SWORD_QI, SPIDER_WEB, MULTI_SLASH, SLASH_WALL, FROST_CALM, ICE_FALL, TOGGLE_DISMANTLE, SHAPE_SLASH, CROSS_FLIGHT, FREEHAND, WORLD_SLASH, MOTION_BACKFLIP, MOTION_KICK, SPIN_SLASH, LINGER_SLASH, PURPLE_FURNACE, DRAGON_STAR, CLEAVE_LINGER, SIXFOLD_FINALE, BLACK_ROPE;

    public static final int SKILL_COUNT=41;
    public static boolean isSkill(int skill){return skill>=0&&skill<SKILL_COUNT;}
    public static SkillAction forSkill(int skill){
        return switch(skill){
            case 0 -> DISMANTLE;
            case 1 -> SLASH_NET;
            case 2 -> CLEAVE;
            case 3 -> FLAME_BOW;
            case 4 -> FURNACE;
            case 5 -> DOMAIN;
            case 6 -> SOUL_PEEL;
            case 7 -> GUARDIAN_PUNCH;
            case 8 -> GUARDIAN_DIVERGENT;
            case 9 -> GUARDIAN_BLOOD;
            case 10 -> GUARDIAN_BOOST;
            case 11 -> GUARDIAN_GRID;
            case 12 -> GUARDIAN_DOMAIN;
            case 13 -> GUARDIAN_COMBO;
            case 14 -> GUARDIAN_REVERSE;
            case 15 -> GUARDIAN_GUARD;
            case 16 -> GUARDIAN_BEAM;
            case 17 -> CROSS_SLASH;
            case 18 -> SWORD_QI;
            case 19 -> SPIDER_WEB;
            case 20 -> MULTI_SLASH;
            case 21 -> SLASH_WALL;
            case 22 -> FROST_CALM;
            case 23 -> ICE_FALL;
            case 24 -> SHAPE_SLASH;
            case 25 -> CROSS_FLIGHT;
            case 26 -> FREEHAND;
            case 27 -> WORLD_SLASH;
            case 28 -> SPIN_SLASH;
            case 29 -> LINGER_SLASH;
            case 30 -> PURPLE_FURNACE;
            case 31 -> DRAGON_STAR;
            case 32 -> CLEAVE_LINGER;
            case 33 -> SIXFOLD_FINALE;
            case 34 -> GOJO_BLUE;
            case 35 -> GOJO_RED;
            case 36 -> GOJO_RED_MAX;
            case 37 -> GOJO_PURPLE;
            case 38 -> GOJO_UNLIMITED;
            case 39 -> GOJO_TELEPORT;
            case 40 -> GOJO_MELEE;
            case 41 -> GOJO_DOMAIN;
            case 42 -> GOJO_BLUE_MAX;
            default -> throw new IllegalArgumentException("无效技能："+skill);
        };
    }
}
