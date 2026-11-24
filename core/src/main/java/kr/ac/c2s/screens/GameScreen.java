package kr.ac.c2s.screens;
import kr.ac.c2s.common.Protocol.Action;
import kr.ac.c2s.C2SGame;
import kr.ac.c2s.common.Protocol.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.*;
import com.badlogic.gdx.utils.Align;
import static kr.ac.c2s.ui.Theme.*;

public final class GameScreen extends BaseScreen {
    private Snapshot s;private int selected,prediction,betAmount=1;private Card chosen;private Boolean disguiseOdd;
    private Label hudChips,topChips,betChips,chipEffect;
    private int chipsFrom,chipsTo,shownChips;private float chipElapsed=1f,effectElapsed=3f;
    private String chipEffectText="";private boolean chipDecrease;
    public GameScreen(C2SGame app,Snapshot state){super(app);accept(state);}
    public void accept(Snapshot state){
        if(s==null || s.round!=state.round){selected=0;betAmount=1;prediction=0;chosen=null;disguiseOdd=null;}
        if(s!=null && s.phase!=Phase.ROUND_RESULT && state.phase==Phase.ROUND_RESULT)app.sound(state.roundWinner==state.you?"win":"click");
        if(s==null){shownChips=chipsFrom=chipsTo=state.me().chips;}
        else if(state.me().chips!=s.me().chips){
            chipsFrom=shownChips;chipsTo=state.me().chips;chipElapsed=0;
        }
        if(s!=null&&state.bettingEnabled){
            boolean placed=state.me().betReady&&(!s.me().betReady||s.round!=state.round);
            boolean settled=state.phase==Phase.ROUND_RESULT&&s.phase!=Phase.ROUND_RESULT;
            if(placed&&state.ownBet>0){chipEffectText="−"+state.ownBet+"칩 · 베팅";chipDecrease=true;effectElapsed=0;}
            if(settled){
                int refund=state.roundWinner<0?state.ownBet:state.ownBet-state.matchedBet;
                int payout=state.roundWinner==state.you?state.matchedBet*2:0;
                chipEffectText=(refund>0?(state.roundWinner<0?"무승부 반환 +":"초과분 반환 +")+refund+"칩":"")
                    +(refund>0&&payout>0?"   ·   ":"")+(payout>0?"승리 획득 +"+payout+"칩":"");
                chipDecrease=false;effectElapsed=0;
            }
        }
        s=state;if(s.ownTile>0)selected=s.ownTile;build();
    }
    private void hud(){heading("ROUND "+s.round+" / 9",s.me().name+"  "+s.me().wins+" : "+s.opponent().wins+"  "+s.opponent().name+"     무승부 "+s.draws);
        panel(42,628,300,120);gold("나 · "+s.me().name,55,700,275,32,"default");heartLabel(s.me().life,60,650,270);
        panel(1258,628,300,120);gold("상대 · "+s.opponent().name,1270,700,275,32,"default");heartLabel(s.opponent().life,1270,650,270);
        hudChips=label("남은 타일 "+s.me().remaining+"장 · 보유 칩 "+shownChips,42,585,300,32,"small",Align.center);
        label("남은 타일 "+s.opponent().remaining+"장 · 특수카드 "+s.opponent().specialCount+"장",1210,585,390,32,"small",Align.center);
        gold("상대 보유 칩  "+s.opponent().chips,1210,548,390,32,"default").setName("opponent-chips");
        panel(43,175,280,380);gold("경기 기록",60,505,245,40,"default");matchHistory(s.history,63,192,240,302);
        button("테이블 나가기",1350,55,210,48,"small",app::confirmLeave);
    }
    private void build(){clear();hudChips=topChips=betChips=chipEffect=null;hud();if(s.phase==Phase.BET_SELECT){betting();addChipEffect();return;}
        if(s.bettingEnabled){topChips=gold("보유 칩  "+shownChips+"     |     상대  "+s.opponent().chips,390,752,820,32,"small");}
        if(s.phase==Phase.ROUND_RESULT){result();addChipEffect();return;}
        String instruction=s.phase==Phase.TILE_SELECT?(s.me().selected?"상대의 타일 선택을 기다리는 중…":"사용할 숫자 타일을 선택하세요"):"특수카드를 사용하거나 이번에는 넘기세요";
        gold(instruction,360,680,880,55,"default");
        tile("?",550,435,150,200,false,false,null);
        tile(selected==0?"?":String.valueOf(selected),895,435,150,200,selected>0&&selected%2==0,false,null);
        gold("VS",730,500,140,60,"large");
        if(s.bettingEnabled)label("베팅  나 "+s.ownBet+" / 상대 "+s.opponentBet+" · 승부 칩 "+s.matchedBet,480,640,640,32,"small",Align.center);label("상대",550,390,150,36);label("나",895,390,150,36);
        var opponent=label(s.opponent().selected?"상대 선택 완료":"상대 선택 중…",590,345,420,36,"small",Align.center);opponent.setColor(s.opponent().selected?MINT:CREAM);
        if(s.phase==Phase.TILE_SELECT){
            label("내 타일",400,288,800,35,"small",Align.center);
            for(int n=1;n<=9;n++){int value=n;TextButton b=tile(String.valueOf(n),365+(n-1)*111,160,94,126,n%2==0,selected==n,()->{selected=value;build();});b.setDisabled(!s.hand.contains(n)||s.me().selected);b.setName("tile-"+n);}
            TextButton confirm=button(s.me().selected?"선택 완료 · 상대 대기":"선택 확정",590,65,420,66,"primary",()->app.send(Command.number(Action.TILE,selected)));confirm.setDisabled(selected==0||s.me().selected);confirm.setName("tile-confirm");
            gold("내 특수카드 "+s.cards.size()+"장",1260,500,280,40,"default");int index=0;
            for(Card c:s.cards){int i=index++;TextButton b=button(c.label,1280,420-i*74,230,56,"small",()->cardDialog(c));b.setName("view-"+c.name());}
        }else special();
        addChipEffect();
    }
    private void betting(){
        if(!s.me().betReady)betAmount=s.me().chips==0?0:Math.max(1,Math.min(betAmount,Math.min(3,s.me().chips)));
        gold("이번 라운드에 걸 칩을 선택하세요",365,665,870,65,"title");
        panel(385,440,830,190);
        betChips=gold("나의 보유 칩  "+shownChips,420,555,350,45,"default");
        gold("상대 보유 칩  "+s.opponent().chips,830,555,350,45,"default");
        label(s.me().betReady?"베팅 차감 완료 · 맡긴 칩 "+s.ownBet:"매 라운드 1~3칩을 선택합니다",420,485,350,45,"small",Align.center);
        label(s.opponent().betReady?"상대 베팅 확정":"상대 선택 중…",830,485,350,45,"small",Align.center);
        for(int n=1;n<=3;n++){int amount=n;TextButton chip=button(n+"",530+(n-1)*190,285,150,130,"circle",()->{betAmount=amount;build();});chip.setChecked(betAmount==n);chip.setDisabled(s.me().betReady||s.me().chips<n);chip.setName("bet-"+n);}
        label("적게 건 금액만큼 승부 · 초과분 반환 · 무승부 전액 반환",370,210,860,45,"small",Align.center);
        TextButton ledger=button("칩 계산 내역 보기",570,155,460,44,"small",this::chipLedger);ledger.setName("chip-ledger");
        TextButton confirm=button(s.me().betReady?"확정 완료 · 상대 대기":betAmount+"칩 베팅 확정",570,65,460,66,"primary",()->app.send(Command.number(Action.BET,betAmount)));confirm.setDisabled(s.me().betReady||s.me().chips<betAmount);confirm.setName("bet-confirm");
    }
    private void addChipEffect(){
        if(chipEffectText.isEmpty()||effectElapsed>=2.5f)return;
        boolean betting=s.phase==Phase.BET_SELECT;
        chipEffect=label(chipEffectText,betting?420:350,betting?520:585,betting?350:900,32,"small",Align.center);
        chipEffect.setName("chip-change-effect");chipEffect.setTouchable(Touchable.disabled);
        updateChipDisplay();
    }
    @Override public void render(float delta){
        chipElapsed=Math.min(.9f,chipElapsed+delta);effectElapsed=Math.min(2.5f,effectElapsed+delta);
        shownChips=chipsFrom+Math.round((chipsTo-chipsFrom)*Math.min(1f,chipElapsed/.9f));
        updateChipDisplay();super.render(delta);
    }
    private void updateChipDisplay(){
        if(hudChips!=null)hudChips.setText("남은 타일 "+s.me().remaining+"장 · 보유 칩 "+shownChips);
        if(topChips!=null)topChips.setText("보유 칩  "+shownChips+"     |     상대  "+s.opponent().chips);
        if(betChips!=null)betChips.setText("나의 보유 칩  "+shownChips);
        if(chipEffect!=null){
            chipEffect.setColor(chipDecrease?RED:MINT);
            chipEffect.getColor().a=Math.max(0,Math.min(1,(2.5f-effectElapsed)/.5f));
            chipEffect.setY((s.phase==Phase.BET_SELECT?520:585)+20*Math.min(1f,effectElapsed/.9f));
        }
    }
    private void chipLedger(){
        Dialog d=titledDialog("칩 계산 내역",780);
        Label text=new Label(s.chipHistory.isEmpty()?"시작 보유 칩 30개":String.join("\n\n",s.chipHistory),app.theme.skin,"small");text.setWrap(true);
        ScrollPane scroll=new ScrollPane(text,app.theme.skin);scroll.setScrollingDisabled(true,false);scroll.setFadeScrollBars(false);
        d.getContentTable().add(scroll).width(780).height(420);d.button("닫기");d.show(stage);scroll.setScrollPercentY(1);stage.setScrollFocus(scroll);
    }
    private TextButton tile(String value,float x,float y,float w,float h,boolean dark,boolean highlight,Runnable action){
        if(highlight)panel(x-5,y-5,w+10,h+10);
        TextButton.TextButtonStyle style=new TextButton.TextButtonStyle(app.theme.skin.get("even",TextButton.TextButtonStyle.class));
        TextureRegionDrawable base=new TextureRegionDrawable(new TextureRegion(app.assets.texture("cards/blank.png")));
        style.up=base.tint(dark?new Color(.20f,.20f,.22f,1):Color.WHITE);style.over=base.tint(new Color(1,.82f,.52f,1));style.down=style.over;
        style.checked=style.up;style.disabled=base.tint(new Color(.32f,.30f,.28f,.85f));style.fontColor=dark?CREAM:Color.valueOf("281d17");style.disabledFontColor=Color.valueOf("938b7d");
        TextButton b=new TextButton(value,style);b.setProgrammaticChangeEvents(false);b.setBounds(x,y,w,h);
        if(action!=null)b.addListener(new ChangeListener(){@Override public void changed(ChangeEvent e,Actor actor){app.sound("click");action.run();}});
        else b.setTouchable(Touchable.disabled);
        content.addActor(b);return b;
    }
    private void special(){
        gold("보유 특수카드",430,292,740,36,"default");int i=0;
        for(Card c:s.cards){float x=s.cards.size()==1?650:520+i*300;TextButton b=button(c.label,x,225,260,60,chosen==c?"primary":"default",()->{chosen=chosen==c?null:c;build();});b.setDisabled(s.me().specialReady);i++;}
        if(s.cards.isEmpty())label("남은 특수카드가 없습니다.",490,225,620,50,"small",Align.center);
        if(chosen==Card.PREDICT){label("상대 숫자 예상",400,174,800,32,"small",Align.center);for(int n=1;n<=9;n++){int value=n;TextButton b=button(String.valueOf(n),475+(n-1)*73,122,62,44,prediction==n?"primary":"small",()->{prediction=value;build();});b.setDisabled(s.me().specialReady);}}
        if(chosen==Card.DISGUISE){
            label("상대에게 보여줄 홀짝 선택",400,174,800,32,"small",Align.center);
            for(int option=0;option<2;option++){boolean odd=option==0;TextButton b=button(odd?"홀수로 표시":"짝수로 표시",570+option*240,122,220,44,disguiseOdd!=null&&disguiseOdd==odd?"primary":"small",()->{disguiseOdd=odd;build();});b.setDisabled(s.me().specialReady);b.setName(odd?"disguise-odd":"disguise-even");}
        }
        TextButton use=button(s.me().specialReady?"확정 완료 · 상대 대기":"선택한 카드 사용",470,55,330,58,"primary",()->app.send(chosen==Card.DISGUISE?Command.disguise(Boolean.TRUE.equals(disguiseOdd)):Command.special(chosen,prediction)));
        use.setDisabled(s.me().specialReady||chosen==null||(chosen==Card.PREDICT&&prediction==0)||(chosen==Card.DISGUISE&&disguiseOdd==null));use.setName("special-use");
        TextButton skip=button("사용 안 함",830,55,290,58,"default",()->app.send(Command.special(null,0)));skip.setDisabled(s.me().specialReady);skip.setName("special-skip");
        if(chosen!=null)paragraph(description(chosen),1240,400,300,120);
    }
    private void result(){
        if(s.bettingEnabled)button("칩 계산 내역",45,115,275,44,"small",this::chipLedger);
        String outcome=s.roundWinner<0?"무승부":s.roundWinner==s.you?"승리":"패배";
        var title=gold(outcome,390,625,820,86,"hero");title.setColor(s.roundWinner==s.you?GOLD:s.roundWinner<0?CREAM:RED);
        tile(String.valueOf(s.ownTile),545,355,155,205,s.ownTile%2==0,false,null);
        tile(s.opponentParity,895,355,155,205,false,false,null);
        gold("VS",725,415,155,64,"large");label("내 숫자",530,305,185,36);label("상대 공개 정보",865,305,215,36);
        panel(430,166,740,120);scrollText(s.privateInfo.isEmpty()?"상대의 정확한 숫자는 공개되지 않습니다.":String.join("\n",s.privateInfo),455,180,690,88);
        TextButton next=button(s.me().acknowledged?"상대 확인 대기…":"룰렛으로 이동",580,70,440,65,"primary",()->app.send(Command.of(Action.NEXT)));next.setDisabled(s.me().acknowledged);next.setName("round-next");
    }
    public static String description(Card c){return switch(c){case REVERSE->"이번 라운드에는 낮은 숫자가 승리합니다. 양쪽 역전은 상쇄되며 1 대 9 예외는 유지합니다.";case SCOUT->"판정 후 상대 숫자가 1~4인지 5~9인지 본인에게만 알려줍니다.";case DISGUISE->"상대에게 보여줄 홀수·짝수를 직접 선택합니다. 실제와 같게 또는 다르게 표시할 수 있으며, 승패에는 영향이 없습니다.";case PREDICT->"정확한 상대 숫자를 맞히면 상대의 남은 타일 중 한 장을 본인만 확인합니다.";};}
    private void cardDialog(Card c){
        Dialog d=titledDialog(c.label,540);
        var art=new kr.ac.c2s.ui.CardArt(app,"cards/specials/"+c.name().toLowerCase(java.util.Locale.ROOT)+".png",0,0,220,330);
        d.getContentTable().add(art).size(220,330).center().padBottom(18).row();
        Label text=new Label(description(c),app.theme.skin,"body");text.setWrap(true);text.setAlignment(Align.left);
        d.getContentTable().add(text).width(540).left().padBottom(8);
        d.button("닫기");d.getButtonTable().getChildren().peek().setName("card-help-close");d.show(stage);
    }
}
