def replace_block(path, start_marker, end_marker, new_text):
    s = open(path, encoding='utf-8').read()
    i = s.index(start_marker)
    j = s.index(end_marker, i)
    s = s[:i] + new_text + s[j:]
    open(path, 'w', encoding='utf-8').write(s)
    print('replaced', start_marker[:40])

p = 'src/main/java/character/jogo/JogoCombat.java'

# 1) castPillars -> 扇形火焰持续通道
replace_block(p, '    public static void castPillars(', '    /** 岩浆陷阱', '''    public static void castPillars(ServerPlayer p,CharacterSkill skill,int rank) {
        float damage=CombatSettings.spellDamage(p,skill.damageAt(rank));
        FANS.add(new Fan(p.serverLevel(),p.getUUID(),p.getLookAngle(),damage,p.getServer().getTickCount()+PILLAR_LIFE,0));
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.FIRECHARGE_USE,SoundSource.PLAYERS,1.3f,0.8f);
    }
    /** 火焰喷发：每 2 刻向前推一波扇形火舌，持续 15 秒。 */
    private static void tickFans() {
        if(FANS.isEmpty())return;
        for(Iterator<Fan> it=FANS.iterator();it.hasNext();){
            Fan fan=it.next();ServerLevel level=fan.level();
            ServerPlayer owner=level.getServer().getPlayerList().getPlayer(fan.owner());
            if(owner==null||level.getServer().getTickCount()>=fan.end()){it.remove();continue;}
            if(level.getServer().getTickCount()%2!=0)continue;
            Vec3 side=fan.dir().cross(new Vec3(0,1,0)).normalize();
            int count=Math.max(3,Math.min(15,1+CharacterService.rank(owner)*5));
            double reach=6+(level.getServer().getTickCount()%60)*0.35;   // 火舌向前推进
            for(int i=0;i<count;i++){
                double angle=Math.toRadians(-22+44.0*(count==1?0.5:i/(double)(count-1)));
                Vec3 dir=fan.dir().scale(Math.cos(angle)).add(side.scale(Math.sin(angle))).normalize();
                Vec3 at=owner.getEyePosition().add(dir.scale(reach));
                Network.send(level,new Network.Effect(JogoFx.PILLAR,0,at,dir,1,6,0));
                for(var target:level.getEntitiesOfClass(LivingEntity.class,
                        new net.minecraft.world.phys.AABB(at,at).inflate(2.2),t->CombatSettings.canHit(owner,t))){
                    AttackTypes.mark(AttackTypes.MAGIC);
                    try{CombatSettings.hurt(owner,target,owner.damageSources().indirectMagic(owner,owner),fan.damage());}
                    finally{AttackTypes.clear();}
                    target.setSecondsOnFire(3);
                }
            }
        }
    }
    /** 火山喷发：一条粗大的火柱，每刻前进 2.5 格，最远 40 格。 */
    private static void tickColumns() {
        if(COLUMNS.isEmpty())return;
        for(Iterator<Column> it=COLUMNS.iterator();it.hasNext();){
            Column c=it.next();ServerLevel level=c.level();
            ServerPlayer owner=level.getServer().getPlayerList().getPlayer(c.owner());
            if(owner==null||c.traveled()>=40){it.remove();continue;}
            Vec3 next=c.pos().add(c.dir().scale(2.5));
            Network.send(level,new Network.Effect(JogoFx.PILLAR,0,next,c.dir(),2,8,0));
            for(var target:level.getEntitiesOfClass(LivingEntity.class,
                    new net.minecraft.world.phys.AABB(c.pos(),next).inflate(2.2),t->CombatSettings.canHit(owner,t))){
                AttackTypes.mark(AttackTypes.MAGIC);
                try{CombatSettings.hurt(owner,target,owner.damageSources().indirectMagic(owner,owner),c.damage());}
                finally{AttackTypes.clear();}
                target.setSecondsOnFire(4);
            }
            // 沿途地表化为焦岩（8 秒后还原）
            int x=(int)Math.floor(next.x),z=(int)Math.floor(next.z);
            int y=level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,x,z);
            var pos=new BlockPos(x,y,z);
            BlockState old=level.getBlockState(pos);
            if(!old.isAir()&&!old.hasBlockEntity()&&old.getDestroySpeed(level,pos)>=0){
                BEAM_JOURNAL.computeIfAbsent(owner.getUUID(),k->new LinkedHashMap<>()).putIfAbsent(pos,old);
                level.setBlock(pos,Blocks.MAGMA_BLOCK.defaultBlockState(),2);
                BEAM_EXPIRE.putIfAbsent(owner.getUUID(),level.getGameTime()+160);
            }
            COLUMNS.set(COLUMNS.indexOf(c),new Column(level,c.owner(),next,c.dir(),c.damage(),c.traveled()+2.5));
        }
    }

''')

# 2) castVolcanoBeam -> 生成 Column
replace_block(p, '    public static void castVolcanoBeam(', '    /** 岩浆陷阱', '''    public static void castVolcanoBeam(ServerPlayer p,CharacterSkill skill,int rank) {
        COLUMNS.add(new Column(p.serverLevel(),p.getUUID(),p.getEyePosition().add(p.getLookAngle().scale(1.2)),
            p.getLookAngle(),CombatSettings.spellDamage(p,skill.damageAt(rank)),0));
        p.serverLevel().playSound(null,p.blockPosition(),SoundEvents.BLAZE_SHOOT,SoundSource.PLAYERS,1.4f,0.6f);
    }
''')

# 3) 删除旧 tickPillars
s = open(p, encoding='utf-8').read()
i = s.index('    private static void tickPillars() {')
j = s.index('    /** 火焰喷发', i) if '    /** 火焰喷发' in s[i:i+4000] else s.index('    private static boolean pillarFlies', i)
s = s[:i] + s[j:]
open(p, 'w', encoding='utf-8').write(s)
print('removed old tickPillars')

# 4) 章节完成条件：不再要求击杀
s = open(p, encoding='utf-8').read()
s = s.replace('if(data.getBoolean(CLASH_DONE)&&data.getBoolean(GOJO_KILL)&&p.level().getGameTime()-data.getLong(CLASH_AT)>=GOJO_SURVIVE){',
              'if(data.getBoolean(CLASH_DONE)&&p.level().getGameTime()-data.getLong(CLASH_AT)>=GOJO_SURVIVE){', 1)
open(p, 'w', encoding='utf-8').write(s)
print('chapter3 no kill')
