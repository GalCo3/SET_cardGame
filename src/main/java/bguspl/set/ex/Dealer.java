package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.security.interfaces.ECKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;


/**
 * This class manages the dealer's threads and data
 */
public class Dealer implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;
    private final Player[] players;
    
    /**
     * The list of card ids that are left in the dealer's deck.
     */
    private final List<Integer> deck;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    private int count;
    private boolean wasSet;

    /**
     * The time when the dealer needs to reshuffle the deck due to turn timeout.
     */
    private long reshuffleTime = Long.MAX_VALUE;

    

    public Dealer(Env env, Table table, Player[] players) {
        this.env = env;
        this.table = table;
        this.players = players;
        deck = IntStream.range(0, env.config.deckSize).boxed().collect(Collectors.toList());
        count=0;
        wasSet=false;
        // reshuffleTime = env.config.turnTimeoutMillis;
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");

        Thread [] p  =  new Thread[env.config.players];
        
        for (int i = 0; i < p.length; i++) {
            p[i] = new Thread(players[i]);
            p[i].start();
        }

        while (!shouldFinish()) {
            count = 0;
            placeCardsOnTable();
            timerLoop();
            updateTimerDisplay(true);
            removeCardsFromTable();
        }
        announceWinners();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
            // removeCardsFromTable();
            // placeCardsOnEmptySlots();
        }
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        // TODO implement
    }

    /**
     * Check if the game should be terminated or the game end conditions are met.
     *
     * @return true iff the game should be finished.
     */
    private boolean shouldFinish() {
        return terminate || env.util.findSets(deck, 1).size() == 0;
    }

    /**
     * Checks cards should be removed from the table and removes them.
     */
    private void removeCardsFromTable() {
        // TODO implement
        List<Integer> tableDeck = table.removeCards();
        for (int i = 0; i < tableDeck.size(); i++) {
            deck.add(tableDeck.get(i));
        }
    }


    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    private void placeCardsOnTable() {
        // TODO implement
        // Collections.shuffle(deck);
        List<Integer> temp = new ArrayList<Integer>();
        for (int i = 0; i < env.config.tableSize; i++) {
            table.placeCard(deck.get(i), i);
            temp.add(deck.get(i));
        }

        for (int i = 0; i < env.config.tableSize; i++) {
            deck.remove(temp.get(i));
        }

    }

    



    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {
        
        try {
            Thread.sleep(400);
        } catch (InterruptedException ignored) {}

        int [] check = table.checkSet();
        wasSet = false;
        if(check[env.config.firstTupleElm] != env.config.notSetToCheck)
        {
            if(check[env.config.firstTupleElm]==env.config.goodSet)
            {
                wasSet = true;
                players[check[env.config.secondTupleElm]].setFlag(env.config.goodSet);

                if(deck.size()>=env.config.tokenToSet)
                {
                    int [] cards =new int[env.config.tokenToSet] ;

                    cards[env.config.firstCard] = deck.get(env.config.firstCard);
                    deck.remove(env.config.firstCard);

                    cards[env.config.secondCard] = deck.get(env.config.firstCard);
                    deck.remove(env.config.firstCard);
                    
                    cards[env.config.thirdCard] = deck.get(env.config.firstCard);
                    deck.remove(env.config.firstCard);
                    
                    
                    table.place_3_cards(cards);
                }
            }
            else
            {
                ////// BAD SET
                players[check[env.config.secondTupleElm]].setFlag(env.config.badSet);
                wasSet = false;
            }
        }

    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // TODO implement
        if(!wasSet)
        {
        try {
            Thread.sleep(600);
        } catch (InterruptedException ignored) {}
        }
        env.ui.setCountdown(env.config.turnTimeoutMillis -count*env.config.oneSec, reset);
        count++;
        wasSet = false;
    }

    // /**
    //  * Returns all the cards from the table to the deck.
    //  */
    // private void removeAllCardsFromTable() {
    //     // TODO implement
    // }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        // TODO implement
    }
}
