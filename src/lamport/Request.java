package lamport;

import java.io.Serial;
import java.io.Serializable;

/**
 * Represents a Critical Section request in Lamport's algorithm
 * Implements Comparable for priority queue ordering based on (timestamp, nodeId)
 */
public record Request(int nodeId, int timestamp) implements Comparable<Request>, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

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
        if (!(obj instanceof Request other)) return false;
        return this.nodeId == other.nodeId && this.timestamp == other.timestamp;
    }

    @Override
    public String toString() {
        return String.format("Request(node=%d, ts=%d)", nodeId, timestamp);
    }
}