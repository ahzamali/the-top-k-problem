# Top-K Hit Counter (Sliding Window)

This project evaluates three different data structures for implementing a **Top-K Hit Counter** over a sliding time window. It simulates a high-throughput event stream (5,000 events/sec) to determine the most efficient storage strategy for maintaining the top elements.

## 🎯 Problem Statement

We need to consume a stream of events (`item`, `count`, `timestamp`), efficiently store them, and answer the query: **"What are the top K items in the last X seconds?"**

**Constraints:**
-   **High Throughput**: 5,000+ events per second.
-   **Sliding Window**: Events older than the window (e.g., 60 seconds) must be expired.
-   **Accuracy**: Must handle out-of-order events (late arrivals) correctly.

## 🛠️ Implemented Solutions

We tested three approaches to store the raw events for window management:

### 1. HeapTopK (MinHeap)
-   **Structure**: A Min-Heap sorted by timestamp.
-   **Ingestion**: $O(\log N)$ (Optimized sift-up).
-   **Expiration**: $O(K \log N)$ to remove old events from the root.
-   **Pros**: Extremely fast ingestion; very memory efficient (array-based).
-   **Cons**: Removing arbitrary elements is slow, but we only remove the *oldest* (min), which fits the heap perfecty.

### 2. TreeTopK (TreeMap / Red-Black Tree)
-   **Structure**: A Red-Black tree sorted by timestamp.
-   **Ingestion**: $O(\log N)$.
-   **Expiration**: $O(\log N)$ (or effectively $O(1)$ amortized if using `pollFirstEntry`).
-   **Pros**: Fastest expiration (pruning); naturally sorted.
-   **Cons**: Higher memory overhead (Node objects); slower ingestion than Heap due to rebalancing.

### 3. ListTopK (ArrayList + Binary Search)
-   **Structure**: A sorted ArrayList.
-   **Ingestion**: $O(N)$ (Binary search + Insert/Shift).
-   **Expiration**: $O(N)$ (Removing from head requires shifting all elements).
-   **Pros**: Simple; $O(1)$ access by index.
-   **Cons**: **Terrible performance** for sliding windows due to memory shifting ($O(N)$) on every insert/delete.

---

## 📊 Evaluation Results

**Scenario**:
-   **Rate**: 5,000 events/second
-   **Window**: 60 seconds (~300,000 active events in steady state)
-   **Metric**: Average time to process **one second** of data (5,000 ops).

| Metric | MinHeap (HeapTopK) | TreeMap (TreeTopK) | ArrayList (ListTopK) |
| :--- | :--- | :--- | :--- |
| **Ingest Latency** (5k ops) | **0.64 ms** 🟢 | 1.45 ms | 2.54 ms |
| **Prune Latency** (Windowing) | 0.96 ms | **0.24 ms** 🟢 | **27.58 ms** 🔴 |
| **Total Query Latency** | 0.37 ms | 0.20 ms | 0.16 ms |
| **Max Window Size** | ~300,000 | ~300,000 | ~300,000 |

### Analysis

1.  **Winner: MinHeap and TreeMap**.
    -   **MinHeap** is the best choice if your system is **write-heavy**. It effectively swallows 5,000 writes in just 0.6ms.
    -   **TreeMap** is the best choice if you need **stable, low-latency pruning**. It removes old events 4x faster than the Heap.
2.  **Loser: ArrayList**.
    -   The `ArrayList` approach collapses under load. Pruning takes **27ms** per second (vs <1ms for others). This is caused by shifting 300,000 elements in memory every second. **Do not use huge sorted lists for sliding windows.**

### Detailed Analysis
For a deeper dive into the theoretical time complexity and initial storage analysis (MinHeap vs BST vs Sorted List), please refer to the [Time Series Storage Analysis](docs/TimeSeriesStorageAnalysis.md).

---

## 🚀 How to Run

1.  **Compile**:
    ```bash
    javac -d bin -sourcepath src src/topk/TopKEvaluation.java
    ```

2.  **Run Benchmark**:
    ```bash
    java -cp bin topk.TopKEvaluation
    ```

### Configurable Run

You can run the benchmark with custom Event Rate (ops/sec) and Duration (seconds).

**Windows (PowerShell):**
```powershell
.\run_benchmark.ps1 -Rate 1000 -Duration 10
```

**Linux / Mac (Bash):**
```bash
chmod +x run_benchmark.sh
./run_benchmark.sh -r 1000 -d 10
```

## 📂 Project Structure

-   `src/topk/TopKEvaluation.java`: The main benchmark harness.
-   `src/topk/HeapTopK.java`: Implementation using `datastructure.MinHeap`.
-   `src/topk/TreeTopK.java`: Implementation using `java.util.TreeMap`.
-   `src/topk/ListTopK.java`: Implementation using `java.util.ArrayList`.
-   `src/datastructure/MinHeap.java`: A custom, efficient MinHeap implementation.
-   `docs/`: Analysis and documentation files.
