# Designing a Real-Time Top-K YouTube View Counter: From 5k to Billions of Events

Imagine you are tasked with building the "Trending Videos" feature for YouTube. You have a constant stream of "View" events arriving every millisecond. Your goal is to maintain a live dashboard showing the **Top 10 Most Viewed Videos in the Last Minute**.

It sounds simple: counts videos, sort them, take the top 10. But when you are dealing with high throughput (thousands of events per second) and a sliding time window (events expire every millisecond), the choice of data structure can mean the difference between a real-time dashboard and a crashed server.

In this article, we'll benchmark four common data structures to solve this problem, analyze why some fail spectacularly, and discuss how to scale this system to handle billions of events.

---

## 🔬 The "Single Node" Benchmark

We simulated a workload of **5,000 events per second** with a **60-second sliding window**. This means roughly 300,000 active events are in memory at any time.

Our requirements for the data structure are:
1.  **Ingest**: Quickly add a new view event (Video ID + Timestamp).
2.  **Prune**: Quickly remove events older than 60 seconds.
3.  **Query**: Return the Top K.

We implemented this using four standard Java structures. Here is what happened.

### 1. The ArrayList (Sorted List)
The intuitive approach. Keep a list of events sorted by timestamp.
-   **Ingestion**: Binary Search to find the spot + Insert.
-   **Pruning**: Remove from the head (oldest).

**Result**: 🔴 **Failed**.
-   **Latency**: ~27ms per second spent just managing the list.
-   **Why?** Removing items from the head of an `ArrayList` requires shifting all 300,000 remaining elements. Doing this thousands of times per second destroys CPU performance.

### 2. The LinkedList
The "O(1) deletions" fix.
-   **Ingestion**: Iterate to find the spot + Insert.
-   **Pruning**: `pollFirst()` is O(1).

**Result**: 🔴 **Failed**.
-   **Latency**: ~85ms per second (!).
-   **Why?** While pruning is instant (0.18ms), **Ingestion** becomes a nightmare. Even with 95% of data arriving in-order, the 5% that arrive late force us to traverse the linked list to find their correct chronological spot. This "pointer chasing" causes massive CPU cache misses, making it even slower than shifting the array.

### 3. The TreeMap (Red-Black Tree)
The "Balanced" approach.
-   **Ingestion**: $O(\log N)$.
-   **Pruning**: `pollFirstEntry()` is efficient.

**Result**: 🟡 **Passable**.
-   **Latency**: ~0.3ms for pruning, ~2.0ms for ingestion.
-   **Why?** It works well, but Trees have high memory overhead. Each entry requires a Node object with pointers to children and parents. For 300k items, this adds tens of megabytes of garbage collection pressure.

### 4. The MinHeap (Priority Queue)
The "Specialized" approach.
-   **Ingestion**: $O(\log N)$ (or better for strictly increasing timestamps).
-   **Pruning**: `peek()` is O(1), `extractMin()` is $O(\log N)$.

**Result**: 🟢 **Winner**.
-   **Latency**: ~0.7ms for ingestion, ~1.0ms for pruning.
-   **Why?** Heaps are array-based (contiguous memory). They offer the $O(\log N)$ benefits of trees without the memory overhead and pointer indirection. It is the most robust solution for high-throughput sliding windows.

---

## 🌍 Scaling to Billions: Beyond a Single Node

Our MinHeap works great for 5,000, maybe even 50,000 events per second. But what about **YouTube scale**? Billions of views per day?

A single machine cannot store all events, nor process the write load. We need a distributed architecture.

### Strategy 1: The "Scatter-Gather" (Map-Reduce) Pattern

1.  **Sharding (Ingest Layer)**:
    -   Partition the input stream. You can shard by `VideoID` (all views for Video A go to Server 1) or Round-Robin.
    -   Each distinct **Ingest Server** maintains its own local "Top K" for the data it sees, using the **MinHeap** strategy above.
    -   Every second, each server emits its local "Top K list" (small data) to an Aggregator.

2.  **Aggregation (Merge Layer)**:
    -   A central **Aggregator** receives "Local Top Ks" from 100 ingest servers.
    -   It merges these lists (conceptually essentially a "Merge K Sorted Lists" problem) to produce the **Global Top K**.

**Trade-off**: High accuracy, but requires moving lists around.

### Strategy 2: Probabilistic Counting (Approximation)

If you have 1 billion unique videos, keeping a counter for every single one (even in a distributed map) is expensive. Do you really care if a video has 5 views or 6? No. You care about the video with 50 million views.

**Solution: Count-Min Sketch**
A Count-Min Sketch is a probabilistic data structure that uses a fixed-size 2D array of counters and multiple hash functions.
-   **Space**: Constant (e.g., 5MB ram can track billions of events).
-   **Logic**: When a view arrives, hash the VideoID to find the specific counters and increment them.
-   **Query**: To find the count of a video, hash it and take the *minimum* value of all its counters.
-   **Pros**: Extremely memory efficient. perfect for identifying "Heavy Hitters" (Trending videos).
-   **Cons**: Slight over-estimation of counts (small collision error), but for Top-K, this is usually acceptable.

### Final Architecture for Scale

1.  **Ingest Layer**: Load balancers distribute traffic to hundreds of nodes.
2.  **Sketching**: Each node updates a local Count-Min Sketch for time windows.
3.  **Aggregation**: A periodic process merges these sketches (which naturally add together) to find the global heavy hitters.

---

## Conclusion

For educational projects and moderate loads: **Use a MinHeap**. It outperforms Lists and Trees for sliding window maintenance.

For massive scale: **Don't count everything perfectly**. Use **Scatter-Gather** with **Count-Min Sketch** sketches to find the signal in the noise.
