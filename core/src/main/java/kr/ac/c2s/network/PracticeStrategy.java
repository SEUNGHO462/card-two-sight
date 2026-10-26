package kr.ac.c2s.network;
import java.util.*;
import kr.ac.c2s.common.Protocol.*;

/** Uses the bot's own permitted snapshot only; never reads the rival's hidden choice. */
public final class PracticeStrategy {
    private PracticeStrategy(){}
    public static int tile(Snapshot s,PracticeDifficulty mode,Random random){
        List<Integer> hand=new ArrayList<>(s.hand);Collections.sort(hand);
        if(mode==PracticeDifficulty.EASY)return hand.get(random.nextInt(Math.min(3,hand.size())));
        if(mode==PracticeDifficulty.NORMAL)return hand.get(random.nextInt(hand.size()));
        // Score against all possible opposing numbers; the actual hidden hand is unavailable.
        int best=hand.get(0),bestScore=-100;
        for(int n:hand){int score=0;for(int rival=1;rival<=9;rival++)score+=n==rival?0:(n==1&&rival==9)||(n>rival&&!(n==9&&rival==1))?1:-1;
            if(score>bestScore||(score==bestScore&&random.nextBoolean())){best=n;bestScore=score;}}
        return best;
    }
    public static Card special(Snapshot s,PracticeDifficulty mode,Random random){
        if(mode==PracticeDifficulty.EASY||s.cards.isEmpty())return null;
        if(mode==PracticeDifficulty.NORMAL)return random.nextBoolean()||s.round>6?s.cards.get(random.nextInt(s.cards.size())):null;
        if(s.ownTile<=4&&s.cards.contains(Card.REVERSE))return Card.REVERSE;
        if(s.round<=7&&s.cards.contains(Card.SCOUT))return Card.SCOUT;
        for(Card card:s.cards)if(card!=Card.REVERSE||s.round==9)return card;
        return null;
    }
    public static int chamber(Snapshot s,Random random){
        List<Integer> safe=new ArrayList<>(),unknown=new ArrayList<>();
        for(int i=0;i<9;i++){Boolean hit=s.knownChambers.get(i);if(Boolean.FALSE.equals(hit))safe.add(i);else if(hit==null)unknown.add(i);}
        List<Integer> options=!safe.isEmpty()?safe:unknown;
        return options.isEmpty()?random.nextInt(9):options.get(random.nextInt(options.size()));
    }
}
