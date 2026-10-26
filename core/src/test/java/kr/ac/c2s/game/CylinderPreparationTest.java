package kr.ac.c2s.game;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CylinderPreparationTest {
    @Test void selectionIsBlockedUntilLoadingAndSpinFinish(){
        CylinderPreparation p=new CylinderPreparation();assertFalse(p.canChoose());
        assertThrows(IllegalArgumentException.class,()->p.align(2));
        p.advance(2.7f);assertEquals(8,p.loaded());assertFalse(p.canChoose());
        p.advance(.5f);assertEquals(CylinderPreparation.Phase.SPINNING,p.phase());assertEquals(9,p.loaded());
        p.advance(1.5f);assertTrue(p.canChoose());
    }
    @Test void alignmentTargetsTheChosenChamberAndCancelDoesNotReload(){
        CylinderPreparation p=new CylinderPreparation();p.advance(5);p.align(3);
        assertFalse(p.canChoose());assertThrows(IllegalArgumentException.class,p::chooseAgain);
        p.advance(1);assertEquals(CylinderPreparation.Phase.LOCKED,p.phase());assertEquals(120,p.rotation(),.001);
        p.chooseAgain();assertTrue(p.canChoose());assertEquals(9,p.loaded());assertEquals(0,p.rotation(),.001);
    }
}
