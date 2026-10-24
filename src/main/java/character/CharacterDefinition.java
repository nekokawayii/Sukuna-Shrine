package character;

import java.util.*;
import character.CharacterConfig.Stats;

/** 角色定义：选择书、轮盘、成长与个人线共同读取，不引用客户端类。 */
public record CharacterDefinition(String id, String name, String technique, String description,
        Family family, Resource resource, int color, List<Integer> skills, List<Chapter> story,
        CharacterConfig config) {
    public enum Family { LIMITLESS, ICE, BLOOD, MARTIAL, SHRINE, HEAVENLY, CUSTOM }
    public enum Resource { CURSED_ENERGY, STAMINA }
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
        this(id,name,technique,description,family,resource,color,skills,story,CharacterConfig.legacy(color));
    }
    public CharacterDefinition {
        if(id==null||!id.matches("[a-z][a-z0-9_]{0,47}"))throw new IllegalArgumentException("角色编号不合法");
        skills=List.copyOf(skills);story=List.copyOf(story);
        if(family==null||resource==null||name==null||name.isBlank()||technique==null||description==null||config==null)throw new IllegalArgumentException("角色信息不能为空");
        if(name.length()>64||technique.length()>64||description.length()>1024)throw new IllegalArgumentException("角色名和术式名限64字，选择说明限1024字");
        if(story.isEmpty()||story.size()>32)throw new IllegalArgumentException("个人线需定义1至32章");
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
