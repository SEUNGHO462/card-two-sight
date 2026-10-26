package kr.ac.c2s.screens;
import kr.ac.c2s.C2SGame;
import kr.ac.c2s.common.Protocol.*;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import static kr.ac.c2s.ui.Theme.*;

public final class FinalResultScreen extends BaseScreen {
    private Snapshot s;
    public FinalResultScreen(C2SGame app,Snapshot state){super(app);app.sound(state.winner==state.you?"win":"blank");accept(state);}
    public void accept(Snapshot state){s=state;build();}
    private void build(){clear();boolean win=s.winner==s.you,draw=s.winner<0;int shown=draw?s.you:s.winner;
        portrait(s.players[shown].characterId,80,225,690,555,true);
        var title=gold(draw?"무승부":win?"승리":"패배",710,630,760,115,"hero");title.setColor(draw?CREAM:win?GOLD:RED);
        label(draw?"두 사람의 승부는 끝나지 않았습니다.":win?"마지막 테이블의 주인":"이번 테이블은 여기까지입니다.",720,550,740,60);
        panel(850,264,590,260);gold("최종 결과",880,460,530,47,"title");
        gold(s.players[0].name+"  "+s.players[0].wins+" : "+s.players[1].wins+"  "+s.players[1].name,880,390,530,50,"title");
        label("무승부 "+s.draws+"회 · "+s.round+"라운드",880,335,530,40,"small",Align.center);
        label(s.reason,880,286,530,36,"small",Align.center);
        label("내 누적 기록  "+app.records.wins+"승 "+app.records.losses+"패 "+app.records.draws+"무",470,175,810,40,"small",Align.center);
        TextButton again=button(s.me().rematch?"상대의 다시 하기를 기다리는 중…":"다시 하기",470,80,460,68,"primary",()->app.send(Command.of(Action.REMATCH)));again.setDisabled(s.me().rematch);again.setName("rematch");
        button("메인으로",960,80,320,68,"default",app::home).setName("result-home");
    }
}
