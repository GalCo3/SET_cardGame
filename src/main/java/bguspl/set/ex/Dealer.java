package bguspl.set.ex;

import bguspl.set.Config;
import bguspl.set.Env;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.xml.sax.ext.DeclHandler;

import java.security.interfaces.ECKey;
import java.security.spec.EncodedKeySpec;
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
        wasSet=false;
    }

    /**
     * The dealer thread starts here (main loop for the dealer thread).
     */
    @Override
    public void run() {
        env.logger.info("Thread " + Thread.currentThread().getName() + " starting.");
        
        Thread [] p  =  new Thread[env.config.players];

        for (int i = 0; i < p.length; i++) {
            p[i] = new Thread(players[i]);
            p[i].start();
        }

        while (!shouldFinish()) {
            placeCardsOnTable();
            unfreezeAllPlayers();
            timerLoop();
            updateTimerDisplay(true);
            freezeAllPlayers();
            removeCardsFromTable();
        }
        announceWinners();
        env.logger.info("Thread " + Thread.currentThread().getName() + " terminated.");
    }


    public void freezeAllPlayers()
    {
        synchronized(table.lock){
        for (int i = 0; i < players.length; i++) {
            players[i].freeze();
        }
        }
    }


    private void unfreezeAllPlayers()
    {
        synchronized(table.lock){
        for (int i = 0; i < players.length; i++) {
            players[i].unfreeze();
        }
        }
    }
    /**
     * The inner loop of the dealer thread that runs as long as the countdown did not time out.
     */
    private void timerLoop() {
        updateTimerDisplay(true);
        while (!terminate && System.currentTimeMillis() < reshuffleTime) {
            sleepUntilWokenOrTimeout();
            updateTimerDisplay(false);
        }
    }

    

    
    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        // TODO implement
        for (int i = 0; i < players.length; i++) {
            players[i].terminate();
        }
        terminate = true;
        // Thread.currentThread().interrupt();
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
        synchronized(table.lock)
        {
            table.isInShuflle = true;
            List<Integer> tableDeck = table.removeCards();
            for (int i = 0; i < tableDeck.size(); i++) {
                deck.add(tableDeck.get(i));
            }
        }
    }


    /**
     * Check if any cards can be removed from the deck and placed on the table.
     */
    public void placeCardsOnTable() {
        // TODO implement

        synchronized(table.lock){
        Collections.shuffle(deck);
            List<Integer> temp = new ArrayList<Integer>();
            for (int i = 0; i < env.config.tableSize & i<deck.size(); i++) {
                table.placeCard(deck.get(i), i);
                temp.add(deck.get(i));
            }
            int size = deck.size();
            for (int i = 0; i < env.config.tableSize & i<size; i++) 
            {
                deck.remove(temp.get(i));
            }
            table.isInShuflle = false;
        }


    }





    /**
     * Sleep for a fixed amount of time or until the thread is awakened for some purpose.
     */
    private void sleepUntilWokenOrTimeout() {

        try {
            Thread.sleep(Table.dealerTurnSleep);
        } catch (InterruptedException ignored) {}

        int [] check=table.checkSet();
        wasSet = false;

        if(check[Table.firstTupleElm] != Table.notSetToCheck)
        {
            if(check[Table.firstTupleElm]==Table.goodSet)
            {
                wasSet = true;
                players[check[Table.secondTupleElm]].setFlag(Table.goodSet);
                
                if(deck.size()>=Table.tokenToSet)
                {
                    int [] cards =new int[Table.tokenToSet] ;

                    cards[Table.firstCard] = deck.get(Table.firstCard);
                    deck.remove(Table.firstCard);

                    cards[Table.secondCard] = deck.get(Table.firstCard);
                    deck.remove(Table.firstCard);

                    cards[Table.thirdCard] = deck.get(Table.firstCard);
                    deck.remove(Table.firstCard);


                    table.place_3_cards(cards);
                }
                else
                {
                    //deck is empty
                    if(table.empty_Table())
                        terminate = true;
                }
                
                
            }
            else
            {
                ////// BAD SET
                players[check[Table.secondTupleElm]].setFlag(Table.badSet);
                wasSet = false;
            }
        }
        else if (check[Table.secondTupleElm] != Table.notSetToCheck)
        {
            players[check[Table.secondTupleElm]].removeNotActiveTokens();
        }

    }

    /**
     * Reset and/or update the countdown and the countdown display.
     */
    private void updateTimerDisplay(boolean reset) {
        // TODO implement
        if(reset|| wasSet)
        {
            reshuffleTime = System.currentTimeMillis() + env.config.turnTimeoutMillis ;//+ env.config.delay;
            env.ui.setCountdown(reshuffleTime - System.currentTimeMillis(), false);
        }
        else 
        {
            Boolean tenSec = reshuffleTime - System.currentTimeMillis() <=Table.tenSec; 
            env.ui.setCountdown(reshuffleTime - System.currentTimeMillis(), tenSec);
            wasSet = false;
        }        
    }

    // /**
    //  * Returns all the cards from the table to the deck.
    //  */
    private void removeAllCardsFromTable() {
        // TODO implement
    }

    /**
     * Check who is/are the winner/s and displays them.
     */
    private void announceWinners() {
        // TODO implement
        env.ui.setCountdown(Table.resetFreeze, false);
        int max = players[0].score();
        Queue<Integer> winners = new ConcurrentLinkedQueue<>();

        

        if(!terminate)
        {
            terminate();
        }

        for (int i = 1; i < players.length; i++) {
            if(players[i].score() > max)
                max = players[i].score();
        }

        for (int i = 0; i < players.length; i++) {
            if(players[i].score() == max)
                winners.add(i);
        }

        int [] out = new int[winners.size()];
        for (int i = 0; i < out.length; i++) {
            out[i] = winners.poll();
        }


        env.ui.announceWinner(out);
    }

    public int getDeckSize()
    {
        return deck.size();
    }
}
