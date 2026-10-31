package cn.blockforge.generated.nnnnr1201forge2;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/** 跟随游戏输入事件，不轮询系统键盘，不重置全局按键，不接管鼠标焦点。 */
public final class ClientInput {
    private static final HeldInput wheel=new HeldInput(),cast=new HeldInput();
    private static int modifiers;
    private static HeldInput.Binding binding(KeyMapping mapping){
        var key=mapping.getKey();
        HeldInput.Kind kind=key.getType()==InputConstants.Type.MOUSE?HeldInput.Kind.MOUSE:key.getType()==InputConstants.Type.SCANCODE?HeldInput.Kind.SCANCODE:HeldInput.Kind.KEYSYM;
        int modifier=switch(mapping.getKeyModifier()){
            case SHIFT->GLFW.GLFW_MOD_SHIFT;
            case CONTROL->Minecraft.ON_OSX?GLFW.GLFW_MOD_SUPER:GLFW.GLFW_MOD_CONTROL;
            case ALT->GLFW.GLFW_MOD_ALT;
            default->0;
        };
        return new HeldInput.Binding(kind,mapping.isUnbound()?-1:key.getValue(),modifier);
    }
    private static boolean available(KeyMapping mapping){
        var mc=Minecraft.getInstance();
        if(mc.level==null||mc.player==null||!mc.player.isAlive()||!mc.isWindowActive())return false;
        if(mc.screen==null)return mc.mouseHandler.isMouseGrabbed();
        return mapping==Client.WHEEL&&(mc.screen instanceof SkillWheel||mc.screen instanceof character.CharacterWheel||mc.screen instanceof TenShadowsScreen)||mapping==Client.CAST&&FreehandClient.canvasOpen();
    }
    public static boolean held(KeyMapping mapping){
        HeldInput input=mapping==Client.WHEEL?wheel:cast;
        return input.down(binding(mapping),available(mapping),modifiers);
    }
    private static void event(HeldInput.Kind kind,int code,int action,int mods){
        modifiers=mods;
        if(action==GLFW.GLFW_REPEAT)return;
        boolean down=action==GLFW.GLFW_PRESS;
        wheel.event(binding(Client.WHEEL),kind,code,down,available(Client.WHEEL),mods);
        cast.event(binding(Client.CAST),kind,code,down,available(Client.CAST),mods);
    }
    public static void key(int key,int scan,int action,int mods){
        if(TojiQteClient.key(key,action,mods))return;
        event(HeldInput.Kind.KEYSYM,key,action,mods);
        event(HeldInput.Kind.SCANCODE,scan,action,mods);
        // 不等下一游戏刻：同一刻内按下又松开也必须完整送出施术起手/松手。
        if(action!=GLFW.GLFW_REPEAT)TechniqueInputClient.inputChanged();
    }
    public static void mouse(int button,int action,int mods){if(NaoyaClient.mouse(button,action,mods))return;if(TojiQteClient.mouse(button,action,mods))return;event(HeldInput.Kind.MOUSE,button,action,mods);TechniqueInputClient.inputChanged();}
    public static void mouse(int button,int action){mouse(button,action,modifiers);}
    @SubscribeEvent public static void key(InputEvent.Key e){key(e.getKey(),e.getScanCode(),e.getAction(),e.getModifiers());}
    @SubscribeEvent public static void mouse(InputEvent.MouseButton.Post e){mouse(e.getButton(),e.getAction(),e.getModifiers());}
    public static void tick(){
        // 菜单、死亡、失去焦点后仅丢弃本模组的临时按住记录；需重新按下才会施术。
        held(Client.WHEEL);held(Client.CAST);
    }
    public static void clear(){wheel.clear();cast.clear();modifiers=0;}
    private ClientInput(){}
}
