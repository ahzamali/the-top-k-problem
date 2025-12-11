package topk;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public abstract class BaseTopKHandler implements TopKHandler {
    protected Map<String, Long> counts = new HashMap<>();
    protected long windowDurationMs;

    public BaseTopKHandler(long windowDurationMs) {
        this.windowDurationMs = windowDurationMs;
    }

    // Abstract method to handle the time component.
    // Subclasses must implement this to store the "Event" for aging out later.
    protected abstract void addToWindow(String item, int count, long timestamp);

    // Abstract method for subclasses to poll expired events.
    // Should return null if no more expired events.
    protected abstract Event pollExpired(long threshold);

    // Abstract method to get current window size (number of events)
    public abstract long getWindowSize();

    @Override
    public void add(String item, int count, long timestamp) {
        counts.put(item, counts.getOrDefault(item, 0L) + count);
        addToWindow(item, count, timestamp);
    }

    @Override
    public void prune(long currentTime) {
        long threshold = currentTime - windowDurationMs;
        while (true) {
            Event expired = pollExpired(threshold);
            if (expired == null) {
                break;
            }
            // Decrement count
            counts.compute(expired.item, (k, v) -> {
                if (v == null)
                    return null; // Should not happen
                long newVal = v - expired.count;
                return newVal <= 0 ? null : newVal; // Remove if 0
            });
        }
    }

    @Override
    public List<Map.Entry<String, Long>> getTopK(int k) {
        // Simple O(M log M) approach where M is unique items.
        // For production with massive M, one might use a MinHeap of size K, but Sort is
        // fine for evaluation.
        List<Map.Entry<String, Long>> sorted = new ArrayList<>(counts.entrySet());
        sorted.sort(Map.Entry.<String, Long>comparingByValue().reversed());

        if (sorted.size() > k) {
            return sorted.subList(0, k);
        }
        return sorted;
    }

    protected static class Event implements Comparable<Event> {
        String item;
        int count;
        long timestamp;

        public Event(String item, int count, long timestamp) {
            this.item = item;
            this.count = count;
            this.timestamp = timestamp;
        }

        @Override
        public int compareTo(Event other) {
            return Long.compare(this.timestamp, other.timestamp);
        }
    }
}
