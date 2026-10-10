package cn.blockforge.generated.nnnnr1201forge2;

/** 纯加法计算；服务端配置加载和重载时替换整张表。 */
public final class GrowthRules {
    public record Row(double health,double attack,double spell,double energy){
        public Row {if(!valid(health)||!valid(attack)||!valid(spell)||!valid(energy))throw new IllegalArgumentException("成长数值必须非负且有限");}
        private static boolean valid(double value){return Double.isFinite(value)&&value>=0;}
        public double column(int column){return switch(column){case 0->health;case 1->attack;case 2->spell;case 3->energy;default->throw new IllegalArgumentException("成长列越界");};}
    }
    public static final Row[] DEFAULTS={new Row(4,1,4,200),new Row(10,6,50,600),new Row(20,4,20,800),new Row(20,10,100,1000),new Row(40,7,70,1700),new Row(200,20,200,2000)};
    private record Table(Row[] rows,double baseEnergy,double residueAttack,double residueSpell){}
    private static volatile Table table=new Table(DEFAULTS.clone(),1000,3,3);
    public static void configure(Row[] rows,double baseEnergy,double residueAttack,double residueSpell){
        if(rows.length!=6||!Row.valid(baseEnergy)||!Row.valid(residueAttack)||!Row.valid(residueSpell))throw new IllegalArgumentException("成长表无效");
        for(Row row:rows)if(row==null)throw new IllegalArgumentException("成长行缺失");
        table=new Table(rows.clone(),baseEnergy,residueAttack,residueSpell);
    }
    public static void reset(){configure(DEFAULTS,1000,3,3);}
    private static double total(Table table,int fingers,boolean body,boolean nine,boolean soul,boolean voidWin,boolean guardian,int column){
        Row[] r=table.rows;return Math.max(0,Math.min(20,fingers))*r[0].column(column)+(body?r[1].column(column):0)+(nine?r[2].column(column):0)+(soul?r[3].column(column):0)+(voidWin?r[4].column(column):0)+(guardian?r[5].column(column):0);
    }
    public static double health(int f,boolean b,boolean n,boolean s,boolean v,boolean g){return total(table,f,b,n,s,v,g,0);}
    public static double attack(int f,boolean b,boolean n,boolean s,boolean v,boolean g){return total(table,f,b,n,s,v,g,1);}
    public static double residueAttack(int residue){return Math.max(0,residue)*table.residueAttack;}
    public static double spell(int f,boolean b,boolean n,boolean s,boolean v,boolean g,int residue){Table t=table;return total(t,f,b,n,s,v,g,2)+Math.max(0,residue)*t.residueSpell;}
    public static double energy(int f,boolean b,boolean n,boolean s,boolean v,boolean g){Table t=table;return t.baseEnergy+total(t,f,b,n,s,v,g,3);}
    private GrowthRules(){}
}
