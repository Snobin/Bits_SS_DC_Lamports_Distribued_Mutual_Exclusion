// ==================== FILE 2: lamport/Request.java ====================
package lamport;

import java.io.Serializable;
import java.util.Objects;

/**
 * Represents a Critical Section request in Lamport's algorithm
 * Implements Comparable for priority queue ordering based on (timestamp, nodeId)
 */
public class Request implements Comparable<Request>, Serializable {
    private static final long serialVersionUID = 1L;

    public final int nodeId;
    public final int timestamp;

    /**
     * Create a new CS request
     * @param nodeId ID of the requesting node
     * @param timestamp Logical timestamp of the request
     */
    public Request(int nodeId, int timestamp) {
        this.nodeId = nodeId;
        this.timestamp = timestamp;
    }

    /**
     * Compare requests for priority queue ordering
     * Priority: lower timestamp first, then lower nodeId
     */
    @Override
    public int compareTo(Request other) {
        if (this.timestamp != other.timestamp) {
            return Integer.compare(this.timestamp, other.timestamp);
        }
        return Integer.compare(this.nodeId, other.nodeId);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Request)) return false;
        Request other = (Request) obj;
        return this.nodeId == other.nodeId && this.timestamp == other.timestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeId, timestamp);
    }

    @Override
    public String toString() {
        return String.format("Request(node=%d, ts=%d)", nodeId, timestamp);
    }
}