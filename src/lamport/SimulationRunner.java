// ==================== FILE 4: lamport/SimulationRunner.java ====================
package lamport;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class to run multiple nodes for testing Lamport's algorithm
 * Can run nodes as separate processes or in separate threads
 */
public class SimulationRunner {

    /**
     * Run simulation with specified number of nodes
     * @param args Optional: number of nodes (default 3)
     */
    public static void main(String[] args) {
        int numNodes = 3; // Default number of nodes

        if (args.length > 0) {
            numNodes = Integer.parseInt(args[0]);
        }

        System.out.println("===========================================");
        System.out.println("Lamport's Distributed Mutual Exclusion");
        System.out.println("Starting simulation with " + numNodes + " nodes");
        System.out.println("===========================================\n");

        System.out.println("Cleaning up any existing RMI registries...");
        cleanupPorts();

        System.out.println("Running nodes in separate threads...\n");
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
                        System.out.println("Cleaned up: " + name + " on port " + (basePort + i));
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

        System.out.println("All " + numNodes + " nodes started!\n");
        System.out.println("Waiting for nodes to initialize and connect...\n");

        // Wait for all threads to complete
        for (int i = 0; i < threads.size(); i++) {
            try {
                threads.get(i).join();
                System.out.println("Node " + i + " completed");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        System.out.println("\n===========================================");
        System.out.println("All nodes completed successfully!");
        System.out.println("===========================================");
    }
}