package character.example;

import character.domain.DomainPresentation;
import character.domain.PictureDomainApi;

/** 可编译范例，默认不登记；role 的技能目录必须先包含 9100。复用现有贴图，不另造 NPC 版。 */
public final class PictureDomainExample {
    public static final int SKILL=9100;
    public static void install(String role){
        var sky=new DomainPresentation.Style("jujutsu_kaisen_cursed_paths:textures/block/yuki_star_shell.png",0x77BBFF,0,1.5f,true);
        PictureDomainApi.register(role,SKILL,new PictureDomainApi.Definition(
            20,600,20,20,sky,PictureDomainApi.damage(6)));
    }
    private PictureDomainExample(){ }
}
