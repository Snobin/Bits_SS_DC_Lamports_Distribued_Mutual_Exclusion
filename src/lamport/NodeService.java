// ==================== FILE 1: lamport/NodeService.java ====================
package lamport;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RPC Interface for Lamport's Distributed Mutual Exclusion Algorithm
 * Defines remote methods for inter-node communication
 */
public interface NodeService extends Remote {
    /**
     * Handle Critical Section request from another node
     * @param senderId ID of the requesting node
     * @param timestamp Logical timestamp of the request
     * @throws RemoteException if RPC communication fails
     */
    void requestCS(int senderId, int timestamp) throws RemoteException;

    /**
     * Handle reply message from another node
     * @param senderId ID of the replying node
     * @param timestamp Logical timestamp of the reply
     * @throws RemoteException if RPC communication fails
     */
    void replyCS(int senderId, int timestamp) throws RemoteException;

    /**
     * Handle release notification from another node
     * @param senderId ID of the node releasing CS
     * @param timestamp Logical timestamp of the release
     * @throws RemoteException if RPC communication fails
     */
    void releaseCS(int senderId, int timestamp) throws RemoteException;
}
