package edu.vub.at.nfcpoker.ui.tools;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import edu.vub.at.nfcpoker.Card;
import edu.vub.at.nfcpoker.Hand;

public class CardScoreUtility {
	
	
	public static int evaluateHand(Set<Card> base, Collection<Card> holeCards, int numberOfPlayers){
		
		if(null==base||base.size()==0){
			
			// Just the two cards
			Iterator<Card> it = holeCards.iterator();
			
			Card card1 = it.next();
			Card card2 = it.next();
			
			int topProb = 0;
			int bottomProb = 0;
			
			// if there are more than 10 players, approximate to 10 players
			if(numberOfPlayers>10){
				numberOfPlayers=10;
			};
			
			Card selectedCard;
			
			if(card1.getRank()==card2.getRank()){
				
				// Pairs
				// Odds from this table: http://www.westonpoker.com/pokerInfo/preFlopOdds.php
				int[] topPair = {85,73,64,56,49,44,39,35,31};
				int[] bottomPair = {50,31,22,18,16,14,13,13,12};
				
				topProb = topPair[numberOfPlayers-2];
				bottomProb = bottomPair[numberOfPlayers-2];
				
				selectedCard = card1;
				
			} else {
				
				// Non-pairs
				int[] topNonPair = {67,51,41,35,31,28,25,23,21};
				int[] bottomNonPair = {31,20,14,11,9,8,7,6,6};
				
				topProb = topNonPair[numberOfPlayers-2];
				bottomProb = bottomNonPair[numberOfPlayers-2];
				
				// Approximate using the higher card in the two cards
				if(getRealRank(card1)>getRealRank(card2)){
					selectedCard=card1;
				} else {
					selectedCard=card2;
				}
				
			}
			
			// return linear approximation of probability 
			return bottomProb + ((topProb-bottomProb)/12*(getRealRank(selectedCard)-1));
			
		} else {
			
			Hand h = Hand.makeBestHand(base, holeCards);
			
			int[] prob = {10,50,90,95,97,98,99,100};
			
			if(h.getValue()>prob.length){
				return prob[prob.length-1];
			} else {
				return prob[h.getValue()-1];
			}
		}
	}
	
	private static int getRealRank(Card c){
		
		if(c.getRank()==0){
			// It's an ace
			return 13;
		} else {
			return c.getRank();
		}
		
	}
	
	

}
