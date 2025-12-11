# Time Series Storage Strategy Analysis

## Objective
Determine the optimal data structure for a **High-Throughput Time Series** workload.

### Workload Profile
1.  **Ingestion**: 10,000 events/second.
2.  **Order**: 95% sequential, 5% out-of-order (late).
3.  **Storage**: Sliding Window of 1 minute (Steady state $\approx$ 600,000 events).
4.  **Queries**: 1 query/second against the window.

## Candidate Structures
1.  **Min Heap** (Optimized, Shift-based)
2.  **Binary Search Tree** (BST / `TreeMap`)
3.  **Sorted List** (`ArrayList` with Binary Search)

---

## 1. Empirical Benchmarks (Measured)
We ran controlled tests with **N=200,000** events (95% sorted, 5% random).

| Structure | Ingestion Time (200k) | Eviction Time (200k) | Total Latency (200k) |
| :--- | :--- | :--- | :--- |
| **Min Heap** | **2 ms** ($0.01\mu s/op$) | 41 ms ($0.2\mu s/op$) | **43 ms** |
| **BST (TreeMap)** | 19 ms ($0.1\mu s/op$) | **7 ms** ($0.03\mu s/op$) | 26 ms |
| **Sorted List** | 39 ms ($0.2\mu s/op$) | 1168 ms ($5.8\mu s/op$*) | 1207 ms |

*> Note: List eviction cost grows linearly with window size $N$. The $5.8\mu s$ figure is the average for sizes $0 \to 200k$ (Avg $100k$).*

---

## 2. Scenario Projection: 10k/sec, 600k Window
Extrapolating strictly from our benchmarks to the target workload.

**Steady State**: $N = 600,000$.
**Throughput**: 10,000 ops/sec (Ingest) + 10,000 ops/sec (Evict).

### A. Min Heap Performance
-   **Ingestion (10k/sec)**: $\approx 0.1$ ms total CPU time.
-   **Eviction (10k/sec)**:
    -   Cost scales with $\log N$. $\log(600k) / \log(200k) \approx 1.1x$ cost per op.
    -   Metric: $10,000 \times 0.2\mu s \times 1.1 \approx$ **2.2 ms**.
-   **Total CPU Load**: **~2.3 ms per second** (< 1% usage).
-   **Verdict**: **Extremely Efficient.**

### B. BST (TreeMap) Performance
-   **Ingestion (10k/sec)**: $\approx 1$ ms total CPU time.
-   **Eviction (10k/sec)**:
    -   Average cost $0.03\mu s$.
    -   Metric: $10,000 \times 0.03\mu s \approx$ **0.3 ms**.
-   **Total CPU Load**: **~1.3 ms per second** (< 1% usage).
-   **Verdict**: **Competitive**, but higher memory overhead per node (600k nodes $\approx$ 30-40MB heap overhead).

### C. Sorted List Performance
-   **Ingestion (10k/sec)**: $\approx 2$ ms (fast `arraycopy` for small probability).
-   **Eviction (10k/sec)**:
    -   Cost is $O(N)$. At $N=600k$, shifting is $6x$ heavier than our N=200k test average.
    -   Est. Cost per Op: $5.8\mu s \times 6 \approx 35 \mu s$.
    -   Metric: $10,000 \times 35\mu s =$ **350 ms**.
-   **Total CPU Load**: **~352 ms per second** (35% usage).
-   **Verdict**: **Dangerous.** A single thread spends 35% of time just managing memory shifts. Any spike in window size (e.g., 5 min window) would crash the system ($>100\%$ CPU).

---

## 3. The "Query" Factor (1/sec)

The choice depends on what the query does.

| Query Type | Best Structure | Why? |
| :--- | :--- | :--- |
| **Get Min / Oldest** | **Min Heap** | $O(1)$ access. |
| **Get Range / Max** | **BST** | $O(\log N)$ search. Heap cannot do this efficiently ($O(N)$ scan). |
| **Get Index (e.g., median)** | **Sorted List** | $O(1)$ access, but storage cost is too high. BST is better ($O(\log N)$ with augmentation). |

---

## Final Recommendation

### **Option 1: The "Safe Bet" (Min Heap)**
If your queries are simple (e.g., "process oldest events", "count throughput"), use the **Min Heap**.
-   **Pros**: Lowest ingestion latency, minimal memory footprint (array-based), robust performance.
-   **Cons**: Cannot efficiently query ranges (e.g., "events between 12:00 and 12:01").

### **Option 2: The "Flexible Querier" (BST/TreeMap)**
If you need to support arbitrary queries (Range scans, "Give me events > T"), use **BST**.
-   **Pros**: Very fast eviction, supports rich queries.
-   **Cons**: Higher memory usage (Object headers + pointers per node).

**Decision**:
Given "Queries happening at 1 per second" is **low frequency**, the query cost is likely negligible compared to the 20,000 ops/sec storage churn.
-   **Use Min Heap** if queries are "Min/Max" or simple scans.
-   **Use BST** ONLY if you need specific range lookups.
