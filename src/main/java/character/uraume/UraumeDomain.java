package character.uraume;

/** 判断目标是否在里梅「霜天宫殿」里（r289）。 */
public final class UraumeDomain {
    public static boolean inDomain(net.minecraft.world.entity.Entity entity) {
        return character.npc.NpcDomain.inIceDomain(entity);
    }
    private UraumeDomain() { }
}
