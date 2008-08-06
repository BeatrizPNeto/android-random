package org.devtcg.games.solitaire.model;

/**
 * Simple card interface for {@link Deck}.
 */
public class Card
{
	protected Suit mSuit;
	protected Rank mRank;
	
	public Card(Card card)
	{
		this(card.getSuit(), card.getRank());
	}

	public Card(Suit suit, Rank rank)
	{
		mSuit = suit;
		mRank = rank;
	}

    public Card(int suit, int rank)
    {
        switch (rank)
        {
        	case 1:  mRank = Rank.ACE; break;
        	case 2:  mRank = Rank.DEUCE; break;
        	case 3:  mRank = Rank.THREE; break;
        	case 4:  mRank = Rank.FOUR; break;
        	case 5:  mRank = Rank.FIVE; break;
        	case 6:  mRank = Rank.SIX; break;
        	case 7:  mRank = Rank.SEVEN; break;
        	case 8:  mRank = Rank.EIGHT; break;
        	case 9:  mRank = Rank.NINE; break;
        	case 10: mRank = Rank.TEN; break;
        	case 11: mRank = Rank.JACK; break;
        	case 12: mRank = Rank.QUEEN; break;
        	case 13: mRank = Rank.KING; break;
        	default: throw new IllegalArgumentException("rank must be between 1 and 13");
        }

        switch (suit)
        {
        	case 0: mSuit = Suit.DIAMONDS; break;
        	case 1: mSuit = Suit.CLUBS; break;
        	case 2: mSuit = Suit.HEARTS; break;
        	case 3: mSuit = Suit.SPADES; break;
        	default: throw new IllegalArgumentException("suit must be between 0 and 3");
        }
    }
    
    public Suit getSuit()
    {
    	return mSuit;
    }
    
    public Rank getRank()
    {
    	return mRank;
    }
    
    public int getRankOrdinal()
    {
    	return mRank.ordinal() + 1;
    }

    public String getSuitString()
    {
    	return suits[mSuit.ordinal()];
    }

    public String getRankString()
    {
    	return ranks[mRank.ordinal()];
    }

    public String getRankAbbr()
    {
    	int ord = getRankOrdinal();
    	
    	if (ord >= 2 && ord <= 10)
    		return String.valueOf(ord);
    	
    	switch (mRank)
    	{
    		case ACE:   return "A";
    		case JACK:  return "J";
    		case QUEEN: return "Q";
    		case KING:  return "K";
        	
        	/* Impossible. */
    		default:    return null;
    	}
    }
    
    @Override
    public String toString()
    {
    	return getRankString() + " of " + getSuitString();
    }
    
    @Override
    public boolean equals(Object o)
    {
    	Card oo = (Card)o;

    	if (mRank != oo.mRank)
    		return false;

    	if (mSuit != oo.mSuit)
    		return false;

    	return true;
    }
    
	public enum Suit {
		CLUBS, SPADES, HEARTS, DIAMONDS
	}

	protected static final String suits[] =
	  { "Clubs", "Spades", "Hearts", "Diamonds" };

	public enum Rank {
		ACE, DEUCE, THREE, FOUR, FIVE, SIX, SEVEN, EIGHT, NINE, TEN, JACK,
		QUEEN, KING
	}

	protected static final String ranks[] =
	  { "Ace", "2", "3", "4", "5", "6", "7", "8", "9", "10", "Jack",
	    "Queen", "King" };
}