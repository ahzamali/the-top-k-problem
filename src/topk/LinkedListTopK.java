package topk;

import java.util.LinkedList;
import java.util.ListIterator;

public class LinkedListTopK extends BaseTopKHandler {
    private LinkedList<Event> list = new LinkedList<>();

    public LinkedListTopK(long windowDurationMs) {
        super(windowDurationMs);
    }

    @Override
    protected void addToWindow(String item, int count, long timestamp) {
        Event e = new Event(item, count, timestamp);

        // Optimization for 95% in-order case:
        // If list is empty or new event is newer than the last one, just append.
        if (list.isEmpty() || timestamp >= list.getLast().timestamp) {
            list.addLast(e);
            return;
        }

        // Otherwise (5% case), find insertion point from the end (since it's likely
        // recent).
        ListIterator<Event> it = list.listIterator(list.size());
        while (it.hasPrevious()) {
            Event current = it.previous();
            if (current.timestamp <= timestamp) {
                // Determine direction: we went back one step too far for insertion?
                // current.ts <= new.ts. So new goes AFTER current.
                // iterator cursor is now BEFORE current.
                it.next(); // Move back over current
                it.add(e);
                return;
            }
        }
        // If we exhausted the list, it's the oldest event
        list.addFirst(e);
    }

    @Override
    protected Event pollExpired(long threshold) {
        if (!list.isEmpty()) {
            Event oldest = list.getFirst();
            if (oldest.timestamp < threshold) {
                return list.pollFirst();
            }
        }
        return null;
    }

    @Override
    public long getWindowSize() {
        return list.size();
    }
}
