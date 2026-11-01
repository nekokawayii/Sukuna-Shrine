# 新 NPC 如何直接使用玩家同款术式、领域（r291）

适用本工程 Minecraft 1.20.1 / Forge。**接口**就是一套约好的函数：NPC 声明“我是谁、我会什么”，公共战斗系统负责调用，不用再写一份弱化版技能。

> 本篇替代 r290 教程里的 NPC 接入和甚尔说明。模型、刷怪蛋、阵营与普通移动仍由生物实体定义；下面的接口负责术式与领域，不会把第三方技能自动变成可调用技能。

## 一、最简单：新增角色只写一个函数

给你的 `Mob` / `PathfinderMob` 子类实现 [NpcTechniqueUser](src/main/java/character/npc/NpcTechniqueUser.java)：

```java
public final class MyNpc extends PathfinderMob implements NpcTechniqueUser {
    // 构造器、属性、模型、寻路和索敌沿用你自己的生物定义。

    @Override
    public NpcLoadout npcTechniques() {
        return NpcLoadout.all("your_role");
    }
}
```

需要导入：

```java
import character.npc.NpcTechniqueUser;
import character.npc.NpcLoadout;
```

`your_role` 换成已经注册的角色 ID（例如 `blood`、`jogo`、`yuta`、`yuki`、`toji`）。**这段是要加进实体类的 Java 代码，不是游戏指令。**

### all 和 only

```java
// 同角色玩家技能目录里的全部术式和领域。
return NpcLoadout.all("yuta");

// 限定学习列表：仅此角色的这几招。领域也放同一张表。
return NpcLoadout.only("your_role", 9101, 9102, 9100);
```

- `all` 读取玩家的技能目录；以后给角色增加技能，NPC 无须同步抄写一份列表。
- 角色和技能号共同确定技能；不要把不同角色都叫 `1002` 的招式混在一起。
- `only` 是限制，不是赠送任意角色资格。不能写一个别人的编号就绕过玩家实现的家族检查。
- 新角色的全部招式需要先登记进 `CharacterDefinition.skills`；自定义技能还需要 `CharacterConfig.customSkills`。
- 宿傩、赤血操术、甚尔保留历史编号适配；甚尔的 `1～20` 是连招，不是咒术轮盘号，更不是领域。
- 现有五位共用人形 NPC 已采用此接口，见 [SorcererNpcEntity](src/main/java/cn/blockforge/generated/nnnnr1201forge2/SorcererNpcEntity.java)。

### 不方便改实体类时

服务端生成后绑定一次：

```java
boolean ok = character.TechniqueApi.bind(mob, "blood");
if (ok) mob.setTarget(enemy);
```

绑定只是给已有生物指定角色，不改皮肤、生命、模型或阵营。已经有活动领域时不允许切换角色，避免旧领域遗留。不要每刻重复绑定，更不要给已有独立首领 AI 的五条、虎杖、守护者叠第二个调度器。

## 二、什么叫“和玩家同款”

实际路径如下：

```text
NPC 的 npcTechniques()
  → NpcCombatCasting 选择招式、检查交战门槛
  → NPC 独立施术上下文 NpcActor
  → CharacterCasting 或历史技能入口
  → 玩家本来使用的效果函数 / 领域状态机
```

这里的 `NpcActor` 只是兼容旧代码的施术上下文，不会伪造一个在线账号，也不需要附近有玩家。它使用 NPC 本体的 UUID、生命、装备和咒力池。

**相同的是**伤害实现、领域内部效果、贴图/特效、敌友规则、简易领域与对拼逻辑。**不相同的是**人类按键改成 AI 决策，NPC 领域另有 150 秒冷却和交战等待；NPC 咒力上限按本体生命计算，而不是照抄玩家成长值。

### 自动战斗的前提

- NPC 是服务端存活的 `Mob`，AI 没有被永久关闭。
- `mob.getTarget()` 是同维度的有效敌人，距离不超过 64 格。
- 自己的生物仍需配置寻路与索敌目标，例如 `MeleeAttackGoal`、`HurtByTargetGoal`。
- 不在硬直、对拼、熔断或技能动作锁里，咒力够用。
- 领域须先交战 10 秒；此后受到敌方领域覆盖或低血量可开，正常满血交战 20 秒可开。

手动 AI 选择某招时用：

```java
boolean submitted = character.TechniqueApi.cast(mob, enemy, skillId);
```

它仍会检查门槛和技能名单。返回成功表示已经提交调度，不保证命中、敌人未挡住或延迟效果最终命中。

## 三、以后新增玩家技能，怎样让 NPC 同时可用

优先使用 [TechniqueApi](src/main/java/character/TechniqueApi.java) 和 [SkillCaster](src/main/java/character/SkillCaster.java)：

```java
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

初始化顺序：**登记角色和技能配置 → registerSkill/registerDomain → NPC 生成使用**。不要重复登记同一角色同一技能；公共初始化只执行一次。

重要规则：

1. `body()` 才是真实碰撞实体；不要把兼容上下文当成实际站在世界里的玩家。
2. 查询 `LivingEntity`，不能只找 `Player`，否则 NPC 对 NPC 没效果。
3. 每次命中、延迟脉冲、召唤物攻击都重新检查敌友和对拼。
4. `CharacterCasting` 已按配置扣起手费，效果函数不要再扣同一笔。
5. 持续费可用 `caster.spend()`，不足就停止维持，而不是负咒力继续打。
6. 用 `caster.hurt()` / `CombatSettings.hurt()`，不要在伤害事件拒绝后用 `setHealth()` 强行扣血。
7. 不调用 `p.connection.send()` 给假玩家发包；个人提示用 `Network.sendTo()`，旁观者特效使用同维度广播。
8. 技能依赖菜单选择、蓄力松手或连续按键时，要提供自动输入适配，但**结算函数仍共用玩家版本**。仅绑定角色不能替你自动猜菜单选项。

### 持续技能与被动也要共用维护

不要只监听真人的 `PlayerTickEvent`，否则 NPC 的持续状态不会推进。新角色可以登记：

```java
TechniqueApi.registerTick("your_role", caster -> {
    // 每个游戏刻调用一次，约 20 次/秒。
    // 在此推进自己已有的技能状态；别在每刻重新释放技能。
    // 查询 caster.body() 是否存活；需要时检查 DomainClash.active(caster.body())。
});
```

真实玩家和 NPC 都会调用；不要又手动跑相同维护函数导致双倍扣费。跨维度、死亡、离场要清理自己的延迟任务。图片领域模板已经自带维护，不需要再登记 tick。

## 四、领域的接入与普通技能一样简单

现有玩家领域：NPC 的 `all("role")` 自动取得其领域编号，直接调用真实领域。

新建图片领域：使用 [PictureDomainApi](src/main/java/character/domain/PictureDomainApi.java)，完整教程见 [天空 UV 与对拼接口](领域天空UV与对拼接口-r291.md)。

已有复杂领域：

```java
TechniqueApi.registerDomain("your_role", 9100, (caster, skill, rank) -> {
    // 先建状态，再 opened。玩家与 NPC 必须共用这一个实现。
});
```

`registerDomain` 同时让主线和 NPC 知道“这是领域”，不能只在名称里写“领域展开”。领域本身仍须登记生命周期，或者直接使用图片领域模板。

## 五、本轮甚尔 NPC 的自动 QTE

QTE 就是限定时机按键接招；NPC 不等待人按键，改为服务端自动选择。

- 近身威胁触发玩家同款闪避窗口；安全移动使用真实 NPC 碰撞盒，不能穿墙。
- 中距离目标允许身法起手，再进入连招窗口。
- 在 20 条玩家连招中轮换选择，情境不满足就尝试下一条。
- 有合适弹幕优先弹反；暴君处决仍要求累计三次完美闪避。
- 空中招式仍要求腾空，墙壁反杀仍要求墙，破魔仍要求目标有增益；不是强行每招都必定成立。
- 自动连招间隔 40 刻（2 秒），普通/进阶/处决分别消耗 16/24/40 体术。Jade 显示体术，不再把甚尔显示成有咒力。
- 共用 `TojiCombos` 的伤害与世界特效，共用 `TojiActions` 的人形关节动作采样；NPC 不是播放玩家专用渲染器。
- 处决取消原来绕过伤害事件再直接压生命的操作，防止受保护目标被强制扣血。

相关实现：[TojiNpcCombat](src/main/java/character/toji/TojiNpcCombat.java)、[TojiCombos](src/main/java/character/toji/TojiCombos.java)。

## 六、NPC 不动、Jade 不扣费：如何区分

### 领域对拼

有双方名字的进度条、双色球壳与交界波纹；Jade 显示“领域对拼中”。双方短暂固定位置是规则，不是寻路坏了。纯 NPC 对拼最多 **2 秒准备 + 20 秒计分**，玩家参战仍保留原来的 60 秒计分上限。

### 短暂硬直

统一使用 [NpcControl](src/main/java/character/npc/NpcControl.java)：

```java
NpcControl.stun(mob, "your_skill", 30); // 1.5 秒
NpcControl.release(mob, "your_skill"); // 提前解除自己这一份
```

保存原 `NoAI`，按来源分别计时，不清空战斗目标，重载后也按截止时间恢复。不得直接 `setNoAi(true)` 后只放一条内存定时器；也不得直接 `setNoAi(false)` 把其他技能的控制提前解除。人为设成永久 NoAI 的装饰 NPC 不会被本接口擅自启用。

### 咒力

Jade 已采用服务端数据提供器，显示真正的 `NpcCurseEnergy/NpcCurseMax`；客户端不再自己造一个满值。普通咒力消耗后暂停回复 3 秒，再按每秒 2% 回复。秤中奖的快速回复是原角色效果，不是漏扣费。

已有旧版残留的永久 NoAI 若没有来源或截止记录，无法安全判断它是故障还是用户主动设置；请用新刷怪蛋验证新版，再处理旧实体。

## 七、最小验收顺序

1. 新测试世界，保留 Jade，放胀相或漏壶和敌人，看咒力在施法后下降。
2. 放甚尔与高生命敌人，观察闪避、连招、体术下降与收招后的反击窗口。
3. 放两个不同阵营的领域角色，等交战足够时间，观察世界对拼和解除后继续战斗。
4. 同队角色不能误伤或对拼；另测生存玩家、NPC、联机两位玩家。
5. 领域展开后测试自然结束、主动收起、死亡、换维度、卸载区块和正常关服。

编译与源码断言不等于游戏实测；光影兼容、帧率和复杂跨领域地形重叠，请在备份世界验证。
