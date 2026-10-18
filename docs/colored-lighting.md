# 苍 / 赫 / 茈 的真彩色光照（r268 已实现）

> 状态：**已实现并随 r268 发布**。本文件记录为什么最后没走"自写光影包"，以及现在这套是怎么工作的。

## 一、为什么不自写光影（原方案 C 的死因）

原计划是"模组把光源数组塞进 shader，让光影包按位置算彩色光"。查证结论：**做不到**。

- Oculus（1.20.1 的 Iris 移植）对模组开放的 `IrisApi` v0.2 只有 7 个方法：
  `isShaderPackInUse` / `isRenderingShadowPass` / `openMainIrisScreenObj` /
  `getMainScreenLanguageKey` / `getConfig` / `createTextVertexSink` / `getSunPathRotation`
  —— **没有 uniform、没有 buffer、没有自定义图像**的写入通道。
  （源码：`Asek3/Oculus` 分支 `1.20.1-DH` 的 `net/irisshaders/iris/api/v0/IrisApi.java`）
- `shaders.properties` 的 `custom_uniforms` 只能用**已有的** uniform 在 CPU 上算值，
  模组没有地方往里写。
- 要走这条路只剩**反射进 Iris 内部**改程序对象：跟版本强耦合，Oculus 一更新就可能崩。

所以改成：**彩色光照交给专门的彩色光照 mod，我们只负责把"光源"放到光球位置上。**

## 二、现在的实现

| 环节 | 做法 |
|---|---|
| 方块 | 注册 `sukuna:blue_light` / `red_light` / `purple_light`（`ColoredLightBlock`）：**不可见**（`RenderShape.INVISIBLE`）、无碰撞、不挡光、不挡视线、**砸不掉**（`strength(-1)`）、**推不动**（`PushReaction.BLOCK`）；亮度 苍 **12** / 赫 **9** / 茈 **13** |
| 颜色 | 模组 jar 自带 `assets/sukuna/light/emitters.json`：`blue_light #5AA8FF`、`red_light #FF5B3A`、`purple_light #B062FF`。Colorful Lighting 用 `listResources("light", …)` 扫**所有命名空间**，所以放自家命名空间即可，不跟别人的表打架 |
| 搬运 | `GojoColoredLights` 挂在 `Network.send(ServerLevel, Effect)` 这个统一出口：苍(`BLUE`)/赫(`RED`)/茈(`PURPLE`) 每刻把方块挪到光球那一格；`BLUE_END`/`RED_END`/`PURPLE_BURST` 撤掉 |
| 光照 | 完全交给**原版光照引擎**逐格传播 —— 真的照亮方块与生物、墙后就是暗的（真阴影）、穿过彩色玻璃会被染色；装 Oculus 光影时 Colorful Lighting 会自己给光影包打补丁，颜色照样成立 |

## 三、四条保命规则

1. **没装 `colorful_lighting` 就完全不写世界** —— 退回 r267 的顶点通路发光壳（`GojoOrbGlow`），玩法一点不变；
2. **只在空气格放置**（原位不行就试上方／四周一格），回收前先确认那格还是自己的方块 —— **绝不覆盖地形**；
3. **光球跨格才挪方块**（同一格只续期），避免每刻 `setBlock` 刷爆光照引擎；最多同时 **4** 个光源，超了先回收最早的；8 刻没刷新就回收；
4. 崩溃残块**自愈**：方块带 `randomTicks()`，随机刻发现没有任何活动光球指向它就自己变回空气。

## 四、怎么验

1. 日志里搜 `[彩色光照]`：
   - 开局一行 `[彩色光照] 彩色光照 前置=已装 colorful_lighting 活动光源=0/4`
     （或 `前置=未装，退回自绘发光壳`）；
   - 第一次真正放下光源时一行 `[彩色光照] 已接管苍/赫/茈：光源方块开始跟随光球（亮度 苍12 赫9 茈13…）`。
2. 游戏里：夜里／洞穴放 苍 / 赫 / 茈，附近墙面地板应被**染上对应颜色**，并且光球飞过时亮区跟着移动；
   把光球打到墙后，墙后应该是暗的。
3. 反向验：把 `colorful_lighting` 从 mods 拿走再进 —— 光照层消失、只剩自绘发光壳，**不应该报缺依赖、不应该崩**。

## 五、已知限制

- 光是**按方块格**传播的（跟火把一样），所以边缘会看到方块级的分级，不是平滑的球形衰减（Colorful Lighting 自己做了三线性插值，看起来比原版白光源好）。
- 只对**已加载区块**生效；光源方块是写进世界的（这是"真阴影"的代价），所以理论上崩溃时会残留 —— 靠第三条的自愈机制处理。
- Colorful Lighting 是**客户端 mod**；专用服务器没装它时服务端不会放光源方块（单机＝同一个 JVM，不受影响）。
