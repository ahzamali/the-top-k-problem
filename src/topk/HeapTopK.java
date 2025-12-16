package topk;

import java.util.PriorityQueue;

public class HeapTopK extends BaseTopKHandler {
    private PriorityQueue<Event> minHeap = new PriorityQueue<>();

    public HeapTopK(long windowDurationMs) {
        super(windowDurationMs);
    }

    @Override
    protected void addToWindow(String item, int count, long timestamp) {
        minHeap.add(new Event(item, count, timestamp));
    }

    @Override
    protected Event pollExpired(long threshold) {
        if (!minHeap.isEmpty()) {
            Event oldest = minHeap.peek();
            if (oldest.timestamp < threshold) {
                return minHeap.poll();
            }
        }
        return null;
    }

    @Override
    public long getWindowSize() {
        return minHeap.size();
    }
}
