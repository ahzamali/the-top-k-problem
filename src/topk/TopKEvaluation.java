package topk;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TopKEvaluation {

    // Config
    private static final int EVENTS_PER_SEC = 5000;
    private static final int DURATION_SECONDS = 80; // Run longer to fill window (60s) + steady state
    private static final long WINDOW_MS = 60000;
    private static final int NUM_ITEMS = 1000; // Cardinality
    private static final double OUT_OF_ORDER_RATIO = 0.05;

    public static void main(String[] args) {
        System.out.println("Top K Hit Counter Evaluation");
        System.out.printf("Events/Sec: %d, Window: %d ms, Duration: %d sec%n", EVENTS_PER_SEC, WINDOW_MS,
                DURATION_SECONDS);
        System.out.println("--------------------------------------------------------------------------------");

        // Generate dataset upfront to ensure fairness
        List<Tick> timeline = generateTimeline();

        evaluate("HeapTopK", new HeapTopK(WINDOW_MS), timeline);
        evaluate("TreeTopK", new TreeTopK(WINDOW_MS), timeline);
        evaluate("ListTopK", new ListTopK(WINDOW_MS), timeline);
    }

    private static void evaluate(String name, TopKHandler handler, List<Tick> timeline) {
        System.out.printf("Evaluating %-10s... ", name);
        long startTotal = System.nanoTime();

        long totalIngestNs = 0;
        long totalPruneNs = 0;
        long totalQueryNs = 0;
        long maxWindowSize = 0;

        for (Tick tick : timeline) {
            // 1. Ingest
            long t0 = System.nanoTime();
            for (EventData e : tick.events) {
                handler.add(e.item, e.count, e.timestamp);
            }
            totalIngestNs += (System.nanoTime() - t0);

            // 2. Prune (Window maintenance)
            long t1 = System.nanoTime();
            handler.prune(tick.currentTime);
            totalPruneNs += (System.nanoTime() - t1);

            // 3. Query (1 per second)
            long t2 = System.nanoTime();
            handler.getTopK(10);
            totalQueryNs += (System.nanoTime() - t2);

            long size = handler.getWindowSize();
            if (size > maxWindowSize)
                maxWindowSize = size;
        }

        long endTotal = System.nanoTime();
        double totalSec = (endTotal - startTotal) / 1e9;

        System.out.println("Done.");
        System.out.printf("  Total Time: %.2f s%n", totalSec);
        System.out.printf("  Avg Ingest (5k ops): %.2f ms%n", (totalIngestNs / timeline.size()) / 1e6);
        System.out.printf("  Avg Prune  (1 op):   %.2f ms%n", (totalPruneNs / timeline.size()) / 1e6);
        System.out.printf("  Avg Query  (1 op):   %.2f ms%n", (totalQueryNs / timeline.size()) / 1e6);
        System.out.printf("  Max Window Size:     %d%n", maxWindowSize);
        System.out.println("--------------------------------------------------------------------------------");
    }

    private static List<Tick> generateTimeline() {
        List<Tick> timeline = new ArrayList<>();
        Random rand = new Random(42);
        long startTime = 1000000; // Start at arbitrary time

        for (int s = 0; s < DURATION_SECONDS; s++) {
            long currentTime = startTime + (s * 1000);
            List<EventData> events = new ArrayList<>();

            for (int i = 0; i < EVENTS_PER_SEC; i++) {
                // Item
                String item = "item-" + rand.nextInt(NUM_ITEMS);
                int count = 1;

                // Timestamp
                long ts = currentTime + rand.nextInt(1000); // Within this second
                if (rand.nextDouble() < OUT_OF_ORDER_RATIO) {
                    // Late arrival: 1 to 10 seconds late
                    ts -= (1000 + rand.nextInt(10000));
                }

                events.add(new EventData(item, count, ts));
            }
            // Add a tick marker
            // We simulate that 'currentTime' is the end of this second
            timeline.add(new Tick(currentTime + 1000, events));
        }
        return timeline;
    }

    static class Tick {
        long currentTime;
        List<EventData> events;

        public Tick(long currentTime, List<EventData> events) {
            this.currentTime = currentTime;
            this.events = events;
        }
    }

    static class EventData {
        String item;
        int count;
        long timestamp;

        public EventData(String item, int count, long timestamp) {
            this.item = item;
            this.count = count;
            this.timestamp = timestamp;
        }
    }
}
