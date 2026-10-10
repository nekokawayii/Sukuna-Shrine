package cn.blockforge.generated.nnnnr1201forge2;

/** 只记住本模组一次真实按下；不修改游戏或其他模组的按键状态。 */
public final class HeldInput {
    public enum Kind { KEYSYM, SCANCODE, MOUSE }
    public record Binding(Kind kind,int code,int modifiers) {
        public boolean bound(){return kind!=null&&code>=0;}
    }
    private Binding pressed;
    public void event(Binding binding,Kind kind,int code,boolean down,boolean mayStart,int modifiers){
        if(!down){
            if(pressed!=null&&pressed.kind()==kind&&pressed.code()==code)clear();
            return;
        }
        if(mayStart&&binding.bound()&&binding.kind()==kind&&binding.code()==code&&modifiersMatch(binding,modifiers))pressed=binding;
    }
    public boolean down(Binding binding,boolean focused,int modifiers){
        if(!focused||!binding.equals(pressed)||!modifiersMatch(binding,modifiers))clear();
        return pressed!=null;
    }
    private static boolean modifiersMatch(Binding binding,int modifiers){return (modifiers&binding.modifiers())==binding.modifiers();}
    public void clear(){pressed=null;}
}
