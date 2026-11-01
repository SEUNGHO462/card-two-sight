package kr.ac.c2s.screens;
import kr.ac.c2s.C2SGame;
import kr.ac.c2s.common.Protocol;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;

public final class CharacterSelectScreen extends BaseScreen {
    private static final String[] BIOS={"거친 테이블을 지켜온 승부사","표정에서 속내를 읽기 어려운 인물","수많은 판을 거친 중년의 베테랑","노련한 미소 뒤에 비밀을 감춘 도박사","마지막 기회를 노리는 거친 청년","여유로운 미소 뒤에 속내를 감춘 승부사"};
    private static final float POP=1.22f,POP_TIME=.22f;
    private int selected,previous=-1;
    public CharacterSelectScreen(C2SGame app){super(app);selected=Math.max(0,java.util.Arrays.asList(Protocol.CHARACTER_IDS).indexOf(app.records.character));build();}
    private void build(){clear();heading("캐릭터 선택","테이블에 앉을 인물을 선택하세요");
        portrait(Protocol.CHARACTER_IDS[selected],840,275,650,490,true);
        var name=label(Protocol.CHARACTER_NAMES[selected],160,576,570,80,"hero",Align.left);name.setColor(kr.ac.c2s.ui.Theme.GOLD);
        paragraph(BIOS[selected],160,440,570,95);
        Image popped=null;TextButton poppedName=null;
        for(int i=0;i<6;i++){final int n=i;float x=110+i*235;panel(x,150,215,202);
            Image face=portrait(Protocol.CHARACTER_IDS[i],x+5,197,205,150,true);face.setOrigin(Align.bottom);
            // 선택한 인물은 이름표를 축으로 커져서 머리가 테두리 위로 나온다.
            if(i==selected){face.setScale(previous<0?POP:1);if(previous>=0)face.addAction(Actions.scaleTo(POP,POP,POP_TIME,Interpolation.pow2Out));popped=face;}
            else if(i==previous){face.setScale(POP);face.addAction(Actions.scaleTo(1,1,POP_TIME,Interpolation.pow2Out));}
            TextButton b=button(Protocol.CHARACTER_NAMES[i],x+5,155,205,40,"small",()->{if(n!=selected){previous=selected;selected=n;build();}});b.setChecked(i==selected);b.setName("character-"+i);if(i==selected)poppedName=b;}
        popped.toFront();poppedName.toFront();
        button("선택 완료",600,60,400,65,"primary",()->{app.records.character=Protocol.CHARACTER_IDS[selected];app.records.save();app.go(new ConnectionScreen(app));}).setName("character-confirm");
        button("뒤로",40,60,160,50,"small",()->app.go(new MainMenuScreen(app)));
    }
    @Override protected void back(){app.go(new MainMenuScreen(app));}
}
