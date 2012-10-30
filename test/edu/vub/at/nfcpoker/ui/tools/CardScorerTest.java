package edu.vub.at.nfcpoker.ui.tools;

import java.util.ArrayList;

import junit.framework.TestCase;
import edu.vub.at.nfcpoker.Card;

public class CardScorerTest extends TestCase {

	public void testEvaluateHandPairNoBase() {
		
		// Pair of Ace
		Card c1 = new Card((short)0,(short)0);
		Card c2 = new Card((short)1,(short)0);
		
		ArrayList<Card> holeCardsAce = new ArrayList<Card>(); 
		holeCardsAce.add(c1);
		holeCardsAce.add(c2);
		
		// For 2 player game
		assertEquals(85, CardScoreUtility.evaluateHand(null, holeCardsAce, 2));
		assertEquals(31, CardScoreUtility.evaluateHand(null, holeCardsAce, 10));
		
		// Pair of Twos
		Card c3 = new Card((short)0,(short)1);
		Card c4 = new Card((short)1,(short)1);
				
		ArrayList<Card> holeCardsTwos = new ArrayList<Card>(); 
		holeCardsTwos.add(c3);
		holeCardsTwos.add(c4);
		
		// For 2 player game
		assertEquals(50, CardScoreUtility.evaluateHand(null, holeCardsTwos, 2));
		assertEquals(12, CardScoreUtility.evaluateHand(null, holeCardsTwos, 10));
		
	}
	
	
	
	
	
	
	public void testEvaluateHand() {
		fail("Not yet implemented");
	}

}
