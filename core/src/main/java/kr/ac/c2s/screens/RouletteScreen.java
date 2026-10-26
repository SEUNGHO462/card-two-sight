package kr.ac.c2s.screens;
import kr.ac.c2s.common.Protocol.Action;
import kr.ac.c2s.C2SGame;
import kr.ac.c2s.common.Protocol.*;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.utils.Align;
import static kr.ac.c2s.ui.Theme.*;

public final class RouletteScreen extends BaseScreen {
    private Snapshot s;private int selected=-1;
    private kr.ac.c2s.ui.RouletteDuel duel;
    private TextButton resultNext;
    private Label resultPhase;
    private Label[] resultCaptions,resultLives;
    private boolean lifePresented;
    private final kr.ac.c2s.ui.CylinderArt cylinderArt;
    private kr.ac.c2s.ui.RevolverCylinder cylinder;
    private kr.ac.c2s.game.CylinderPreparation preparation;
    private Label preparationStatus;
    private TextButton mainAction;
    private boolean fireDialogShown,preparationFrozen;
    public RouletteScreen(C2SGame app,Snapshot state){super(app);cylinderArt=new kr.ac.c2s.ui.CylinderArt();accept(state);}
    public void accept(Snapshot state){
        boolean sameKey=s!=null&&s.round==state.round&&s.matchId.equals(state.matchId);
        boolean sameResult=sameKey&&s.phase==Phase.ROULETTE_RESULT&&state.phase==Phase.ROULETTE_RESULT;
        if(sameResult){s=state;refreshResultUI();return;}
        boolean samePreparation=sameKey&&s.phase==Phase.ROULETTE_SELECT&&state.phase==Phase.ROULETTE_SELECT;
        if(!sameKey)selected=-1;
        if(state.phase==Phase.ROULETTE_SELECT&&!samePreparation){preparation=new kr.ac.c2s.game.CylinderPreparation();preparationFrozen=false;fireDialogShown=false;}
        s=state;duel=null;if(s.ownChamber>=0)selected=s.ownChamber;build();
    }
    private void build(){
        clear();mainAction=null;preparationStatus=null;
        if(s.phase==Phase.ROULETTE_RESULT){result();return;}
        if(s.me().chamberReady){if(preparation.phase()!=kr.ac.c2s.game.CylinderPreparation.Phase.LOCKED)preparation.previewAt(kr.ac.c2s.game.CylinderPreparation.Phase.LOCKED,0,selected);waiting();return;}
        if(preparation.phase()==kr.ac.c2s.game.CylinderPreparation.Phase.SPINNING)spinningScreen();else preparationScreen();
    }
    private void preparationScreen(){
        dim=.48f;
        heading("ROUND "+s.round+" / 9",null);
        String situation;
        if(s.roundWinner<0)situation="숫자 대결 무승부 · 탄 수 유지";
        else if(s.roundWinner==s.you)situation=s.dangerDelta<0?"숫자 대결 승리 → 실탄 "+(-s.dangerDelta)+"발 감소":"숫자 대결 승리 · 실탄 0발 유지";
        else situation="숫자 대결 패배 → 실탄 "+s.dangerDelta+"발 추가";
        panel(325,735,950,64);
        Label event=gold(situation,335,747,930,48,"title");
        event.setColor(s.roundWinner==s.you?MINT:s.roundWinner<0?CREAM:RED);
        gold("현재 실탄 "+s.me().danger+"발  ·  공탄 "+(9-s.me().danger)+"발",350,700,900,42,"default");
        panel(35,300,265,385);gold("경기 기록",55,630,225,40,"default");scrollText(String.join("\n\n",s.history),55,320,225,303);
        panel(35,137,265,137);gold("나 · "+s.me().name,55,218,225,35,"default");heartLabel(s.me().life,55,168,225);
        cylinder=new kr.ac.c2s.ui.RevolverCylinder(app,cylinderArt,preparation,340,180,520,selected,s.me().danger,s.knownChambers,n->{selected=n;build();});content.addActor(cylinder);
        preparationStatus=gold("",340,130,520,38,"default");
        boolean ready=preparation.canChoose()||preparation.phase()==kr.ac.c2s.game.CylinderPreparation.Phase.ALIGNING||preparation.phase()==kr.ac.c2s.game.CylinderPreparation.Phase.LOCKED;
        if(ready)hintShop();else loadingLegend();
        mainAction=button("",385,55,430,66,"primary",this::prepareShot);mainAction.setName("chamber-confirm");
        image("revolver/gun.png",1245,28,260,108);
        label("실탄 위치는 공개되지 않습니다.",920,105,620,30,"small",Align.center);
        label("1 장전  →  2 회전  →  3 탄실 선택  →  4 격발 준비",350,15,1170,29,"tiny",Align.center);
        syncPreparationLabels();
    }
    private void spinningScreen(){
        dim=.65f;heading("탄창 회전 중","ROUND "+s.round+" / 9  ·  실탄 "+s.me().danger+"발  ·  공탄 "+(9-s.me().danger)+"발");
        cylinder=new kr.ac.c2s.ui.RevolverCylinder(app,cylinderArt,preparation,540,190,520,-1,s.me().danger,s.knownChambers,n->{});content.addActor(cylinder);
        gold("회전이 끝나면 탄실을 선택하세요",390,125,820,48,"body");
        label("실탄 위치는 공개되지 않습니다.",400,72,800,35,"small",Align.center);
    }
    private void loadingLegend(){
        panel(930,295,620,390);gold("탄창 장전",960,620,560,42,"title");
        Image live=new Image(cylinderArt.live);live.setBounds(1050,445,60,146);content.addActor(live);
        Image blank=new Image(cylinderArt.blank);blank.setBounds(1350,445,60,146);content.addActor(blank);
        gold("실탄",980,397,200,38,"default");label("구리색 탄두",980,360,200,32,"small",Align.center);
        gold("공탄",1280,397,200,38,"default");label("회색으로 막힌 탄피",1250,360,260,32,"small",Align.center);
        paragraph("검은 구멍은 아직 비어 있는 탄실입니다.\n탄을 채운 뒤 회전이 끝나면 탄실을 고를 수 있습니다.",955,307,570,52);
        panel(930,165,620,110);gold("장전 중에는 탄실을 선택할 수 없습니다.",950,198,580,42,"default");
    }
    private void hintShop(){
        panel(930,310,620,375);gold("힌트 상점",950,628,580,39,"default");
        label("보유 칩 "+s.me().chips+"  ·  힌트당 2칩",950,588,580,32,"small",Align.center);
        String[] names={"범위 탐색","구역 비교","연결 탐색","탄실 검사"};
        String[] descriptions={"선택 구역의 실탄 수","이웃 구역과 실탄 수 비교","선택·다음 탄실의 실탄 수","선택 탄실 한 칸 확인"};
        String[] ids={"RANGE","COMPARE","ADJACENT","INSPECT"};
        for(int i=0;i<4;i++){
            String id=ids[i];boolean bought=s.boughtHints.contains(id);
            TextButton b=button(names[i]+(bought?" · 구매 완료":" · 2칩")+"\n"+descriptions[i],950+(i%2)*295,474-(i/2)*115,275,99,"small",()->app.send(Command.hint(id,selected)));
            b.setDisabled(!preparation.canChoose()||selected<0||bought||s.me().chips<2);b.setName("hint-"+id);
        }
        panel(930,165,620,125);gold("내가 확인한 정보",950,244,580,35,"default");
        scrollText(s.privateInfo.isEmpty()?"탄실을 선택한 뒤 필요한 힌트를 구매하세요.":String.join("\n",s.privateInfo),953,176,575,63);
    }
    private void syncPreparationLabels(){
        if(mainAction==null||preparationStatus==null)return;
        var phase=preparation.phase();
        switch(phase){
            case LOADING->{preparationStatus.setText("탄실 장전 중  "+preparation.loaded()+" / 9");mainAction.setText("탄을 넣는 중…");}
            case SPINNING->{preparationStatus.setText("탄창 회전 중");mainAction.setText("회전이 끝나면 선택하세요");}
            case READY->{preparationStatus.setText(selected<0?"탄실 1~9 중 하나를 선택하세요":"선택한 탄실  "+(selected+1)+"번");mainAction.setText(selected<0?"탄실을 선택하세요":(selected+1)+"번 탄실로 준비");}
            case ALIGNING->{preparationStatus.setText((selected+1)+"번 탄실을 격발 위치로 정렬 중");mainAction.setText("탄창 정렬 중…");}
            case LOCKED->{preparationStatus.setText("격발 준비 완료");mainAction.setText("격발 준비 완료");}
        }
        mainAction.setDisabled(!preparation.canChoose()||selected<0);
    }
    private void prepareShot(){
        if(!preparation.canChoose()||selected<0)return;
        preparation.align(selected);fireDialogShown=false;app.sound("spin");build();
    }
    private void showFireDialog(){
        fireDialogShown=true;final int chamber=selected;
        Dialog d=new Dialog("",app.theme.skin){@Override protected void result(Object value){
            if(Boolean.TRUE.equals(value))app.send(Command.number(Action.CHAMBER,chamber));
            else{preparation.chooseAgain();fireDialogShown=false;build();}
        }};
        d.getTitleTable().setVisible(false);d.setMovable(false);d.getContentTable().pad(25);
        Label title=new Label("격발 준비 완료",app.theme.skin,"title");title.setColor(GOLD);d.getContentTable().add(title).padBottom(15).row();
        Image gun=new Image(app.assets.texture("revolver/gun.png"));gun.setScaling(com.badlogic.gdx.utils.Scaling.fit);d.getContentTable().add(gun).size(400,145).pad(15).row();
        Label text=new Label((chamber+1)+"번 탄실이 격발 위치에 정렬됐습니다.\n두 사람이 확정하면 결과를 함께 확인합니다.",app.theme.skin);text.setAlignment(Align.center);
        d.getContentTable().add(text).width(690).pad(20);
        d.getButtonTable().defaults().minWidth(220).minHeight(62).pad(10);d.button("다시 선택",false);d.button("격발 확정",true);
        d.getButtonTable().getChildren().peek().setName("fire-confirm");d.getButtonTable().pad(10,15,20,15);d.show(stage);
    }
    private void waiting(){
        dim=.48f;heading("ROUND "+s.round+" / 9","격발 준비 완료");
        cylinder=new kr.ac.c2s.ui.RevolverCylinder(app,cylinderArt,preparation,600,300,400,selected,s.me().danger,s.knownChambers,n->{});content.addActor(cylinder);
        panel(440,188,720,90);gold("상대의 격발 준비를 기다리는 중…",465,207,670,50,"title");
        Label own=label("내 준비 완료",495,126,280,45);own.setColor(MINT);label("상대 준비 중",830,126,280,45);
        label("두 사람 모두 확정하면 총격 결과로 이어집니다.",340,69,920,34,"small",Align.center);
    }
    public boolean isPreparationReady(){return preparation!=null&&preparation.canChoose();}
    public boolean isFireDialogReady(){return preparation!=null&&preparation.phase()==kr.ac.c2s.game.CylinderPreparation.Phase.LOCKED&&fireDialogShown;}
    public void previewPreparationAt(kr.ac.c2s.game.CylinderPreparation.Phase phase,float time,int chosen){
        preparation.previewAt(phase,time,chosen);selected=chosen;preparationFrozen=true;fireDialogShown=true;build();
    }
    public void previewFireDialog(){showFireDialog();}
    private void result(){
        dim=.32f;
        heading("ROUND "+s.round+" / 9",null);
        duel=new kr.ac.c2s.ui.RouletteDuel(app,s);content.addActor(duel);
        resultPhase=label("",400,765,800,35,"small",Align.center);
        resultCaptions=new Label[2];resultLives=new Label[2];lifePresented=false;
        for(int side=0;side<2;side++){
            int p=side==0?s.you:1-s.you;float x=side==0?80:900;
            resultCaptions[side]=gold("",x,724,600,44,"title");
            panel(x+80,110,450,64);gold(s.players[p].name,x+95,121,190,40,"default");
            resultLives[side]=heartLabel(s.players[p].life,x+285,120,220);
        }
        resultNext=button("결과 확인 중…",570,42,460,58,"primary",()->app.send(Command.of(Action.NEXT)));
        resultNext.setName("roulette-next");refreshResultUI();
    }
    private void refreshResultUI(){
        if(duel==null)return;
        float t=duel.timeline.time();
        resultPhase.setText(t<kr.ac.c2s.game.ShotTimeline.FIRE?"방아쇠를 당깁니다…":duel.timeline.finished()?"룰렛 결과":"숨을 고릅니다…");
        for(int side=0;side<2;side++){
            int p=side==0?s.you:1-s.you;
            resultLives[side].setText(hearts(duel.timeline.visibleLife(s.players[p].life,s.hit[p])));
            boolean reveal=t>=kr.ac.c2s.game.ShotTimeline.LIFE;
            resultCaptions[side].setText(reveal?(s.hit[p]?"피격 · 목숨 -1":"생존"):"");
            resultCaptions[side].setColor(s.hit[p]?RED:MINT);
            if(reveal&&!lifePresented&&s.hit[p])resultLives[side].addAction(Actions.sequence(Actions.color(Color.WHITE,.08f),Actions.color(RED,.3f)));
        }
        if(t>=kr.ac.c2s.game.ShotTimeline.LIFE)lifePresented=true;
        resultNext.setDisabled(!duel.timeline.finished()||s.me().acknowledged);
        resultNext.setText(!duel.timeline.finished()?"결과 확인 중…":s.me().acknowledged?"상대 확인 대기…":"다음 라운드 →");
    }
    public boolean isResultReady(){return duel!=null&&duel.timeline.finished();}
    public float resultTime(){return duel==null?0:duel.timeline.time();}
    public void previewResultAt(float time){if(duel!=null){duel.previewAt(time);refreshResultUI();}}
    public kr.ac.c2s.game.CylinderPreparation.Phase preparationPhase(){return preparation==null?null:preparation.phase();}
    @Override public void dispose(){super.dispose();cylinderArt.dispose();}
    @Override public void render(float delta){
        if(s.phase==Phase.ROULETTE_SELECT&&preparation!=null&&!preparationFrozen&&!s.me().chamberReady){
            var before=preparation.phase();int loaded=preparation.loaded();
            preparation.advance(Math.min(delta,.05f));
            if(preparation.loaded()>loaded)app.sound(loaded<s.me().danger?"cylinder-load-live":"cylinder-load-blank");
            if(before!=preparation.phase()){
                if(preparation.phase()==kr.ac.c2s.game.CylinderPreparation.Phase.SPINNING)app.sound("spin");
                if(preparation.phase()==kr.ac.c2s.game.CylinderPreparation.Phase.LOCKED||preparation.phase()==kr.ac.c2s.game.CylinderPreparation.Phase.READY)app.sound("cylinder-lock");
                build();
            }
            if(preparation.phase()==kr.ac.c2s.game.CylinderPreparation.Phase.LOCKED&&!fireDialogShown)showFireDialog();
            syncPreparationLabels();
        }
        if(duel!=null){
            float strength=duel.hasHit()?duel.timeline.shake():0;
            float t=duel.timeline.time();
            stage.getCamera().position.set(W/2+(float)Math.sin(t*97)*strength,H/2+(float)Math.cos(t*73)*strength*.55f,0);
            stage.getCamera().update();refreshResultUI();
        }
        super.render(delta);
    }

}
