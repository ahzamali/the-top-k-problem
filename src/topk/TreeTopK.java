package topk;

import java.util.TreeMap;
import java.util.Map;

public class TreeTopK extends BaseTopKHandler {
    // Key: Timestamp + Unique ID (to handle duplicates), Value: Event
    // We use a counter to ensure uniqueness for identical timestamps
    private TreeMap<Long, Event> tree = new TreeMap<>();
    private long logicalClock = 0;

    public TreeTopK(long windowDurationMs) {
        super(windowDurationMs);
    }

    @Override
    protected void addToWindow(String item, int count, long timestamp) {
        // Simple mechanism to prevent key collision:
        // Mix timestamp with a counter.
        // Assuming timestamp is in ms, we can shift left.
        // Or simpler: just use a unique ID key.
        // Let's us (timestamp << 20) | (logicalClock++ & 0xFFFFF)
        // This supports 1 million events per millisecond.
        long key = (timestamp << 20) | (logicalClock++ & 0xFFFFF);
        tree.put(key, new Event(item, count, timestamp));
    }

    @Override
    protected Event pollExpired(long threshold) {
        if (!tree.isEmpty()) {
            Map.Entry<Long, Event> entry = tree.firstEntry();
            if (entry.getValue().timestamp < threshold) {
                tree.pollFirstEntry();
                return entry.getValue();
            }
        }
        return null;
    }

    @Override
    public long getWindowSize() {
        return tree.size();
    }
}
