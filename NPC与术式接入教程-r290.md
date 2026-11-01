# NPC 与术式接入教程（r290）

适用：本工程的 Minecraft 1.20.1 / Forge。本文里的类名都对应实际源码，不需要在线玩家替 NPC 施法。

## 1. 先理解这三层

1. **角色定义**：`CharacterRegistry` → `CharacterDefinition` → `CharacterConfig` → `CharacterSkill`，决定轮盘、技能编号、消耗、冷却。
2. **技能效果**：普通技能用 `TechniqueApi.registerSkill`；领域用 `TechniqueApi.registerDomain`。玩家和 NPC 共用效果。
3. **生物 AI**：选目标、移动、择机施法。统一入口 `TechniqueApi.cast(mob, target, skill)` 会检查资源和冷却，不能只调用效果方法绕过去。

`SkillCaster` 表示“施术者”，不是“在线玩家”。`body()` 是真实实体；`spend()` 扣其资源；`hurt()` 保留伤害归属与保护事件。

旧代码需要 `ServerPlayer` 时，`TechniqueApi.actor(body)` 为已接入 NPC 提供独立的兼容上下文；它与本体共享 UUID、咒力、生命和装备，不会在服务器伪造玩家登录。**不要保存附近玩家作为 NPC 的技能主人。**

## 2. 让一个已有生物使用现有角色技能

在服务端，拿到真实 `Mob` 和敌人后：

```java
import character.TechniqueApi;

// 只在生成/配置时绑定一次；blood 是胀相对应的角色编号。
boolean bound = TechniqueApi.bind(mob, "blood");
if (bound) {
    mob.setTarget(enemy);
    // 可由自己的 AI 选择技能，也可让公共 NPC 调度器轮流施放。
    TechniqueApi.cast(mob, enemy, 60);
}
```

- 绑定只替换术式，不会改变模型、皮肤、属性或索敌目标。
- 生物必须有 AI 且有有效敌人；装饰生物、`NoAI` 生物不会自动战斗。
- 角色编号如 `nanami`、`yuta`、`yuki`、`toji`、`mahito`、`jogo`、`blood`。
- 不要在每刻反复绑定不同角色，更不要给自带独立技能状态机的五条/虎杖/守护者首领再挂第二套施法 AI。
- 返回 `false` 表示门槛未通过；返回 `true` 代表技能调度已提交，**不等于一定命中目标**。

## 3. 添加新角色与普通技能

先用已有角色目录作为模板创建 `CharacterDefinition`，把技能加入其 `skills` 和 `config.customSkills`。同一角色技能号不可重复，自定义编号为 **1000～9999**。不同角色可以重复编号，因此判断领域必须同时看“角色编号 + 技能编号”，不能看到 `1002` 就当作某个固定领域。

以下配置/注册片段放在公共初始化中，先注册角色，再注册效果；不要只在客户端运行：

```java
CharacterSkill pulse = new CharacterSkill(
    9101, "回响冲击", "回响", "攻击附近敌人", 0xFF55AAFF,
    80, 60, CharacterSkill.Shape.BURST,
    6, 2, 180, 8, 0, 8, 0, 0, 0
);
// 将 pulse 加入你的角色定义；CharacterRegistry 登记完成之后：
TechniqueApi.registerSkill("your_role", 9101, (caster, skill, rank) -> {
    for (var target : caster.level().getEntitiesOfClass(
        net.minecraft.world.entity.LivingEntity.class,
        caster.body().getBoundingBox().inflate(6),
        t -> TechniqueApi.canAffect(caster, t, false)
            && caster.body().distanceToSqr(t) <= 36
            && caster.body().hasLineOfSight(t))) {
        caster.hurt(target, 8);
    }
});
```

`your_role` 是示例名称，不是本版已注册的角色。此片段需要配合你的完整角色定义，不能在世界聊天栏粘贴执行。

### 扣费只扣一次

`CharacterCasting` 会按 `CharacterSkill.cost/cooldown` 收取起手费用和写入冷却，所以普通效果里**不要再扣相同的起手费**。持续激光、召唤物攻击、领域维持可以另行收费，余额不足应停止对应效果。本版对 NPC 原配置为零费用的完整领域补收 300 起手咒力，乙骨 NPC 召唤里香收 150；玩家原配置不变。

旧 `NpcSkillCast`、`NpcSkillDriver` 已转接到统一入口；其旧 `cost` 参数只保留调用兼容，不再额外扣一笔，也不再创建仅攻击玩家的简化技能。

### 敌友与命中

- 查询目标用 `LivingEntity`，不是只遍历 `Player`，才能同时覆盖 PvE、EvE、PvP。
- 每次延迟命中都重新执行 `TechniqueApi.canAffect` / `CombatSettings.canHit`。
- 用 `caster.hurt` 或 `CombatSettings.hurt`，不要在伤害事件拒绝后再尝试 `generic` 或 `setHealth` 强行伤害。
- NPC 不伤本体和自己的里香，遵守队伍；玩家之间仍检查服务器 PvP 及敌友设置。
- 新角色的近战、召唤物、蓄力、位移都要考虑真实 `body()`。假玩家不在世界实体列表，不能拿它代替本体做碰撞排除。

## 4. NPC 的自动战斗规则

`character.npc.NpcBattleRules` 是可单测的规则文件：

| 规则 | 当前实现 |
|---|---|
| 领域冷却 | 3000 刻，即正常 20 TPS 下 150 秒，从施法/展开计时，不因换目标重置 |
| 最早开领域 | 连续交战至少 200 刻（10 秒） |
| 10 秒后可开条件 | 敌方领域覆盖自己，或生命不高于 65% |
| 正常开领域 | 连续交战 400 刻（20 秒）后可开 |
| 普通施法间隔 | 至少 24 刻；还受招式本身冷却和动作状态限制 |
| 普通熔断修复 | 等待 60 刻，之后尝试支付修复费用；不足则稍后重试 |
| 对拼败北 | 两分钟惩罚期间不能提前修复；治疗按原对拼规则收取上限 55% 咒力 |
| 胀相死胎星 | 必须已有血之领域且生命不高于 25%，避免开场贫血导致只开领域 |

普通 NPC 的咒力由 `NpcCurse` 管理，默认上限 `max(100, 最大生命×20)`，持续技能也会消耗。修复不会补满咒力。甚尔沿用天与咒缚的**体术余额**，并非免费咒术。

旧五条/虎杖/守护者首领保留动作状态机，通过 `NpcLegacyCombat` 接入相同领域门槛和资源收费，而不是同时跑两套 AI。

## 5. 本版新增五位 NPC

| NPC / 实体 ID | 战斗方式 |
|---|---|
| 七海建人 `sukuna:nanami_npc` | 十划咒法、弱点标记、原有七海战斗技能与加班；没有凭空新增领域 |
| 乙骨忧太 `sukuna:yuta_npc` | 剑击、里香、复制已支持的对手术式、自疗、真赝相爱；复制不打开玩家菜单 |
| 九十九由基 `sukuna:yuki_npc` | 质量拳、迦楼罗、加重、星崩拳、星轨坠亡；保留原角色的高伤害定位 |
| 伏黑甚尔 `sukuna:toji_npc` | 游云、突进、重击、跃身；使用体术资源，不开领域 |
| 真人 `sukuna:mahito_npc` | 近战/灵魂打击、自闭圆盾裹；NPC 不把玩家或生物直接删掉变物品 |

四位术师默认主动攻击怪物/真人，遭攻击会反击；真人会主动攻击非创造玩家与其他新增术师。队伍与召唤物归属仍参与保护。

真人战斗术式采用普通伤害事件和短暂减速/虚弱；普通魂击基础 `min(26, 8+目标最大生命×2.5%)`，领域脉冲基础 `min(16, 6+目标最大生命×1.5%)`，最终仍受既有伤害系统影响。领域脉冲检查简易领域与对拼暂停。

乙骨 NPC 的领域默认维持 30 秒（对拼暂停/获胜可延长），不再每两刻回满咒力；里香攻击有额外耗能。复制池不是所有第三方模组技能的自动识别器，后续技能要主动接入。

## 6. 皮肤与动作接入

- `SorcererNpcRenderer` 使用标准宽臂 `PlayerModel`，五个实体分别读取 `textures/entity/<角色>_npc.png`。
- 上传的 2:1 旧皮肤不能直接缩放成正方形：先保留上半区，再按面镜像补齐左臂/左腿。外套层保留透明度，基础身体 UV 区域需不透明。
- 本版修复鹿紫云一与日车的旧式长方皮肤，五位新 NPC 也使用对应上传皮肤；不是把概念图整张贴在身体上。
- 漏壶保留原 `.bbmodel`、骨骼和动画。客户端按同步的移动速度/挥手状态选择走路、跑步和连招，不依赖客户端没有同步的 `getTarget()`。
- 新动画要服务端发动作状态，客户端只负责播放；不要用动画是否结束来决定服务端伤害。

## 7. 最小测试顺序

1. 新建测试世界并备份，创造栏搜索五人的名字，放出刷怪蛋。
2. 放一只怪物观察基础术式；再用生存模式攻击术师观察反击。
3. 两只不同阵营的领域 NPC 交战，确认不会见面立即展开、第二次受 150 秒门槛约束。
4. 生存玩家与 NPC 对拼；双人服务器互相标敌再对拼；另测同队不对拼。
5. 测缺咒力、死亡、离开维度、区块卸载和正常关服。

当前交付只有编译、纯规则/源码接线断言与离线图像检查，**没有进行游戏内战斗/帧率/联机实测**。领域会改地形，先在备份世界验证。

下一篇：[领域与特效教程](领域与特效教程-r290.md)。
