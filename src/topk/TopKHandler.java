package topk;

import java.util.List;
import java.util.Map;

public interface TopKHandler {
    void add(String item, int count, long timestamp);

    List<Map.Entry<String, Long>> getTopK(int k);

    void prune(long currentTime);

    // For metrics
    long getWindowSize();
}
