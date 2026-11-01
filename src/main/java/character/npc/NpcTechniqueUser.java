package character.npc;

/** 新 NPC 实现此接口，只声明角色与会的招式；公共调度器处理施法、领域、费用和冷却。 */
public interface NpcTechniqueUser {
    NpcLoadout npcTechniques();
}
