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
    private void build(){
        clear();boolean win=s.winner==s.you,draw=s.winner<0;int shown=draw?s.you:s.winner;
        portrait(s.players[shown].characterId,80,245,690,535,true);
        var title=gold(draw?"무승부":win?"승리":"패배",820,650,650,100,"hero");title.setName("final-outcome");title.setAlignment(Align.center);title.setColor(draw?CREAM:win?GOLD:RED);
        label(draw?"두 사람의 승부는 끝나지 않았습니다.":win?"마지막 테이블의 주인":"이번 테이블은 여기까지입니다.",820,578,650,50,"default",Align.center);
        panel(850,255,590,305);
        gold(draw?"최종 결과 · 무승부":"최종 승자 · "+s.players[s.winner].name,875,495,540,46,"title");
        gold("숫자 대결 승수",880,447,530,35,"default");
        gold(s.me().name+"  "+s.me().wins+" : "+s.opponent().wins+"  "+s.opponent().name,875,394,540,48,"title");
        label("남은 목숨 · 나 "+s.me().life+" / 상대 "+s.opponent().life,875,350,540,35,"small",Align.center);
        String reason=s.reason.equals("목숨 소진")?(draw?"두 사람 모두 목숨 소진":win?"상대 목숨 소진으로 최종 승리":"내 목숨 소진으로 최종 패배"):s.reason;
        label(reason,875,308,540,36,"small",Align.center).setName("final-reason");
        label("무승부 "+s.draws+"회 · "+s.round+"라운드",875,267,540,32,"small",Align.center);
        var recordFrame=new com.badlogic.gdx.scenes.scene2d.ui.Image(app.theme.skin.getDrawable("resultRecord"));recordFrame.setBounds(385,165,830,74);recordFrame.setName("result-record-panel");content.addActor(recordFrame);
        gold("내 누적 기록  "+app.records.wins+"승 "+app.records.losses+"패 "+app.records.draws+"무",405,180,790,44,"title").setName("result-record-summary");
        TextButton again=button(s.me().rematch?"상대 다시 하기 대기…":"다시 하기",385,80,400,68,"primary",()->app.send(Command.of(Action.REMATCH)));again.setDisabled(s.me().rematch);again.setName("rematch");
        button("메인으로",815,80,400,68,"default",app::home).setName("result-home");
    }
}
