package kr.ac.c2s.screens;
import kr.ac.c2s.C2SGame;
import kr.ac.c2s.common.Protocol;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;

public final class ConnectionScreen extends BaseScreen {
    private TextField nickname,host,port,password;
    private boolean localServer=true;
    private TextButton create,join,practice;
    public ConnectionScreen(C2SGame app){super(app);build();}
    private void build(){heading("테이블 입장","혼자 연습하거나, 같은 비밀번호로 두 사람이 접속하세요.");
        panel(85,185,580,550);portrait(app.records.character,185,420,380,280,true);
        label("닉네임",130,380,150,40,"small",Align.left);nickname=field(app.records.nickname,"닉네임 1~12자",280,375,320,48);nickname.setMaxLength(12);
        practice=button("연습 테이블 입장",140,270,470,70,"primary",()->{if(saveName())app.practice();});practice.setName("practice");
        label("연습 상대와 9라운드 전체 흐름을 확인합니다.",110,210,530,42,"small",Align.center);
        panel(715,185,800,550);gold("2인 온라인 테이블",780,653,670,56,"title");
        label("서버 주소",760,568,180,40,"small",Align.left);host=field("127.0.0.1","방장 PC의 IP 주소",960,565,490,48);
        label("포트",760,498,180,40,"small",Align.left);port=field("54555","포트",960,495,200,48);port.setMaxLength(5);
        label("방 비밀번호",760,428,180,40,"small",Align.left);password=field("DRAGON","한글·영문·숫자 3~16자",960,425,490,48);password.setMaxLength(16);
        TextButton toggle=button("내 PC에서 서버도 실행: 켜짐",770,350,680,48,"small",()->{});
        toggle.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener(){@Override public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent e,float x,float y){localServer=!localServer;toggle.setText("내 PC에서 서버도 실행: "+(localServer?"켜짐":"꺼짐"));}});
        create=button("방 만들기",770,260,325,64,"primary",()->enter(true));join=button("방 입장",1125,260,325,64,"default",()->enter(false));
        paragraph("방장은 서버 주소 127.0.0.1로 방을 만듭니다. 다른 PC는 방장의 IP와 같은 포트·비밀번호를 입력하세요. 외부 인터넷 대전은 별도 서버 설정이 필요합니다.",765,190,700,55);
        button("뒤로",60,70,170,50,"small",()->app.go(new CharacterSelectScreen(app)));
    }
    private boolean saveName(){try{app.records.nickname=Protocol.cleanName(nickname.getText());app.records.save();return true;}catch(IllegalArgumentException e){app.toast(e.getMessage());return false;}}
    private void enter(boolean making){
        if(!saveName())return;
        try{String word=Protocol.cleanPassword(password.getText());int n=Integer.parseInt(port.getText());if(n<1024||n>65535)throw new IllegalArgumentException("포트는 1024~65535로 입력하세요.");
            String address=host.getText().strip();if(address.isBlank())throw new IllegalArgumentException("서버 주소를 입력하세요.");
            app.online(address,n,word,making,making&&localServer);
        }catch(IllegalArgumentException e){app.toast(e instanceof NumberFormatException?"포트 번호를 숫자로 입력하세요.":e.getMessage());}
    }
    @Override public void render(float delta){create.setDisabled(app.connecting);join.setDisabled(app.connecting);practice.setDisabled(app.connecting);super.render(delta);}
    @Override protected void back(){app.disconnect();app.go(new CharacterSelectScreen(app));}
}
