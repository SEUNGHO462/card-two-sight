package kr.ac.c2s.screens;
import kr.ac.c2s.C2SGame;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.Vector2;
import kr.ac.c2s.ui.MenuDecor;
import com.badlogic.gdx.utils.Align;
import static kr.ac.c2s.ui.Theme.*;

public final class MainMenuScreen extends BaseScreen {
    private final java.util.List<TextButton> menuButtons=new java.util.ArrayList<>();
    private Runnable pending;
    private float shotDelay;
    private boolean previewHold;
    private Vector2 shotPoint;
    public void holdShotForPreview(boolean hold){previewHold=hold;}
    public boolean shotPending(){return pending!=null;}
    private TextButton ornate(String text,float x,float y,float w,float h,boolean primary,String name,Runnable action){
        TextButton.TextButtonStyle style=new TextButton.TextButtonStyle();
        style.up=MenuDecor.plaque(app,Color.WHITE);style.over=MenuDecor.plaque(app,new Color(1,.93f,.80f,1));style.down=MenuDecor.plaque(app,new Color(.82f,.70f,.60f,1));style.disabled=style.up;
        style.font=app.theme.skin.getFont(primary?"title":"default");style.fontColor=CREAM;style.overFontColor=Color.WHITE;style.disabledFontColor=CREAM;
        TextButton b=new TextButton(text,style);b.setBounds(x,y,w,h);b.setName(name);b.setProgrammaticChangeEvents(false);content.addActor(b);menuButtons.add(b);
        b.addListener(new InputListener(){@Override public boolean touchDown(InputEvent e,float px,float py,int pointer,int button){shotPoint=b.localToStageCoordinates(new Vector2(px,py));return false;}});
        b.addListener(new ChangeListener(){@Override public void changed(ChangeEvent e,Actor a){
            if(pending!=null)return;
            Vector2 point=shotPoint==null?new Vector2(x+w-65,y+h/2):shotPoint;shotPoint=null;
            Image hole=new Image(new TextureRegionDrawable(app.assets.trimmed("ui/menu/bullet-impact-v1.png")));
            hole.setBounds(point.x-32,point.y-32,64,64);hole.setOrigin(32,32);hole.setScale(1.3f);hole.setTouchable(Touchable.disabled);hole.setName("menu-impact");
            hole.addAction(Actions.scaleTo(1,1,.12f));content.addActor(hole);
            app.sound("trigger-shot-v2");pending=action;shotDelay=.32f;for(TextButton item:menuButtons)item.setDisabled(true);
        }});return b;
    }
    @Override public void render(float delta){
        super.render(delta);
        if(pending!=null&&!previewHold){shotDelay-=Math.min(delta,.05f);if(shotDelay<=0){Runnable action=pending;pending=null;for(TextButton b:menuButtons)b.setDisabled(false);action.run();}}
    }
    public MainMenuScreen(C2SGame app){
        super(app);dim=.20f;
        Image logo=new Image(new TextureRegionDrawable(app.assets.trimmed("ui/menu/logo-antique-v1.png")));
        logo.setScaling(com.badlogic.gdx.utils.Scaling.fit);logo.setBounds(330,505,940,315);logo.setTouchable(Touchable.disabled);content.addActor(logo);
        label("오늘 밤, 마지막 한 판이 시작된다.",400,445,800,44);
        ornate("테이블에 앉기",500,285,600,90,true,"start",()->app.go(new CharacterSelectScreen(app)));
        ornate("게임 방법",420,190,240,70,false,"help",this::help);
        ornate("설정",680,190,240,70,false,"settings",this::settings);
        ornate("종료",940,190,240,70,false,"exit",Gdx.app::exit);
        label("기록  "+app.records.wins+"승  "+app.records.losses+"패  "+app.records.draws+"무",470,100,660,36,"small",Align.center);
        label("2인 온라인 대전 · 연습 모드    /    F11 전체 화면",400,60,800,32,"tiny",Align.center);
    }
    private void help(){modal("게임 방법","게임 시작 전 특수카드 4종 중 서로 다른 2장을 직접 선택합니다.\n\n각자 숫자 타일 1~9를 한 번씩 사용합니다. 높은 숫자가 승리하고, 같은 수는 무승부입니다. 단, 1은 9를 이깁니다.\n\n서로 타일을 확정한 뒤 특수카드 1장 또는 사용 안 함을 선택합니다. 상대 숫자는 끝까지 숨기고, 판정 뒤 홀짝만 표시합니다.\n\n9칸 탄창에 기본 실탄 1발로 시작합니다. 패배하면 원하는 빈 탄실을 골라 실탄 1발을 넣으며, 승리·무승부에는 유지됩니다. 탄창 회전 뒤 탄실 1~9을 골라 격발합니다. 실탄이면 목숨 1개가 줄어듭니다.\n\n목숨은 3개입니다. 목숨이 0이면 즉시 패배하며, 둘 다 살아서 9라운드를 마치면 라운드 승리 횟수로 최종 판정합니다.\n\n힌트는 내 실탄 배치를 추리하는 기능입니다. 시작 칩 12개를 한 경기 동안 사용합니다. 범위 탐색은 4칩, 탄실 검사는 8칩이며 라운드마다 칩이 충전되지는 않습니다.");}
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
