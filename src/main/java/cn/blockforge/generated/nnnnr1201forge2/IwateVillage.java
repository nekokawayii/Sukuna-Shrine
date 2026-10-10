package cn.blockforge.generated.nnnnr1201forge2;

import net.minecraft.core.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.chunk.ChunkAccess;

/** 跨区块写入始终裁剪到当前区块，保证大型院落与村落无接缝。 */
public final class IwateVillage {
    public static final int HOME_X=48,HOME_Z=-64,HOME_SIZE=128,Y=1301;
    private static int local(int n){return Math.floorMod(n+128,384)-128;}
    public static int height(int x,int z){return IwateTerrain.height(x,z);}
    private static boolean owns(ChunkAccess c,int x,int z){if(IwateStructures.capturing())return true;return x>=c.getPos().getMinBlockX()&&x<=c.getPos().getMaxBlockX()&&z>=c.getPos().getMinBlockZ()&&z<=c.getPos().getMaxBlockZ();}
    public static void set(ChunkAccess c,int x,int y,int z,Block b){set(c,x,y,z,b.defaultBlockState());}
    public static void set(ChunkAccess c,int x,int y,int z,BlockState s){if(IwateStructures.capture(x,y,z,s))return;if(owns(c,x,z))c.setBlockState(new BlockPos(x,y,z),s,false);}
    private static void blockNbt(ChunkAccess c,CompoundTag tag){if(!IwateStructures.captureNbt(tag))c.setBlockEntityNbt(tag);}
    public static void box(ChunkAccess c,int x,int y,int z,int xx,int yy,int zz,Block b){box(c,x,y,z,xx,yy,zz,b.defaultBlockState());}
    private static void box(ChunkAccess c,int x,int y,int z,int xx,int yy,int zz,BlockState s){
        if(IwateStructures.capturing()){for(int i=x;i<=xx;i++)for(int k=z;k<=zz;k++)for(int j=y;j<=yy;j++)set(c,i,j,k,s);return;}
        for(int i=Math.max(x,c.getPos().getMinBlockX());i<=Math.min(xx,c.getPos().getMaxBlockX());i++)for(int k=Math.max(z,c.getPos().getMinBlockZ());k<=Math.min(zz,c.getPos().getMaxBlockZ());k++)for(int j=y;j<=yy;j++)set(c,i,j,k,s);
    }
    public static void sign(ChunkAccess c,int x,int y,int z,String a,String b,String d,String e,int rotation){
        if(!owns(c,x,z))return;set(c,x,y,z,Blocks.OAK_SIGN.defaultBlockState().setValue(BlockStateProperties.ROTATION_16,rotation));
        CompoundTag tag=blockTag("minecraft:sign",x,y,z),front=new CompoundTag();ListTag lines=new ListTag();
        for(String s:new String[]{a,b,d,e})lines.add(StringTag.valueOf(Component.Serializer.toJson(Component.literal(s))));
        front.put("messages",lines);front.putString("color","brown");front.putBoolean("has_glowing_text",false);tag.put("front_text",front);tag.put("back_text",front.copy());tag.putBoolean("is_waxed",true);blockNbt(c,tag);
    }
    private static CompoundTag blockTag(String id,int x,int y,int z){CompoundTag t=new CompoundTag();t.putString("id",id);t.putInt("x",x);t.putInt("y",y);t.putInt("z",z);return t;}
    private static void chest(ChunkAccess c,int x,int y,int z,boolean photo,String... items){
        if(!owns(c,x,z))return;set(c,x,y,z,Blocks.CHEST);CompoundTag t=blockTag("minecraft:chest",x,y,z);
        if(photo){t.putString("LootTable","sukuna:chests/iwate_house");t.putLong("LootTableSeed",x*341873128712L^z*132897987541L);}
        else{ListTag list=new ListTag();for(int i=0;i<items.length;i++){CompoundTag item=new CompoundTag();item.putByte("Slot",(byte)i);item.putString("id","minecraft:"+items[i]);item.putByte("Count",(byte)1);list.add(item);}t.put("Items",list);}blockNbt(c,t);
    }
    private static void photoChest(ChunkAccess c,int x,int y,int z){
        if(!owns(c,x,z))return;set(c,x,y,z,Blocks.CHEST);CompoundTag t=blockTag("minecraft:chest",x,y,z);t.putString("LootTable","sukuna:chests/yuji_home");t.putLong("LootTableSeed",x*341873128712L^z*132897987541L);blockNbt(c,t);
    }
    private static boolean intersects(ChunkAccess c,int x,int z,int w,int d){if(IwateStructures.capturing())return true;return c.getPos().getMaxBlockX()>=x&&c.getPos().getMinBlockX()<x+w&&c.getPos().getMaxBlockZ()>=z&&c.getPos().getMinBlockZ()<z+d;}
    static void blueprint(String name){switch(name){case "farmhouse"->{farmhouse(null,0,0);for(int x=1;x<=17;x++)for(int z=25;z<=35;z++){set(null,x,Y-2,z,Blocks.CLAY);if(x==1||x==17||z==25||z==35)set(null,x,Y-1,z,Blocks.MOSSY_COBBLESTONE);else {set(null,x,Y-1,z,Blocks.WATER);if((x+z)%3!=0)set(null,x,Y,z,SoulContent.RICE.get());}}}case "warehouse"->warehouse(null,0,0);case "shrine"->shrine(null,0,0);case "bus"->bus(null,0,0);case "school"->school(null,0,0);case "clinic"->clinic(null,0,0);case "station"->station(null,0,0);case "home"->home(null);default->{shell(null,0,0,13,13,1);table(null,3,Y,6);bedding(null,3,Y,2);chest(null,9,Y,3,true);door(null,6,Y,12,false);sign(null,6,Y,14,"森林木屋","旧日的回忆","", "",0);}}}
    public static void generate(ChunkAccess c){
        for(int x=c.getPos().getMinBlockX();x<=c.getPos().getMaxBlockX();x++)for(int z=c.getPos().getMinBlockZ();z<=c.getPos().getMaxBlockZ();z++){
            if(x>=48&&x<176&&z>=-64&&z<64)continue;int h=height(x,z);
            if(IwateTerrain.river(x,z)){
                set(c,x,h,z,Blocks.GRAVEL);for(int y=h+1;y<=IwateTerrain.RIVER_LEVEL;y++)set(c,x,y,z,Blocks.WATER);
                if(Math.floorMod(z,96)<3)set(c,x,IwateTerrain.RIVER_LEVEL+1,z,Blocks.SPRUCE_PLANKS);
            }else if(IwateTerrain.terrace(x,z)){
                set(c,x,h-1,z,Blocks.CLAY);
                if(IwateTerrain.terraceBank(x,z))set(c,x,h,z,Blocks.MOSSY_COBBLESTONE);
                else {set(c,x,h,z,Blocks.WATER);if(Math.floorMod(x+z,3)!=0)set(c,x,h+1,z,SoulContent.RICE.get());}
            }else if(!IwateTerrain.forest(x,z)&&(Math.floorMod(x,96)<3||Math.floorMod(z,96)<3))set(c,x,h,z,Blocks.DIRT_PATH);
            else if(Math.floorMod(IwateTerrain.hash(x,z),41)==0)set(c,x,h+1,z,Blocks.GRASS);
        }
        for(int gx=Math.floorDiv(c.getPos().getMinBlockX()-5,9);gx<=Math.floorDiv(c.getPos().getMaxBlockX()+5,9);gx++)for(int gz=Math.floorDiv(c.getPos().getMinBlockZ()-5,9);gz<=Math.floorDiv(c.getPos().getMaxBlockZ()+5,9);gz++){long hash=IwateTerrain.hash(gx,gz);int x=gx*9+(int)Math.floorMod(hash,5),z=gz*9+(int)Math.floorMod(hash>>>8,5);if(IwateTerrain.forest(x,z)&&IwateTerrain.riverDistance(x,z)>8&&Math.floorMod(hash,5)!=0)tree(c,x,height(x,z)+1,z,8+(int)Math.floorMod(hash,6),true);}
    }
    private static void legacyGenerate(ChunkAccess c){
        for(int x=c.getPos().getMinBlockX();x<=c.getPos().getMaxBlockX();x++)for(int z=c.getPos().getMinBlockZ();z<=c.getPos().getMaxBlockZ();z++){
            int h=height(x,z),a=local(x),b=local(z);boolean home=x>=48&&x<176&&z>=-64&&z<64;
            if(home)continue;
            boolean road=Math.abs(a-24)<=2||Math.abs(b-24)<=2||Math.abs(b+24)<=2||Math.abs(b-88)<=2||Math.abs(b-136)<=2||Math.abs(a-112)<=2&&b>=24&&b<=100;
            if(road)set(c,x,h,z,Math.abs(a-24)==1||Math.abs(b-24)==1?Blocks.COARSE_DIRT:Blocks.DIRT_PATH);
            else if(a>=-104&&a<=-40&&b>=48&&b<=120){
                boolean bank=Math.floorMod(a+104,12)==0||Math.floorMod(b-48,18)==0;
                if(bank)set(c,x,h,z,Blocks.MOSSY_COBBLESTONE);
                else {set(c,x,h-1,z,Blocks.CLAY);set(c,x,h,z,Blocks.WATER);if(Math.floorMod(x+z,3)!=0)set(c,x,h+1,z,SoulContent.RICE.get());}
            }else if(Math.abs(a+18-(int)(Math.sin(z*0.04)*5))<=2){
                set(c,x,h-1,z,Math.floorMod(x*7+z,11)==0?Blocks.AMETHYST_BLOCK:Blocks.SMOOTH_STONE);set(c,x,h,z,Blocks.WATER);
            }else if(Math.floorMod(x*73428767L^z*912931L,43)==0)set(c,x,h+1,z,(x+z)%3==0?Blocks.OXEYE_DAISY:Blocks.GRASS);
        }
        int tileX=Math.floorDiv(c.getPos().getMinBlockX()+128,384),tileZ=Math.floorDiv(c.getPos().getMinBlockZ()+128,384);
        for(int tx=tileX-1;tx<=tileX+1;tx++)for(int tz=tileZ-1;tz<=tileZ+1;tz++)village(c,tx*384,tz*384);
        home(c);
    }
    private static void village(ChunkAccess c,int ox,int oz){
        farmhouse(c,ox-90,oz-70);warehouse(c,ox-80,oz-14);shrine(c,ox+14,oz+72);bus(c,ox+30,oz+3);
        school(c,ox+78,oz+100);clinic(c,ox+34,oz+104);station(c,ox-92,oz+142);
        for(int k=0;k<7;k++){int x=ox-118+k*47,z=oz+209;tree(c,x,height(x,z)+1,z,9+k%4,k%2==0);}
        for(int k=0;k<5;k++){int x=ox+215,z=oz-104+k*59;tree(c,x,height(x,z)+1,z,10,true);}
        int hx=ox+192,hz=oz+188,hy=height(hx,hz)+1;tree(c,hx,hy,hz,24,true);bench(c,hx-5,hy,hz+4,Blocks.STONE_BRICK_STAIRS);
        if(intersects(c,hx-7,hz+10,15,14)){
            box(c,hx-4,hy-6,hz+11,hx+4,hy-2,hz+21,Blocks.STONE);box(c,hx-2,hy-5,hz+10,hx+2,hy-3,hz+20,Blocks.AIR);
            set(c,hx-1,hy-5,hz+15,Blocks.LANTERN);set(c,hx+1,hy-5,hz+15,Blocks.CAULDRON);set(c,hx+1,hy-6,hz+15,Blocks.RAIL);
            sign(c,hx,hy-5,hz+11,"旧矿洞","锈蚀矿车","矿灯仍亮着","",0);
        }
        for(int k=0;k<3;k++){int x=ox-98+k*20,z=oz+65;scarecrow(c,x,height(x,z)+1,z,false);}
        int wx=ox-12,wz=oz+36,wy=height(wx,wz)+1;
        box(c,wx,wy,wz,wx+3,wy,wz+2,Blocks.SMOOTH_STONE_SLAB);set(c,wx+1,wy+1,wz+1,Blocks.LIGHT_BLUE_CARPET);sign(c,wx+3,wy+1,wz,"洗衣石台","褪色的童装","","",0);
        for(int k=0;k<4;k++)pole(c,ox+29,oz-105+k*75);
    }
    private static void tree(ChunkAccess c,int x,int y,int z,int h,boolean cedar){
        if(!intersects(c,x-5,z-5,11,11))return;
        Block leaves=cedar?Blocks.SPRUCE_LEAVES:Blocks.OAK_LEAVES;
        for(int j=3;j<h;j++){int r=cedar?Math.max(1,4-j*3/h):3; if(cedar&&j%3==0)r=Math.max(1,r-1);box(c,x-r,y+j,z-r,x+r,y+j,z+r,leaves.defaultBlockState().setValue(BlockStateProperties.PERSISTENT,true));}
        box(c,x,y,z,x,y+h,z,cedar?Blocks.SPRUCE_LOG:Blocks.OAK_LOG);set(c,x,y+h+1,z,leaves.defaultBlockState().setValue(BlockStateProperties.PERSISTENT,true));
    }
    private static void pole(ChunkAccess c,int x,int z){int y=height(x,z)+1;box(c,x,y,z,x,y+8,z,Blocks.STRIPPED_SPRUCE_LOG);box(c,x-2,y+7,z,x+2,y+7,z,Blocks.SPRUCE_FENCE);for(int j=0;j<4;j++)set(c,x+2,y+6-j,z+j,Blocks.CHAIN);}
    private static void scarecrow(ChunkAccess c,int x,int y,int z,boolean seated){
        set(c,x,y,z,Blocks.OAK_FENCE);set(c,x,y+1,z,seated?Blocks.BLUE_WOOL:Blocks.BROWN_WOOL);set(c,x,y+2,z,Blocks.HAY_BLOCK);box(c,x-1,y+1,z,x+1,y+1,z,Blocks.SPRUCE_FENCE);set(c,x,y+1,z,seated?Blocks.BLUE_WOOL:Blocks.BROWN_WOOL);set(c,x,y+3,z,Blocks.BROWN_CARPET);
    }
    private static void shell(ChunkAccess c,int x,int z,int w,int d,int stories){
        box(c,x-1,1297,z-1,x+w,1300,z+d,Blocks.STONE);box(c,x,Y,z,x+w-1,Y+stories*6+5,z+d-1,Blocks.AIR);
        for(int floor=0;floor<stories;floor++){
            int y=Y+floor*6;box(c,x,y-1,z,x+w-1,y-1,z+d-1,Blocks.SPRUCE_PLANKS);
            box(c,x,y,z,x+w-1,y+4,z,Blocks.SPRUCE_PLANKS);box(c,x,y,z+d-1,x+w-1,y+4,z+d-1,Blocks.SPRUCE_PLANKS);
            box(c,x,y,z,x,y+4,z+d-1,Blocks.WHITE_TERRACOTTA);box(c,x+w-1,y,z,x+w-1,y+4,z+d-1,Blocks.WHITE_TERRACOTTA);
            for(int i=3;i<w-2;i+=5){box(c,x+i,y+1,z,x+i+1,y+2,z,Blocks.GLASS_PANE);box(c,x+i,y+1,z+d-1,x+i+1,y+2,z+d-1,Blocks.GLASS_PANE);}
            for(int i=0;i<w;i+=6){box(c,x+i,y,z,x+i,y+4,z,Blocks.STRIPPED_SPRUCE_LOG);box(c,x+i,y,z+d-1,x+i,y+4,z+d-1,Blocks.STRIPPED_SPRUCE_LOG);}
        }
        box(c,x+w/2-1,Y,z+d-1,x+w/2+1,Y+2,z+d-1,Blocks.AIR);
        int roof=Y+stories*6-1;for(int j=0;j<4;j++)box(c,x-1+j,roof+j,z-1,x+w-j,roof+j,z+d,Blocks.DEEPSLATE_TILE_SLAB);
        for(int i=2;i<w-1;i+=7)set(c,x+i,roof+4,z+2,Blocks.MOSS_CARPET);
        set(c,x+w/2,Y+3,z+d/2,Blocks.LANTERN);
    }
    private static void bench(ChunkAccess c,int x,int y,int z,Block b){box(c,x,y,z,x+3,y,z,b);}
    private static void table(ChunkAccess c,int x,int y,int z){box(c,x,y,z,x+3,y,z+1,Blocks.SPRUCE_SLAB);box(c,x,y,z-1,x+3,y,z-1,Blocks.BROWN_CARPET);box(c,x,y,z+2,x+3,y,z+2,Blocks.GREEN_CARPET);}
    private static void tv(ChunkAccess c,int x,int y,int z){box(c,x,y,z,x+2,y,z,Blocks.SPRUCE_PLANKS);box(c,x,y+1,z,x+2,y+2,z,Blocks.GRAY_CONCRETE);set(c,x+1,y+1,z+1,Blocks.BLACK_STAINED_GLASS_PANE);set(c,x+2,y+3,z,Blocks.IRON_BARS);}
    private static void bedding(ChunkAccess c,int x,int y,int z){box(c,x,y,z,x+2,y,z+3,Blocks.WHITE_CARPET);box(c,x,y,z+1,x+2,y,z+3,Blocks.BROWN_CARPET);}
    private static void kitchen(ChunkAccess c,int x,int y,int z){box(c,x,y,z,x+4,y,z,Blocks.SMOOTH_STONE);set(c,x,y+1,z,Blocks.FURNACE);set(c,x+1,y+1,z,Blocks.GRAY_CARPET);set(c,x+3,y+1,z,Blocks.FLOWER_POT);set(c,x+4,y+1,z,Blocks.FLOWER_POT);chest(c,x+6,y,z,false,"bowl","bowl","wooden_sword","glass_bottle");}
    private static void farmhouse(ChunkAccess c,int x,int z){if(!intersects(c,x-2,z-2,23,25))return;shell(c,x,z,19,17,1);table(c,x+5,Y,z+8);tv(c,x+3,Y,z+2);kitchen(c,x+10,Y,z+3);bedding(c,x+3,Y,z+12);box(c,x+2,Y,z+20,x+2,Y+3,z+20,Blocks.OAK_FENCE);box(c,x+12,Y,z+20,x+12,Y+3,z+20,Blocks.OAK_FENCE);box(c,x+2,Y+3,z+20,x+12,Y+3,z+20,Blocks.CHAIN);for(int k=4;k<12;k+=3)set(c,x+k,Y+2,z+20,Blocks.GRAY_BANNER);sign(c,x+9,Y,z+18,"農家","古びた家","","",0);}
    private static void warehouse(ChunkAccess c,int x,int z){if(!intersects(c,x-2,z-2,23,22))return;shell(c,x,z,19,16,1);chest(c,x+3,Y,z+4,true);chest(c,x+6,Y,z+4,false,"iron_hoe","stone_hoe","shears","wheat","leather_boots");box(c,x+3,Y,z+10,x+7,Y+1,z+12,Blocks.HAY_BLOCK);set(c,x+12,Y,z+4,Blocks.COMPOSTER);set(c,x+14,Y,z+4,Blocks.BARREL);sign(c,x+12,Y+1,z+8,"旧日历","2003年 3月20日","没有翻过页","",0);sign(c,x+9,Y,z+17,"木仓库","农具与竹筐","","",0);door(c,x+8,Y,z+15,true);}
    private static void door(ChunkAccess c,int x,int y,int z,boolean open){BlockState state=Blocks.SPRUCE_DOOR.defaultBlockState().setValue(BlockStateProperties.OPEN,open);set(c,x,y,z,state);set(c,x,y+1,z,state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF,DoubleBlockHalf.UPPER));}
    private static void shrine(ChunkAccess c,int x,int z){if(!intersects(c,x-5,z-3,22,25))return;shell(c,x,z,11,11,1);box(c,x+3,Y,z+2,x+7,Y+2,z+3,Blocks.DARK_OAK_PLANKS);box(c,x+4,Y+1,z+3,x+6,Y+2,z+3,Blocks.AIR);set(c,x+4,Y,z+6,Blocks.FLOWER_POT);set(c,x+6,Y,z+6,Blocks.WHITE_CANDLE);sign(c,x+5,Y+1,z+6,"御供","米酒 · 饭团","","",0);for(int i=2;i<9;i+=2)set(c,x+i,Y-1,z+8,Blocks.CHISELED_POLISHED_BLACKSTONE);box(c,x+1,Y,z+16,x+1,Y+5,z+16,Blocks.STRIPPED_OAK_LOG);box(c,x+9,Y,z+16,x+9,Y+5,z+16,Blocks.STRIPPED_OAK_LOG);box(c,x-1,Y+5,z+16,x+11,Y+5,z+16,Blocks.DARK_OAK_SLAB);box(c,x,Y+3,z+16,x+10,Y+3,z+16,Blocks.OAK_FENCE);}
    private static void bus(ChunkAccess c,int x,int z){if(!intersects(c,x,z,17,14))return;box(c,x,Y-1,z,x+12,Y-1,z+8,Blocks.SMOOTH_STONE);box(c,x,Y,z,x,Y+4,z,Blocks.IRON_BARS);box(c,x+12,Y,z,x+12,Y+4,z,Blocks.IRON_BARS);box(c,x-1,Y+4,z-1,x+13,Y+4,z+8,Blocks.DARK_OAK_SLAB);bench(c,x+4,Y,z+3,Blocks.SPRUCE_STAIRS);scarecrow(c,x+5,Y,z+3,true);sign(c,x+1,Y+1,z+6,"岩…前","旧公交站","末班 17:40","时间停止了",0);sign(c,x+10,Y+1,z+3,"时刻表","07:10  12:30","17:40","",0);}
    private static void school(ChunkAccess c,int x,int z){if(!intersects(c,x-2,z-2,48,48))return;shell(c,x,z,40,26,2);
        for(int f=0;f<2;f++){int y=Y+f*6;box(c,x+3,y+1,z+2,x+16,y+3,z+2,Blocks.GREEN_CONCRETE);sign(c,x+9,y+2,z+3,"三年二班","春の思い出","また明日","",0);for(int i=4;i<23;i+=5)for(int k=7;k<20;k+=5){set(c,x+i,y,z+k,Blocks.SPRUCE_STAIRS);set(c,x+i,y,z+k-1,Blocks.SPRUCE_SLAB);}for(int k=3;k<23;k+=3){set(c,x+36,y,z+k,Blocks.BARREL);chest(c,x+37,y,z+k,false,"book","leather_boots","leather_chestplate");set(c,x+35,y,z+k,Blocks.IRON_TRAPDOOR.defaultBlockState().setValue(BlockStateProperties.OPEN,true));}}
        for(int j=0;j<6;j++){box(c,x+28,Y+j-1,z+18-j,x+30,Y+j-1,z+18-j,Blocks.SPRUCE_STAIRS);box(c,x+28,Y+j,z+18-j,x+30,Y+6,z+18-j,Blocks.AIR);}
        box(c,x+25,Y+6,z+3,x+30,Y+8,z+5,Blocks.BLACK_CONCRETE);box(c,x+25,Y+7,z+6,x+30,Y+7,z+6,Blocks.WHITE_CARPET);for(int i=25;i<31;i+=2)set(c,x+i,Y+8,z+6,Blocks.POLISHED_BLACKSTONE_BUTTON);bench(c,x+26,Y+6,z+8,Blocks.DARK_OAK_STAIRS);
        for(int dx:new int[]{4,14})box(c,x+dx,Y,z+36,x+dx,Y+5,z+36,Blocks.IRON_BARS);box(c,x+4,Y+5,z+36,x+14,Y+5,z+36,Blocks.EXPOSED_COPPER);// 秋千座板与悬索由客户端绘制摆动，不放置重叠的静态座板。
        for(int i=0;i<5;i++)box(c,x+27,Y+4-i,z+32+i,x+29,Y+4-i,z+32+i,Blocks.CUT_COPPER_SLAB);box(c,x+27,Y,z+31,x+29,Y+3,z+31,Blocks.LADDER.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING,Direction.NORTH));
        sign(c,x+20,Y,z+27,"岩手小学校","休校","音楽室 · 二階","",0);
    }
    private static void clinic(ChunkAccess c,int x,int z){if(!intersects(c,x-2,z-2,31,29))return;shell(c,x,z,27,22,1);bench(c,x+3,Y,z+16,Blocks.BIRCH_STAIRS);set(c,x+4,Y+1,z+16,Blocks.WHITE_CARPET);sign(c,x+3,Y+1,z+17,"旧报纸","昨日的新闻","","",0);box(c,x+15,Y,z+4,x+18,Y,z+9,Blocks.IRON_BLOCK);box(c,x+15,Y+1,z+4,x+18,Y+1,z+9,Blocks.WHITE_CARPET);set(c,x+16,Y+1,z+4,Blocks.LIGHT_GRAY_CARPET);box(c,x+3,Y,z+3,x+8,Y+2,z+3,Blocks.BOOKSHELF);for(int i=3;i<9;i+=2)set(c,x+i,Y+3,z+3,Blocks.FLOWER_POT);chest(c,x+10,Y,z+3,false,"glass_bottle","glass_bottle","paper");sign(c,x+20,Y+1,z+4,"人体解剖图","頭 · 胸 · 腹","金色咒文","魂は形を覚える",0);sign(c,x+13,Y,z+23,"岩手診療所","休診","","",0);}
    private static void station(ChunkAccess c,int x,int z){if(!intersects(c,x-2,z-2,39,32))return;shell(c,x,z,20,14,1);for(int i=3;i<16;i+=4)box(c,x+i,Y,z+3,x+i,Y+2,z+8,Blocks.SPRUCE_SLAB);box(c,x+13,Y,z+10,x+17,Y,z+10,Blocks.SMOOTH_STONE);set(c,x+15,Y+1,z+10,Blocks.POLISHED_BLACKSTONE_STAIRS);set(c,x+15,Y,z+11,Blocks.SPRUCE_TRAPDOOR.defaultBlockState().setValue(BlockStateProperties.OPEN,true));box(c,x+1,Y-1,z+15,x+31,Y-1,z+27,Blocks.GRAY_CONCRETE);for(int i:new int[]{3,27})box(c,x+i,Y,z+24,x+i,Y+5,z+24,Blocks.EXPOSED_COPPER);box(c,x,Y+5,z+16,x+32,Y+5,z+27,Blocks.QUARTZ_SLAB);for(int i:new int[]{8,20}){box(c,x+i,Y,z+20,x+i+1,Y+1,z+20,Blocks.WEATHERED_COPPER);set(c,x+i,Y+2,z+20,Blocks.BLACK_STAINED_GLASS);box(c,x+i+2,Y,z+20,x+i+2,Y+2,z+20,Blocks.CHAIN);}box(c,x+12,Y,z+23,x+16,Y,z+24,Blocks.BLACK_CARPET);sign(c,x+23,Y,z+15,"給油所","営業終了","","",0);}
    private static void home(ChunkAccess c){
        int x=HOME_X,z=HOME_Z;if(!intersects(c,x,z,128,128))return;
        box(c,x,1297,z,x+127,1300,z+127,Blocks.STONE_BRICKS);box(c,x,Y,z,x+127,Y+14,z+127,Blocks.AIR);
        box(c,x,Y,z,x+127,Y+2,z,Blocks.WHITE_TERRACOTTA);box(c,x,Y,z+127,x+127,Y+2,z+127,Blocks.WHITE_TERRACOTTA);box(c,x,Y,z,x,Y+2,z+127,Blocks.WHITE_TERRACOTTA);box(c,x+127,Y,z,x+127,Y+2,z+127,Blocks.WHITE_TERRACOTTA);
        shell(c,x+8,z+8,112,24,1);shell(c,x+8,z+96,112,24,1);shell(c,x+8,z+32,24,64,1);shell(c,x+96,z+32,24,64,1);
        // 外墙与屋檐之间仍属于128格宅地，铺路连到正南入口。
        box(c,x+61,1300,z+119,x+66,1300,z+127,Blocks.POLISHED_ANDESITE);
        box(c,x+32,1300,z+32,x+95,1300,z+95,Blocks.POLISHED_ANDESITE);
        // 四房朝院子的门道，以及正南玄关。
        box(c,x+61,Y,z+31,x+66,Y+2,z+31,Blocks.AIR);box(c,x+61,Y,z+96,x+66,Y+2,z+96,Blocks.AIR);box(c,x+31,Y,z+61,x+31,Y+2,z+66,Blocks.AIR);box(c,x+96,Y,z+61,x+96,Y+2,z+66,Blocks.AIR);box(c,x+61,Y,z+127,x+66,Y+3,z+127,Blocks.AIR);
        for(int i=38;i<=90;i+=13){set(c,x+i,Y,z+36,Blocks.LANTERN);set(c,x+i,Y,z+91,Blocks.LANTERN);}
        tv(c,x+55,Y,z+12);table(c,x+58,Y,z+22);bedding(c,x+77,Y,z+15);bedding(c,x+82,Y,z+15);sign(c,x+64,Y,z+32,"北房 · 正房","虎杖爷爷的房间","","",0);
        table(c,x+103,Y,z+58);set(c,x+104,Y+1,z+58,Blocks.RED_CANDLE);set(c,x+106,Y+1,z+58,Blocks.RED_CANDLE);chest(c,x+112,Y,z+61,false,"leather","leather","paper");sign(c,x+106,Y+1,z+59,"旧拳套","磨损的红色皮革","","",12);bedding(c,x+109,Y,z+76);box(c,x+117,Y+1,z+44,x+117,Y+3,z+48,Blocks.GRAY_WOOL);sign(c,x+116,Y+2,z+46,"褪色海报","大会の記憶","","",4);sign(c,x+95,Y,z+64,"东房","虎杖的房间","","",4);
        kitchen(c,x+13,Y,z+43);kitchen(c,x+13,Y,z+53);box(c,x+12,Y,z+75,x+18,Y+2,z+75,Blocks.BOOKSHELF);photoChest(c,x+21,Y,z+75);sign(c,x+32,Y,z+64,"西房","厨房与杂物间","双人合照收在旧箱子里","",12);
        for(int i=20;i<105;i+=16){chest(c,x+i,Y,z+109,false,"iron_hoe","shears","wheat","leather_boots");set(c,x+i+3,Y,z+109,Blocks.BARREL);set(c,x+i+5,Y,z+109,Blocks.COMPOSTER);}sign(c,x+64,Y,z+95,"南房","玄关与储物间","","",8);
        // 偶数边长的院落以东南中央格作为唯一交互中心；上方完整留空一格。
        int cx=x+64,cz=z+64;box(c,cx-1,1300,cz-1,cx+1,1300,cz+1,Blocks.CHISELED_POLISHED_BLACKSTONE);set(c,cx,1300,cz,Blocks.AIR);set(c,cx,1299,cz,SoulContent.PHOTO_SLOT.get());
        for(int[] d:new int[][]{{-1,0},{1,0},{0,-1},{0,1}})set(c,cx+d[0],Y,cz+d[1],Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE);
        sign(c,cx+3,Y,cz,"金色咒文刻痕","一格深的方形凹槽","放入虎杖家双人合照","",0);sign(c,x+67,Y,z+124,"虎杖家","四方庭院 · 128×128","","",0);
    }
    private IwateVillage(){}
}
