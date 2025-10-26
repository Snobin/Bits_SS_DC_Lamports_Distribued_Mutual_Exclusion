// ==================== FILE 4: lamport/SimulationRunner.java ====================
package lamport;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Utility class to run multiple nodes for testing Lamport's algorithm
 * Can run nodes as separate processes or in separate threads
 */
public class SimulationRunner {
    private static final Logger logger = Logger.getLogger(SimulationRunner.class.getName());
    /**
     * Run simulation with specified number of nodes
     * @param args Optional: number of nodes (default 3)
     */
    public static void main(String[] args) {
        int numNodes = 3; // Default number of nodes

        if (args.length > 0) {
            numNodes = Integer.parseInt(args[0]);
        }

        logger.info("===========================================");
        logger.info("Lamport's Distributed Mutual Exclusion");
        logger.log(java.util.logging.Level.INFO, "Starting simulation with {0} nodes", numNodes);
        logger.info("===========================================\n");

        logger.info("Cleaning up any existing RMI registries...");
        cleanupPorts();

        logger.info("Running nodes in separate threads...\n");
        runInThreads(numNodes);
    }

    /**
     * Attempt to cleanup ports from previous runs
     */
    private static void cleanupPorts() {
        int basePort = 5001;
        for (int i = 0; i < 10; i++) {
            try {
                Registry registry = LocateRegistry.getRegistry("localhost", basePort + i);
                String[] names = registry.list();
                for (String name : names) {
                    try {
                        registry.unbind(name);
                        logger.log(java.util.logging.Level.INFO, "Cleaned up: {0} on port {1}", new Object[]{name, basePort + i});
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            } catch (Exception e) {
                // No registry on this port, that's fine
            }
        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run nodes in separate threads - works perfectly in IntelliJ
     * @param numNodes Number of nodes to simulate
     */
    public static void runInThreads(int numNodes) {
        List<Thread> threads = new ArrayList<>();

        // Start all node threads with proper delays
        for (int i = 0; i < numNodes; i++) {
            final int nodeId = i;
            Thread thread = new Thread(() -> {
                Node.main(new String[]{String.valueOf(nodeId), String.valueOf(numNodes)});
            });
            thread.setName("Node-" + i);
            threads.add(thread);
            thread.start();

            // Minimal delay - nodes will wait internally
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        logger.log(java.util.logging.Level.INFO, "All {0} nodes started!\n", numNodes);
        logger.info("Waiting for nodes to initialize and connect...\n");

        // Wait for all threads to complete
        for (int i = 0; i < threads.size(); i++) {
            try {
                threads.get(i).join();
                logger.log(java.util.logging.Level.INFO, "Node {0} completed", i);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        logger.info("\n===========================================");
        logger.info("All nodes completed successfully!");
        logger.info("===========================================");
    }
}