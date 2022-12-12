package bguspl.set.ex;

import bguspl.set.Env;
import bguspl.set.Util;

import java.io.UTFDataFormatException;
import java.security.spec.EncodedKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.ObjDoubleConsumer;
import java.util.stream.Collectors;


import javax.swing.UIDefaults.ProxyLazyValue;

/**
 * This class contains the data that is visible to the player.
 *
 * @inv slotToCard[x] == y iff cardToSlot[y] == x
 */
public class Table {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Mapping between a slot and the card placed in it (null if none).
     */
    protected final Integer[] slotToCard; // card per slot (if any)

    /**
     * Mapping between a card and the slot it is in (null if none).
     */
    protected final Integer[] cardToSlot; // slot per card (if any)

    private Queue<Integer> [] pQueues; //array of queues --> tokens for each player

    private Queue<Integer>pIdqQueue; // Queue of players who reached 3 tokens

    public Object lock ; 
    
    /**
     * Constructor for testing.
     *
     * @param env        - the game environment objects.
     * @param slotToCard - mapping between a slot and the card placed in it (null if none).
     * @param cardToSlot - mapping between a card and the slot it is in (null if none).
     */
    public Table(Env env, Integer[] slotToCard, Integer[] cardToSlot) {

        this.env = env;
        this.slotToCard = slotToCard;
        this.cardToSlot = cardToSlot;
        pQueues = new Queue[env.config.players];
        for (int i = 0; i < env.config.players; i++) {
            pQueues[i] = new ConcurrentLinkedQueue<Integer>();
        }
        pIdqQueue = new ConcurrentLinkedQueue<Integer>();
        lock = new Object();   
    }

    /**
     * Constructor for actual usage.
     *
     * @param env - the game environment objects.
     */
    public Table(Env env) {

        this(env, new Integer[env.config.tableSize], new Integer[env.config.deckSize]);
    }

    /**
     * This method prints all possible legal sets of cards that are currently on the table.
     */
    public void hints() {
        List<Integer> deck = Arrays.stream(slotToCard).filter(Objects::nonNull).collect(Collectors.toList());
        env.util.findSets(deck, Integer.MAX_VALUE).forEach(set -> {
            StringBuilder sb = new StringBuilder().append("Hint: Set found: ");
            List<Integer> slots = Arrays.stream(set).mapToObj(card -> cardToSlot[card]).sorted().collect(Collectors.toList());
            int[][] features = env.util.cardsToFeatures(set);
            System.out.println(sb.append("slots: ").append(slots).append(" features: ").append(Arrays.deepToString(features)));
        });
    }

    /**
     * Count the number of cards currently on the table.
     *
     * @return - the number of cards on the table.
     */
    public int countCards() {
        int cards = 0;
        for (Integer card : slotToCard)
            if (card != null)
                ++cards;
        return cards;
    }

    public boolean isCard(int slot)
    {
        return slotToCard[slot] != env.config.emptySlot;
    }

    public boolean empty_Table()
    {
        for (int i = 0; i < slotToCard.length; i++) {
            if(slotToCard[i] != env.config.emptySlot)
                return false;
        }
        return true;
    }

    /**
     * Places a card on the table in a grid slot.
     * @param card - the card id to place in the slot.
     * @param slot - the slot in which the card should be placed.
     *
     * @post - the card placed is on the table, in the assigned slot.
     */
    public void placeCard(int card, int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}

        cardToSlot[card] = slot;
        slotToCard[slot] = card;

        // TODO implement
        env.ui.placeCard(card, slot);
    }

    /**
     * Removes a card from a grid slot on the table.
     * @param slot - the slot from which to remove the card.
     */
    public void removeCard(int slot) {
        try {
            Thread.sleep(env.config.tableDelayMillis);
        } catch (InterruptedException ignored) {}


        env.ui.removeCard(slot);
        // TODO implement
        for (int i = 0; i < pQueues.length; i++) {
            if(pQueues[i].contains(slotToCard[slot]))
            {
                pQueues[i].remove(slotToCard[slot]);
                // pIdqQueue.remove(i);
                env.ui.removeToken(i, slot);
            }
        }
        slotToCard[slot] = env.config.emptySlot;
    }

    private boolean cardOnTable(int card)
    {
        for (int i = 0; i < slotToCard.length; i++) {
            if(slotToCard[i] == card)
                return true;
        }
        return false;
    }

    /**
     * Places a player token on a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot on which to place the token.
     */
    public void placeToken(int player, int slot) {
        if(pQueues[player].size()<env.config.tokenToSet)
        {
            pQueues[player].add(slotToCard[slot]);
            env.ui.placeToken(player, slot);

            if(pQueues[player].size()==3)
                pIdqQueue.add(player);
        }
    }

    /**
     * Removes a token of a player from a grid slot.
     * @param player - the player the token belongs to.
     * @param slot   - the slot from which to remove the token.
     * @return       - true iff a token was successfully removed.
     */
    public boolean removeToken(int player, int slot) {
        pQueues[player].remove(slotToCard[slot]);
        env.ui.removeToken(player, slot);
        return false;
    }

    public int[] checkSet()
    {
        int [] out = new int[env.config.tupleSize];
        if(!pIdqQueue.isEmpty())
        {
            // boolean tooLate = false;
            int playerId = pIdqQueue.poll();
            if(pQueues[playerId].isEmpty())
            {
                out[env.config.firstTupleElm] =env.config.notSetToCheck;
                out[env.config.secondTupleElm] =playerId;
                return out;
            }

            int [] playersCards = new int [env.config.tokenToSet]; 

            
            for (int i = 0; i < playersCards.length; i++) {
                playersCards[i]=pQueues[playerId].poll();
                pQueues[playerId].add(playersCards[i]);
            }

            
            
            if(env.util.testSet(playersCards))
            {
                ////// good

                ///remove tokens
                // for (int i = 0; i < env.config.tokenToSet; i++) {
                //     removeToken(playerId, cardToSlot[pQueues[playerId].poll()]);
                // }
                /// remove cards
                for (int i = 0; i < playersCards.length; i++) {
                    removeCard(cardToSlot[playersCards[i]]);
                }
                pQueues[playerId].clear();
                ///score player
                out[env.config.firstTupleElm] = env.config.goodSet;
                out[env.config.secondTupleElm] = playerId;
                return out;
                
            }
            else
            {
                out[env.config.firstTupleElm] = env.config.badSet;
                out[env.config.secondTupleElm] = playerId;
                return out; 
            }
            
            
        }
        out[env.config.firstTupleElm] =env.config.notSetToCheck;
        out[env.config.secondTupleElm] =env.config.notSetToCheck;
        return out;
    }

    public void place_3_cards(int [] cards)
    {
        int counter =0 ;
        for (int i = 0; i < slotToCard.length & counter<env.config.tokenToSet; i++) {
            if(slotToCard[i]==env.config.emptySlot)
            {
                //cardToSlot[i] = cards[counter];
                // slotToCard[i] = cards[counter];
                placeCard(cards[counter],i);
                counter++;
            }
        }
    }

    public List<Integer> removeCards ()
    {
        env.ui.removeTokens();

        List<Integer> out = new ArrayList<>();
        for (int i = 0; i < slotToCard.length; i++) {
            if(slotToCard[i]!=env.config.emptySlot)
                {
                    out.add(slotToCard[i]);
                    removeCard(i);
                    env.ui.removeCard(i);
                }
        }
        for (int i = 0; i < pQueues.length; i++) {
            pQueues[i].clear();
        }
        pIdqQueue.clear();
        return out;


    }
}
