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
        ornate("튜토리얼",330,190,220,70,false,"tutorial",()->TutorialCoach.offer(app,this));
        ornate("게임 방법",570,190,220,70,false,"help",this::help);
        ornate("설정",810,190,220,70,false,"settings",this::settings);
        ornate("종료",1050,190,220,70,false,"exit",Gdx.app::exit);
        label("2인 온라인 대전 · 연습 모드    /    F11 전체 화면",400,60,800,32,"tiny",Align.center);
    }
    private void help(){
        Dialog d=titledDialog("게임 방법",760);d.setName("help-dialog");
        Table rules=new Table();rules.top().left();
        helpSection(rules,"게임 준비","특수카드 4종 중 서로 다른 2장을 선택합니다. 각 카드는 경기 중 한 번씩, 라운드당 최대 1장 사용할 수 있습니다.");
        helpSection(rules,"숫자 대결","숫자 타일 1~9를 한 번씩 사용합니다. 높은 숫자가 승리하고 같은 숫자는 무승부입니다. 단, 1은 9를 이깁니다.");
        helpSection(rules,"베팅","보유 칩 30개로 시작합니다. 매 라운드 각자 1~3칩을 걸고, 적게 건 금액만큼 숫자 대결로 승부합니다. 초과분과 무승부는 반환하며, 같은 칩으로 힌트를 구매합니다. 칩이 없으면 0칩으로 진행합니다.");
        helpSection(rules,"특수카드","숫자를 확정한 뒤 카드 사용 여부를 선택합니다. 상대 숫자는 숨겨지고, 판정 뒤 홀짝과 사용한 특수카드를 확인할 수 있습니다.");
        helpSection(rules,"장전과 격발","9칸 탄창에 실탄 1발로 시작합니다. 패배하면 원하는 빈칸에 1발을 추가하며, 승리·무승부에는 유지됩니다. 회전 뒤 탄실을 골라 격발하고 피격 시 목숨이 1 감소합니다.");
        helpSection(rules,"승리 조건","목숨은 3개입니다. 목숨이 0이면 패배하고, 둘 다 9라운드까지 생존하면 숫자 대결 승수로 최종 판정합니다.");
        helpSection(rules,"힌트 구매","베팅과 같은 보유 칩을 사용합니다. 범위 탐색은 4칩, 탄실 검사는 8칩이며 두 힌트 중 하나만 라운드당 1회 구매할 수 있습니다. 칩은 추가 지급되지 않습니다.");
        ScrollPane scroll=new ScrollPane(rules,app.theme.skin);scroll.setName("help-scroll");
        scroll.setFadeScrollBars(false);scroll.setScrollingDisabled(true,false);
        d.getContentTable().add(scroll).width(760).height(400).row();
        d.button("닫기");d.getButtonTable().getChildren().peek().setName("help-close");
        d.show(stage);stage.setScrollFocus(scroll);
    }
    private void helpSection(Table table,String title,String text){
        Label heading=new Label(title,app.theme.skin,"default");heading.setColor(GOLD);
        Label body=new Label(text,app.theme.skin,"small");body.setWrap(true);body.setAlignment(Align.topLeft);
        table.add(heading).width(714).left().padBottom(8).row();
        table.add(body).width(714).left().padBottom(22).row();
    }
    private void settings(){
        Dialog d=titledDialog("설정",420);
        TextButton sound=new TextButton(app.records.sound?"효과음 켜짐":"효과음 꺼짐",app.theme.skin);
        sound.addListener(new ChangeListener(){@Override public void changed(ChangeEvent e,Actor a){app.records.sound=!app.records.sound;sound.setText(app.records.sound?"효과음 켜짐":"효과음 꺼짐");app.records.save();}});
        TextButton music=new TextButton(app.records.music?"배경음악 켜짐":"배경음악 꺼짐",app.theme.skin);
        music.addListener(new ChangeListener(){@Override public void changed(ChangeEvent e,Actor a){app.records.music=!app.records.music;music.setText(app.records.music?"배경음악 켜짐":"배경음악 꺼짐");app.updateMusic();app.records.save();}});
        Slider volume=new Slider(0,1,.05f,false,app.theme.skin);volume.setValue(app.records.volume);
        volume.addListener(new ChangeListener(){@Override public void changed(ChangeEvent e,Actor a){app.records.volume=volume.getValue();app.updateMusic();app.records.save();}});
        d.getContentTable().add(sound).width(380).height(58).row();if(app.assets.hasMusic())d.getContentTable().add(music).width(380).height(58).padTop(10).row();d.getContentTable().add(new Label("음량",app.theme.skin)).padTop(24).row();d.getContentTable().add(volume).width(380).height(45).row();
        d.getContentTable().add(new Label("F11: 전체 화면 전환",app.theme.skin,"small")).padTop(24);d.button("닫기");d.getButtonTable().getChildren().peek().setName("settings-close");d.show(stage);
    }
}
