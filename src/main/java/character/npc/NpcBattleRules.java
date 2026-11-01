package character.npc;

/** 不依赖游戏环境的 NPC 调度规则；1 秒为 20 刻。 */
public final class NpcBattleRules {
    public static final int DOMAIN_COOLDOWN = 150 * 20;
    public static final int FIRST_DOMAIN_DELAY = 10 * 20;
    public static final int NORMAL_DOMAIN_DELAY = 20 * 20;
    public static final int CAST_INTERVAL = 24;
    public static final int REPAIR_DELAY = 60;

    public static boolean domainReady(long now, long readyAt, int combatTicks,
                                      float healthFraction, boolean enemyDomain) {
        return now >= readyAt && combatTicks >= FIRST_DOMAIN_DELAY
            && (enemyDomain || healthFraction <= .65f || combatTicks >= NORMAL_DOMAIN_DELAY);
    }
    public static boolean deathStarReady(float health, float maximum, boolean domain) {
        return maximum > 0 && health > 0 && health / maximum <= .25f && domain;
    }
    private NpcBattleRules() { }
}
