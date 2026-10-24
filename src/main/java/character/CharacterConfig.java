package character;

import java.util.List;
import java.util.Map;

/** 可复制的角色配置；不依赖客户端或游戏类，服务端与界面使用同一份数据。 */
public record CharacterConfig(Wheel wheel, Stats base, Stats perChapter,
        List<CharacterSkill> customSkills, Map<Integer,Integer> unlockChapters,
        Map<Integer,SkillText> text) {
    public record Wheel(String title, String subtitle, int background, int idle, int selected, int locked, int text) {
        public Wheel { if(title==null||subtitle==null)throw new IllegalArgumentException("轮盘标题不能为空；空字符串表示沿用角色名/术式名"); }
        public static Wheel defaults(int accent) { return new Wheel("", "", 0xD6101520, 0xD12C2B3B, 0xD0000000|(accent&0xFFFFFF), 0xC0272930, 0xFFF7E9EF); }
    }
    /** energy是咒力上限；health是额外生命；attack是额外近战；spell是术式加成；recovery是每秒恢复。 */
    public record Stats(double energy, double health, double attack, double spell, double recovery) {
        public static final Stats ZERO = new Stats(0,0,0,0,0);
        public Stats { for(double n:new double[]{energy,health,attack,spell,recovery})if(!Double.isFinite(n)||n<0||n>1000000)throw new IllegalArgumentException("成长数值需为0至1000000的有限数"); }
        public Stats plus(Stats s) { return new Stats(energy+s.energy,health+s.health,attack+s.attack,spell+s.spell,recovery+s.recovery); }
    }
    /** 这里只换显示，不更改旧技能的实际消耗、伤害或冷却。 */
    public record SkillText(String name, String label, String description, int color) {
        public SkillText { if(name==null||name.isBlank()||label==null||label.isBlank()||description==null)throw new IllegalArgumentException("技能显示文字不能为空"); }
    }
    public CharacterConfig {
        if(wheel==null||base==null||perChapter==null)throw new IllegalArgumentException("轮盘和成长配置不能为空");
        customSkills=List.copyOf(customSkills);unlockChapters=Map.copyOf(unlockChapters);text=Map.copyOf(text);
        if(customSkills.stream().map(CharacterSkill::id).distinct().count()!=customSkills.size())throw new IllegalArgumentException("同一角色的自定义技能编号不能重复");
    }
    public static CharacterConfig legacy(int color) { return new CharacterConfig(Wheel.defaults(color),new Stats(1800,10,2,0,15),new Stats(400,10,2,8,0),List.of(),Map.of(),Map.of()); }
    public CharacterSkill skill(int id) { return customSkills.stream().filter(s->s.id()==id).findFirst().orElse(null); }
    public int unlockChapter(int id) { return unlockChapters.getOrDefault(id,0); }
}
