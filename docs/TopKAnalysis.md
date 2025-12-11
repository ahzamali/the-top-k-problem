# TopK Time Window Analysis

## Experiment Setup
-   **Workload**: 5,000 events/second.
-   **Window**: 60 seconds (Steady State: ~300,000 active events).
-   **Structure**:
    -   **Aggregator**: `HashMap` for counts.
    -   **Window Store**: `Heap` vs `TreeMap` vs `ArrayList`.
-   **Data**: 95% sequential, 5% out-of-order latency.

## Benchmark Results (N=300,000)

| Metric | HeapStore (MinHeap) | TreeStore (TreeMap) | ListStore (ArrayList) |
| :--- | :--- | :--- | :--- |
| **Ingest Latency** (5k ops) | **0.68 ms** ($0.1\mu s/op$) | 1.59 ms ($0.3\mu s/op$) | 2.64 ms ($0.5\mu s/op$) |
| **Prune Latency** (1 op) | 1.05 ms | **0.27 ms** | 28.09 ms |
| **Total CPU Time** | **0.17 s** | 0.17 s | 2.47 s |
| **Max Overhead** | Low (Array) | High (Nodes) | Low (Array) |

### Analysis

#### 1. HeapStore (MinHeap)
-   **Performance**: Best-in-class ingestion. Pruning is slightly slower than Tree because `extractMin` requires sifting down from root, whereas Tree just unlinks the first node.
-   **Consistency**: Very stable. The cost of pruning 5000 items (1 sec of data) is efficient enough (~1ms).
-   **Recommendation**: **Primary Choice**. Lowest overhead for high-throughput ingestion.

#### 2. TreeStore (TreeMap)
-   **Performance**: Pruning is exceptionally fast (0.27ms) because `pollFirstEntry` on a Red-Black tree is efficient for sequential removal. Ingestion is 2x slower than Heap due to object allocation and rebalancing.
-   **Trade-off**: If your workload is **Time-Bound** (e.g., precise windowing is more critical than raw ingest throughput), Tree is viable. However, at 300k items, the memory overhead of `TreeMap$Entry` objects (~40 bytes/entry) is significant (~12MB extra heap).

#### 3. ListStore (ArrayList)
-   **Performance**: **Failed**. Pruning (removing from head) requires shifting 295,000 elements *per second*. 28ms latency per second is huge compared to 1ms.
-   **Verdict**: Do not use `ArrayList` for sliding windows.

## Design Recommendation

For a **Top K Hit Counter Hit Counter** with 10k+ TPS:
1.  **Ingestion Path**: Use a **MinHeap** to store the time-ordered events.
2.  **Aggregation**: Use a `ConcurrentHashMap` (or sharded maps) for the counters.
3.  **Windowing**: A background thread (or the ingest thread) polls the Heap.
    -   Use the **Heap** optimization (Shifts vs Swaps) to minimize CPU usage.
