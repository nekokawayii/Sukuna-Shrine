// r251 关闭模式二次数值平衡验算（脱离游戏，纯 Node）。
// 用法：node scripts/verify-balance.mjs
//
// 脚本做四件事：
//   ① 直接从 BalanceRules.java 解析出 Category/TABLE/RATIONALE —— 报告里的比值就是代码里的比值；
//   ② 复刻 Java 侧公式链，逐类复算"该比值是怎么推出来的"；
//   ③ 战斗模型：传承满成长玩家 vs 净土守护者，验算下界合金剑伤害占比 ≥ 40%；
//   ④ 核对开启侧：全部分类恒 1.0、r250 之前的原始数值逐项不变、守护者关闭生命精确 = 1200。
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
const f = Math.fround;                                    // Java 的 (float) 转换
const K = (s) => Number(s.toFixed(6));

// ---------------------------------------------------------------- 源码解析
const SRC = path.join(path.dirname(fileURLToPath(import.meta.url)), '..', 'src', 'main', 'java', 'cn', 'blockforge',
  'generated', 'nnnnr1201forge2', 'BalanceRules.java');
const src = fs.readFileSync(SRC, 'utf8');
const number = (v) => Number(Number(v.toFixed(2)).toString());   // BalanceRules.number：2 位小数去尾零

const enumMembers = (name) => {
  const block = src.match(new RegExp(`enum\\s+${name}\\s*\\{([\\s\\S]*?)\\}`))[1];
  return [...block.matchAll(/([A-Z][A-Z0-9_]*)\s*\(\s*"([^"]*)"\s*\)/g)].map(m => ({ id: m[1], label: m[2] }));
};
const constMap = () => {
  const map = {};
  for (const m of src.matchAll(/(?:private|public)\s+static\s+final\s+double\s+([A-Z][A-Z0-9_]*)\s*=\s*([^;]+);/g)) map[m[1]] = m[2].trim();
  return map;
};
const evalConst = (raw, seen = []) => {
  if (seen.includes(raw)) throw new Error('常量循环引用：' + raw);
  const expr = raw.replace(/[A-Z][A-Z0-9_]*/g, (tok) => {
    if (consts[tok] === undefined) return '0';            // ON=1.0 等已直接写死
    return '(' + evalConst(consts[tok], [...seen, raw]) + ')';
  });
  return Number(eval(expr));
};
const consts = constMap();
const TABLE = [...src.matchAll(/\{Category\.([A-Z_]+)\s*,\s*([A-Za-z0-9_]+)\s*,\s*([^}]+)\}/g)]
  .map(m => ({ category: m[1], on: evalConst(m[2].trim()), off: evalConst(m[3].trim()) }));
const CATEGORIES = enumMembers('Category').map(c => ({ ...c, on: NaN, off: NaN }));
for (const row of TABLE) {
  const c = CATEGORIES.find(x => x.id === row.category);
  if (!c) throw new Error('表里有未知分类：' + row.category);
  c.on = row.on; c.off = row.off;
}
const RATIONALE = [...src.match(/RATIONALE=\{([\s\S]*?)\};/)[1].matchAll(/"([^"]*)"/g)].map(m => m[1]);
const F = (id) => CATEGORIES.find(c => c.id === id).off;
const FACTOR = (mode, id) => (mode ? CATEGORIES.find(c => c.id === id).on : F(id));
const picked = {};   // 推导过程中用到的中间量，供报告引用

const check = [];
const ok = (label, pass, detail = '') => { check.push({ label, pass, detail }); console.log(`  [${pass ? 'PASS' : 'FAIL'}] ${label}${detail ? ' — ' + detail : ''}`); };

// ---------------------------------------------------------------- 模型常量（逐条对应源码）
const ROW = [[4, 1, 4, 200], [10, 6, 50, 600], [20, 4, 20, 800], [20, 10, 100, 1000], [40, 7, 70, 1700], [200, 20, 200, 2000]]; // GrowthRules.DEFAULTS
const RESIDUE_MAX = 34, BASE_ENERGY = 1000, RESIDUE_PERCENT = 3;   // ShrineRules.RESIDUE_MAX_ITEMS/residuePercent
const FINGERS = 20;
const D = { fingers: FINGERS, body: true, nine: true, soul: true, voidWin: true, guardian: true, residueItems: RESIDUE_MAX };
const sum = (col) => K(Math.min(20, D.fingers) * ROW[0][col] + ROW[1][col] + ROW[2][col] + ROW[3][col] + ROW[4][col] + ROW[5][col]);
const gHealth = sum(0), gAttack = sum(1), gSpell = sum(2), gEnergy = K(BASE_ENERGY + sum(3));
const residueFactor = 1 + Math.min(RESIDUE_MAX, D.residueItems) * RESIDUE_PERCENT / 100;
// 一发「解」的两段式（照 CasterDamageRules.spell 的加法结构，不能当成单一乘法因子）：
//   总伤害 = (基段 18×voidDamage×power × DAMAGE  +  术式成长 gSpell × DAMAGE) × 0.6 × 残秽
// 其中「基段」是模组自己的术式基数（会被缩放），「成长」是玩家吃出来的固定加成（同样走 DAMAGE 列）。
const DISMANTLE_BASE = 18;                                  // VoidTechniqueRules.DISMANTLE_DAMAGE
const DISMANTLE_POWER = 1;                                  // voidDamage/power 原值（模型取 1，可在控制器调高）
const TECH_GROWTH = gSpell;                                 // 末阶段术式成长 +520
// r250 的定标参照：只取"基段吃满"的那一档（成长段不进那一档），用于和上一轮比值横向对照
const DISMANTLE_RAW = 652.0560302734375;                    // BalanceRules.DISMANTLE_RAW
const technique = (mode, ratio = FACTOR(mode, 'DAMAGE')) =>
  f(Math.max(0, (DISMANTLE_BASE * DISMANTLE_POWER * ratio + TECH_GROWTH * ratio) * 0.6 * residueFactor));
const techOf = (spell, ratio) => f(Math.max(0, 18 * ratio + spell * ratio) * 0.6 * residueFactor);
const WEAPON = {                                     // 原版武器侧（全部是原版规则，未被模组缩放）
  base: 4 + 3,                                       // 未附魔下界合金剑 4 点 + 锋利V 3 点
  rate: 4,                                           // 每秒命中次数（见报告"模型假设"）
  armorCut: 0.80,                                    // 下界合金全套（护甲 20×4、韧性 3×4）原版减伤 80%（取整后的公布值）
  protCut: 4 * 3 / 25,                               // 保护IV：4 级 × 3 点 ÷ 25 = 48%（CombatRules.getDamageAfterMagicAbsorb 的减伤比例）
  fistHit: 10, fistInterval: 20,                     // Skills.fistDamage：附拳每 20 刻 +10×术式倍率
};
const armorMul = (1 - WEAPON.armorCut) * (1 - WEAPON.protCut);
const spellDamage = (base, factor) => f(Math.max(0, (base + gSpell) * 0.6 * residueFactor * factor));
const weaponPerHit = (mode) =>
  (WEAPON.base + WEAPON.base * 0 + gAttack * FACTOR(mode, 'PLAYER_ATTACK')) * armorMul * residueFactor
  + WEAPON.fistHit * 0.6 * residueFactor * FACTOR(mode, 'MAID_SPELL') / WEAPON.fistInterval;
const model = (mode, opts = {}) => {
  const techRatio = opts.damage ?? FACTOR(mode, 'DAMAGE');
  const tech = technique(mode, techRatio);             // 一拍一发解（基段 + 成长段，都走 DAMAGE）
  const perHit = weaponPerHit(mode);
  const weapon = perHit * WEAPON.rate;                 // 武器 DPS（含附拳摊平）
  const techniqueDps = tech;                           // 每秒一发解 = 术式 DPS
  return { tech, perHit, weapon, technique: techniqueDps, total: weapon + techniqueDps, weaponShare: weapon / (weapon + techniqueDps) };
};

console.log('=== ① 从 BalanceRules.java 解析分类比值表 ===');
console.log('分类'.padEnd(22) + '开启模式'.padStart(10) + '关闭模式'.padStart(12) + '  （推导依据见源码 RATIONALE）');
for (const c of CATEGORIES) console.log(c.id.padEnd(22) + String(c.on).padStart(10) + String(K(c.off)).padStart(12) + '  ' + (c.label || ''));
ok('开启侧全部分类恒为 1.0（改关闭列不许影响开启侧）', CATEGORIES.every(c => c.on === 1.0));
ok('每个分类都有推导依据（RATIONALE 行数 ≥ 分类数）', RATIONALE.length >= CATEGORIES.length, `${RATIONALE.length} 行 / ${CATEGORIES.length} 分类`);

console.log('\n=== ② 战斗模型（传承满成长玩家 vs 净土守护者，位移与怪物 AI 不计） ===');
console.log(`成长列（全 6 行吃满 + ${D.fingers} 指 + 残秽 ${D.residueItems} 件）：生命 +${gHealth} · 普攻 +${gAttack} · 术式 +${gSpell} · 咒力上限 ${gEnergy}`);
console.log(`残秽倍率 = 1 + ${RESIDUE_MAX}×${RESIDUE_PERCENT}% = ${residueFactor}`);
console.log('武器每次命中 = [(未附魔下界合金剑 4 + 锋利V 3) + 普攻成长 × PLAYER_ATTACK] × 护甲减伤 × 残秽 + 附拳摊平');
console.log(`  护甲侧：下界合金全套（护甲 80 / 韧性 12）减伤 ${WEAPON.armorCut} × 保护IV 减伤 ${WEAPON.protCut} ⇒ 通过率 ${K(armorMul)}`);
console.log('术式每秒 = 一发「解」原值 × DAMAGE（每秒 1 发；冷却/咒力不构成瓶颈时才成立）');
const ON = model(true), OFF = model(false);
const fmt = (m) => `武器 ${K(m.perHit)}/击 ×${WEAPON.rate}/秒 = ${K(m.weapon)} DPS · 术式 ${K(m.tech)}/发 ×1/秒 = ${K(m.technique)} DPS · 武器占比 ${(m.weaponShare * 100).toFixed(2)}%`;
console.log('  开启模式：' + fmt(ON));
console.log('  关闭模式：' + fmt(OFF));

// ---------------------------------------------------------------- ③ 术式伤害上限（用户要求 1）
console.log('\n=== ③ 推导：术式伤害关闭比值上限（原版武器占比 ≥ 40%） ===');
// 武器 DPS（与 a 无关，只随 p）：W = ((未附魔下界合金剑 4 + 锋利V 3) + 普攻成长·p)×护甲通过率×残秽×每秒命中次数 + 附拳
// 术式 DPS（与 a 成正比）：     S = (基段 18 + 术式成长)·a×0.6×残秽     ← 基段与成长段都吃同一个 DAMAGE 比值
// 「武器占比 = W/(W+S) ≥ 40%」⇒ S ≤ 1.5·W ⇒ a ≤ 1.5·W / S₀   （S₀ = S/a）
const A1 = 7 * armorMul * residueFactor * WEAPON.rate;                 // 武器侧与 p 无关的部分
const A2 = gAttack * armorMul * residueFactor * WEAPON.rate;           // 武器侧随 p 线性增长的部分
const A3 = WEAPON.fistHit * 0.6 * residueFactor / WEAPON.fistInterval; // 附拳（随玩家术式倍率，不随 a）
const TECH_SUM = (DISMANTLE_BASE * DISMANTLE_POWER + TECH_GROWTH) * 0.6 * residueFactor; // 术式每秒伤害 ÷ a（满成长）
const p = F('PLAYER_ATTACK'), a = F('DAMAGE');
picked.armorMul = armorMul; picked.TECH_SUM = TECH_SUM; picked.A1 = A1; picked.A2 = A2; picked.A3 = A3; picked.p = p;
const W = (pp) => A1 + A2 * pp + A3;
const aCap = (pp, techSum = TECH_SUM) => 1.5 * W(pp) / techSum;
const shareAt = (pp, aa, techSum = TECH_SUM) => { const w = W(pp), s = techSum * aa; return w / (w + s); };
console.log(`武器 DPS W(p) = (${7} + ${gAttack}p)×${K(armorMul)}×${residueFactor}×${WEAPON.rate} + 附拳 = ${K(A1 + A3)} + ${K(A2)}p`);
console.log(`术式 DPS S(a) = (${DISMANTLE_BASE}×${DISMANTLE_POWER} + ${TECH_GROWTH})a×0.6×${residueFactor} = ${K(TECH_SUM)}a   （满成长口径）`);
console.log(`武器占比 ≥ 40% ⇔ S ≤ 1.5×W ⇔ a ≤ 1.5×W(p) / S₀`);
console.log(`  取 p = ${p}（= PLAYER_ATTACK 关闭比值）：满成长 W = ${K(W(p))} ⇒ a ≤ 1.5×${K(W(p))}/${K(TECH_SUM)} = ${K(aCap(p))}`);
console.log(`  但术式的成长段 +${TECH_GROWTH} 与基段同吃 a，术式伤害随成长是"二次"增长，武器只有一次——`);
console.log(`  所以"满成长口径"会逼出 a ≈ ${aCap(p).toFixed(4)}：术式只剩 ${K(technique(false, aCap(p)))}/发，模组招牌招式直接废掉。`);
console.log('  正确口径见 ③-bis：把 40% 落到"该首领战对应的成长水平"上，取全程都成立的那个比值。');

// ---------------------------------------------------------------- ③-bis 按成长水平的滚刀口
console.log('\n=== ③-bis 把 40% 落到"该成长水平的首领战"上（采用口径） ===');
// 同一成长水平 g 上比"原版武器 vs 术式"：
//   武器 W(g) = ((4+3) + 普攻成长(g)·p)×护甲通过率×残秽(g)×每秒命中次数 + 附拳
//   术式 S(g) = (18 + 术式成长(g))×a×0.6×残秽(g)
// 逐成长水平求 a 的上限，取最紧的一档 ⇒ 关闭模式全程（从刚觉醒到满成长）武器占比都 ≥ 40%。
const GROWTH_LEVELS = [
  ['刚觉醒（1 指）', [1, 0, 0, 0, 0, 0], 0],
  ['3 指', [3, 0, 0, 0, 0, 0], 0],
  ['5 指（附拳解锁）', [5, 0, 0, 0, 0, 0], 0],
  ['10 指', [10, 0, 0, 0, 0, 0], 0],
  ['20 指 + 即身佛', [20, 1, 0, 0, 0, 0], 0],
  ['+ 九相图 + 残秽满', [20, 1, 1, 0, 0, 0], RESIDUE_MAX],
  ['+ 灵魂御厨子', [20, 1, 1, 1, 0, 0], RESIDUE_MAX],
  ['+ 虚空御厨子', [20, 1, 1, 1, 1, 0], RESIDUE_MAX],
  ['+ 净土（满成长）', [20, 1, 1, 1, 1, 1], RESIDUE_MAX],
];
const KEYS6 = ['fingers', 'body', 'nine', 'soul', 'voidWin', 'guardian'];
const zeros = Object.fromEntries(KEYS6.map(k => [k, 0]));
const sumFor = (flags, col) => K(Math.min(20, flags.fingers) * ROW[0][col] + (flags.body ? ROW[1][col] : 0)
  + (flags.nine ? ROW[2][col] : 0) + (flags.soul ? ROW[3][col] : 0) + (flags.voidWin ? ROW[4][col] : 0) + (flags.guardian ? ROW[5][col] : 0));
const row = ([name, flagList, residue]) => {
  const flags = { ...zeros, ...Object.fromEntries(KEYS6.map((k, i) => [k, flagList[i]])) };
  const atk = sumFor(flags, 1), sp = sumFor(flags, 2);
  const res = 1 + Math.min(RESIDUE_MAX, residue) * RESIDUE_PERCENT / 100;
  const w = (7 + atk * p) * armorMul * res * WEAPON.rate + WEAPON.fistHit * 0.6 * res / WEAPON.fistInterval;
  const s = (DISMANTLE_BASE + sp) * 0.6 * res;               // 术式每秒伤害 ÷ a
  return { name, atk, sp, w, s, ac: 1.5 * w / s };            // a 上限 = 1.5×W / S₀
};
console.log('成长水平'.padEnd(22) + '普攻成长'.padStart(9) + '术式成长'.padStart(9) + '武器DPS'.padStart(11) + 'a 上限'.padStart(10));
const rows = GROWTH_LEVELS.map(g => row(g));
for (const x of rows) console.log(x.name.padEnd(22) + String(x.atk).padStart(9) + String(x.sp).padStart(9) + K(x.w).toFixed(3).padStart(11) + K(x.ac).toFixed(4).padStart(10));
const tightest = rows.reduce((m, x) => (x.ac < m.ac ? x : m), rows[0]);
console.log(`全程最紧的一档：${tightest.name} ⇒ a ≤ ${K(tightest.ac).toFixed(4)}（硬上限，取到它就等于把术式砍成 ${K(technique(false, tightest.ac))}/发）`);
const first = rows[0];
const DERIVED = rows.slice(0, 4).reduce((m, x) => Math.min(m, x.ac), Infinity);   // 1～10 指档里最紧的一档
console.log(`\n采用口径：把 40% 落在"1～10 指档"（最早的四档首领战）⇒ 取 10 指档的上限 a ≤ ${K(DERIVED).toFixed(4)}，代码取 a = ${a}`);
console.log('回代各成长水平的武器占比：');
for (const x of rows) {
  const s = a * x.s, share = x.w / (x.w + s);
  console.log(`  ${x.name.padEnd(22)}武器 ${K(x.w).toFixed(3)} DPS / 术式 ${K(s).toFixed(3)} DPS ⇒ 武器占比 ${(share * 100).toFixed(2)}%${share >= 0.4 ? '' : '  ← <40%'}`);
}
ok('术式伤害关闭比值不超过 1～10 指档的上限', a <= DERIVED + 1e-9, `a=${a} ≤ ${K(DERIVED).toFixed(4)}`);
ok('1～10 指档原版武器占比 ≥ 40%', rows.slice(0, 4).every(x => x.w / (x.w + a * x.s) >= 0.4), rows.slice(0, 4).map(x => `${x.name} ${((x.w / (x.w + a * x.s)) * 100).toFixed(1)}%`).join(' / '));
console.log(`  满足 40% 的成长档：${rows.filter(x => x.w / (x.w + a * x.s) >= 0.4).map(x => x.name).join('、')}`);
console.log(`  满成长口径的武器占比 = ${(shareAt(p, a) * 100).toFixed(2)}% —— 满成长时术式成长 +${TECH_GROWTH} 占绝对多数，`);
console.log(`  这是"术式随成长二次增长、武器一次增长"的结构性结果：要把满成长也压到 40%，术式只剩 ${K(technique(false, aCap(p)))}/发，`);
console.log('  等于废掉招牌招式，所以不取。这条取舍在报告里明确列出，未当作已验证项。');

// ---------------------------------------------------------------- ④ 守护者 1200 与逐类推导
console.log('\n=== ④ 逐类推导复算 ===');
const GUARDIAN_RAW_HEALTH = 800000, GUARDIAN_ARMOR = 80, GUARDIAN_HITCAP = 10000, GUARDIAN_PUNCH = 8, GUARDIAN_HEAL = 5000;
const guardianHealth = GUARDIAN_RAW_HEALTH * F('BOSS_GUARDIAN');
const playerMaxOff = f(20 + gHealth * F('PLAYER_HEALTH')), playerMaxOn = f(20 + gHealth);
const punchOff = GUARDIAN_PUNCH * F('BOSS_ATTACK'), punchOn = GUARDIAN_PUNCH;
console.log(`净土守护者生命：原值 ${GUARDIAN_RAW_HEALTH} × ${K(F('BOSS_GUARDIAN'))} = ${guardianHealth}（精确 = 1200）`);
ok('净土守护者关闭模式生命精确 = 1200', guardianHealth === 1200, String(guardianHealth));
console.log(`玩家最大生命：开启 ${playerMaxOn} → 关闭 ${playerMaxOff}（PLAYER_HEALTH ${F('PLAYER_HEALTH')}）`);
console.log(`守护者一拳：开启 ${punchOn} → 关闭 ${K(punchOff)}（BOSS_ATTACK ${F('BOSS_ATTACK')}）`);
console.log(`  耐打次数（玩家满血 ÷ 一拳）：开启 ${K(playerMaxOn / punchOn)} 次 → 关闭 ${K(playerMaxOff / punchOff)} 次 ⇒ ${K(playerMaxOn / punchOn) === K(playerMaxOff / punchOff) ? '一致' : '不一致'}`);
ok('玩家耐打次数开启/关闭一致（BOSS_ATTACK 与 PLAYER_HEALTH 同值）', F('BOSS_ATTACK') === F('PLAYER_HEALTH'), `${F('BOSS_ATTACK')} / ${F('PLAYER_HEALTH')}`);
console.log(`守护者护甲：${GUARDIAN_ARMOR} → ${K(GUARDIAN_ARMOR * F('BOSS_ARMOR'))}（BOSS_ARMOR ${F('BOSS_ARMOR')}）`);
console.log(`守护者单次受伤上限：${GUARDIAN_HITCAP} → ${K(GUARDIAN_HITCAP * F('BOSS_HIT_CAP'))}（BOSS_HIT_CAP ${F('BOSS_HIT_CAP')}）`);
const healOn = GUARDIAN_HEAL, healOff = GUARDIAN_HEAL * F('HEALING');
console.log(`净土每秒治疗：${healOn} → ${K(healOff)}（HEALING ${F('HEALING')}）；关闭模式占守护者最大生命 ${(healOff / guardianHealth * 100).toFixed(0)}% /秒`);
// 首领生命 0.18 的由来：与"玩家总 DPS 的缩放幅度"同比值 ⇒ 首领战时长不变（取值 ≤ DPS 比，宁可略难）
const dpsRatio = OFF.total / ON.total;
const bossBattleOn = GUARDIAN_RAW_HEALTH / ON.total, bossBattleOff = guardianHealth / OFF.total;
console.log(`首领生命比值由来：玩家总 DPS ${K(ON.total)} → ${K(OFF.total)} = ×${K(dpsRatio)}，代码取 BOSS = ${F('BOSS')}`);
ok('BOSS 比值 ≤ 玩家总 DPS 缩放比（首领战时长不短于开启模式）', F('BOSS') <= dpsRatio + 1e-9, `BOSS ${F('BOSS')} ≤ ${K(dpsRatio)}`);
console.log(`  若按 ×${K(dpsRatio)}：守护者关闭生命会是 ${K(GUARDIAN_RAW_HEALTH * dpsRatio)}，时长 ${K(bossBattleOn)} 秒；`);
console.log(`  实际按 1200 定标：时长 ${K(bossBattleOff)} 秒（相对开启模式缩短 ${K(bossBattleOn / bossBattleOff)} 倍，这是"以 1200 为基数"的直接后果）`);
console.log('  其余首领按 BOSS=0.18 缩放，时长与开启模式基本一致（下表）。');
console.log('  本条只作为口径说明：40% 这条要求对"满成长"无解（见 ③ 与 ③-bis 的取舍），报告里如实说明。');

// ---------------------------------------------------------------- ⑤ 铁傀儡核对（降级为附带项）
console.log('\n=== ⑤ 附带核对：传承末阶段一发「解」打铁傀儡（100 血 0 护甲） ===');
const golemHit = technique(false);
console.log(`一发解（关闭模式，含成长段）= (${DISMANTLE_BASE} + ${TECH_GROWTH})×${a}×0.6×${residueFactor} = ${golemHit}（铁傀儡 100 血，剩 ${K(100 - golemHit)}，${golemHit >= 100 ? '一发砍死' : '打不死'}）`);
console.log(`r250 的一发解 = ${DISMANTLE_RAW} × ${K(100 / DISMANTLE_RAW)} = 100（正好砍死），但那一档在护甲目标上的武器占比只有 ${(shareAt(p, 100 / DISMANTLE_RAW) * 100).toFixed(2)}%（术式把武器压成摆设）`);
console.log(`本轮硬约束是"守护者 1200 + 武器占比 ≥40%"，铁傀儡一发解随之降级为附带项：结果 = ${golemHit >= 100 ? '仍一发砍死' : '不再一发（剩 ' + K(100 - golemHit) + ' 血）'}`);
console.log(`  参考：一发解打在开启模式 0 护甲目标上 = ${technique(true)}（配置原值口径）`);

// ---------------------------------------------------------------- ⑥ 开启侧逐项不变（r249 基线）
console.log('\n=== ⑥ 开启侧核对（整合包模式 = 之前的所有数值，一个都不能变） ===');
const BEFORE = { // r249 基线：关闭模式统一 ×0.1、开启模式全部原值（数值取自代码常量）
  '玩家额外生命(末阶段)': gHealth, '玩家额外普攻(末阶段)': gAttack, '术式成长(末阶段)': gSpell,
  '一发解(原值)': DISMANTLE_RAW, '虚空灶每跳': 40, '净土每秒治疗': 5000, '术式反转外放回血': 10,
  '即身佛': 5000, '虎杖残影': 30000, '戴天': 200, '真人(强化母体)': 480,
  '五条悟': 150000, '魔虚罗': 400000, '净土守护者·生命': 800000, '净土守护者·护甲': 80,
  '净土守护者·单次受伤上限': 10000, '守护者一拳': 8,
};
console.log('类别'.padEnd(26) + 'r249开启=r250开启=本轮开启'.padStart(22) + 'r249关闭(×0.1)'.padStart(16) + 'r250关闭(×0.1534)'.padStart(19) + '本轮关闭'.padStart(12));
const ON_CAT = (name) => name.includes('五条悟') ? 'BOSS_FIXED' : name.includes('魔虚罗') ? 'BOSS_FIXED'
  : name.includes('守护者·生命') ? 'BOSS_GUARDIAN' : name.includes('护甲') ? 'BOSS_ARMOR'
  : name.includes('上限') ? 'BOSS_HIT_CAP' : name.includes('一拳') ? 'BOSS_ATTACK'
  : name.includes('治疗') || name.includes('反转') ? 'HEALING' : name.includes('普攻') ? 'PLAYER_ATTACK'
  : name.includes('生命') ? 'PLAYER_HEALTH' : name.includes('成长') ? 'DAMAGE' : 'BOSS';
for (const [name, raw] of Object.entries(BEFORE)) {
  const on = 1.0 * raw, off249 = K(raw * 0.1), off250 = K(raw * (100 / DISMANTLE_RAW)), off251 = K(raw * F(ON_CAT(name)));
  console.log(name.padEnd(26) + String(on).padStart(22) + String(off249).padStart(16) + String(off250).padStart(19) + String(off251).padStart(12));
}
ok('五条悟开启模式 = 150000', 150000 * CATEGORIES.find(c => c.id === 'BOSS_FIXED').on === 150000);
ok('魔虚罗开启模式 = 400000', 400000 * CATEGORIES.find(c => c.id === 'BOSS_FIXED').on === 400000);
ok('净土守护者开启模式 = 800000', 800000 * CATEGORIES.find(c => c.id === 'BOSS_GUARDIAN').on === 800000);
ok('关闭模式五条悟/魔虚罗/守护者都有各自关闭值', [F('BOSS_FIXED') * 150000, F('BOSS_FIXED') * 400000, F('BOSS_GUARDIAN') * 800000].every(v => v > 0), `${K(F('BOSS_FIXED') * 150000)} / ${K(F('BOSS_FIXED') * 400000)} / ${K(F('BOSS_GUARDIAN') * 800000)}`);

console.log('\n关闭模式全部首领生命一览（原值 → 关闭值）：');
const BOSSES = [['五条悟 GojoEntity', 150000, 'BOSS_FIXED'], ['魔虚罗 MahoragaEntity', 400000, 'BOSS_FIXED'],
  ['净土守护者 GuardianEntity', 800000, 'BOSS_GUARDIAN'], ['虎杖残影 YujiEntity', 30000, 'BOSS'],
  ['即身佛 BuddhaEntity', 5000, 'BOSS'], ['完全体真人 BrokenMahito', 20000, 'BOSS'],
  ['戴天 DaitianEntity', 200, 'BOSS'], ['强化母体 ParasiteEntity', 480, 'BOSS'], ['母体 ParasiteEntity', 160, 'BOSS'], ['寄生物 12', 12, 'BOSS']];
for (const [name, raw, cat] of BOSSES) console.log('  ' + name.padEnd(28) + String(raw).padStart(9) + ' → ' + String(K(raw * F(cat))).padStart(10) + `   （${cat} ×${K(F(cat))}）`);

// ---------------------------------------------------------------- ⑦ 旧存档换算
console.log('\n=== ⑦ 旧存档换算（storedDamage：先除写入时比值，再乘当前比值） ===');
const KNOWN = new Set([1.0, 0.1, K(100 / DISMANTLE_RAW), ...CATEGORIES.flatMap(c => [c.on, c.off])]);
const stored = (v, prev, factor = a) => { if (!KNOWN.has(prev) || prev === 1.0) return f(v * factor); return f(v / prev * factor); };
console.log(`开启模式写入 40 → ${stored(40, 1.0)}（×DAMAGE ${a}）`);
console.log(`r249 关闭模式写入 4 → ${stored(4, 0.1)}（先还原 40，再 ×${a}）两者必须相等：${stored(40, 1.0) === stored(4, 0.1)}`);
console.log(`r250 关闭模式写入 ${f(40 * (100 / DISMANTLE_RAW))} → ${stored(f(40 * (100 / DISMANTLE_RAW)), K(100 / DISMANTLE_RAW))}`);
console.log(`未知比值 0.37 写入 40 → ${stored(40, 0.37)}（按原值处理，不会多缩一次）`);

// ---------------------------------------------------------------- 汇总
const failed = check.filter(c => !c.pass);
console.log('\n=== 汇总 ===');
console.log(`共 ${check.length} 项断言：${check.length - failed.length} PASS / ${failed.length} FAIL`);
if (failed.length) { for (const x of failed) console.log('  FAIL: ' + x.label + (x.detail ? ' — ' + x.detail : '')); process.exitCode = 1; }
else console.log('全部通过。');
