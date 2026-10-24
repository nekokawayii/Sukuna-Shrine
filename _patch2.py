def patch(path, pairs):
    s = open(path, encoding='utf-8').read()
    for old, new in pairs:
        assert old in s, (path, old[:70])
        s = s.replace(old, new, 1)
    open(path, 'w', encoding='utf-8').write(s)
    print('patched', path)

# ---------- 1) 技能定义：两个技能区分开 ----------
patch('src/main/java/character/jogo/JogoSkills.java', [
 ('''            new CharacterSkill(LAVA_BURST,"灾厄·火焰喷发","火焰喷发",
                "单手前指，向前方扇形喷出 1~15 条岩浆柱，灼烧范围内的敌人。",
                0xFFFFB024,220,160,CONE,18,2,22,14,3,15,3,40,1),''',
  '''            new CharacterSkill(LAVA_BURST,"灾厄·火焰喷发","火焰喷发",
                "向前方持续喷出扇形火焰，火舌不断向前飞，持续 15 秒。",
                0xFFFFB024,220,160,CONE,22,2,22,10,2,15,3,40,1),'''),
 ('''            new CharacterSkill(VOLCANO_BEAM,"火山喷发","火山喷发",
                "掌心喷出高压岩浆柱，直线贯穿前方 10 格，沿途方块化为焦岩。",
                0xFFFF5A10,260,180,RAY,10,.6,35,20,3,1,1,0,0),''',
  '''            new CharacterSkill(VOLCANO_BEAM,"火山喷发","火山喷发",
                "掌心喷出一条粗大的火柱，向前飞 40 格，沿途方块化为焦岩。",
                0xFFFF5A10,260,180,RAY,40,1.4,35,26,4,1,1,0,0),'''),
])

# ---------- 2) 章节：第三章不再需要击杀五条悟 ----------
patch('src/main/java/character/jogo/JogoStory.java', [
 ('"用漏壶击败五条悟，并至少完成一次领域对拼（无量空处 vs 盖棺铁围山），对拼后活过 3 分钟","灾厄火焰：五条悟"',
  '"与无量空处完成一次领域对拼（无量空处 vs 盖棺铁围山），对拼结束后活过 3 分钟","灾厄火焰：五条悟"'),
])

# ---------- 3) JogoCombat：扇形火焰持续前飞 15 秒 + 40 格粗火柱 ----------
p = 'src/main/java/character/jogo/JogoCombat.java'
patch(p, [
 # 去掉击杀条件
 ('if(data.getBoolean(CLASH_DONE)&&data.getBoolean(GOJO_KILL)&&p.level().getGameTime()-data.getLong(CLASH_AT)>=GOJO_SURVIVE){',
  'if(data.getBoolean(CLASH_DONE)&&p.level().getGameTime()-data.getLong(CLASH_AT)>=GOJO_SURVIVE){'),
 # 旧的三道岩浆柱状态改成"扇形火焰通道"
 ('''    private record Pillar(ServerLevel level, UUID owner, Vec3 pos, Vec3 dir, float damage, Set<UUID> hit, int ticks) {}''',
  '''    private record Pillar(ServerLevel level, UUID owner, Vec3 pos, Vec3 dir, float damage, Set<UUID> hit, int ticks) {}
    /** 火焰喷发：持续 15 秒不断向前飞的扇形火焰。 */
    private record Fan(ServerLevel level, UUID owner, Vec3 dir, float damage, int end, int wave) {}
    /** 火山喷发：一条粗大的火柱，向前飞 40 格。 */
    private record Column(ServerLevel level, UUID owner, Vec3 pos, Vec3 dir, float damage, double traveled) {}
    private static final List<Fan> FANS = new ArrayList<>();
    private static final List<Column> COLUMNS = new ArrayList<>();'''),
 # 施法：火焰喷发改登记 Fan；火山喷发改生成 Column
 ('''        for(int i=0;i<count;i++){
            double angle=Math.toRadians(-22+44.0*(count==1?0.5:i/(double)(count-1)));
            Vec3 dir=look.scale(Math.cos(angle)).add(side.scale(Math.sin(angle))).normalize();
            PILLARS.add(new Pillar(p.serverLevel(),p.getUUID(),start.add(dir.scale(0.6)),dir,damage,new HashSet<>(),0));
        }''',
  '''        FANS.add(new Fan(p.serverLevel(),p.getUUID(),look,damage,p.getServer().getTickCount()+PILLAR_LIFE,0));
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.FIRECHARGE_USE,SoundSource.PLAYERS,1.3f,0.8f);'''),
 ('''    public static void castVolcanoBeam(ServerPlayer p,CharacterSkill skill,int rank) {
        Vec3 start=p.getEyePosition(),dir=p.getLookAngle(),end=start.add(dir.scale(skill.range()));''',
  '''    public static void castVolcanoBeam(ServerPlayer p,CharacterSkill skill,int rank) {
        Vec3 start=p.getEyePosition(),dir=p.getLookAngle();
        COLUMNS.add(new Column(p.serverLevel(),p.getUUID(),start.add(dir.scale(1.2)),dir,
            CombatSettings.spellDamage(p,skill.damageAt(rank)),0));
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.BLAZE_SHOOT,SoundSource.PLAYERS,1.4f,0.6f);
    }
    /** 火山喷发旧实现（直线贯穿 + 焦岩）保留为内部方法，改由飞行火柱逐段调用。 */
    private static void legacyBeamTick(ServerPlayer p,Vec3 start,Vec3 end,float dmg) {
        Vec3 dir=end.subtract(start).normalize();
        {'''),
 # tickPillars 改为 tickFans，并加 tickColumns
 ('        tickBugs();tickMeteors();tickFalling();tickPillars();tickDomains();JogoTerrain.tick();tickBeam();',
  '        tickBugs();tickMeteors();tickFalling();tickFans();tickColumns();tickDomains();JogoTerrain.tick();tickBeam();'),
])
print('done')
