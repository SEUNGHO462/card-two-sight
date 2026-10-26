package kr.ac.c2s.screens;
import kr.ac.c2s.C2SGame;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Align;
import static kr.ac.c2s.ui.Theme.*;

public final class MainMenuScreen extends BaseScreen {
    public MainMenuScreen(C2SGame app){
        super(app);dim=.20f;
        gold("CARD",400,627,800,95,"hero");gold("TWO SIGHT",270,531,1060,98,"hero");
        label("오늘 밤, 마지막 한 판이 시작된다.",400,460,800,44);
        button("테이블에 앉기",565,285,470,78,"primary",()->app.go(new CharacterSelectScreen(app))).setName("start");
        button("게임 방법",490,200,190,54,"small",this::help).setName("help");
        button("설정",705,200,190,54,"small",this::settings).setName("settings");
        button("종료",920,200,190,54,"small",Gdx.app::exit);
        label("기록  "+app.records.wins+"승  "+app.records.losses+"패  "+app.records.draws+"무",470,100,660,36,"small",Align.center);
        label("2인 온라인 대전 · 연습 모드    /    F11 전체 화면",400,60,800,32,"tiny",Align.center);
    }
    private void help(){modal("게임 방법","각자 숫자 타일 1~9를 한 번씩 사용합니다. 높은 숫자가 승리하고, 같은 수는 무승부입니다. 단, 1은 9를 이깁니다.\n\n서로 타일을 확정한 뒤 특수카드 1장 또는 사용 안 함을 선택합니다. 상대 숫자는 끝까지 숨기고, 판정 뒤 홀짝만 표시합니다.\n\n패배하면 내 실탄이 1발 늘고, 승리하면 1발 줄어듭니다. 각자 탄실 1~9를 고르고 룰렛을 진행합니다. 실탄이면 목숨 1개가 줄어듭니다.\n\n목숨은 3개입니다. 목숨이 0이면 즉시 패배하며, 둘 다 살아서 9라운드를 마치면 라운드 승리 횟수로 최종 판정합니다.\n\n힌트는 내 실탄 배치를 추리하는 기능입니다. 시작 칩 12개, 힌트당 2개를 사용합니다.");}
    private void settings(){
        Dialog d=titledDialog("설정",420);
        TextButton sound=new TextButton(app.records.sound?"효과음 켜짐":"효과음 꺼짐",app.theme.skin);
        sound.addListener(new ChangeListener(){@Override public void changed(ChangeEvent e,Actor a){app.records.sound=!app.records.sound;sound.setText(app.records.sound?"효과음 켜짐":"효과음 꺼짐");app.records.save();}});
        Slider volume=new Slider(0,1,.05f,false,app.theme.skin);volume.setValue(app.records.volume);
        volume.addListener(new ChangeListener(){@Override public void changed(ChangeEvent e,Actor a){app.records.volume=volume.getValue();app.records.save();}});
        d.getContentTable().add(sound).width(380).height(58).row();d.getContentTable().add(new Label("음량",app.theme.skin)).padTop(24).row();d.getContentTable().add(volume).width(380).height(45).row();
        d.getContentTable().add(new Label("F11: 전체 화면 전환",app.theme.skin,"small")).padTop(24);d.button("닫기");d.getButtonTable().getChildren().peek().setName("settings-close");d.show(stage);
    }
}
