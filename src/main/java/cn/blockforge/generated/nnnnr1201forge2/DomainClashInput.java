package cn.blockforge.generated.nnnnr1201forge2;

import java.util.ArrayDeque;

/** 单场单人的输入门禁，重复按下、准备期、过量请求都不计分。 */
public final class DomainClashInput {
    private final ArrayDeque<Long> presses=new ArrayDeque<>();
    private boolean down;
    private int pending;
    public boolean accept(boolean pressed,boolean ready,long now){
        if(down==pressed)return false;down=pressed;
        if(!pressed||!ready)return false;
        while(!presses.isEmpty()&&presses.peekFirst()<=now-20)presses.removeFirst();
        if(presses.size()>=DomainClashRules.MAX_CPS)return false;
        presses.addLast(now);pending++;return true;
    }
    public int drain(){int value=pending;pending=0;return value;}
}
