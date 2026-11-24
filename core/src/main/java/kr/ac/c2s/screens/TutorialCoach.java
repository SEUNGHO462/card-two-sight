package kr.ac.c2s.screens;
import kr.ac.c2s.C2SGame;
import kr.ac.c2s.common.Protocol.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.utils.Align;
import java.util.*;
import static kr.ac.c2s.ui.Theme.*;

/**
 * 튜토리얼 모드. 쉬움 AI와 실제 연습 경기를 하면서, 지금 눌러야 할 진짜 버튼에 금색 테두리를 두르고
 * 옆에 말풍선으로 설명한다. 사용자가 그 버튼을 직접 누르면 다음 설명으로 넘어간다.
 * 화면 코드는 건드리지 않고 버튼 이름(setName)으로 위치를 찾는다.
 */
public final class TutorialCoach {
    private final C2SGame app;
    private final Set<String> done=new HashSet<>();
    private String currentKey;
    private boolean off;
    private Guide guide;
    private Texture ring,tailDown,tailUp;
    public TutorialCoach(C2SGame app){this.app=app;}

    /** 메인 메뉴의 튜토리얼 버튼: 기존 팝업 스타일로 한 번 안내한 뒤 연습 경기를 시작한다. */
    public static void offer(C2SGame app,BaseScreen screen){
        Dialog d=screen.titledDialog("튜토리얼",720);d.setName("tutorial-offer");
        Label body=new Label("쉬움 난이도 AI와 실제로 한 판을 두면서 배웁니다.\n\n지금 눌러야 할 버튼에 금색 테두리와 말풍선이 나타납니다. 설명을 읽고 그 버튼을 직접 눌러 보세요.",app.theme.skin,"small");
        body.setColor(Color.valueOf("fff2dc"));body.setWrap(true);body.setAlignment(Align.topLeft);
        d.getContentTable().add(body).width(720).left().row();
        TextButton start=new TextButton("시작하기",app.theme.skin,"primary");start.setName("tutorial-start");
        start.addListener(new ChangeListener(){@Override public void changed(ChangeEvent e,Actor a){app.sound("click");d.hide();app.startTutorial();}});
        d.getButtonTable().add(start);
        d.button("닫기");d.getButtonTable().getChildren().peek().setName("tutorial-cancel");
        d.show(screen.stage);
    }

    /** 상태가 바뀔 때마다 C2SGame이 부른다. 현재 화면 위에 말풍선 레이어를 붙인다. */
    public void onState(BaseScreen screen,Snapshot s){
        if(off)return;
        if(guide==null||guide.getStage()!=screen.stage){guide=new Guide(screen);screen.stage.addActor(guide);}
        else guide.toFront();
    }

    public void stop(){off=true;if(guide!=null)guide.remove();guide=null;}

    // ───────────────────────── 단계 정의 ─────────────────────────
    /** anchor: 말풍선이 가리킬 버튼들, extra: 함께 테두리만 두를 버튼들. 둘 다 없으면 화면 위쪽 가운데에 말풍선만. */
    private record Step(String key,String text,String[] anchor,String[] extra){
        Step(String key,String text,String... anchor){this(key,text,anchor,new String[0]);}
    }
    private static String[] names(String... n){return n;}
    private static String[] range(String prefix,int from,int to){String[] r=new String[to-from+1];for(int i=from;i<=to;i++)r[i-from]=prefix+i;return r;}

    private Step step(BaseScreen screen,Snapshot s){
        Player me=s.me();
        switch(s.phase){
            case LOBBY->{
                if(!me.ready)return new Step("lobby-ready","튜토리얼에 오신 걸 환영합니다! 쉬움 AI와 실제로 한 판을 두면서 배워요.\n먼저 [준비하기]를 눌러 보세요.","ready");
                return new Step("lobby-start","좋아요. 이제 [게임 시작]을 누르면 경기가 시작됩니다.","game-start");
            }
            case LOADOUT_SELECT->{
                if(me.loadoutReady)return new Step("loadout-wait","상대가 카드를 고르는 중입니다. 잠시만 기다리세요.");
                java.util.List<String> free=new ArrayList<>();int chosen=0;
                for(String id:names("REVERSE","SCOUT","DISGUISE","PREDICT")){
                    Actor a=find(screen,"loadout-"+id);
                    if(a instanceof TextButton b&&b.getText().toString().startsWith("선택됨"))chosen++;else free.add(id);
                }
                if(chosen==0)return new Step("loadout-1","경기에 가져갈 특수카드 2장을 고릅니다.\n처음이라면 [역전]이 쓰기 쉬워요. 그 라운드는 낮은 숫자가 이기게 됩니다. [선택하기]를 눌러 보세요.","loadout-REVERSE");
                if(chosen==1){String next=free.contains("SCOUT")?"SCOUT":free.get(0);
                    return new Step("loadout-2","한 장 더 고르세요.\n[정찰]은 판정 뒤 상대 숫자가 1~4인지 5~9인지 나만 알려 줍니다.","loadout-"+next);}
                return new Step("loadout-confirm","두 장을 골랐어요. 카드마다 경기 중 한 번, 한 라운드에 1장까지 쓸 수 있습니다.\n[이 카드로 시작]을 누르세요.","loadout-confirm");
            }
            case BET_SELECT->{
                if(me.betReady)return new Step("bet-wait","베팅 완료! 상대가 칩을 거는 중입니다.");
                return new Step("bet","숫자를 내기 전에 칩 1~3개를 겁니다. 둘 중 적게 건 만큼만 승부에 걸리고, 이긴 쪽이 가져가요.\n칩을 고른 뒤 아래 확정 버튼을 누르세요.",range("bet-",1,3),names("bet-confirm"));
            }
            case TILE_SELECT->{
                if(me.selected)return new Step("tile-wait","상대가 숫자를 고르는 중입니다.");
                Actor confirm=find(screen,"tile-confirm");
                if(confirm instanceof TextButton b&&b.isDisabled())
                    return new Step("tile-pick","숫자 타일 하나를 고르세요. 높은 숫자가 이기고 같으면 무승부, 단 1은 9를 이깁니다.\n한 번 낸 숫자는 다시 못 쓰니 큰 숫자는 아껴 두는 게 좋아요.",range("tile-",1,9));
                return new Step("tile-confirm","이 숫자로 할까요? [선택 확정]을 누르면 바꿀 수 없습니다.","tile-confirm");
            }
            case SPECIAL_SELECT->{
                if(me.specialReady)return new Step("special-wait","상대가 특수카드를 쓸지 고민하고 있습니다.");
                return new Step("special","이번 라운드에 특수카드를 쓸지 정합니다. 카드를 고르고 [선택한 카드 사용]을 누르거나,\n아껴 두려면 [사용 안 함]을 누르세요.",names("special-skip"),names("special-use"));
            }
            case ROUND_RESULT->{
                if(me.acknowledged)return null;
                return new Step("result","상대의 정확한 숫자는 공개되지 않고 홀짝만 보여요. 위장 카드를 썼다면 거짓일 수도 있습니다.\n진 쪽은 리볼버에 실탄이 1발 늘어나요. [룰렛으로 이동]을 누르세요.","round-next");
            }
            case AMMO_LOAD->{
                if(s.pendingAmmo<=0||!(screen instanceof RouletteScreen r)||!r.isAmmoPlacementReady())return null;
                Actor confirm=find(screen,"chamber-confirm");
                if(confirm instanceof TextButton b&&b.isDisabled())
                    return new Step("ammo-pick","숫자 대결에서 졌기 때문에 실탄 1발을 직접 넣어야 합니다. 비어 있는 칸 하나를 누르세요.",range("chamber-",0,8));
                return new Step("ammo-confirm","이 칸에 실탄을 넣습니다. 장전이 끝나면 탄창이 돌아 위치가 섞여요.","chamber-confirm");
            }
            case ROULETTE_SELECT->{
                if(me.chamberReady)return new Step("shot-wait","격발 준비 완료! 상대를 기다립니다.");
                if(!(screen instanceof RouletteScreen r)||!r.isPreparationReady())return new Step("spin","탄창이 돌고 있습니다. 회전이 끝나면 실탄 위치는 보이지 않아요.");
                Actor confirm=find(screen,"chamber-confirm");
                if(confirm instanceof TextButton b&&b.isDisabled())return new Step("chamber-pick","탄실 1~9 중 하나를 고르세요.",range("chamber-",0,8));
                if(!s.boughtHints.isEmpty())return new Step("hint-bought","힌트 결과는 오른쪽 아래 [내가 구매한 힌트]에 나와요.\n결과를 보고 탄실을 바꾸거나 그대로 둔 뒤, 아래 격발 버튼을 누르세요.","chamber-confirm");
                if(!done.contains("hint"))return new Step("hint","칩으로 힌트를 살 수 있어요. 라운드마다 하나만 살 수 있고, 안 사도 됩니다.\n준비되면 아래 격발 버튼을 누르세요. 실탄이면 목숨이 1 줄어요.",names("hint-RANGE","hint-INSPECT"),names("chamber-confirm"));
                return new Step("hint","탄실을 골랐으면 격발 버튼을 누르세요.","chamber-confirm");
            }
            case ROULETTE_RESULT->{
                if(!(screen instanceof RouletteScreen r)||!r.isResultReady()||me.acknowledged)return null;
                return new Step("shot-result","목숨 3개가 모두 없어지면 패배, 둘 다 9라운드를 버티면 숫자 대결을 더 많이 이긴 쪽이 승리합니다.\n다음 라운드부터 같은 순서가 반복돼요. 이제 직접 해 보세요!","roulette-next");
            }
            case FINAL->{
                return new Step("final","튜토리얼 완료! [다시 하기]로 한 판 더 하거나, 메인으로 가서 온라인 대전을 시작해 보세요.",names("rematch","result-home"));
            }
        }
        return null;
    }

    private static Actor find(BaseScreen screen,String name){Actor a=screen.stage.getRoot().findActor(name);return a!=null&&a.isVisible()&&a.getStage()!=null?a:null;}

    // ───────────────────────── 말풍선 레이어 ─────────────────────────
    private final class Guide extends Group {
        private final BaseScreen screen;
        private final Table bubble=new Table();
        private final Label text;
        private final Image tail=new Image();
        private final java.util.List<Image> rings=new ArrayList<>();
        private float t;
        Guide(BaseScreen screen){
            this.screen=screen;setTouchable(Touchable.childrenOnly);setName("tutorial-guide");
            if(ring==null)makeTextures();
            bubble.setBackground(app.theme.panel());bubble.pad(16,22,12,22);bubble.setTouchable(Touchable.childrenOnly);bubble.setName("tutorial-bubble");
            text=new Label("",app.theme.skin,"body");text.setColor(Color.valueOf("fff2dc"));text.setWrap(true);text.setAlignment(Align.topLeft);text.setName("tutorial-text");
            bubble.add(text).width(540).left();
            tail.setTouchable(Touchable.disabled);
            addActor(tail);addActor(bubble);
            // 건너뛰기는 말풍선과 떨어진 왼쪽 아래 구석에 작게 두고, 누르면 확인창을 한 번 더 띄운다.
            TextButton.TextButtonStyle quiet=new TextButton.TextButtonStyle(app.theme.skin.get("small",TextButton.TextButtonStyle.class));
            quiet.font=app.theme.skin.getFont("tiny");quiet.fontColor=Color.valueOf("bfae92");
            TextButton skip=new TextButton("튜토리얼 건너뛰기",quiet);skip.setName("tutorial-skip");skip.setBounds(24,14,168,32);
            skip.addListener(new ChangeListener(){@Override public void changed(ChangeEvent e,Actor a){app.sound("click");confirmSkip();}});
            addActor(skip);
        }
        private void confirmSkip(){
            Dialog d=screen.titledDialog("튜토리얼 건너뛰기",560);d.setName("tutorial-skip-dialog");
            Label body=new Label("튜토리얼을 끝내고 메인 화면으로 돌아갑니다.\n진행 중인 연습 경기는 종료됩니다.",app.theme.skin,"small");
            body.setColor(Color.valueOf("fff2dc"));body.setWrap(true);body.setAlignment(Align.center);
            d.getContentTable().add(body).width(560).center().row();
            TextButton yes=new TextButton("메인으로",app.theme.skin);yes.setName("tutorial-skip-confirm");
            yes.addListener(new ChangeListener(){@Override public void changed(ChangeEvent e,Actor a){app.sound("click");d.hide();stop();app.home();}});
            d.getButtonTable().add(yes);
            d.button("계속 배우기");d.getButtonTable().getChildren().peek().setName("tutorial-skip-cancel");
            d.show(screen.stage);
        }
        @Override public void act(float delta){
            super.act(delta);t+=delta;
            Snapshot s=app.state;
            Actor skip=findActor("tutorial-skip");if(skip!=null)skip.setVisible(s==null||s.phase!=Phase.FINAL);
            Step step=s==null||s.me()==null?null:step(screen,s);
            String key=step==null?null:step.key;
            if(!Objects.equals(key,currentKey)){if(currentKey!=null&&!currentKey.endsWith("-wait")&&!currentKey.equals("spin"))done.add(currentKey);currentKey=key;}
            boolean waiting=step!=null&&(step.key.endsWith("-wait")||step.key.equals("spin"));
            boolean hide=step==null||(s.round>1&&(done.contains(step.key)||waiting));
            if(step!=null&&step.key.equals("hint")&&s.round>1)hide=true;
            for(Image r:rings)r.remove();rings.clear();
            if(hide){bubble.setVisible(false);tail.setVisible(false);return;}
            if(step.key.equals("final"))done.add("final");
            text.setText(step.text);
            Rectangle anchor=bounds(step.anchor);
            float pulse=.55f+.45f*(float)Math.sin(t*5);
            if(anchor!=null)ringAround(anchor,pulse);
            for(String n:step.extra){Rectangle r=bounds(n);if(r!=null)ringAround(r,pulse*.7f);}
            bubble.setVisible(true);bubble.pack();
            float bw=bubble.getWidth(),bh=bubble.getHeight();
            float W=BaseScreen.W,H=BaseScreen.H;
            if(anchor==null){bubble.setPosition((W-bw)/2,H-170-bh);tail.setVisible(false);bubble.toFront();return;}
            float cx=anchor.x+anchor.width/2;
            // 카드 선택 화면은 버튼 위에 카드 설명이 있어 말풍선을 버튼 아래에 둔다.
            boolean preferBelow=step.key.startsWith("loadout-")&&!step.key.equals("loadout-confirm");
            boolean above=!preferBelow&&anchor.y+anchor.height+bh+40<H-20;
            float by=above?anchor.y+anchor.height+26:anchor.y-26-bh;
            if(!above&&by<10){above=true;by=Math.min(anchor.y+anchor.height+26,H-bh-10);}
            float bx=MathUtils.clamp(cx-bw/2,16,W-bw-16);
            float bob=0; // 글씨가 흔들리면 읽기 어려워 말풍선은 고정한다.
            bubble.setPosition(bx,by+bob);
            tail.setDrawable(new TextureRegionDrawable(new TextureRegion(above?tailDown:tailUp)));
            tail.setSize(34,20);
            tail.setPosition(MathUtils.clamp(cx-17,bx+18,bx+bw-52),(above?by-18:by+bh-2)+bob);
            tail.setVisible(true);bubble.toFront();
        }
        private Rectangle bounds(String... names){
            Rectangle total=null;
            for(String n:names){
                Actor a=find(screen,n);if(a==null)continue;
                Vector2 p=a.localToStageCoordinates(new Vector2(0,0));
                Rectangle r=new Rectangle(p.x,p.y,a.getWidth()*a.getScaleX(),a.getHeight()*a.getScaleY());
                total=total==null?r:total.merge(r);
            }
            return total;
        }
        private void ringAround(Rectangle r,float alpha){
            Image i=new Image(new NinePatchDrawable(new NinePatch(ring,10,10,10,10)));
            i.setBounds(r.x-8,r.y-8,r.width+16,r.height+16);i.setColor(1,1,1,alpha);i.setTouchable(Touchable.disabled);
            addActor(i);rings.add(i);i.toBack();
        }
    }

    private void makeTextures(){
        var skin=app.theme.skin;
        if(skin.has("tutorialRing",Texture.class)){ring=skin.get("tutorialRing",Texture.class);tailDown=skin.get("tutorialTailDown",Texture.class);tailUp=skin.get("tutorialTailUp",Texture.class);return;}
        Pixmap p=new Pixmap(32,32,Pixmap.Format.RGBA8888);p.setColor(0,0,0,0);p.fill();
        p.setColor(GOLD);for(int k=0;k<4;k++)p.drawRectangle(k,k,32-2*k,32-2*k);
        p.setColor(GOLD.r,GOLD.g,GOLD.b,.35f);for(int k=4;k<7;k++)p.drawRectangle(k,k,32-2*k,32-2*k);
        ring=new Texture(p);p.dispose();
        tailDown=triangle(true);tailUp=triangle(false);
        app.theme.skin.add("tutorialRing",ring);app.theme.skin.add("tutorialTailDown",tailDown);app.theme.skin.add("tutorialTailUp",tailUp);
    }
    /** 말풍선 꼬리. 패널과 같은 금색 테두리·어두운 바탕. */
    private static Texture triangle(boolean down){
        int w=34,h=20;Pixmap p=new Pixmap(w,h,Pixmap.Format.RGBA8888);p.setColor(0,0,0,0);p.fill();
        int tip=down?h-1:0,base=down?0:h-1;
        p.setColor(GOLD);p.fillTriangle(0,base,w-1,base,w/2,tip);
        p.setColor(Color.valueOf("17120f"));int inset=3;
        p.fillTriangle(inset+1,base,w-2-inset,base,w/2,down?tip-inset-1:tip+inset+1);
        Texture t=new Texture(p);p.dispose();return t;
    }
}
