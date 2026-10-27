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
    private boolean fireDialogShown,preparationFrozen,loadingRequest;
    private java.util.List<Integer> occupied=new java.util.ArrayList<>();
    public RouletteScreen(C2SGame app,Snapshot state){super(app);cylinderArt=new kr.ac.c2s.ui.CylinderArt();accept(state);}
    public void accept(Snapshot state){
        boolean sameKey=s!=null&&s.round==state.round&&s.matchId.equals(state.matchId);
        boolean sameResult=sameKey&&s.phase==Phase.ROULETTE_RESULT&&state.phase==Phase.ROULETTE_RESULT;
        if(sameResult){s=state;refreshResultUI();return;}
        boolean samePreparation=sameKey&&(s.phase==Phase.AMMO_LOAD||s.phase==Phase.ROULETTE_SELECT)&&(state.phase==Phase.AMMO_LOAD||state.phase==Phase.ROULETTE_SELECT);
        if(!sameKey){selected=state.pendingAmmo>0?-1:0;occupied=new java.util.ArrayList<>(state.loadedChambers);loadingRequest=false;}
        if(sameKey&&state.hintHistory.size()>s.hintHistory.size())app.toast(state.hintHistory.get(state.hintHistory.size()-1));
        if((state.phase==Phase.AMMO_LOAD||state.phase==Phase.ROULETTE_SELECT)&&!samePreparation){preparation=new kr.ac.c2s.game.CylinderPreparation(state.me().danger-Math.max(0,state.dangerDelta),Math.max(0,state.dangerDelta));preparationFrozen=false;fireDialogShown=false;}
        if(state.phase==Phase.AMMO_LOAD){occupied=new java.util.ArrayList<>(state.loadedChambers);if(state.ammoChoice>=0)occupied.remove(Integer.valueOf(state.ammoChoice));}
        if(samePreparation&&s.pendingAmmo>0&&state.pendingAmmo==0&&state.ammoChoice>=0){
            preparation.loadAt(state.ammoChoice);loadingRequest=false;selected=state.ammoChoice;
        }
        s=state;duel=null;if(s.ownChamber>=0)selected=s.ownChamber;build();
    }
    private void build(){
        clear();mainAction=null;preparationStatus=null;
        gold("상대 보유 칩  "+s.opponent().chips,1260,827,305,34,"small").setName("opponent-chips");
        if(s.phase==Phase.ROULETTE_RESULT){result();return;}
        if(s.me().chamberReady){if(preparation.phase()!=kr.ac.c2s.game.CylinderPreparation.Phase.LOCKED)preparation.previewAt(kr.ac.c2s.game.CylinderPreparation.Phase.LOCKED,0,selected);waiting();return;}
        preparationScreen();
    }
    private void preparationScreen(){
        dim=.48f;
        heading("ROUND "+s.round+" / 9",null);
        String situation;
        if(s.roundWinner<0)situation="숫자 대결 무승부 · 탄 수 유지";
        else if(s.roundWinner==s.you)situation="숫자 대결 승리 · 추가 장전 없이 회전";
        else situation=s.dangerDelta>0?"숫자 대결 패배 → 빈 탄실에 실탄 1발 추가":"숫자 대결 패배 · 실탄 9발 유지";
        panel(325,735,950,64);
        Label event=gold(situation,335,747,930,48,"title");
        event.setColor(s.roundWinner==s.you?MINT:s.roundWinner<0?CREAM:RED);
        gold("현재 실탄 "+s.me().danger+"발  ·  빈 탄실 "+(9-s.me().danger)+"칸",930,697,620,34,"default");
        panel(35,300,265,385);gold("경기 기록",55,630,225,40,"default");scrollText(String.join("\n\n",s.history),55,320,225,303);
        panel(35,137,265,137);gold("나 · "+s.me().name,55,218,225,35,"default");heartLabel(s.me().life,55,168,225);
        cylinder=new kr.ac.c2s.ui.RevolverCylinder(app,cylinderArt,preparation,315,130,600,selected,s.me().danger,s.knownChambers,n->{selected=n;build();},occupied);content.addActor(cylinder);
        preparationStatus=gold("",315,94,600,30,"default");
        hintShop();
        mainAction=button("",400,32,430,54,"primary",this::prepareShot);mainAction.setName("chamber-confirm");
        image("revolver/gun.png",1245,28,260,108);
        label((preparation.canPlace()||preparation.phase()==kr.ac.c2s.game.CylinderPreparation.Phase.LOADING)?"금색 탄 뒷면 = 장전됨 · 검은 구멍 = 빈칸":"회전 뒤에는 실탄 위치가 숨겨집니다.",920,105,620,30,"small",Align.center);
        label("직접 장전 → 탄창 회전 → 힌트 확인 → 격발",315,4,600,24,"tiny",Align.center);
        syncPreparationLabels();
    }
    private void hintShop(){
        panel(930,365,620,320);gold("힌트 상점",950,635,580,39,"default");
        label("보유 칩 "+s.me().chips+" / 시작 12칩 · 경기 전체 예산",950,588,580,32,"small",Align.center);
        String[] names={"범위 탐색","탄실 검사"};
        String[] descriptions={"선택 구역 1~3 / 4~6 / 7~9의 실탄 개수 확인","선택한 탄실 한 칸이 실탄인지 빈칸인지 정확히 확인"};
        String[] ids={"RANGE","INSPECT"};
        for(int i=0;i<ids.length;i++){
            String id=ids[i];int cost=kr.ac.c2s.common.Protocol.hintCost(id);boolean bought=s.boughtHints.contains(id);
            TextButton b=button(names[i]+(bought?" · 구매 완료":s.me().chips<cost?" · "+cost+"칩 부족":" · "+cost+"칩")+"\n"+descriptions[i],950,482-i*100,580,86,"small",()->app.send(Command.hint(id,selected)));
            b.setDisabled(s.phase!=Phase.ROULETTE_SELECT||!preparation.canChoose()||selected<0||bought||s.me().chips<cost);b.setName("hint-"+id);
        }
        panel(930,145,620,200);gold("내가 구매한 힌트 · 경기 누적",950,296,580,35,"default");
        java.util.List<String> receipts=new java.util.ArrayList<>(s.hintHistory);java.util.Collections.reverse(receipts);
        String info=receipts.isEmpty()?(s.me().chips<4?"칩이 부족합니다. 이번 경기에서 구매한 힌트가 없습니다.":"선택한 탄실의 힌트를 구매하면 여기에 결과가 표시됩니다."):String.join("\n\n",receipts);
        scrollText(info,953,159,575,127);
    }
    private void syncPreparationLabels(){
        if(mainAction==null||preparationStatus==null)return;
        var phase=preparation.phase();
        switch(phase){
            case PLACING->{preparationStatus.setText("장전할 빈 탄실을 직접 선택하세요");mainAction.setText(loadingRequest?"장전 요청 중…":selected<0?"빈 탄실을 선택하세요":(selected+1)+"번 빈칸에 실탄 넣기");}
            case LOADING->{preparationStatus.setText("빈 탄실에 1발 장전 중 · 실탄 "+preparation.loaded()+" / 9");mainAction.setText("탄을 넣는 중…");}
            case SPINNING->{preparationStatus.setText("탄창 회전 중");mainAction.setText("탄창 회전 중…");}
            case READY->{preparationStatus.setText(selected<0?"탄실 1~9 중 하나를 선택하세요":"선택한 탄실  "+(selected+1)+"번");mainAction.setText(selected<0?"탄실을 선택하세요":(selected+1)+"번 탄실로 격발");}
            case ALIGNING->{preparationStatus.setText((selected+1)+"번 탄실을 격발 위치로 정렬 중");mainAction.setText("탄창 정렬 중…");}
            case LOCKED->{preparationStatus.setText("격발 준비 완료");mainAction.setText("격발 준비 완료");}
        }
        mainAction.setDisabled(loadingRequest||selected<0||!(preparation.canPlace()&&s.pendingAmmo>0&&!occupied.contains(selected)||preparation.canChoose()&&s.phase==Phase.ROULETTE_SELECT));
    }
    private void prepareShot(){
        if(selected<0)return;
        if(preparation.canPlace()&&s.pendingAmmo>0&&!occupied.contains(selected)&&!loadingRequest){loadingRequest=true;syncPreparationLabels();app.send(Command.number(Action.LOAD_AMMO,selected));return;}
        if(preparation.canChoose()&&s.phase==Phase.ROULETTE_SELECT)app.send(Command.number(Action.CHAMBER,selected));
    }
    private void waiting(){
        dim=.48f;heading("ROUND "+s.round+" / 9","격발 준비 완료");
        cylinder=new kr.ac.c2s.ui.RevolverCylinder(app,cylinderArt,preparation,600,300,400,selected,s.me().danger,s.knownChambers,n->{});content.addActor(cylinder);
        panel(440,188,720,90);gold("상대의 격발 준비를 기다리는 중…",465,207,670,50,"title");
        Label own=label("내 준비 완료",495,126,280,45);own.setColor(MINT);label("상대 준비 중",830,126,280,45);
        label("두 사람 모두 확정하면 총격 결과로 이어집니다.",340,69,920,34,"small",Align.center);
    }
    public boolean isAmmoPlacementReady(){return preparation!=null&&preparation.canPlace()&&s.pendingAmmo>0;}
    public boolean isPreparationReady(){return preparation!=null&&preparation.canChoose();}
    public boolean isFireDialogReady(){return preparation!=null&&preparation.phase()==kr.ac.c2s.game.CylinderPreparation.Phase.LOCKED&&fireDialogShown;}
    public void previewPreparationAt(kr.ac.c2s.game.CylinderPreparation.Phase phase,float time,int chosen){
        if(phase==kr.ac.c2s.game.CylinderPreparation.Phase.LOADING&&preparation.canPlace())preparation.loadAt(chosen);
        preparation.previewAt(phase,time,chosen);selected=chosen;preparationFrozen=true;fireDialogShown=true;build();
    }
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
        if((s.phase==Phase.AMMO_LOAD||s.phase==Phase.ROULETTE_SELECT)&&preparation!=null&&!preparationFrozen&&!s.me().chamberReady){
            var before=preparation.phase();int loaded=preparation.loaded();
            preparation.advance(Math.min(delta,.05f));
            if(preparation.loaded()>loaded)app.sound("cylinder-load-live");
            if(before!=preparation.phase()){
                if(preparation.phase()==kr.ac.c2s.game.CylinderPreparation.Phase.SPINNING)app.sound("spin");
                if(preparation.phase()==kr.ac.c2s.game.CylinderPreparation.Phase.LOCKED||preparation.phase()==kr.ac.c2s.game.CylinderPreparation.Phase.READY)app.sound("cylinder-lock");
                build();
            }
            // A single spin is followed by direct fire; no second alignment dialog.
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
