package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

/** 主 config 目录中的成长配置；不改变已经获得的阶段，只重新计算固定加成。 */
public final class GrowthConfig {
    public static final String FILE="sukuna-growth.toml";
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.DoubleValue BASE_ENERGY, RESIDUE_ATTACK, RESIDUE_SPELL;
    public static final ForgeConfigSpec.IntValue REVEAL_DENOMINATOR;
    public static final ForgeConfigSpec.BooleanValue CONTROLLER_CRAFTING, MODPACK_MODE;
    public static final Stage FINGER,BODY,NINE,SOUL,VOID,GUARDIAN;
    public record Stage(ForgeConfigSpec.DoubleValue health,ForgeConfigSpec.DoubleValue attack,
                        ForgeConfigSpec.DoubleValue spell,ForgeConfigSpec.DoubleValue energy) {
        public String description(){return description(BalanceRules.factor());}
        public String description(double factor){return "生命+"+number(value(health)*factor)+"、普攻+"+number(value(attack)*factor)+"、术式+"+number(value(spell)*factor)+"、咒力上限+"+number(value(energy));}
    }
    static {
        ForgeConfigSpec.Builder b=new ForgeConfigSpec.Builder();
        b.comment("整合包模式：配置内始终填写原始数值；修改后彻底重启游戏或服务器。联机战斗以服务端为准。").push("balance");
        MODPACK_MODE=b.comment("true：保留r63战斗强度；false（默认）：本模组的生命/攻击成长、术式与首领伤害、首领生命/护甲/受伤上限、治疗均为原值10%。", "咒力上限、恢复、消耗始终使用配置原值，不受此开关影响。不改持续时间、冷却、数量、次数、概率、阶段阈值、范围、移动/弹道速度、减伤比例、物品掉落和任务条件；不改原版/其他模组的基础属性。女仆术式原有减半仍保留。").define("modpackMode",false);
        b.pop();
        b.comment("固定成长：全部采用加法；文件位于游戏主目录 config/sukuna-growth.toml。修改后重启游戏或服务器，已有角色也会重新计算。生命2点=1颗心。").push("growth");
        BASE_ENERGY=b.comment("无成长时的基础咒力上限").defineInRange("baseEnergy",1000.0,0.0,1000000.0);
        FINGER=stage(b,"finger","每根手指，最多计20根",GrowthRules.DEFAULTS[0]);
        BODY=stage(b,"body","即身佛肉体融合，仅一次",GrowthRules.DEFAULTS[1]);
        NINE=stage(b,"nine","九相图融合，仅一次",GrowthRules.DEFAULTS[2]);
        SOUL=stage(b,"soul","击败虎杖残影，仅一次",GrowthRules.DEFAULTS[3]);
        VOID=stage(b,"void","击败五条悟，仅一次",GrowthRules.DEFAULTS[4]);
        GUARDIAN=stage(b,"guardian","净土通关，仅一次",GrowthRules.DEFAULTS[5]);
        RESIDUE_ATTACK=b.comment("每次残魂凝练的额外普攻，保留原有成长").defineInRange("residueAttack",3.0,0.0,10000.0);
        RESIDUE_SPELL=b.comment("每次残魂凝练的额外术式伤害").defineInRange("residueSpell",3.0,0.0,10000.0);
        b.pop().push("revelation");
        REVEAL_DENOMINATOR=b.comment("术式公开概率的分母：50代表1/50，即2%。伤害额外+10至+100，越高越稀有。").defineInRange("oneIn",50,1,1000000);
        b.pop().push("crafting");
        CONTROLLER_CRAFTING=b.comment("是否允许用工作台九格泥土合成咒术战斗控制器。默认关闭；修改后重启游戏或服务器，联机以服务端为准。").define("controllerCrafting",false);
        b.pop();SPEC=b.build();
    }
    private static Stage stage(ForgeConfigSpec.Builder b,String key,String comment,GrowthRules.Row row){
        double h=row.health(),a=row.attack(),s=row.spell(),e=row.energy();
        b.comment(comment).push(key);
        Stage result=new Stage(b.comment("额外生命点数").defineInRange("health",h,0.0,10000.0),
            b.comment("额外普攻伤害").defineInRange("attack",a,0.0,10000.0),
            b.comment("额外术式伤害").defineInRange("spell",s,0.0,1000000.0),
            b.comment("额外咒力上限").defineInRange("energy",e,0.0,1000000.0));
        b.pop();return result;
    }
    private static double value(ForgeConfigSpec.DoubleValue v){return SPEC.isLoaded()?v.get():v.getDefault();}
    private static String number(double v){return BalanceRules.number(v);}
    private static void changed(net.minecraftforge.fml.event.config.ModConfigEvent event){
        if(event.getConfig().getSpec()!=SPEC)return;
        if(event instanceof net.minecraftforge.fml.event.config.ModConfigEvent.Unloading){GrowthRules.reset();BalanceRules.configure(false);return;}
        applyCurrentValues();
    }
    /** 配置界面保存后同步纯数值缓存；配方和已有实体仍按提示重启后生效。 */
    static void applyCurrentValues(){
        if(!SPEC.isLoaded())return;
        BalanceRules.configure(MODPACK_MODE.get());
        Stage[] stages={FINGER,BODY,NINE,SOUL,VOID,GUARDIAN};GrowthRules.Row[] rows=new GrowthRules.Row[stages.length];
        for(int i=0;i<stages.length;i++){Stage s=stages[i];rows[i]=new GrowthRules.Row(s.health().get(),s.attack().get(),s.spell().get(),s.energy().get());}
        GrowthRules.configure(rows,BASE_ENERGY.get(),RESIDUE_ATTACK.get(),RESIDUE_SPELL.get());
    }
    public static void register(){ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON,SPEC,FILE);net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus().addListener(GrowthConfig::changed);}
    private GrowthConfig(){}
}
