# High-Throughput Sliding Window Top-K

> **Architectural Analysis of Real-Time Ranking Strategies**

This repository explores the system design trade-offs involved in maintaining a **Top-K Ranking** over a high-velocity sliding window. It simulates a mock production workload (e.g., "Trending Videos", "DDoS IP Limiting") to evaluate four distinct data structures on **Ingestion Latency**, **Eviction Costs**, and **Memory Locality**.

---

## 1. Problem Context & Constraints

The "Top-K" problem appears simple but becomes non-trivial under strict latency and correctness constraints.

### The Scenario
-   **Workload**: 5,000 to 100,000 events per second.
-   **Window**: Sliding time window (e.g., "Last 60 seconds").
-   **Query**: "Who are the top 10 most frequent items right now?"

### The Constraints
1.  **Write-Heavy**: The system must handle 99% writes (ingest) and <1% reads (query).
2.  **Strict Eviction**: Events must expire precisely when they fall out of the window.
3.  **Out-of-Order Data**: 5% of events arrive late (simulating network jitter), preventing naive append-only optimizations.
4.  **Garbage Collection**: Minimizing object churn is critical to avoid "stop-the-world" pauses.

---

## 2. Design Space Exploration

We evaluated four canonical data structures. Each represents a different logical approach to the `insert`/`evict` problem.

| Strategy | Structure | Ingest Complexity | Evict Complexity | Optimization Theory | Principal Insight |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **MinHeap** | Priority Queue | $O(\log N)$ | $O(\log N)$ | Sift-up/down on Array | **Winner**. Excellent cache locality. Removing the *oldest* item aligns perfectly with Min-Heap semantics. |
| **TreeMap** | Red-Black Tree | $O(\log N)$ | $O(\log N)$ | Balanced Tree | **Passable**. Good theoretical bounds, but heavy pointer indirection and object overhead (Nodes) degrade performance vs Arrays. |
| **LinkedList** | Doubly Linked | $O(N)$ * | $O(1)$ | Constant time removal | **Failure**. The "O(1) Eviction" promise is a trap. Inserting out-of-order data requires $O(N)$ traversal, which is cache-hostile (pointer chasing). |
| **ArrayList** | Sorted Array | $O(N)$ | $O(N)$ | Binary Search + Shift | **Failure**. While Binary Search is fast, shifting 300k items in memory for every eviction saturates memory bandwidth. |

> ***Note**: LinkedList ingestion is technically $O(N)$, but practically much slower than ArrayList ingestion due to lack of CPU cache spatial locality.*

---

## 3. Evaluation Methodology

### Workload Synthetic
-   **Rate**: 5,000 events/sec.
-   **Duration**: 80 seconds (Steady state reached at 60s).
-   **Pattern**: 95% strictly increasing timestamps, 5% random late arrivals (jitter).
-   **Hardware**: Windows x64 host.

### Key Metrics
1.  **Ingest Latency**: Cost to add a new event.
2.  **Prune Latency**: Cost to remove expired events (Window maintenance).
3.  **Total Latency**: The "Tax" paid per second of data processing.

### Results (Steady State, N=300,000)

| Metric | MinHeap | TreeMap | ArrayList | LinkedList |
| :--- | :--- | :--- | :--- | :--- |
| **Ingest** (5k ops) | **0.70 ms** 🟢 | 2.18 ms | 2.59 ms | **85.37 ms** 🔴 |
| **Prune** (Window) | 1.08 ms | **0.31 ms** 🟢 | **26.18 ms** 🔴 | **0.18 ms** 🟢 |
| **Total Cost** | **~1.8 ms** | ~2.5 ms | ~29 ms | ~85 ms |

---

## 4. Recommendations

### For Production Systems
1.  **Default Choice: MinHeap**.
    -   Use this for 90% of sliding window problems. It provides the best balance of write throughput and memory safety.
    -   *Why?* It respects the hardware. Array-backed heaps minimize pointer chasing and allocation overhead.

2.  **For Complex Queries: TreeMap**.
    -   Use only if you need Range Queries (e.g., "Count items between 12:00 and 12:05") in addition to Top-K.
    -   *Cost*: 2x-3x higher latency and much higher GC pressure.

3.  **For "Billions of Events": Sketching**.
    -   Do not store raw events. Use a **Count-Min Sketch** in a **Scatter-Gather** architecture.
    -   See [Architecture Article](docs/Article_TopK_YouTube.md) for the distributed design.

---

## 5. Running the Experiments

**Run Benchmarks:**
```bash
# Windows
.\run_benchmark.ps1 -Rate 5000 -Duration 80

# Linux
./run_benchmark.sh -r 5000 -d 80
```
