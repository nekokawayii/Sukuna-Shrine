package character;

/** 自定义即时技能的数据。编号只在当前角色内使用，不占用旧术式编号。 */
public record CharacterSkill(int id, String name, String label, String description,
        int color, int cost, int cooldown, Shape shape, double range, double radius,
        double halfAngle, double damage, double damagePerChapter, int targets,
        int targetsPerChapter, int slowTicks, int slowAmplifier) {
    public static final int FIRST = 1000, LAST = 9999;
    public enum Shape { RAY, CONE, BURST }
    public CharacterSkill {
        if (!custom(id)) throw new IllegalArgumentException("自定义技能编号必须为1000至9999");
        if (name == null || name.isBlank() || label == null || label.isBlank() || description == null || shape == null)
            throw new IllegalArgumentException("技能名称、轮盘短名、说明和形状不能为空");
        if (cost < 0 || cost > 1000000 || cooldown < 1 || cooldown > 72000)
            throw new IllegalArgumentException("技能消耗应为0至1000000，冷却应为1至72000刻");
        finite(range, .5, 32, "距离"); finite(radius, .05, 8, "半径");
        finite(halfAngle, 1, 180, "扇形半角"); finite(damage, 0, 100000, "伤害");
        finite(damagePerChapter, 0, 100000, "每章伤害增量");
        if (targets < 1 || targets > 32 || targetsPerChapter < 0 || targetsPerChapter > 32 || slowTicks < 0 || slowTicks > 1200 || slowAmplifier < 0 || slowAmplifier > 4)
            throw new IllegalArgumentException("目标数限1至32，每章增加0至32；减速限0至1200刻、等级参数0至4");
    }
    private static void finite(double value, double min, double max, String name) {
        if (!Double.isFinite(value) || value < min || value > max) throw new IllegalArgumentException(name+"超出模板范围");
    }
    public static boolean custom(int id) { return id >= FIRST && id <= LAST; }
    public double damageAt(int chapter) { return damage + damagePerChapter * Math.max(0, Math.min(128, chapter)); }
    public int targetsAt(int chapter) { return Math.min(32, targets + targetsPerChapter * Math.max(0, Math.min(128, chapter))); }
}
