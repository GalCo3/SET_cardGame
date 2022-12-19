// package bguspl.set.ex;

// import bguspl.set.Config;
// import bguspl.set.Env;
// import bguspl.set.UserInterface;
// import bguspl.set.Util;
// import bguspl.set.ex.TableTest.MockLogger;

// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Test;
// import org.junit.jupiter.api.TestClassOrder;

// import java.util.List;
// import java.util.Properties;
// import java.util.logging.Logger;

// import static org.junit.jupiter.api.Assertions.assertEquals;
// import static org.mockito.ArgumentMatchers.booleanThat;
// import static org.mockito.Mockito.when;

// import bguspl.set.*;;


// class DealerTest {

//     private Dealer dealer;
//     private Config config;
//     Player [] players;

//     @BeforeEach
//     void setUp() {
//         // purposely do not find the configuration files (use defaults here).

//         Properties properties = new Properties();
//         properties.put("Rows", "2");
//         properties.put("Columns", "2");
//         properties.put("FeatureSize", "3");
//         properties.put("FeatureCount", "4");
//         properties.put("TableDelaySeconds", "0");
//         properties.put("PlayerKeys1", "81,87,69,82");
//         properties.put("PlayerKeys2", "85,73,79,80");
//         MockLogger logger = new MockLogger();
        
//         config = new Config(logger, properties);
        

//         Env env = new Env(logger, config, new MockUserInterface(), new MockUtil());
//         Table table = new Table(env);

//         players=  new Player[1];
//         dealer= new Dealer(env, table, players);

//         players[0] = new Player(env, dealer, table, 0, false);

//     }

//     @Test
//     void placeCardsOnTable()
//     {
//         int expected = dealer.getDeckSize() - config.tableSize;
//         dealer.placeCardsOnTable();

//         assertEquals(expected, dealer.getDeckSize());
//     }

//     @Test
//     void freezeAllPlayers()
//     {
//         players[0].unfreeze();
//         boolean expected = !players[0].getFreeze();
//         dealer.freezeAllPlayers();
//         assertEquals(players[0].getFreeze(), expected);

//     }


//     static class MockUserInterface implements UserInterface {
//         @Override
//         public void dispose() {}
//         @Override
//         public void placeCard(int card, int slot) {}
//         @Override
//         public void removeCard(int slot) {}
//         @Override
//         public void setCountdown(long millies, boolean warn) {}
//         @Override
//         public void setElapsed(long millies) {}
//         @Override
//         public void setScore(int player, int score) {}
//         @Override
//         public void setFreeze(int player, long millies) {}
//         @Override
//         public void placeToken(int player, int slot) {}
//         @Override
//         public void removeTokens() {}
//         @Override
//         public void removeTokens(int slot) {}
//         @Override
//         public void removeToken(int player, int slot) {}
//         @Override
//         public void announceWinner(int[] players) {}
//     };

//     static class MockUtil implements Util {
//         @Override
//         public int[] cardToFeatures(int card) {
//             return new int[0];
//         }

//         @Override
//         public int[][] cardsToFeatures(int[] cards) {
//             return new int[0][];
//         }

//         @Override
//         public boolean testSet(int[] cards) {
//             return false;
//         }

//         @Override
//         public List<int[]> findSets(List<Integer> deck, int count) {
//             return null;
//         }

//         @Override
//         public void spin() {}
//     }

//     static class MockLogger extends Logger {
//         protected MockLogger() {
//             super("", null);
//         }
//     }
// }
