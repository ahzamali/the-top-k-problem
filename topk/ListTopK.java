package topk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ListTopK extends BaseTopKHandler {
    private ArrayList<Event> list = new ArrayList<>();

    public ListTopK(long windowDurationMs) {
        super(windowDurationMs);
    }

    @Override
    protected void addToWindow(String item, int count, long timestamp) {
        Event e = new Event(item, count, timestamp);
        if (list.isEmpty() || timestamp >= list.get(list.size() - 1).timestamp) {
            list.add(e);
        } else {
            // Binary search for insertion point
            int pos = Collections.binarySearch(list, e);
            if (pos < 0) {
                pos = -(pos) - 1;
            }
            list.add(pos, e);
        }
    }

    @Override
    protected Event pollExpired(long threshold) {
        if (!list.isEmpty()) {
            Event oldest = list.get(0);
            if (oldest.timestamp < threshold) {
                list.remove(0);
                return oldest;
            }
        }
        return null;
    }

    @Override
    public long getWindowSize() {
        return list.size();
    }
}
