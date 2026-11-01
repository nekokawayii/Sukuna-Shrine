package cn.blockforge.generated.nnnnr1201forge2;

import java.util.List;

/** 公开玩法手册内容：分类 + 每类若干行；独立 GUI 与 Patchouli 版共用这一份。 */
public final class ManualContent {
    /** 手册专用 Effect 号（打开自带 GUI）。 */
    public static final int EFFECT_OPEN = 560;

    public record Section(String title, List<String> lines) {}

    public static final List<Section> SECTIONS = List.of(
        new Section("入门与流程", List.of("开局拿到《咒术选择书》右键选角色", "每个角色 3 章剧情，靠击杀与技能推进", "N 选技能 / R 释放 / L 共同主线 / J 个人线", "开局会收到本手册；装帕秋莉时右键是帕秋莉版")),
        new Section("角色（一）", List.of("宿傩：解 / 捌 / 开 / 灶 / 伏魔御厨子", "五条悟：六眼九式 / 无量空处", "里梅：冰凝咒法", "虎杖：咒力强化 + 赤血", "伏黑甚尔：纯 QTE 连招")),
        new Section("角色（二）", List.of("漏壶：灾厄火焰 / 盖棺铁围山", "真人：无为转变 / 自闭圆盾裹", "伏黑惠：十种影法术", "日车宽见：诛伏赐死 + 判决", "鹿紫云一：幻兽琥珀", "东堂葵：不义游戏", "七海建人：十划咒法")),
        new Section("角色（三）", List.of("石流龙：咒力强化 / 打上花火", "禅院直哉：投射咒法 / 时胞月宫殿", "秤金次：坐杀博徒", "九十九由基：星之怒", "乙骨忧太：真赝相爱", "领域王：万域")),
        new Section("共同主线（L）", List.of("反转术式 / 反转术式外放", "结界术 / 简易领域", "领域展开 / 领域展延（G 键）", "达成条件见 L 面板，解锁后全角色通用")),
        new Section("领域对拼", List.of("两个领域球体重叠 → 按空格比手速", "胜方领域延长 20 秒", "败方领域解除并进入熔断", "秤金次的坐杀博徒对拼必胜")),
        new Section("世界与祭坛", List.of("探索结构：祭祀废墟 / 分校 / 古墓 / 火山祭坛 / 樱花藏身处 / 羂索实验室", "祭坛：潜行右键选配方，普通右键检查阵型并投料，成品右键领取", "蛊毒缸 / 天元核心：另外两种工作台")),
        new Section("物品与材料", List.of("残秽（低/中/高）：击杀咒灵；符纸、合金、原液的底材", "咒力结晶：右键回 150 咒力", "不灭之火 / 灰烬团 / 血樱花瓣：火山与血樱", "虚空钥匙：残渣×4 + 咒力结晶×4 + 金色舍利（可合成）")),
        new Section("首领", List.of("咒胎戴天 → 即身佛 → 五条悟 → 魔虚罗", "虎杖残影（虚空竞技场）→ 净土守护者", "破碎真人 / 寄生佛 / 母体", "全部首领已解除前置限制")),
        new Section("提示", List.of("破坏地形需开启：/sukuna gamerule desblock true", "出问题可 /sukuna reset 复位", "隐藏支线不在本手册范围")),
        new Section("探索与祭坛玩法手册", List.of("完整资料见仓库根目录的", "01-入门与全部角色流程 ~ 07-内容覆盖与验收", "以及附表 A~H"))
    );

    private ManualContent() {}
}
