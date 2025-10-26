// ==================== FILE 3: lamport/Node.java ====================
package lamport;

import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Implementation of Lamport's Distributed Mutual Exclusion Algorithm
 * Uses Java RMI for remote procedure calls between nodes
 */
public class Node implements NodeService {
    private int nodeId;
    private int numNodes;
    private AtomicInteger logicalClock;
    private PriorityBlockingQueue<Request> requestQueue;
    private Set<Integer> replySet;
    private Map<Integer, NodeService> peers;
    private boolean inCS;
    private Random random;

    public Node(int nodeId, int numNodes) {
        this.nodeId = nodeId;
        this.numNodes = numNodes;
        this.logicalClock = new AtomicInteger(0);
        this.requestQueue = new PriorityBlockingQueue<>();
        this.replySet = Collections.synchronizedSet(new HashSet<>());
        this.peers = new HashMap<>();
        this.inCS = false;
        this.random = new Random();
    }

    /**
     * Increment logical clock for local events
     * @return new clock value
     */
    private synchronized int incrementClock() {
        return logicalClock.incrementAndGet();
    }

    /**
     * Update logical clock on receiving message
     * Implements Lamport's clock rule: LC = max(LC, received_timestamp) + 1
     */
    private synchronized void updateClock(int receivedTimestamp) {
        logicalClock.set(Math.max(logicalClock.get(), receivedTimestamp) + 1);
    }

    /**
     * RPC method: Handle CS request from another node
     * Adds request to queue and sends reply
     */
    @Override
    public void requestCS(int senderId, int timestamp) throws RemoteException {
        updateClock(timestamp);

        synchronized (requestQueue) {
            requestQueue.add(new Request(senderId, timestamp));
            System.out.println("[Node " + nodeId + "] Received REQUEST from Node " +
                    senderId + " with timestamp " + timestamp);
        }

        // Send reply - check if peer is connected
        try {
            NodeService peer = peers.get(senderId);
            if (peer != null) {
                int replyTimestamp = incrementClock();
                peer.replyCS(nodeId, replyTimestamp);
                System.out.println("[Node " + nodeId + "] Sent REPLY to Node " +
                        senderId + " with timestamp " + replyTimestamp);
            } else {
                System.err.println("[Node " + nodeId + "] Peer Node " + senderId + " not connected yet");
            }
        } catch (Exception e) {
            System.err.println("[Node " + nodeId + "] Error sending reply: " + e.getMessage());
        }
    }

    /**
     * RPC method: Handle reply from another node
     * Tracks replies received for CS entry condition
     */
    @Override
    public void replyCS(int senderId, int timestamp) throws RemoteException {
        updateClock(timestamp);

        synchronized (replySet) {
            replySet.add(senderId);
            System.out.println("[Node " + nodeId + "] Received REPLY from Node " +
                    senderId + " (Total replies: " + replySet.size() + ")");
            replySet.notifyAll();
        }
    }

    /**
     * RPC method: Handle release notification
     * Removes sender's request from queue
     */
    @Override
    public void releaseCS(int senderId, int timestamp) throws RemoteException {
        updateClock(timestamp);

        synchronized (requestQueue) {
            requestQueue.removeIf(req -> req.nodeId == senderId);
            System.out.println("[Node " + nodeId + "] Received RELEASE from Node " +
                    senderId + " with timestamp " + timestamp);
            requestQueue.notifyAll();
        }
    }

    /**
     * Request access to Critical Section
     * Sends REQUEST to all other nodes and waits for entry conditions
     */
    public void requestCriticalSection() {
        int requestTimestamp = incrementClock();
        Request myRequest = new Request(nodeId, requestTimestamp);

        synchronized (requestQueue) {
            requestQueue.add(myRequest);
        }

        System.out.println("\n[Node " + nodeId + "] Requesting CS with timestamp " +
                requestTimestamp);

        // Send request to all other nodes
        for (Map.Entry<Integer, NodeService> entry : peers.entrySet()) {
            try {
                entry.getValue().requestCS(nodeId, requestTimestamp);
            } catch (Exception e) {
                System.err.println("[Node " + nodeId + "] Error sending request to Node " +
                        entry.getKey() + ": " + e.getMessage());
            }
        }

        // Wait for replies from all nodes and check if this node is at the front
        waitForCSAccess(myRequest);
    }

    /**
     * Wait until conditions are met to enter CS
     * Conditions: (1) Received replies from all nodes, (2) This request is at front of queue
     */
    private void waitForCSAccess(Request myRequest) {
        while (true) {
            synchronized (replySet) {
                synchronized (requestQueue) {
                    // Check if we have replies from all other nodes
                    if (replySet.size() == numNodes - 1) {
                        // Check if our request is at the front of the queue
                        Request frontRequest = requestQueue.peek();
                        if (frontRequest != null && frontRequest.equals(myRequest)) {
                            System.out.println("[Node " + nodeId + "] Entering Critical Section");
                            inCS = true;
                            return;
                        }
                    }
                }

                // Wait for more replies or queue changes
                try {
                    replySet.wait(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

            // Also check queue changes
            synchronized (requestQueue) {
                try {
                    requestQueue.wait(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Execute Critical Section
     * Simulates work being done in the critical section
     */
    public void executeCriticalSection() {
        if (!inCS) {
            System.err.println("[Node " + nodeId + "] ERROR: Not in Critical Section!");
            return;
        }

        System.out.println("[Node " + nodeId + "] *** EXECUTING CRITICAL SECTION ***");

        // Simulate some work in CS
        try {
            Thread.sleep(1000 + random.nextInt(1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("[Node " + nodeId + "] *** LEAVING CRITICAL SECTION ***");
    }

    /**
     * Release Critical Section
     * Removes own request from queue and notifies all other nodes
     */
    public void releaseCriticalSection() {
        int releaseTimestamp = incrementClock();

        synchronized (requestQueue) {
            requestQueue.removeIf(req -> req.nodeId == nodeId);
        }

        synchronized (replySet) {
            replySet.clear();
        }

        inCS = false;

        System.out.println("[Node " + nodeId + "] Released CS with timestamp " +
                releaseTimestamp + "\n");

        // Notify all other nodes about release
        for (Map.Entry<Integer, NodeService> entry : peers.entrySet()) {
            try {
                entry.getValue().releaseCS(nodeId, releaseTimestamp);
            } catch (Exception e) {
                System.err.println("[Node " + nodeId + "] Error sending release to Node " +
                        entry.getKey() + ": " + e.getMessage());
            }
        }
    }

    /**
     * Connect to peer nodes via RMI registry lookup
     * @param basePort Base port number (each node uses basePort + nodeId)
     */
    public void connectToPeers(int basePort) {
        for (int i = 0; i < numNodes; i++) {
            if (i != nodeId) {
                int attempts = 0;
                int maxAttempts = 10;
                boolean connected = false;

                while (!connected && attempts < maxAttempts) {
                    try {
                        Registry registry = LocateRegistry.getRegistry("localhost", basePort + i);
                        NodeService peer = (NodeService) registry.lookup("Node" + i);
                        peers.put(i, peer);
                        System.out.println("[Node " + nodeId + "] Connected to Node " + i);
                        connected = true;
                    } catch (Exception e) {
                        attempts++;
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }

                if (!connected) {
                    System.err.println("[Node " + nodeId + "] Failed to connect to Node " + i +
                            " after " + maxAttempts + " attempts");
                }
            }
        }
    }

    /**
     * Start RPC server
     * Exports this node as RMI object and binds to registry
     */
    public void startServer(int port) {
        try {
            NodeService stub = (NodeService) UnicastRemoteObject.exportObject(this, 0);
            Registry registry = null;

            // Try to get existing registry first
            try {
                registry = LocateRegistry.getRegistry("localhost", port);
                // Test if it's accessible
                registry.list();
            } catch (Exception e) {
                // Registry doesn't exist, create new one
                registry = LocateRegistry.createRegistry(port);
            }

            // Unbind any existing binding for this node
            try {
                registry.unbind("Node" + nodeId);
            } catch (Exception ignore) {}

            // Bind this node
            registry.rebind("Node" + nodeId, stub);
            System.out.println("[Node " + nodeId + "] RPC Server started on port " + port);

        } catch (Exception e) {
            System.err.println("[Node " + nodeId + "] Server error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Main method to run a node
     * Usage: java lamport.Node <nodeId> <numNodes>
     */
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java lamport.Node <nodeId> <numNodes>");
            return;
        }

        int nodeId = Integer.parseInt(args[0]);
        int numNodes = Integer.parseInt(args[1]);
        int basePort = 6000;

        Node node = new Node(nodeId, numNodes);
        node.startServer(basePort + nodeId);

        // Wait longer for all nodes to start their servers
        try {
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Connect to peers
        node.connectToPeers(basePort);

        // Wait for all connections to be fully established bidirectionally
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify all peers are connected
        System.out.println("[Node " + nodeId + "] Total peers connected: " + node.peers.size());

        // Simulate multiple CS requests
        Random random = new Random();
        for (int i = 0; i < 3; i++) {
            try {
                Thread.sleep(random.nextInt(3000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            node.requestCriticalSection();
            node.executeCriticalSection();
            node.releaseCriticalSection();
        }

        System.out.println("[Node " + nodeId + "] Completed all CS requests");
    }
}
