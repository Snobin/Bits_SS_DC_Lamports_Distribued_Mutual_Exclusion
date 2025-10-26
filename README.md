# Lamport's Distributed Mutual Exclusion Algorithm

[![Java](https://img.shields.io/badge/Java-17+-orange.svg)](https://www.oracle.com/java/)
[![RMI](https://img.shields.io/badge/RPC-Java%20RMI-blue.svg)](https://docs.oracle.com/javase/8/docs/technotes/guides/rmi/)

A complete implementation of **Lamport's Distributed Mutual Exclusion Algorithm** using Java RMI (Remote Method Invocation) for distributed systems. This project demonstrates mutual exclusion, fairness, and causal ordering in a distributed environment.

## Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Algorithm](#algorithm)
- [Project Structure](#project-structure)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Usage](#usage)
- [How It Works](#how-it-works)
- [Output Example](#output-example)
- [Configuration](#configuration)
- [Contributing](#contributing)
- [License](#license)
- [References](#references)



## Overview

This project implements the classic **Lamport's Distributed Mutual Exclusion Algorithm**, which ensures that multiple distributed nodes can safely access a shared critical section without conflicts. The implementation uses **Java RMI** for remote procedure calls between nodes.

### Key Concepts

- **Mutual Exclusion**: Only one node can be in the critical section at any time
- **Fairness**: Requests are granted in happened-before order (FIFO based on logical timestamps)
- **Lamport's Logical Clocks**: Maintains causal ordering of events across distributed nodes

##  Features

- ✅ **Pure Java Implementation** - No external dependencies
- ✅ **Java RMI for RPC** - Remote method invocation for inter-node communication
- ✅ **Lamport's Logical Clocks** - Implements happened-before relationship
- ✅ **Priority Queue Ordering** - Requests ordered by (timestamp, nodeId)
- ✅ **Thread-Safe Operations** - Proper synchronization mechanisms
- ✅ **Scalable Design** - Works with N independent nodes
- ✅ **Comprehensive Logging** - Clear output showing algorithm execution
- ✅ **Easy to Run** - Single command execution with `SimulationRunner`

##  Algorithm

Lamport's algorithm follows these phases:

### 1. **Request Phase**
```
Node i wants to enter CS:
  - Increment logical clock
  - Add request to local queue
  - Send REQUEST(nodeId, timestamp) to all other nodes
```

### 2. **Reply Phase**
```
Node j receives REQUEST from Node i:
  - Update logical clock: LC = max(LC, received_timestamp) + 1
  - Add request to local queue
  - Send REPLY to Node i
```

### 3. **Entry Condition**
```
Node i can enter CS when:
  - Has received REPLY from all other nodes
  - Its request is at the front of its priority queue
```

### 4. **Release Phase**
```
Node i exits CS:
  - Remove its request from local queue
  - Send RELEASE to all other nodes
  - Clear reply set
```

##  Project Structure

```
lamport-distributed-mutex/
├── src/
│   └── lamport/
│       ├── NodeService.java      # RMI interface for remote communication
│       ├── Request.java           # Request class with timestamp ordering
│       ├── Node.java              # Main node implementation
│       └── SimulationRunner.java # Utility to run multiple nodes
├── README.md
└── LICENSE
```

### File Descriptions

| File | Description |
|------|-------------|
| `NodeService.java` | RMI Remote interface defining `requestCS()`, `replyCS()`, `releaseCS()` |
| `Request.java` | Represents a CS request with `Comparable` implementation for priority queue |
| `Node.java` | Core implementation with Lamport's algorithm logic and RMI server |
| `SimulationRunner.java` | Helper class to launch and manage multiple node instances |

##  Prerequisites

- **Java JDK 8+** (Tested with Java 17)
- **IntelliJ IDEA** (recommended) or any Java IDE
- Basic understanding of distributed systems and RMI

##  Installation

### Clone the Repository

```bash
git clone https://github.com/yourusername/lamport-distributed-mutex.git
cd lamport-distributed-mutex
```

### Compile the Project

#### Using IntelliJ IDEA:
1. Open project in IntelliJ
2. Go to **Build → Build Project** (or `Ctrl+F9` / `Cmd+F9`)

#### Using Command Line:
```bash
cd src
javac lamport/*.java
```

##  Usage

### Option 1: Using SimulationRunner (Recommended)

#### In IntelliJ:
1. Right-click `SimulationRunner.java`
2. Select **"Run 'SimulationRunner.main()'"**

#### Command Line:
```bash
cd src
java lamport.SimulationRunner         # Run with 3 nodes (default)
java lamport.SimulationRunner 5       # Run with 5 nodes
```

### Option 2: Manual Node Startup

Open **3 separate terminals**:

```bash
# Terminal 1 - Node 0
cd src
java lamport.Node 0 3

# Terminal 2 - Node 1
java lamport.Node 1 3

# Terminal 3 - Node 2
java lamport.Node 2 3
```

**Arguments:**
- First argument: Node ID (0, 1, 2, ...)
- Second argument: Total number of nodes

##  How It Works

### Logical Clock Updates

```java
// On local event
timestamp = ++logicalClock;

// On receiving message
logicalClock = max(logicalClock, receivedTimestamp) + 1;
```

### Priority Queue Ordering

```java
public int compareTo(Request other) {
    if (this.timestamp != other.timestamp) {
        return Integer.compare(this.timestamp, other.timestamp);
    }
    return Integer.compare(this.nodeId, other.nodeId);  // Tie-breaker
}
```

### Entry Conditions

A node enters the Critical Section when:
1. ✅ Received `REPLY` from **all** other nodes
2. ✅ Its request is at the **front** of the priority queue

##  Output Example

```
===========================================
Lamport's Distributed Mutual Exclusion
Starting simulation with 3 nodes
===========================================

[Node 0] RPC Server started on port 6000
[Node 1] RPC Server started on port 6001
[Node 2] RPC Server started on port 6002

[Node 0] Connected to Node 1
[Node 0] Connected to Node 2
[Node 1] Connected to Node 0
[Node 1] Connected to Node 2
[Node 2] Connected to Node 0
[Node 2] Connected to Node 1

[Node 2] Requesting CS with timestamp 1
[Node 0] Received REQUEST from Node 2 with timestamp 1
[Node 1] Received REQUEST from Node 2 with timestamp 1
[Node 0] Sent REPLY to Node 2 with timestamp 3
[Node 1] Sent REPLY to Node 2 with timestamp 3
[Node 2] Entering Critical Section
[Node 2] *** EXECUTING CRITICAL SECTION ***
[Node 2] *** LEAVING CRITICAL SECTION ***
[Node 2] Released CS with timestamp 10

[Node 1] Requesting CS with timestamp 4
[Node 1] Entering Critical Section
[Node 1] *** EXECUTING CRITICAL SECTION ***
...
```

### Observations:
- ✅ Only ONE node in CS at a time (mutual exclusion)
- ✅ Requests serviced in timestamp order (fairness)
- ✅ Logical clocks properly synchronized

## Configuration

### Change Number of CS Requests

Edit `Node.java` line ~340:

```java
for (int i = 0; i < 3; i++) {  // Change 3 to desired count
    node.requestCriticalSection();
    node.executeCriticalSection();
    node.releaseCriticalSection();
}
```

### Change Critical Section Duration

Edit `Node.java` in `executeCriticalSection()`:

```java
Thread.sleep(1000 + random.nextInt(1000));  // 1-2 seconds
```

### Change Base Port

Edit `Node.java` line ~313:

```java
int basePort = 6000;  // Change to any available port range
```

### Run with More Nodes

```bash
java lamport.SimulationRunner 10  # Run with 10 nodes
```

## Testing Scenarios

### Test 1: Basic Mutual Exclusion (3 nodes)
```bash
java lamport.SimulationRunner 3
```
**Verify:** No overlapping CS executions

### Test 2: Fairness Test (5 nodes)
```bash
java lamport.SimulationRunner 5
```
**Verify:** Nodes enter CS in timestamp order

### Test 3: Scalability (10 nodes)
```bash
java lamport.SimulationRunner 10
```
**Verify:** Algorithm scales with more nodes

## Troubleshooting

### Port Already in Use
```bash
# Mac/Linux
lsof -ti:6000,6001,6002 | xargs kill -9

# Or restart your IDE to clean up processes
```

### Connection Refused
- Ensure all nodes are started
- Check firewall settings
- Verify ports 6000-600X are available

### Class Not Found
```bash
cd src
javac lamport/*.java
```

## Algorithm Properties

| Property | Description | Implementation |
|----------|-------------|----------------|
| **Safety** | At most one process in CS | Priority queue + reply mechanism |
| **Liveness** | Every request eventually granted | All nodes send replies |
| **Fairness** | FIFO ordering | Lamport's logical timestamps |
| **No Starvation** | All requests eventually satisfied | Timestamp-based priority |

##  Correctness Guarantees

1. **Mutual Exclusion**: Only one node accesses CS at a time
2. **Progress**: If no node is in CS and some nodes want to enter, one will eventually succeed
3. **Bounded Waiting**: Requests are granted in happened-before order

##  References

- **Lamport, Leslie** (1978). "Time, Clocks, and the Ordering of Events in a Distributed System". *Communications of the ACM* 21 (7): 558–565.
- [Java RMI Documentation](https://docs.oracle.com/javase/8/docs/technotes/guides/rmi/)
- [Distributed Systems Concepts](https://www.distributed-systems.net/)

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Academic Use

This implementation is suitable for:
- Distributed Systems courses
- Operating Systems labs
- Concurrent Programming assignments
- Research on distributed algorithms
