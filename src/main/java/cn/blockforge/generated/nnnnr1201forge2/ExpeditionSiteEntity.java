package cn.blockforge.generated.nnnnr1201forge2;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
/** 结构里的单次遭遇锚点；成功刷出后保存，不因重进游戏重新刷Boss。 */
public final class ExpeditionSiteEntity extends BlockEntity {
    private String role="";private int count=1,spawned;private int clock;
    public ExpeditionSiteEntity(BlockPos p,BlockState s){super(ExpeditionContent.SITE.get(),p,s);}
    public static void tick(Level l,BlockPos p,BlockState s,ExpeditionSiteEntity b){if(!(l instanceof ServerLevel sl)||++b.clock%40!=0||b.spawned>=b.count||b.role.isEmpty())return;var player=sl.getNearestPlayer(p.getX(),p.getY(),p.getZ(),28,false);if(!(player instanceof net.minecraft.server.level.ServerPlayer sp)||sp.isSpectator()||sp.isCreative())return;
        for(int tries=0;tries<10&&b.spawned<b.count;tries++){BlockPos at=p.offset(sl.random.nextInt(9)-4,1,sl.random.nextInt(9)-4);if(sl.getBlockState(at.below()).isAir())continue;var mob=ExpeditionMobs.spawn(sl,at,b.role,sp);if(mob!=null){b.spawned++;b.setChanged();}}
    }
    @Override protected void saveAdditional(CompoundTag n){super.saveAdditional(n);n.putString("Role",role);n.putInt("Count",count);n.putInt("Spawned",spawned);}
    @Override public void load(CompoundTag n){super.load(n);role=n.getString("Role");count=Math.max(1,Math.min(12,n.getInt("Count")));spawned=n.getInt("Spawned");}
}
