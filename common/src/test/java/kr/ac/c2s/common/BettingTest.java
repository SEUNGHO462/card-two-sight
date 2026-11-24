package kr.ac.c2s.common;
import org.junit.jupiter.api.Test;
import java.util.*;
import static kr.ac.c2s.common.Protocol.*;
import static org.junit.jupiter.api.Assertions.*;
class BettingTest {
    private GameEngine game(){return new GameEngine(new Player("A","male_punk"),new Player("B","male_veteran"),new Random(7),false,true);}
    private void resolve(GameEngine e,int a,int b){e.apply(0,Command.number(Action.TILE,a));e.apply(1,Command.number(Action.TILE,b));e.apply(0,Command.special(null,0));e.apply(1,Command.special(null,0));}
    @Test void unequalWagersReturnExcessAndWinnerReceivesOnlyMatchedPot(){
        for(boolean firstWins:new boolean[]{false,true}){
            GameEngine e=game();e.apply(0,Command.number(Action.BET,1));e.apply(1,Command.number(Action.BET,2));
            assertEquals(29,e.snapshot(0).me().chips);assertEquals(28,e.snapshot(1).me().chips);
            resolve(e,firstWins?8:2,firstWins?2:8);
            assertEquals(firstWins?31:29,e.snapshot(0).me().chips);
            assertEquals(firstWins?29:31,e.snapshot(1).me().chips);
            assertEquals(60,e.snapshot(0).me().chips+e.snapshot(1).me().chips);
        }
    }
    @Test void threeVersusTwoSettlesToThirtyTwoAndTwentyEight(){
        GameEngine e=game();e.apply(0,Command.number(Action.BET,3));assertEquals(27,e.snapshot(0).me().chips);
        e.apply(1,Command.number(Action.BET,2));assertEquals(28,e.snapshot(1).me().chips);resolve(e,8,2);
        assertEquals(32,e.snapshot(0).me().chips);assertEquals(28,e.snapshot(1).me().chips);
        assertTrue(e.snapshot(0).chipHistory.get(2).contains("27 + 반환 1 + 획득 4 = 32"));
    }
    @Test void drawRefundsBothWagersAndCommandsCannotSpendTwice(){
        GameEngine e=game();assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.number(Action.BET,0)));
        assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.number(Action.BET,4)));
        assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.number(Action.TILE,8)));
        e.apply(0,Command.number(Action.BET,3));assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.number(Action.BET,2)));
        e.apply(1,Command.number(Action.BET,1));resolve(e,5,5);
        assertEquals(30,e.snapshot(0).me().chips);assertEquals(30,e.snapshot(1).me().chips);
        assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.special(null,0)));
    }
    @Test void opponentWagerAndBalanceStayHiddenUntilBothConfirm(){
        GameEngine e=game();e.apply(1,Command.number(Action.BET,3));
        Snapshot s=JSON.fromJson(JSON.toJson(e.snapshot(0)),Snapshot.class);
        assertTrue(s.opponent().betReady);assertEquals(0,s.opponentBet);assertEquals(0,s.opponent().bet);assertEquals(30,s.opponent().chips);
        e.apply(0,Command.number(Action.BET,1));assertEquals(3,e.snapshot(0).opponentBet);assertEquals(1,e.snapshot(0).matchedBet);
    }
    @Test void hintsSpendTheSameBalanceAndEmptyWalletCanContinue(){
        GameEngine e=new GameEngine(new Player("A","male_punk"),new Player("B","male_veteran"),new Random(0){@Override public int nextInt(int n){return 0;}},false,true);
        for(int round=1;round<=5;round++){
            int bet=e.snapshot(0).me().chips==0?0:1;
            e.apply(0,Command.number(Action.BET,bet));e.apply(1,Command.number(Action.BET,1));resolve(e,round,round);
            e.apply(0,Command.of(Action.NEXT));e.apply(1,Command.of(Action.NEXT));
            if(round<=3){e.apply(0,Command.hint("INSPECT",0));assertEquals(30-8*round,e.snapshot(0).me().chips);}
            if(round==4){e.apply(0,Command.hint("RANGE",0));assertEquals(2,e.snapshot(0).me().chips);assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.hint("INSPECT",0)));}
            e.apply(0,Command.number(Action.CHAMBER,0));e.apply(1,Command.number(Action.CHAMBER,0));
            e.apply(0,Command.of(Action.NEXT));e.apply(1,Command.of(Action.NEXT));
        }
        assertEquals(2,e.snapshot(0).me().chips);
        // Exercise an empty wallet through the same command validation without mutating the live snapshot.
        GameEngine zero=game();java.lang.reflect.Field field;
        try{field=GameEngine.class.getDeclaredField("players");field.setAccessible(true);((Player[])field.get(zero))[0].chips=0;}catch(ReflectiveOperationException ex){throw new AssertionError(ex);}
        assertThrows(IllegalArgumentException.class,()->zero.apply(0,Command.number(Action.BET,1)));
        zero.apply(0,Command.number(Action.BET,0));zero.apply(1,Command.number(Action.BET,3));resolve(zero,8,2);
        assertEquals(0,zero.snapshot(0).me().chips);assertEquals(30,zero.snapshot(1).me().chips);
    }
    @Test void disconnectRefundsPendingBetsOrAwardsCommittedPotOnce(){
        GameEngine pending=game();pending.apply(0,Command.number(Action.BET,2));pending.forfeit(1);
        assertEquals(30,pending.snapshot(0).me().chips);pending.forfeit(1);assertEquals(30,pending.snapshot(0).me().chips);
        GameEngine committed=game();committed.apply(0,Command.number(Action.BET,1));committed.apply(1,Command.number(Action.BET,3));committed.forfeit(1);
        assertEquals(31,committed.snapshot(0).me().chips);assertEquals(60,committed.snapshot(0).me().chips+committed.snapshot(1).me().chips);
    }
}
