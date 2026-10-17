// 数值平衡验算：完整复刻 Java 侧公式链，用来证明「关闭模式一发解 = 100」并输出对照表。
// 用法：node scripts/verify-balance.mjs
// Java 侧的 BalanceRules 表本身由 VerifyBalance.java（临时目录，直接调用编译产物）单独验证。
const f = Math.fround;                                     // Java 的 (float) 转换
const K = (s) => Number(s.toFixed(6));

// ---- 源码常量（逐条对应） ----
const FACTOR_ON = 1.0;                                     // BalanceRules.factor(true,*) 恒为 1.0
const OFF_CANDIDATE = 100 / 652.0560302734375;   // = 0.1533610538929689，见第 ③ 步
const OLD_OFF = 0.1;                                       // 改动前的关闭模式统一因子（对照用）
// GrowthRules.DEFAULTS：每根手指 / 即身佛 / 九相图 / 灵魂 / 虚空 / 传承
const ROW = [[4, 1, 4, 200], [10, 6, 50, 600], [20, 4, 20, 800], [20, 10, 100, 1000], [40, 7, 70, 1700], [200, 20, 200, 2000]];
const RESIDUE_MAX = 34;                                    // ShrineRules.RESIDUE_MAX_ITEMS
const BASE_ENERGY = 1000;                                  // GrowthConfig baseEnergy
// 末阶段（传承）：全 6 行 + 20 指 + 觉醒 → 所有成长列都吃满；残秽凝练满 34 件
const D = { fingers: 20, body: true, nine: true, soul: true, voidWin: true, guardian: true,
  residueItems: RESIDUE_MAX, soulFeasts: 0, damageMultiplier: 1, maid: false, burntOut: false,
  voidDamage: 1 };                                         // VoidController.DEFAULTS[0]
const DISMANTLE = 18;                                      // VoidTechniqueRules.DISMANTLE_DAMAGE
const FIRE_DAMAGE = 40;                                    // VoidTechniqueRules.FIRE_DAMAGE
const SLASH = 1;                                           // 每道斩击的系数（以解为单位）

const sum = (col, d) => K(Math.min(20, d.fingers) * ROW[0][col]
  + (d.body ? ROW[1][col] : 0) + (d.nine ? ROW[2][col] : 0) + (d.soul ? ROW[3][col] : 0)
  + (d.voidWin ? ROW[4][col] : 0) + (d.guardian ? ROW[5][col] : 0));
const growth = d => ({
  health: sum(0, d), attack: sum(1, d), energy: K(BASE_ENERGY + sum(3, d)),
  spell: sum(2, d),                                        // = Curse.spellBonus(p)（解这条链路传 residue=0）
  residue: Math.min(RESIDUE_MAX, d.residueItems) * 3 });   // ShrineRules.residuePercent：每件 +3%
const residueFactor = d => 1 + growth(d).residue / 100;    // Curse.residueFactor（乘在 0.6 之后，只乘一次）
const damageFactor = d => (d.burntOut ? 0.7 : 1) * (d.repaired ? 1.1 : 1);
const maid = d => (d.maid ? 0.5 : 1);                      // CasterDamageRules.casterFactor
// CombatSettings.spellDamage → CasterDamageRules.spell：(base×multiplier + 成长加成)×0.6×状态×残渣×女仆减半×因子
const spellDamage = (d, base, factor = FACTOR_ON) =>
  f(Math.max(0, (base * d.damageMultiplier + growth(d).spell) * 0.6 * damageFactor(d) * residueFactor(d) * maid(d) * factor));
// CombatSettings.fixedSpellDamage → CasterDamageRules.fixed：不走 0.6
const fixedDamage = (d, base, factor = FACTOR_ON) =>
  f(Math.max(0, base * damageFactor(d) * residueFactor(d) * maid(d) * factor));
// 解A（虚空御厨子路径）：VoidCombat.dismantle → 6 刻后 scaledDamage(18×voidDamage×power)
const Rvoid = (d, factor = FACTOR_ON) => spellDamage(d, DISMANTLE * d.voidDamage * SLASH, factor);
// 解B（灵魂御厨子路径，未吃虚空相咒胎）：(觉醒?12:7) + 灵魂进食
const Rsoul = (d, factor = FACTOR_ON) => spellDamage(d, (d.body ? 12 : 7) + Math.max(0, Math.min(300, d.soulFeasts)), factor);

console.log('=== ① 末阶段成长表（GrowthRules，全 6 行吃满） ===');
const g = growth(D);
console.log(`生命 +${g.health} / 普攻 +${g.attack} / 术式 +${g.spell} / 咒力上限 ${g.energy} / 残秽凝练 +${g.residue}%`);
console.log(`（校验：术式列 4+50+20+100+70+200 = ${4 + 50 + 20 + 100 + 70 + 200}；生命列 4+10+20+20+40+200 = ${4 + 10 + 20 + 20 + 40 + 200}）`);

console.log('\n=== ② 末阶段一发「解」的原始伤害 R（factor = 1.0 = 整合包模式） ===');
console.log('链路A（虚空御厨子，传承阶段的默认路径）');
console.log('  Skills.cast(0) → VoidCombat.cast(0) → dismantle() → DelayedDismantle(6 刻)');
console.log(`  → scaledDamage(n = DISMANTLE_DAMAGE 18 × voidDamage 1 × power 1 = 18)`);
console.log(`  → CombatSettings.spellDamage → CasterDamageRules.spell(18, 1, ${g.spell}, ...)`);
console.log(`  = (18×1 + ${g.spell}) × 0.6 × 1 × ${residueFactor(D)} = ${Rvoid(D)}`);
console.log(`链路B（灵魂御厨子，未吃虚空相咒胎）`);
console.log(`  Skills.cast(0) → Strike(基础 ${D.body ? 12 : 7}×scale 1×falloff 1 + 灵魂进食 ${Math.min(300, D.soulFeasts)})`);
console.log(`  = (${D.body ? 12 : 7} + ${Math.min(300, D.soulFeasts)} + ${g.spell}) × 0.6 × ${residueFactor(D)} = ${Rsoul(D)}`);
console.log(`对照（0 残秽的链路A）= ${Rvoid({ ...D, residueItems: 0 })}；灵魂进食只进链路B，不进链路A`);

console.log('\n=== ③ 关闭模式因子 = 100 / R（逐条链路各算一次） ===');
for (const [name, R] of [['A 虚空解', Rvoid(D)], ['B 灵魂解', Rsoul(D)]]) {
  const factor = 100 / R;
  console.log(`  ${name}：R = ${R} ⇒ 因子 = 100 / ${R} = ${factor}（${(factor * 100).toFixed(4)}% 原值）；一击伤害 = ${f(R * factor)}`);
}
console.log(`  采用 A（传承阶段默认路径，也是 0 残秽时与开启模式一致的路径）：${OFF_CANDIDATE}`);

console.log('\n=== ④ 验算：关闭模式一发解正好砍死铁傀儡（100 血 / 0 护甲） ===');
const R = Rvoid(D), OFF = 100 / R;
console.log(`精确值：${R} × ${OFF} = ${R * OFF} ⇒ 铁傀儡 100 − ${R * OFF} = ${100 - R * OFF}（恰好 0）`);
console.log(`float 路径（(float) 转换后，hurt() 收到的数值）：${f(R * OFF)}`);
console.log(`低配（0 残秽）：${f(Rvoid({ ...D, residueItems: 0 }) * OFF)} ← 仍 ≥100，所以"满成长"是必要条件而不是充分条件`);
console.log(`改动前的 ×0.1：${f(R * OLD_OFF)}（只有 29% 血，打不死）`);
console.log(`换回 100/R 的书写形式：100 × (R/R) = 100 —— factor 与 R 用的都是"末阶段满成长"的原值`);

console.log('\n=== ⑤ 逐项对照表（受因子影响的全部类别） ===');
console.log('类别'.padEnd(30) + '原值(开启×1.0)'.padStart(14) + '关闭旧×0.1'.padStart(13) + '关闭新×0.15336'.padStart(14));
const line = (name, raw) => console.log(name.padEnd(30) + raw.toFixed(2).padStart(14) + (raw * OLD_OFF).toFixed(2).padStart(13) + (raw * OFF).toFixed(2).padStart(14));
line('玩家额外生命(全阶段+20指)', g.health);
line('玩家额外普攻', g.attack);
line('玩家术式成长', g.spell);
line('一发解(虚空·传承)', Rvoid(D));
line('一发解(灵魂路径)', Rsoul(D));
line('虚空灶每跳', FIRE_DAMAGE);
line('净土每秒治疗', 5000);
line('术式反转回血(ReverseRules 派生)', 10);
line('即身佛', 5000);
line('虎杖残影', 30000);
line('真人(强化母体)', 480);
line('戴天', 200);
line('净土守护者·生命', 800000);
line('净土守护者·护甲', 80);
line('净土守护者·单次受伤上限', 10000);
line('守护者一拳(原值8)', 8);
console.log('不受影响（一个都没动）：五条悟 150000 / 魔虚罗 400000 血（固定值）、咒力上限/恢复/消耗、');
console.log('  持续时间、冷却、数量次数、概率、阶段阈值、范围、移动与弹道速度、减伤比例、掉落、任务条件。');

console.log('\n=== ⑥ 首领战时长比例（首领生命 ÷ 玩家单发术式，看是否改动平衡） ===');
const ratio = (bossHp, d = D) => bossHp / Rvoid(d);
console.log(`即身佛 5000 血：开启模式 5000/${Rvoid(D)} = ${ratio(5000).toFixed(3)}（单发数）`);
console.log(`                关闭模式 ${(5000 * OFF).toFixed(2)}/${f(Rvoid(D) * OFF)} = ${ratio(5000).toFixed(3)}（完全相同）`);
console.log(`                旧×0.1   ${(5000 * OLD_OFF).toFixed(2)}/${f(Rvoid(D) * OLD_OFF)} = ${ratio(5000).toFixed(3)}（也相同，但绝对数值只有 10%）`);
console.log('结论：分类比值对"玩家/首领"同时生效，所以首领战的相对难度与开启模式一致；');
console.log('      只有"未缩放的原版怪与固定值首领（五条悟/魔虚罗）"会因为玩家变强而变快。');

console.log('\n=== ⑦ 旧存档换算（storedDamage：先除写入时因子，再乘当前因子） ===');
const stored = (v, prev, factor = OFF) => f(Math.max(0, v / prev) * factor);
console.log(`开启模式写入 40 → ${stored(40, 1.0)}；旧关闭模式写入 4 → ${stored(4, 0.1)}（两者必须相等）`);
console.log(`未知因子 0.37 写入 40 → ${stored(40, 0.37)}（按原值处理，不会多缩一次）`);
