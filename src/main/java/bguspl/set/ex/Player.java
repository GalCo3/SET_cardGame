package bguspl.set.ex;

import java.security.spec.EncodedKeySpec;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import bguspl.set.Env;

/**
 * This class manages the players' threads and data
 *
 * @inv id >= 0
 * @inv score >= 0
 */
public class Player implements Runnable {

    /**
     * The game environment object.
     */
    private final Env env;

    /**
     * Game entities.
     */
    private final Table table;

    /**
     * The id of the player (starting from 0).
     */
    public final int id;

    /**
     * The thread representing the current player.
     */
    private Thread playerThread;

    /**
     * The thread of the AI (computer) player (an additional thread used to generate key presses).
     */
    private Thread aiThread;

    /**
     * True iff the player is human (not a computer player).
     */
    private final boolean human;

    /**
     * True iff game should be terminated due to an external event.
     */
    private volatile boolean terminate;

    /**
     * The current score of the player.
     */
    private int score;

    private Queue<Integer> tokQueue;

    private int flag;
    

    /**
     * The class constructor.
     *
     * @param env    - the environment object.
     * @param dealer - the dealer object.
     * @param table  - the table object.
     * @param id     - the id of the player.
     * @param human  - true iff the player is a human player (i.e. input is provided manually, via the keyboard).
     */
    public Player(Env env, Dealer dealer, Table table, int id, boolean human) {
        this.env = env;
        this.table = table;
        this.id = id;
        this.human = human;

        tokQueue=  new ConcurrentLinkedQueue<Integer>();
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + "starting.");
        if (!human) createArtificialIntelligence();

        while (!terminate) {
            // TODO implement main player loop
            pointOrPenalty();
            
        }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " starting.");
            while (!terminate) {
                // TODO implement player key press simulator
                try {
                    synchronized (this) { wait(); }
                } catch (InterruptedException ignored) {}
            }
            env.logger.log(Level.INFO, "Thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        // TODO implement
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        // TODO implement
        if(tokQueue.contains(slot))
            {
                table.removeToken(id, slot);
                tokQueue.remove(slot);
            }
        else
            {
                table.placeToken(id, slot);
                tokQueue.add(slot);
            }

    }

    public void setFlag(int num)
    {
        flag = num;
    }

    public void pointOrPenalty ()
    {
        if(flag == env.config.goodSet)
            {
                point();
                flag = env.config.neutralFlag;
            }
        else if(flag == env.config.badSet)
            {
                penalty();
                flag = env.config.neutralFlag;
            }                
    }

    /**
     * Award a point to a player and perform other related actions.
     *
     * @post - the player's score is increased by 1.
     * @post - the player's score is updated in the ui.
     */
    public void point() { /////need sync
        // TODO implement
        tokQueue.clear();
        int ignored = table.countCards(); // this part is just for demonstration in the unit tests
        env.ui.setScore(id, ++score);
        env.ui.setFreeze(id, env.config.pointFreezeMillis);

        try {
            Thread.sleep(env.config.pointFreezeMillis);
        } catch (InterruptedException ign) {}

        env.ui.setFreeze(id, env.config.resetFreeze);
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() { ///need sync
        // TODO implement
        int count = 0 ;

        while(count<env.config.penaltyCount)
        {
            env.ui.setFreeze(id, env.config.penaltyFreezeMillis - count * env.config.oneSec);
            
            try {
                Thread.sleep(env.config.pointFreezeMillis);
            } catch (InterruptedException ignored) {}
            count++;
        }  
        env.ui.setFreeze(id, env.config.resetFreeze);
    }

    public int getScore() {
        return score;
    }

    public void removeAllTokens()
    {
        tokQueue.clear();
    }
}
