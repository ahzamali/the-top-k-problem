package topk;

import datastructure.MinHeap;

public class HeapTopK extends BaseTopKHandler {
    private MinHeap<Event> minHeap = new MinHeap<>();

    public HeapTopK(long windowDurationMs) {
        super(windowDurationMs);
    }

    @Override
    protected void addToWindow(String item, int count, long timestamp) {
        minHeap.insert(new Event(item, count, timestamp));
    }

    @Override
    protected Event pollExpired(long threshold) {
        if (!minHeap.isEmpty()) {
            Event oldest = minHeap.peek();
            if (oldest.timestamp < threshold) {
                return minHeap.extractMin();
            }
        }
        return null;
    }

    @Override
    public long getWindowSize() {
        return minHeap.size();
    }
}
