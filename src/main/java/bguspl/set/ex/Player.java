package bguspl.set.ex;

import java.io.Console;
import java.nio.file.ClosedWatchServiceException;
import java.security.spec.EncodedKeySpec;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;

import javax.swing.UIDefaults.ProxyLazyValue;

import org.w3c.dom.events.Event;

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
    
    public Object locObject;

    private volatile boolean freeze;

    private volatile boolean needToFreeze;
    

    

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
        freeze = true;
        needToFreeze = false;
        locObject = new Object();
    }

    /**
     * The main player thread of each player starts here (main loop for the player thread).
     */
    @Override
    public void run() {
        playerThread = Thread.currentThread();
        env.logger.info("Thread " + Thread.currentThread().getName() + " starting.");
        if (!human) createArtificialIntelligence();
        while (!terminate) {
            // TODO implement main player loop
            synchronized(this){
                try {wait();} 
                catch (InterruptedException ignored) {}
            }
            pointOrPenalty();

        }
        if (!human) try { aiThread.join(); } catch (InterruptedException ignored) {}
        env.logger.info("Thread " + Thread.currentThread().getName() + " terminated.");
    }

    public void removeNotActiveTokens()
    {
        List<Integer> keep = new ArrayList<>();
        int size = tokQueue.size();

        for (int i = 0; i < size; i++) {
            Integer temp = tokQueue.poll();
            if(table.playerContainsToken(temp, id))
                keep.add(temp);    
        }

        for (int i = 0; i < keep.size(); i++) {
            tokQueue.add(keep.get(i));
        }
                freeze = false;
        synchronized(locObject){
            
                needToFreeze=false;
                locObject.notifyAll();
            
        }
    }

    /**
     * Creates an additional thread for an AI (computer) player. The main loop of this thread repeatedly generates
     * key presses. If the queue of key presses is full, the thread waits until it is not full.
     */
    private void createArtificialIntelligence() {
        // note: this is a very very smart AI (!)
        aiThread = new Thread(() -> {
            env.logger.info("Thread " + Thread.currentThread().getName() + " starting.");
            Random rnd = new Random();
            while (!terminate) {
                // TODO implement player key press simulator

                // synchronized(locObject){
                    keyPressed(rnd.nextInt(3));//
                // }
                    
                    if(needToFreeze){
                        try {
                            synchronized(locObject){
                            {
                                
                                locObject.wait();}
                            }
                        } catch (InterruptedException ignored) {}}
                        
            }
            env.logger.info("Thread " + Thread.currentThread().getName() + " terminated.");
        }, "computer-" + id);
        aiThread.start();
    }

    /**
     * Called when the game should be terminated due to an external event.
     */
    public void terminate() {
        // TODO implement
        terminate = true;
        synchronized(locObject)
        {
            locObject.notifyAll();
        }
        synchronized(this)
        {
            notifyAll();
        }
    }

    /**
     * This method is called when a key is pressed.
     *
     * @param slot - the slot corresponding to the key pressed.
     */
    public void keyPressed(int slot) {
        // TODO implement
        
        
        if(!table.isInShuflle & !freeze)
        {
            
            if(tokQueue.contains(slot) & table.isCard(slot))
            {
                table.removeToken(id, slot);
                tokQueue.remove(slot);
            }
            else if(table.isCard(slot) && tokQueue.size()<Table.tokenToSet)
            {
                tokQueue.add(slot);

                if(tokQueue.size()==Table.tokenToSet)
                    {
                        freeze= true;
                        needToFreeze = true;
                        table.pushPid(id);
                    }

                table.placeToken(id, slot);
                
            }
            
        }
        
            
    }

    public void setFlag(int num)
    {
        flag = num;
        synchronized(this)
        {
            notifyAll();
        }
    }

    public void pointOrPenalty ()
    {
        
        if(flag == Table.goodSet)
            {
                point();
                flag = Table.neutralFlag;
                
            }
        else if(flag == Table.badSet)
            {
                penalty();
                flag = Table.neutralFlag;
                
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
        synchronized(locObject){
            tokQueue.clear();
            int ignored = table.countCards(); // this part is just for demonstration in the unit tests
            env.ui.setScore(id, ++score);
            env.ui.setFreeze(id, env.config.pointFreezeMillis);
            // freeze = true;
            try {
                Thread.sleep(env.config.pointFreezeMillis);
            } catch (InterruptedException ign) {}
            freeze = false;
            env.ui.setFreeze(id, Table.resetFreeze);

            needToFreeze = false;
            locObject.notifyAll();
        }
        
            
    }

    /**
     * Penalize a player and perform other related actions.
     */
    public void penalty() { ///need sync
        // TODO implement
        synchronized(locObject){
        int count = 0 ;
        
        while(count<env.config.penaltyFreezeMillis/1000)
        {
            env.ui.setFreeze(id, env.config.penaltyFreezeMillis - count * Table.oneSec);
            
            try {
                Thread.sleep(Table.oneSec);
            } catch (InterruptedException ignored) {}
            count++;
        } 
        freeze = false; 
        env.ui.setFreeze(id, Table.resetFreeze);
        removeNotActiveTokens();
        }
        
    }

    public int score() {
        return score;
    }

    public void removeAllTokens()
    {
        tokQueue.clear();
    }

    public boolean getFreeze()
    {
        return freeze;
    }

    
    public void freeze()
    {
        // synchronized(locObject){
        needToFreeze = true;
        freeze = true;
        // }
    }

    public void unfreeze()
    {
        synchronized(locObject)
        {
            needToFreeze = false;
            freeze = false;
            tokQueue.clear();
            locObject.notifyAll();
        }
    }


    //###################FOR_TESTS

    public int get_QueueSize()
    {
        return tokQueue.size();
    }
    
}
