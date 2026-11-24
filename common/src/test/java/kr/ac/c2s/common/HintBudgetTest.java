package kr.ac.c2s.common;
import java.util.Random;
import org.junit.jupiter.api.Test;
import static kr.ac.c2s.common.Protocol.*;
import static org.junit.jupiter.api.Assertions.*;
class HintBudgetTest {
 private GameEngine game(){return new GameEngine(new Player("A","male_punk"),new Player("B","female_vest"),new Random(0){@Override public int nextInt(int b){return 0;}});}
 private void roulette(GameEngine e,int n){e.apply(0,Command.number(Action.TILE,n));e.apply(1,Command.number(Action.TILE,n));e.apply(0,Command.special(null,0));e.apply(1,Command.special(null,0));e.apply(0,Command.of(Action.NEXT));e.apply(1,Command.of(Action.NEXT));}
 @Test void eitherHintBlocksBothPurchasesUntilNextRound(){for(String first:new String[]{"RANGE","INSPECT"}){
  GameEngine e=game();roulette(e,1);e.apply(0,Command.hint(first,0));int balance=12-hintCost(first);
  for(String id:new String[]{"RANGE","INSPECT"})assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.hint(id,0)));
  assertEquals(balance,e.snapshot(0).me().chips);assertEquals(1,e.snapshot(0).hintHistory.size());assertTrue(e.snapshot(1).hintHistory.isEmpty());
  e.apply(0,Command.number(Action.CHAMBER,8));e.apply(1,Command.number(Action.CHAMBER,8));e.apply(0,Command.of(Action.NEXT));e.apply(1,Command.of(Action.NEXT));roulette(e,2);
  String second=first.equals("RANGE")?"INSPECT":"RANGE";e.apply(0,Command.hint(second,0));assertEquals(0,e.snapshot(0).me().chips);assertEquals(2,e.snapshot(0).hintHistory.size());
 }}
 @Test void invalidHintDoesNotConsumePurchase(){GameEngine e=game();roulette(e,1);assertThrows(IllegalArgumentException.class,()->e.apply(0,Command.hint("INVALID",0)));e.apply(0,Command.hint("RANGE",0));assertEquals(8,e.snapshot(0).me().chips);}
}
