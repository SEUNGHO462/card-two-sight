package kr.ac.c2s.screens;
import kr.ac.c2s.C2SGame;
import kr.ac.c2s.common.Protocol;
import kr.ac.c2s.common.Protocol.*;
import kr.ac.c2s.network.PracticeDifficulty;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import static kr.ac.c2s.ui.Theme.*;

public final class LobbyScreen extends BaseScreen {
    private Snapshot s;
    public LobbyScreen(C2SGame app,Snapshot s){super(app);accept(s);}
    public void accept(Snapshot state){s=state;build();}
    private void build(){
        clear();boolean practice=app.isPractice();
        heading(practice?"AI 연습 대기방":"게임 대기방",practice?"난이도와 연습 상대를 선택한 뒤 준비하세요.":s.message.isBlank()?"같은 테이블에 두 사람이 모이면 시작할 수 있습니다.":s.message);
        if(!practice)gold("접속 인원 "+(s.opponent()==null?"1 / 2":"2 / 2"),1240,834,300,42,"small");
        portrait(s.me().characterId,100,275,530,445,true);
        if(s.opponent()!=null)portrait(s.opponent().characterId,970,275,530,445,true);
        else{panel(1000,360,470,260);gold("빈 자리",1040,470,390,65,"large");label("상대가 접속하기를 기다리는 중…",1020,400,430,40,"small",Align.center);}
        if(practice)practiceOptions();
        else{
            panel(580,510,440,170);gold("마지막 테이블",610,615,380,42,"default");label("방 비밀번호  "+s.password,600,556,400,38,"default",Align.center);
            button("비밀번호 복사",635,445,330,48,"small",()->{Gdx.app.getClipboard().setContents(s.password);app.toast("방 비밀번호를 복사했습니다.");});
        }
        panel(100,245,530,75);gold((practice?"나 · ":s.you==0?"방장 · 나  ":"나  ")+s.me().name,120,260,490,42,"title");
        panel(970,245,530,75);gold(s.opponent()==null?"상대 대기 중":practice?"연습 상대 · "+characterName(s.opponent().characterId):s.opponent().name,990,260,490,42,"title");
        TextButton ready=button(s.me().ready?"준비 취소":"준비하기",210,160,310,62,s.me().ready?"default":"primary",()->app.send(Command.of(Action.READY)));ready.setName("ready");
        var status=label(s.opponent()!=null&&s.opponent().ready?"준비 완료":"준비 대기",1060,170,350,44,"title",Align.center);status.setColor(s.opponent()!=null&&s.opponent().ready?MINT:CREAM);
        label("9라운드     목숨 3개     특수카드 2장",520,330,560,48,"small",Align.center);
        TextButton start=button(s.you==0?"게임 시작":"방장이 시작하면 입장합니다",575,70,450,66,"primary",()->app.send(Command.of(Action.START)));
        start.setDisabled(s.you!=0 || s.opponent()==null || !s.me().ready || !s.opponent().ready);start.setName("game-start");
        button("방 나가기",1280,70,220,55,"small",app::confirmLeave).setName("leave-room");
    }
    private void practiceOptions(){
        panel(555,395,490,330);gold("연습 설정",580,668,440,42,"default");
        label("난이도",580,624,440,32,"small",Align.left);
        PracticeDifficulty[] modes=PracticeDifficulty.values();
        for(int i=0;i<modes.length;i++){
            PracticeDifficulty mode=modes[i];
            TextButton b=button(mode.name(),580+i*150,566,140,48,mode==app.practiceDifficulty?"primary":"small",()->app.configurePractice(mode,app.practiceOpponent));
            b.setName("difficulty-"+mode.name());
        }
        paragraph(app.practiceDifficulty.description,580,510,440,45);
        label("연습 상대",580,474,440,30,"small",Align.left);
        int selected=java.util.Arrays.asList(Protocol.CHARACTER_IDS).indexOf(app.practiceOpponent);
        button("‹",580,416,62,48,"small",()->chooseOpponent(selected-1)).setName("opponent-prev");
        button(characterName(app.practiceOpponent)+" · 변경",652,416,296,48,"small",this::opponentDialog).setName("opponent-select");
        button("›",958,416,62,48,"small",()->chooseOpponent(selected+1)).setName("opponent-next");
    }
    private void chooseOpponent(int index){app.configurePractice(app.practiceDifficulty,Protocol.CHARACTER_IDS[Math.floorMod(index,Protocol.CHARACTER_IDS.length)]);}
    private void opponentDialog(){
        var d=titledDialog("연습 상대 선택",750);var grid=new com.badlogic.gdx.scenes.scene2d.ui.Table();
        for(int i=0;i<Protocol.CHARACTER_IDS.length;i++){
            final int index=i;var cell=new com.badlogic.gdx.scenes.scene2d.ui.Table();
            var image=new com.badlogic.gdx.scenes.scene2d.ui.Image(app.assets.texture("characters/"+Protocol.CHARACTER_IDS[i]+".png"));image.setScaling(com.badlogic.gdx.utils.Scaling.fit);
            cell.add(image).size(210,175).row();
            TextButton b=new TextButton(Protocol.CHARACTER_NAMES[i],app.theme.skin,"small");b.setName("opponent-choice-"+i);
            b.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ChangeListener(){@Override public void changed(ChangeEvent e,com.badlogic.gdx.scenes.scene2d.Actor a){chooseOpponent(index);d.hide();}});
            cell.add(b).width(210).height(45);grid.add(cell).pad(8);if(i%3==2)grid.row();
        }
        d.getContentTable().add(grid);d.button("닫기");d.show(stage);
    }
    private String characterName(String id){int i=java.util.Arrays.asList(Protocol.CHARACTER_IDS).indexOf(id);return i<0?"상대":Protocol.CHARACTER_NAMES[i];}
}
