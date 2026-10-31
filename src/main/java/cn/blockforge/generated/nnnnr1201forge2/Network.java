package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import java.util.function.Consumer;

public final class Network {
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(new ResourceLocation("sukuna", "skills"), () -> "sukuna-character-r139", "sukuna-character-r139"::equals, "sukuna-character-r139"::equals);
    public static Consumer<Effect> clientReceiver = e -> {};
    public static Boolean clientModpackMode;
    public static double tooltipFactor(BalanceRules.Category category){return BalanceRules.factor(clientModpackMode!=null?clientModpackMode:BalanceRules.modpackMode(),category);}
    public record Cast(SkillAction action) {}
    public record StreamToggle() {}
    public record TestReset() {}
    public record AimState(boolean enabled,double angle,double range,boolean circle) {}
    public static Consumer<AimState> aimReceiver=s->{};
    public static Runnable testResetReceiver=()->{};
    public static Consumer<CombatState> combatReceiver=s->{};
    public record CombatState(boolean finaleReady,int lingers,boolean stream,int noRecovery,int finaleBurnout){}
    public static void syncCombat(ServerPlayer p){if(MaidBridge.isCaster(p))return;long now=p.level().getGameTime();CHANNEL.send(PacketDistributor.PLAYER.with(()->p),new CombatState(SixfoldFinale.ready(p),CombatExpansion.lingerCount(p),CombatExpansion.streaming(p),(int)Math.max(0,Curse.data(p).getLong("FinaleNoRecovery")-now),(int)Math.max(0,Curse.data(p).getLong("FinaleBurnout")-now)));}
    public static Consumer<MotionState> motionReceiver=m->{};
    public record MotionState(int owner,java.util.UUID token,int kind,int age,float yaw,Vec3 position,boolean end){
        static MotionState read(FriendlyByteBuf b){return new MotionState(b.readVarInt(),b.readUUID(),b.readVarInt(),b.readVarInt(),b.readFloat(),vec(b),b.readBoolean());}
        void write(FriendlyByteBuf b){b.writeVarInt(owner);b.writeUUID(token);b.writeVarInt(kind);b.writeVarInt(age);b.writeFloat(yaw);vec(b,position);b.writeBoolean(end);}
    }
    public static Consumer<ClashState> clashReceiver = p -> {};
    public record ClashPress(java.util.UUID id,boolean down) {}
    public record ClashState(java.util.UUID id,boolean active,int percent,int ready,int remaining,int bossCps,String opponent,int result) {
        static ClashState read(FriendlyByteBuf b){return new ClashState(b.readUUID(),b.readBoolean(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readUtf(128),b.readInt());}
        void write(FriendlyByteBuf b){b.writeUUID(id);b.writeBoolean(active);b.writeVarInt(percent);b.writeVarInt(ready);b.writeVarInt(remaining);b.writeVarInt(bossCps);b.writeUtf(opponent,128);b.writeInt(result);}
    }
    public static Consumer<Relations> relationsReceiver = p -> {};
    public record RelationRequest(int page) {}
    public record RelationChange(java.util.UUID target,int mode,int page) {}
    public record RelationRow(java.util.UUID id,String name,int mode,boolean online) {}
    public record Relations(int page,int pages,double multiplier,boolean voidFloor,java.util.List<RelationRow> rows) {
        static Relations read(FriendlyByteBuf b){
            int page=b.readVarInt(),pages=b.readVarInt();double multiplier=b.readDouble();boolean voidFloor=b.readBoolean();int count=b.readVarInt();
            if(count<0||count>CombatSettings.PAGE_SIZE)throw new IllegalArgumentException("敌友列表长度无效");
            java.util.List<RelationRow> rows=new java.util.ArrayList<>();
            for(int i=0;i<count;i++)rows.add(new RelationRow(b.readUUID(),b.readUtf(64),b.readVarInt(),b.readBoolean()));
            return new Relations(page,pages,multiplier,voidFloor,java.util.List.copyOf(rows));
        }
        void write(FriendlyByteBuf b){b.writeVarInt(page);b.writeVarInt(pages);b.writeDouble(multiplier);b.writeBoolean(voidFloor);b.writeVarInt(rows.size());for(RelationRow row:rows){b.writeUUID(row.id());b.writeUtf(row.name(),64);b.writeVarInt(row.mode());b.writeBoolean(row.online());}}
    }
    public record SettingsToggle(int kind) {}
    public record ControllerRequest(int field, int delta) {}
    public record ControllerValueRequest(int field,String value) {}
    public record ControllerState(double voidDamage, int domainSeconds,
                                  double resonance, int maxRifts, int slashLength, int blackFlashChance, boolean redCut, boolean rewardBoost,
                                  double yujiHealth, double yujiFrequency, double yujiDamage, double buddhaHealth, double buddhaFrequency, double buddhaDamage, net.minecraft.nbt.CompoundTag guardian) {
        static ControllerState read(FriendlyByteBuf b) {
            return new ControllerState(b.readDouble(), b.readVarInt(), b.readDouble(),
                b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readBoolean(), b.readBoolean(), b.readDouble(), b.readDouble(), b.readDouble(),
                b.readDouble(), b.readDouble(), b.readDouble(), b.readNbt());
        }
        void write(FriendlyByteBuf b) {
            b.writeDouble(voidDamage);
            b.writeVarInt(domainSeconds); b.writeDouble(resonance); b.writeVarInt(maxRifts); b.writeVarInt(slashLength);
            b.writeVarInt(blackFlashChance); b.writeBoolean(redCut); b.writeBoolean(rewardBoost);
            b.writeDouble(yujiHealth); b.writeDouble(yujiFrequency); b.writeDouble(yujiDamage); b.writeDouble(buddhaHealth); b.writeDouble(buddhaFrequency); b.writeDouble(buddhaDamage);b.writeNbt(guardian);
        }
    }
    public static Consumer<ControllerState> controllerReceiver = p -> {};
    public static Consumer<Progress> progressReceiver = p -> {};
    public record Progress(double energy,double maximum,int fingers,boolean awakened,boolean siphon,boolean disorder,boolean unlocked,boolean nine,boolean rotation,boolean fist,String guide,boolean soulShrine,boolean voidShrine,boolean pureLandVictory,boolean burnout,double reverseHealing,double reverseCost,boolean modpackMode,int clashPenaltyTicks,boolean sukuna,boolean uraume,int iceFish,int wallSpacing,boolean wallDiagonal,boolean dismantleQi,boolean amplification,boolean gojo,int domainRadiusPts,int domainDamagePts,int domainSpeedPts,boolean domainVerticalKill) {
        static Progress read(FriendlyByteBuf b){return new Progress(b.readDouble(),b.readDouble(),b.readVarInt(),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readUtf(2048),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readDouble(),b.readDouble(),b.readBoolean(),b.readVarInt(),b.readBoolean(),b.readBoolean(),b.readVarInt(),b.readVarInt(),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readBoolean());}
        void write(FriendlyByteBuf b){b.writeDouble(energy);b.writeDouble(maximum);b.writeVarInt(fingers);b.writeBoolean(awakened);b.writeBoolean(siphon);b.writeBoolean(disorder);b.writeBoolean(unlocked);b.writeBoolean(nine);b.writeBoolean(rotation);b.writeBoolean(fist);b.writeUtf(guide,2048);b.writeBoolean(soulShrine);b.writeBoolean(voidShrine);b.writeBoolean(pureLandVictory);b.writeBoolean(burnout);b.writeDouble(reverseHealing);b.writeDouble(reverseCost);b.writeBoolean(modpackMode);b.writeVarInt(clashPenaltyTicks);b.writeBoolean(sukuna);b.writeBoolean(uraume);b.writeVarInt(iceFish);b.writeVarInt(wallSpacing);b.writeBoolean(wallDiagonal);b.writeBoolean(dismantleQi);b.writeBoolean(amplification);b.writeBoolean(gojo);b.writeVarInt(domainRadiusPts);b.writeVarInt(domainDamagePts);b.writeVarInt(domainSpeedPts);b.writeBoolean(domainVerticalKill);}
    }
    public record WallOptions(int halfBlocks,boolean diagonal){}
    /** 伏魔御厨子的三项点数（每项 0~总点数，三项之和 ≤ 可用总点数：开关开=10 / 关=11）与「纵向伤敌」开关：界面按「确定」立刻发这条保存，
     *  服务端整包校验后才写 NBT（存档键名与合法性由 {@link SukunaBranches#configureDomain} 负责；可用总点数见裸类 {@code DomainOptions.total(boolean)}）。
     *  注意：同包顶层还有一个同名的 {@code DomainOptions}（数值换算表），本文件里这个名字指的是这条网络记录。 */
    /** 黑闪标记打击（客户端→服务端）：玩家瞄准红圈并左键。 */
    public record BlackFlashStrike(int targetId){}
    public record DomainOptions(int radiusPts,int damagePts,int speedPts,boolean verticalKill){}
    /** 三击空格：请服务端把自己送上御厨子的神龛（无领域时服务端只提示）。 */
    public record ShrineLeap(){}
    public static Consumer<DrawState> drawReceiver=m->{};
    public record DrawState(java.util.UUID id,int state,double pixels){}
    public record DrawInput(java.util.UUID id,int operation,int width,int height,java.util.List<SlashFormRules.Segment> segments,boolean forward,boolean backward,boolean jump,boolean crouch){
        static DrawInput read(FriendlyByteBuf b){java.util.UUID id=b.readUUID();int op=b.readVarInt(),w=b.readVarInt(),h=b.readVarInt(),n=b.readVarInt();if(n<0||n>128)throw new IllegalArgumentException("纵心线段数量超限");var lines=new java.util.ArrayList<SlashFormRules.Segment>();for(int i=0;i<n;i++)lines.add(new SlashFormRules.Segment(b.readDouble(),b.readDouble(),b.readDouble(),b.readDouble()));return new DrawInput(id,op,w,h,java.util.List.copyOf(lines),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readBoolean());}
        void write(FriendlyByteBuf b){b.writeUUID(id);b.writeVarInt(operation);b.writeVarInt(width);b.writeVarInt(height);b.writeVarInt(segments.size());for(var s:segments){b.writeDouble(s.x1());b.writeDouble(s.y1());b.writeDouble(s.x2());b.writeDouble(s.y2());}b.writeBoolean(forward);b.writeBoolean(backward);b.writeBoolean(jump);b.writeBoolean(crouch);}
    }
    // kind：0解，1领域（direction携带神龛偏移），2炎弓，3火箭，4爆炸，5收回领域，6预留，7捌切痕，8捌范围，9捌结束，10领域命中
    // 7/10 的 owner 为受击实体，direction 为命中点相对实体位置的偏移，seed 为随机种子。
    // 壁纸归属与定位实体分离：受击切痕的 owner 是受害者，绝不能据此读取受害者壁纸。
    public record Effect(int kind, int owner, Vec3 position, Vec3 direction, float size, int life, long seed,
                         java.util.UUID wallpaperOwner, boolean desktopSlashes) {
        public Effect(int kind,int owner,Vec3 position,Vec3 direction,float size,int life,long seed){this(kind,owner,position,direction,size,life,seed,null,false);}
        public Effect wallpaper(java.util.UUID source,boolean all){return new Effect(kind,owner,position,direction,size,life,seed,source,all);}
        public Effect fromCaster(ServerPlayer caster){
            ServerPlayer source=caster instanceof MaidBridge.Actor maid?(Authority.owner(maid)!=null?Authority.owner(maid):MaidBridge.owner(maid)):caster;
            return wallpaper(source==null?null:source.getUUID(),WallpaperPolicy.allSlashes(VoidJourney.evolved(caster),GuardianCombat.awakened(caster)));
        }
        static Effect read(FriendlyByteBuf b) {
            int kind=b.readInt(),owner=b.readInt();Vec3 position=vec(b),direction=vec(b);float size=b.readFloat();int life=b.readInt();long seed=b.readLong();
            return new Effect(kind,owner,position,direction,size,life,seed,b.readBoolean()?b.readUUID():null,b.readBoolean());
        }
        void write(FriendlyByteBuf b) { b.writeInt(kind); b.writeInt(owner); vec(b,position); vec(b,direction); b.writeFloat(size); b.writeInt(life); b.writeLong(seed);b.writeBoolean(wallpaperOwner!=null);if(wallpaperOwner!=null)b.writeUUID(wallpaperOwner);b.writeBoolean(desktopSlashes); }
    }
    static Vec3 vec(FriendlyByteBuf b) { return new Vec3(b.readDouble(),b.readDouble(),b.readDouble()); }
    static void vec(FriendlyByteBuf b,Vec3 v) { b.writeDouble(v.x);b.writeDouble(v.y);b.writeDouble(v.z); }
    public static void init() {
        ObserverPackets.register();
        AuthorityPackets.register();
        SlashTerrain.register();
        TenShadows.register();
        ShadowSystem.register();
        MeleeCombo.register();
        CHANNEL.messageBuilder(Cast.class,0,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->b.writeUtf(m.action().name(),32)).decoder(b->new Cast(SkillAction.valueOf(b.readUtf(32)))).consumerMainThread((m,c)->{
            var p=c.get().getSender();
            if(p!=null&&!character.CharacterService.allowsAction(p,m.action())){c.get().setPacketHandled(true);return;}
            if(p!=null&&m.action()!=SkillAction.CANCEL_CHARGE)MeleeCombo.cancel(p);
            if(p!=null&&(PrisonSystem.locked(p)||ObserverRitual.active(p)||TenShadows.locked(p)||DeathStar.charging(p))){c.get().setPacketHandled(true);return;}
            if(p!=null&&m.action()==SkillAction.CANCEL_CHARGE){Skills.cancelCharge(p);TechniqueInput.cancel(p);c.get().setPacketHandled(true);return;}
            // 无量空处的"收起"放行到所有守卫之前：熔断/对拼/其他动作都不能让它变成"按了不收、再按又放一遍"
            if(p!=null&&m.action()==SkillAction.GOJO_DOMAIN&&GojoCombat.domainOpen(p)){GojoCombat.toggleDomain(p);c.get().setPacketHandled(true);return;}
            if(p!=null&&!SixfoldFinale.active(p)&&!SixfoldFinale.controlled(p)&&!DomainClash.active(p)&&!NetBranches.locked(p)&&!Uraume.locked(p)&&!CombatExpansion.controlled(p)&&!DismantleMotion.active(p)&&!GojoEntity.mindLocked(p)){
                switch(m.action()){
                    case TOGGLE_DISMANTLE -> SlashForms.toggle(p);
                    case SHAPE_SLASH -> SlashForms.cast(p,SlashFormRules.SHAPE);
                    case CROSS_FLIGHT -> SlashForms.cast(p,SlashFormRules.CROSS_FLIGHT);
                    case WORLD_SLASH -> SlashForms.cast(p,SlashFormRules.WORLD);
                    case FREEHAND -> {} // 纵心通过带会话校验的绘制消息处理。
                    case DISMANTLE -> Skills.cast(p,0);
                    case MOTION_BACKFLIP -> DismantleMotion.start(p,DismantleMotionRules.BACKFLIP);
                    case MOTION_KICK -> DismantleMotion.start(p,DismantleMotionRules.FLYING_KICK);
                    case SPIN_SLASH -> DismantleMotion.start(p,DismantleMotionRules.SPIN);
                    case PURPLE_FURNACE -> CombatExpansion.purple(p);
                    case DRAGON_STAR -> CombatExpansion.dragonPress(p);
                    case LINGER_SLASH, CLEAVE_LINGER, SIXFOLD_FINALE -> {} // 需要带按下/松手状态的专用输入。
                    case SLASH_NET -> Skills.cast(p,1);
                    case CROSS_SLASH -> NetBranches.cast(p,NetBranchRules.CROSS_SKILL);
                    case SWORD_QI -> NetBranches.cast(p,NetBranchRules.QI_SKILL);
                    case SPIDER_WEB -> SukunaBranches.cast(p,BranchRules.WEB);
                    case MULTI_SLASH -> SukunaBranches.cast(p,BranchRules.MULTI);
                    case SLASH_WALL -> SukunaBranches.cast(p,BranchRules.WALL);
                    case FROST_CALM -> Uraume.cast(p,BranchRules.FROST);
                    case ICE_FALL -> Uraume.cast(p,BranchRules.ICEFALL);
                    case CLEAVE -> Skills.cast(p,2);
                    case FLAME_BOW -> Skills.cast(p,3);
                    case FURNACE -> Skills.cast(p,4);
                    case DOMAIN -> Skills.cast(p,5);
                    case SOUL_PEEL -> Skills.peel(p);
                    case CHARGE_START -> Skills.charge(p,true);
                    case CHARGE_RELEASE -> Skills.charge(p,false);
                    case ROTATE -> Journey.rotate(p);
                    case CANCEL_CHARGE -> Skills.cancelCharge(p);
                    case CLEAVE_START -> Skills.cleaveCharge(p,true);
                    case CLEAVE_RELEASE -> Skills.cleaveCharge(p,false);
                    case TOGGLE_FIST -> Skills.toggleFist(p);
                    case GUARDIAN_PUNCH -> GuardianPlayerSkills.cast(p,0);
                    case GUARDIAN_DIVERGENT -> GuardianPlayerSkills.cast(p,1);
                    case GUARDIAN_BLOOD -> GuardianPlayerSkills.cast(p,2);
                    case GUARDIAN_BOOST -> GuardianPlayerSkills.cast(p,3);
                    case GUARDIAN_GRID -> GuardianPlayerSkills.cast(p,4);
                    case GUARDIAN_DOMAIN -> GuardianPlayerSkills.cast(p,5);
                    case GUARDIAN_COMBO -> GuardianPlayerSkills.cast(p,6);
                    case GUARDIAN_REVERSE -> GuardianPlayerSkills.cast(p,7);
                    case GUARDIAN_GUARD -> GuardianPlayerSkills.cast(p,8);
                    case GUARDIAN_BEAM -> GuardianPlayerSkills.cast(p,9);
                    case REVERSE -> Curse.reverse(p);
                    case REPAIR_BURNOUT -> Curse.repairBurnout(p);
                    case DOMAIN_AMPLIFICATION -> DomainAmplification.toggle(p);
                    case GOJO_BLUE -> GojoCombat.cast(p,0);
                    case GOJO_RED -> GojoCombat.cast(p,1);
                    case GOJO_RED_MAX -> GojoCombat.cast(p,2);
                    case GOJO_PURPLE -> GojoCombat.cast(p,3);
                    case GOJO_UNLIMITED -> GojoCombat.cast(p,4);
                    case GOJO_TELEPORT -> GojoCombat.cast(p,5);
                    case GOJO_MELEE -> GojoCombat.cast(p,6);
                    case GOJO_DOMAIN -> GojoCombat.cast(p,7);
                    case GOJO_BLUE_MAX -> GojoCombat.cast(p,8);
                }
            }
            c.get().setPacketHandled(true);
        }).add();
        CHANNEL.messageBuilder(Effect.class,1,NetworkDirection.PLAY_TO_CLIENT).encoder(Effect::write).decoder(Effect::read).consumerMainThread((m,c)->{clientReceiver.accept(m);c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(Progress.class,2,NetworkDirection.PLAY_TO_CLIENT).encoder(Progress::write).decoder(Progress::read).consumerMainThread((m,c)->{clientModpackMode=m.modpackMode();progressReceiver.accept(m);c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(RelationRequest.class,3,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->b.writeVarInt(m.page())).decoder(b->new RelationRequest(b.readVarInt())).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)CombatSettings.sync(p,m.page());c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(RelationChange.class,4,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{b.writeUUID(m.target());b.writeVarInt(m.mode());b.writeVarInt(m.page());}).decoder(b->new RelationChange(b.readUUID(),b.readVarInt(),b.readVarInt())).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)CombatSettings.change(p,m.target(),m.mode(),m.page());c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(Relations.class,5,NetworkDirection.PLAY_TO_CLIENT).encoder(Relations::write).decoder(Relations::read).consumerMainThread((m,c)->{relationsReceiver.accept(m);c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(SettingsToggle.class,6,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->b.writeVarInt(m.kind())).decoder(b->new SettingsToggle(b.readVarInt())).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)CombatSettings.toggle(p,m.kind());c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(ControllerRequest.class,7,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{b.writeVarInt(m.field());b.writeInt(m.delta());}).decoder(b->new ControllerRequest(b.readVarInt(),b.readInt())).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)VoidController.change(p,m.field(),m.delta());c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(ControllerState.class,8,NetworkDirection.PLAY_TO_CLIENT).encoder(ControllerState::write).decoder(ControllerState::read).consumerMainThread((m,c)->{controllerReceiver.accept(m);c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(ControllerValueRequest.class,9,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{b.writeVarInt(m.field());b.writeUtf(m.value(),256);}).decoder(b->new ControllerValueRequest(b.readVarInt(),b.readUtf(256))).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)VoidController.setValue(p,m.field(),m.value());c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(ClashPress.class,10,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{b.writeUUID(m.id());b.writeBoolean(m.down());}).decoder(b->new ClashPress(b.readUUID(),b.readBoolean())).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)DomainClash.press(p,m.id(),m.down());c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(ClashState.class,11,NetworkDirection.PLAY_TO_CLIENT).encoder(ClashState::write).decoder(ClashState::read).consumerMainThread((m,c)->{clashReceiver.accept(m);c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(WallOptions.class,12,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{b.writeVarInt(m.halfBlocks());b.writeBoolean(m.diagonal());}).decoder(b->new WallOptions(b.readVarInt(),b.readBoolean())).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null&&Journey.unlocked(p)&&Curse.awakened(p)){SukunaBranches.configure(p,m.halfBlocks(),m.diagonal());Curse.sync(p);}c.get().setPacketHandled(true);}).add();
        // 24 = 空格三击跳上神龛（0-23 已被占用，见上面各处 messageBuilder 的 ID）。
        CHANNEL.messageBuilder(ShrineLeap.class,24,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{}).decoder(b->new ShrineLeap()).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)Skills.leapToShrine(p);c.get().setPacketHandled(true);}).add();
        // 25 = 伏魔御厨子的点数分配（界面「确定」时发来）。这里只校验"能开御厨子的玩家"再落盘，不查冷却/结印：
        // 玩家通常在结印那 3 秒里按确定，用 Skills.available 会把这次保存挡掉。
        CHANNEL.messageBuilder(DomainOptions.class,25,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{b.writeVarInt(m.radiusPts());b.writeVarInt(m.damagePts());b.writeVarInt(m.speedPts());b.writeBoolean(m.verticalKill());}).decoder(b->new DomainOptions(b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readBoolean())).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null&&Journey.unlocked(p)){SukunaBranches.configureDomain(p,m.radiusPts(),m.damagePts(),m.speedPts(),m.verticalKill());Curse.sync(p);}c.get().setPacketHandled(true);}).add();
    // 26：黑闪标记打击（PLAY_TO_SERVER）；237 标记特效复用 Effect 包
    CHANNEL.messageBuilder(BlackFlashStrike.class,26,NetworkDirection.PLAY_TO_SERVER)
        .encoder((m,b)->b.writeVarInt(m.targetId())).decoder(b->new BlackFlashStrike(b.readVarInt()))
        .consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)BlackFlashMarks.strike(p,m.targetId());c.get().setPacketHandled(true);}).add();
    }
    public static void registerDrawing(){
        WallpaperPackets.register();
        CHANNEL.messageBuilder(AimState.class,23,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->{b.writeBoolean(m.enabled());b.writeDouble(m.angle());b.writeDouble(m.range());b.writeBoolean(m.circle());}).decoder(b->new AimState(b.readBoolean(),b.readDouble(),b.readDouble(),b.readBoolean())).consumerMainThread((m,c)->{aimReceiver.accept(m);c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(TestReset.class,22,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->{}).decoder(b->new TestReset()).consumerMainThread((m,c)->{testResetReceiver.run();c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(CombatState.class,18,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->{b.writeBoolean(m.finaleReady());b.writeVarInt(m.lingers());b.writeBoolean(m.stream());b.writeVarInt(m.noRecovery());b.writeVarInt(m.finaleBurnout());}).decoder(b->new CombatState(b.readBoolean(),b.readVarInt(),b.readBoolean(),b.readVarInt(),b.readVarInt())).consumerMainThread((m,c)->{combatReceiver.accept(m);c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(TechniqueInput.Input.class,16,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{b.writeVarInt(m.skill());b.writeByte(m.phase());b.writeBoolean(m.forward());b.writeBoolean(m.backward());b.writeBoolean(m.jump());b.writeBoolean(m.crouch());b.writeVarInt(m.motion());b.writeVarInt(m.wheelSkill());}).decoder(b->new TechniqueInput.Input(b.readVarInt(),b.readUnsignedByte(),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readBoolean(),b.readVarInt(),b.readVarInt())).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)TechniqueInput.receive(p,m);c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(StreamToggle.class,17,NetworkDirection.PLAY_TO_SERVER).encoder((m,b)->{}).decoder(b->new StreamToggle()).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)CombatExpansion.toggleStream(p);c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(MotionState.class,15,NetworkDirection.PLAY_TO_CLIENT).encoder(MotionState::write).decoder(MotionState::read).consumerMainThread((m,c)->{motionReceiver.accept(m);c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(DrawInput.class,13,NetworkDirection.PLAY_TO_SERVER).encoder(DrawInput::write).decoder(DrawInput::read).consumerMainThread((m,c)->{var p=c.get().getSender();if(p!=null)FreehandDrawing.input(p,m);c.get().setPacketHandled(true);}).add();
        CHANNEL.messageBuilder(DrawState.class,14,NetworkDirection.PLAY_TO_CLIENT).encoder((m,b)->{b.writeUUID(m.id());b.writeVarInt(m.state());b.writeDouble(m.pixels());}).decoder(b->new DrawState(b.readUUID(),b.readVarInt(),b.readDouble())).consumerMainThread((m,c)->{drawReceiver.accept(m);c.get().setPacketHandled(true);}).add();
    }
    /**
     * 只发给一名玩家：**开界面/弹面板这类"个人 UI"必须用这个**。
     * 用 {@link #send(ServerLevel, Effect)} 广播的话，附近所有玩家的客户端都会打开同一块界面。
     */
    public static void sendTo(ServerPlayer p, Effect e) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), e);
    }
    public static void send(ServerLevel level, Effect e) {
        // 东堂葵：登记这条特效的位置，之后"纯客户端特效"能被注入成实体（不义游戏换位用）
        character.todo.TodoMarks.anchor(level, e);
        SkillDestruction.effect(level,e);
        var entity=level.getEntity(e.owner());ServerPlayer caster=entity instanceof ServerPlayer p?p:MaidBridge.caster(entity);
        // 定位在受击者身上的消息必须由发送点明确提供施术者，不在这里猜测。
        boolean targetEffect=e.kind()==7||e.kind()==10||e.kind()==11||e.kind()==FinaleRules.FX_SCAR||e.kind()==93&&e.direction().lengthSqr()<.01;
        Effect visual=e.wallpaperOwner()==null&&!e.desktopSlashes()&&caster!=null&&!targetEffect?e.fromCaster(caster):e;
        Effect tint=caster==null?null:new Effect(144,e.owner(),e.position(),Vec3.ZERO,1,400,GuardianCombat.awakened(caster)?1:0);
        if(e.size()==SlashReach.INFINITE){for(ServerPlayer listener:level.players()){var audience=PacketDistributor.PLAYER.with(()->listener);if(tint!=null)CHANNEL.send(audience,tint);CHANNEL.send(audience,visual);}return;}
        var audience=PacketDistributor.NEAR.with(()->new PacketDistributor.TargetPoint(e.position.x,e.position.y,e.position.z,512,level.dimension()));if(tint!=null)CHANNEL.send(audience,tint);CHANNEL.send(audience,visual);
    }
    public static void sendController(ServerPlayer p) { if(MaidBridge.isCaster(p))return;CHANNEL.send(PacketDistributor.PLAYER.with(()->p),VoidController.state(p)); }
    public static void sendControllerRequest(int field, int delta) { CHANNEL.sendToServer(new ControllerRequest(field,delta)); }
    public static void sendControllerValue(int field,String value) { CHANNEL.sendToServer(new ControllerValueRequest(field,value)); }
}
