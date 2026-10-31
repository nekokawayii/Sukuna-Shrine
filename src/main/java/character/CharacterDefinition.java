package character;

import java.util.*;
import character.CharacterConfig.Stats;

/** 角色定义：选择书、轮盘、成长与个人线共同读取，不引用客户端类。 */
public record CharacterDefinition(String id, String name, String technique, String description,
        Family family, Resource resource, int color, List<Integer> skills, List<Chapter> story,
        CharacterConfig config, List<EventChapter> events) {
    public enum Family { LIMITLESS, ICE, BLOOD, MARTIAL, SHRINE, HEAVENLY, CUSTOM }
    public enum Resource { CURSED_ENERGY, STAMINA }

    /** 事件型章节的判定方式：模板原有的击杀累计之外的三种。 */
    public enum ChapterKind { VILLAGER_THEN_KILLS, BOSS_KILL, DIMENSION_KILLS }

    /**
     * 事件型章节：给「目睹里香」「决战夏油杰」「百鬼夜行」这种无法用累计击杀表达的个人线用。
     * <p>进度不写在这条数据里，由各角色自己的进度监听器判定并写入存档标记（CharacterService.markChapter）。
     *
     * @param bossId  仅 BOSS_KILL 使用，实体类型编号（如 sukuna:geto）
     * @param target  需要的次数：VILLAGER_THEN_KILLS 是窗口内击杀数，BOSS_KILL 是 1，DIMENSION_KILLS 是该维度击杀数
     */
    public record EventChapter(String id, String title, String goal, int target, ChapterKind kind,
            String bossId, CharacterReward grants) {
        public EventChapter {
            if (id == null || !id.matches("[a-z][a-z0-9_]{0,47}") || title == null || title.isBlank()
                || goal == null || kind == null || grants == null)
                throw new IllegalArgumentException("事件章节编号、标题、目标、判定方式和奖励不能为空");
            if (title.length() > 128 || goal.length() > 512) throw new IllegalArgumentException("事件章节标题限128字、目标512字");
            if (target < 1 || target > 1000000) throw new IllegalArgumentException("事件章节次数需为1至1000000");
            if (kind == ChapterKind.BOSS_KILL && (bossId == null || !bossId.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")))
                throw new IllegalArgumentException("BOSS_KILL 章节需要完整的实体编号");
        }
        public EventChapter(String id, String title, String goal, int target, ChapterKind kind) {
            this(id, title, goal, target, kind, null, CharacterReward.NONE);
        }
    }

    public record Chapter(String id, String title, int kills, String goal, String reward, CharacterReward grants) {
        public Chapter(String title,int kills,String goal,String reward) { this("kills_"+kills,title,kills,goal,reward,CharacterReward.NONE); }
        public Chapter {
            if(id==null||!id.matches("[a-z][a-z0-9_]{0,47}")||title==null||title.isBlank()||goal==null||reward==null||grants==null)
                throw new IllegalArgumentException("章节编号、标题、目标和奖励不能为空");
            if(title.length()>128||goal.length()>512||reward.length()>1024)throw new IllegalArgumentException("章节标题限128字、目标512字、说明1024字");
        }
    }
    /** 兼容原六角色：保留原来的三阶段成长数值和玩法。 */
    public CharacterDefinition(String id,String name,String technique,String description,Family family,Resource resource,int color,List<Integer> skills,List<Chapter> story) {
        this(id,name,technique,description,family,resource,color,skills,story,CharacterConfig.legacy(color),List.of());
    }
    /** 兼容模板的 10 参构造器：不声明事件型章节时与旧版完全一致。 */
    public CharacterDefinition(String id,String name,String technique,String description,Family family,Resource resource,int color,List<Integer> skills,List<Chapter> story,CharacterConfig config) {
        this(id,name,technique,description,family,resource,color,skills,story,config,List.of());
    }
    public CharacterDefinition {
        if(id==null||!id.matches("[a-z][a-z0-9_]{0,47}"))throw new IllegalArgumentException("角色编号不合法");
        skills=List.copyOf(skills);story=List.copyOf(story);events=List.copyOf(events);
        if(family==null||resource==null||name==null||name.isBlank()||technique==null||description==null||config==null)throw new IllegalArgumentException("角色信息不能为空");
        if(name.length()>64||technique.length()>64||description.length()>1024)throw new IllegalArgumentException("角色名和术式名限64字，选择说明限1024字");
        if(story.isEmpty()||story.size()>128)throw new IllegalArgumentException("个人线需定义1至128章");
        if(skills.size()>24||new HashSet<>(skills).size()!=skills.size())throw new IllegalArgumentException("轮盘最多24招且编号不能重复");
        if((family==Family.HEAVENLY)!=(resource==Resource.STAMINA))throw new IllegalArgumentException("天宇暴君使用体术，其余角色使用咒力");
        if(resource==Resource.STAMINA&&!skills.isEmpty())throw new IllegalArgumentException("纯体术角色不使用咒术轮盘");
        if(resource==Resource.CURSED_ENERGY&&skills.isEmpty())throw new IllegalArgumentException("咒术角色至少有一项技能");
        int previous=0;var ids=new HashSet<String>();
        if(family!=Family.SHRINE)for(Chapter chapter:story){
            if(chapter.kills()<=previous||chapter.kills()>1000000)throw new IllegalArgumentException("章节击杀目标需从正数开始递增，最多1000000");
            if(!ids.add(chapter.id()))throw new IllegalArgumentException("章节编号不能重复");previous=chapter.kills();
        }
        for(var e:config.unlockChapters().entrySet())if(!skills.contains(e.getKey())||e.getValue()<0||e.getValue()>story.size())throw new IllegalArgumentException("技能解锁章节超出范围");
        if(!skills.isEmpty()&&config.unlockChapter(skills.get(0))!=0)throw new IllegalArgumentException("第一招必须开局解锁，避免没有可用技能");
        for(int skill:skills){
            if(skill<0||skill>CharacterSkill.LAST)throw new IllegalArgumentException("技能编号超出范围");
            if(CharacterSkill.custom(skill)&&config.skill(skill)==null)throw new IllegalArgumentException("自定义技能没有效果定义："+skill);
            if(!CharacterSkill.custom(skill)&&!legacySkill(family,skill))throw new IllegalArgumentException("技能编号与术式家族不匹配："+skill);
        }
        for(var skill:config.customSkills())if(!skills.contains(skill.id()))throw new IllegalArgumentException("技能效果没有放进轮盘："+skill.id());
        for(int skill:config.text().keySet())if(!skills.contains(skill))throw new IllegalArgumentException("显示配置没有对应轮盘技能");
        if(family==Family.SHRINE&&(!config.customSkills().isEmpty()||!config.unlockChapters().isEmpty()||story.stream().anyMatch(c->!c.grants().equals(CharacterReward.NONE))))throw new IllegalArgumentException("宿傩继续使用原个人线，不接通用章节奖励");
        // 提前检查整条成长线是否超出数值上限。
        Stats total=config.base();for(Chapter chapter:story)total=total.plus(config.perChapter()).plus(chapter.grants().stats());
        // 事件型章节：最多 128 章、编号不重复；有事件章节的角色以事件进度为准（story 仍保留以满足通用校验）。
        if(events.size()>128)throw new IllegalArgumentException("事件章节最多128章");
        var eventIds=new HashSet<String>();
        for(EventChapter chapter:events)if(!eventIds.add(chapter.id()))throw new IllegalArgumentException("事件章节编号不能重复");
    }
    /** 该角色是否使用事件型章节推进个人线。 */
    public boolean hasEvents(){ return !events.isEmpty(); }
    /** 按已完成的事件章节数汇总属性，与 {@link #stats(int)} 同构，但取 events 列表。 */
    public Stats eventStats(int completed){
        Stats total=config.base();
        for(int i=0;i<Math.min(events.size(),Math.max(0,completed));i++)total=total.plus(config.perChapter()).plus(events.get(i).grants().stats());
        return total;
    }
    private static boolean legacySkill(Family family,int skill) {
        return switch(family){
            case LIMITLESS -> skill>=34&&skill<=40;
            case ICE -> skill==22||skill==23;
            case MARTIAL -> skill==7||skill==8||skill==13;
            case BLOOD -> skill>=60&&skill<=79&&skill!=75;
            case SHRINE -> skill<=79;
            default -> false;
        };
    }
    public boolean hasWheel() { return !skills.isEmpty(); }
    public String resourceName() { return resource==Resource.STAMINA?"体术":"咒力"; }
    public int firstSkill() { return skills.isEmpty()?-1:skills.get(0); }
    public int rank(int kills) { int rank=0;for(Chapter chapter:story)if(kills>=chapter.kills())rank++;return rank; }
    public boolean unlocked(int skill,int rank) { return skills.contains(skill)&&rank>=config.unlockChapter(skill); }
    public Stats stats(int completed) {
        Stats total=config.base();for(int i=0;i<Math.min(story.size(),Math.max(0,completed));i++)total=total.plus(config.perChapter()).plus(story.get(i).grants().stats());return total;
    }
    public String rewards(int index) {
        var chapter=story.get(index);var pieces=new ArrayList<String>();
        if(!chapter.reward().isBlank())pieces.add(chapter.reward());
        var grant=chapter.grants();
        if(resource==Resource.CURSED_ENERGY&&family!=Family.SHRINE)grant=new CharacterReward(config.perChapter().plus(grant.stats()),grant.experience(),grant.energy(),grant.items());
        String amounts=grant.summary();if(!amounts.isBlank())pieces.add(amounts);
        for(int skill:skills)if(config.unlockChapter(skill)==index+1){var custom=config.skill(skill);var display=config.text().get(skill);pieces.add("解锁"+(display!=null?display.name():custom!=null?custom.name():"技能"+skill));}
        return String.join("；",pieces);
    }
}
