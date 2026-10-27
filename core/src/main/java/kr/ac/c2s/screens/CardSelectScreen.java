package kr.ac.c2s.screens;
import kr.ac.c2s.C2SGame;
import kr.ac.c2s.common.Protocol.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import java.util.*;
import static kr.ac.c2s.ui.Theme.*;

/** Private two-card loadout; the engine is the authority for confirmation. */
public final class CardSelectScreen extends BaseScreen {
    private Snapshot s;
    private final LinkedHashSet<Card> selected=new LinkedHashSet<>();
    public CardSelectScreen(C2SGame app,Snapshot state){super(app);accept(state);}
    public void accept(Snapshot state){
        if(s!=null&&!s.matchId.equals(state.matchId))selected.clear();
        s=state;if(s.me().loadoutReady){selected.clear();selected.addAll(s.cards);}build();
    }
    private void toggle(Card card){
        if(s.me().loadoutReady)return;
        if(selected.contains(card))selected.remove(card);
        else if(selected.size()<2)selected.add(card);
        else app.toast("두 장을 선택했습니다. 다른 카드를 고르려면 선택한 카드를 해제하세요.");
        app.sound("click");build();
    }
    private void build(){
        clear();heading("특수카드 선택","이번 게임에 사용할 카드 2장을 선택하세요 · 각 카드 1회 사용");
        for(int i=0;i<Card.values().length;i++){
            Card c=Card.values()[i];float x=95+i*355;boolean chosen=selected.contains(c);
            var frame=panel(x,200,340,510);frame.setColor(chosen?GOLD:com.badlogic.gdx.graphics.Color.WHITE);
            var art=image("cards/specials/"+c.name().toLowerCase(Locale.ROOT)+".png",x+62,393,216,295);
            art.addListener(new InputListener(){@Override public boolean touchDown(InputEvent e,float a,float b,int p,int button){toggle(c);return true;}});
            gold(c.label,x+20,342,300,42,"title");
            paragraph(shortDescription(c),x+25,264,290,66);
            TextButton select=button(chosen?"선택됨 · 해제":"선택하기",x+25,212,290,45,chosen?"primary":"small",()->toggle(c));
            select.setDisabled(s.me().loadoutReady);select.setName("loadout-"+c.name());
        }
        String status=s.me().loadoutReady?"선택 확정 · 상대가 카드를 고르는 중입니다.":"선택한 카드  "+selected.size()+" / 2";
        gold(status,350,147,900,40,"default");
        TextButton confirm=button(s.me().loadoutReady?"상대 선택 대기 중":"이 카드로 시작",575,65,450,65,"primary",()->app.send(Command.loadout(new ArrayList<>(selected))));
        confirm.setDisabled(selected.size()!=2||s.me().loadoutReady);confirm.setName("loadout-confirm");
        button("테이블 나가기",1320,65,220,55,"small",app::confirmLeave);
        label("상대에게는 선택한 카드 종류가 공개되지 않습니다.",300,23,1000,30,"small",Align.center);
    }
    private String shortDescription(Card c){return switch(c){
        case REVERSE->"이번 라운드에는 낮은 숫자가 승리합니다. 1 대 9 예외는 유지됩니다.";
        case SCOUT->"판정 후 상대 숫자가 1~4인지 5~9인지 나만 확인합니다.";
        case DISGUISE->"상대에게 보이는 내 숫자의 홀짝 정보를 반대로 표시합니다.";
        case PREDICT->"상대 숫자를 맞히면 상대의 남은 숫자 타일 한 장을 확인합니다.";
    };}
}
