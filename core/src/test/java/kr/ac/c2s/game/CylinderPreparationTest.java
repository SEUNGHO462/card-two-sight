package kr.ac.c2s.game;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CylinderPreparationTest {
    @Test void playerChoosesSocketBeforeAnyLoadingOrSpin(){
        CylinderPreparation p=new CylinderPreparation(1,1);assertTrue(p.canPlace());p.advance(50);assertTrue(p.canPlace());assertEquals(1,p.loaded());
        p.loadAt(7);assertEquals(7,p.loadTarget());assertEquals(CylinderPreparation.Phase.LOADING,p.phase());
        p.advance(1.3f);assertEquals(2,p.loaded());p.advance(.5f);assertEquals(CylinderPreparation.Phase.SPINNING,p.phase());p.advance(1.7f);assertTrue(p.canChoose());
    }
    @Test void winsSkipPlacementAndNineChambersAreValid(){
        CylinderPreparation p=new CylinderPreparation(3,0);assertEquals(CylinderPreparation.Phase.SPINNING,p.phase());p.advance(1.7f);assertTrue(p.canChoose());
        p.align(8);p.advance(1);assertEquals(320,p.rotation(),.001);
        CylinderPreparation full=new CylinderPreparation(9,1);assertEquals(0,full.added());assertFalse(full.canPlace());
        assertThrows(IllegalArgumentException.class,()->new CylinderPreparation(1,1).loadAt(9));
    }
}
