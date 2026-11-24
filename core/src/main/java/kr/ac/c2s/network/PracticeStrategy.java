package kr.ac.c2s.network;
import java.util.*;
import kr.ac.c2s.common.Protocol.*;
import kr.ac.c2s.common.GameEngine;

/** Uses the bot's own permitted snapshot only; never reads the rival's hidden choice. */
public final class PracticeStrategy {
    private PracticeStrategy(){}
    public static int tile(Snapshot s,PracticeDifficulty mode,Random random){
        List<Integer> hand=new ArrayList<>(s.hand);Collections.sort(hand);
        if(hand.isEmpty())throw new IllegalArgumentException("No remaining number tiles");
        if(mode==PracticeDifficulty.EASY)return hand.get(random.nextInt(Math.min(3,hand.size())));
        if(mode==PracticeDifficulty.NORMAL)return hand.get(random.nextInt(hand.size()));
        double[] opponents=opponentTileProbabilities(s);
        Player me=s.players!=null&&s.you>=0&&s.you<s.players.length?s.players[s.you]:null;
        double lifeRisk=me==null?0:(3-me.life)/2.0;
        double ammoRisk=me==null?0:(me.danger-1)/8.0;
        double risk=Math.max(0,Math.min(1,Math.max(lifeRisk,ammoRisk)));
        double future=Math.max(0,Math.min(hand.size()-1,9-s.round))/8.0;
        double reserve=1.4*future*(1-risk);
        double bestScore=Double.NEGATIVE_INFINITY;
        int best=hand.get(0),ties=0;
        for(int n:hand){
            // Match the AI's existing special-card policy without reading the rival's choice.
            boolean reverse=n<=4&&s.cards.contains(Card.REVERSE);
            double score=0;
            for(int rival=1;rival<=9;rival++){
                int result=GameEngine.compare(n,rival,reverse);
                score+=opponents[rival]*(result<0?0:result==0?1:-(1+2*risk));
            }
            double strength=(n-1)/8.0;
            score-=reserve*strength*strength;
            if(score>bestScore+1e-9){best=n;bestScore=score;ties=1;}
            else if(Math.abs(score-bestScore)<=1e-9&&random.nextInt(++ties)==0)best=n;
        }
        return best;
    }
    /** Marginal chance of each remaining rival tile, based solely on this viewer's evidence. */
    static double[] opponentTileProbabilities(Snapshot s){
        double[] fallback=new double[10];Arrays.fill(fallback,1,10,1.0/9);
        if(s.opponentObservations==null||s.opponentObservations.isEmpty())return fallback;
        int count=s.opponentObservations.size();
        if(count>=9)return fallback;
        // At most 512 masks. Distinct assignments avoid counting a previously used tile twice.
        double[] states=new double[1<<9];states[0]=1;
        int round=0;
        for(RoundObservation observation:s.opponentObservations){
            if(observation==null||observation.round!=++round||observation.possibleOpponentTiles==null)return fallback;
            int allowed=0;
            for(Integer tile:observation.possibleOpponentTiles)if(tile!=null&&tile>=1&&tile<=9)allowed|=1<<(tile-1);
            int known=observation.knownRemainingTile;
            if(known<0||known>9)return fallback;
            int unused=known==0?0:1<<(known-1);
            double[] next=new double[states.length];
            for(int mask=0;mask<states.length;mask++){
                if(states[mask]==0)continue;
                for(int options=allowed&~mask;options!=0;options&=options-1){
                    int nextMask=mask|Integer.lowestOneBit(options);
                    // The revealed card was still unused after THIS round, not necessarily forever.
                    if((nextMask&unused)==0)next[nextMask]+=states[mask];
                }
            }
            states=next;
        }
        double total=Arrays.stream(states).sum();
        if(total==0)return fallback;
        double[] probabilities=new double[10];
        for(int mask=0;mask<states.length;mask++)if(states[mask]>0){
            for(int tile=1;tile<=9;tile++)if((mask&(1<<(tile-1)))==0)
                probabilities[tile]+=states[mask]/total/(9-count);
        }
        return probabilities;
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
        for(int i=0;i<kr.ac.c2s.common.Protocol.CHAMBERS;i++){Boolean hit=s.knownChambers.get(i);if(Boolean.FALSE.equals(hit))safe.add(i);else if(hit==null)unknown.add(i);}
        List<Integer> options=!safe.isEmpty()?safe:unknown;
        return options.isEmpty()?random.nextInt(kr.ac.c2s.common.Protocol.CHAMBERS):options.get(random.nextInt(options.size()));
    }
}
