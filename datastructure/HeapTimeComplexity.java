package datastructure;

public class HeapTimeComplexity {
    public static void main(String[] args) {
        demonstrateInsertionComplexity(); // Keeps the original sorted test
        // demoReverse() // Commented out to focus on the new request if desired, or
        // keep both.
        // User said "Lets keep this to sorted order", so I will focus on the
        // comparison.
        System.out.println();
        demonstrateLinkedListComparison();
        System.out.println();
        demonstrateExtractMinComplexity();
        System.out.println();
        demonstrateArrayListExtractComparison();
    }

    private static void demonstrateInsertionComplexity() {
        System.out.println("Demonstrating Min Heap Insertion Time Complexity with Sorted Input");
        System.out.println("-----------------------------------------------------------------");
        System.out.printf("%-15s %-15s %-15s %-20s%n", "N (Elements)", "Time (ms)", "Total Shifts",
                "Avg Time/Insert (ns)");

        int[] sizes = { 100000, 500000, 1000000, 2000000, 5000000 };

        for (int n : sizes) {
            runTest(n, true);
        }
    }

    // Helper to keep the existing runTest or I can refactor.
    // I'll keep runTest for generic MinHeap testing.
    private static void runTest(int n, boolean sorted) {
        MinHeap<Integer> minHeap = new MinHeap<>();
        long startTime = System.nanoTime();
        long totalShifts = 0;

        for (int i = 0; i < n; i++) {
            int val = sorted ? i : (n - i);
            totalShifts += minHeap.insertAndCountSwaps(val);
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1000000;
        double avgTimeNs = (double) (endTime - startTime) / n;

        System.out.printf("%-15d %-15d %-15d %-20.2f%n", n, durationMs, totalShifts, avgTimeNs);
    }

    private static void demonstrateLinkedListComparison() {
        System.out.println("Comparing Min Heap vs LinkedList Insertion (Sorted Input)");
        System.out.println("-----------------------------------------------------------------");
        System.out.printf("%-15s %-20s %-20s%n", "N (Elements)", "MinHeap Time (ms)", "LinkedList Time (ms)");

        int[] sizes = { 100000, 500000, 1000000, 2000000, 5000000 };

        for (int n : sizes) {
            long heapTime = runTestReturnTime(n);
            long listTime = runLinkedListTest(n);
            System.out.printf("%-15d %-20d %-20d%n", n, heapTime, listTime);
        }
    }

    private static void demonstrateExtractMinComplexity() {
        System.out.println();
        System.out.println("Demonstrating Min Heap ExtractMin Time Complexity (After Sorted Insert)");
        System.out.println("-----------------------------------------------------------------------");
        System.out.printf("%-15s %-15s %-15s %-20s%n", "N (Elements)", "Time (ms)", "Total Shifts",
                "Avg Time/Extract (ns)");

        // Using slightly smaller sizes to ensure consistent run times if needed, but 5M
        // is fine for O(N log N)
        int[] sizes = { 100000, 500000, 1000000, 2000000, 5000000 };

        for (int n : sizes) {
            runExtractMinTest(n);
        }
    }

    private static void runExtractMinTest(int n) {
        MinHeap<Integer> minHeap = new MinHeap<>();
        // Pre-populate
        for (int i = 0; i < n; i++) {
            minHeap.insert(i);
        }

        long startTime = System.nanoTime();
        long totalSwaps = 0;

        for (int i = 0; i < n; i++) {
            totalSwaps += minHeap.extractMinAndCountSwaps();
        }

        long endTime = System.nanoTime();
        long durationMs = (endTime - startTime) / 1000000;
        double avgTimeNs = (double) (endTime - startTime) / n;

        System.out.printf("%-15d %-15d %-15d %-20.2f%n", n, durationMs, totalSwaps, avgTimeNs);
    }

    private static void demonstrateArrayListExtractComparison() {
        System.out.println();
        System.out.println("Comparing Min Heap vs ArrayList Extraction (Removing 0-th element)");
        System.out.println("------------------------------------------------------------------");
        System.out.printf("%-15s %-20s %-20s%n", "N (Elements)", "MinHeap Time (ms)", "ArrayList Time (ms)");

        // Reduced sizes for ArrayList because O(N^2) total time
        int[] sizes = { 10000, 50000, 100000, 150000 };

        for (int n : sizes) {
            long heapTime = runExtractMinTestReturnTime(n);
            long arrayListTime = runArrayListExtractionTest(n);
            System.out.printf("%-15d %-20d %-20d%n", n, heapTime, arrayListTime);
        }
    }

    private static long runExtractMinTestReturnTime(int n) {
        MinHeap<Integer> minHeap = new MinHeap<>();
        for (int i = 0; i < n; i++) {
            minHeap.insert(i);
        }
        long startTime = System.nanoTime();
        while (!minHeap.isEmpty()) {
            minHeap.extractMin();
        }
        return (System.nanoTime() - startTime) / 1000000;
    }

    private static long runArrayListExtractionTest(int n) {
        java.util.ArrayList<Integer> list = new java.util.ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            list.add(i);
        }

        long startTime = System.nanoTime();
        // Emulate "extract min" by removing the first element (which is the min in a
        // sorted list)
        while (!list.isEmpty()) {
            list.remove(0);
        }
        return (System.nanoTime() - startTime) / 1000000;
    }

    private static long runTestReturnTime(int n) {
        MinHeap<Integer> minHeap = new MinHeap<>();
        long startTime = System.nanoTime();
        for (int i = 0; i < n; i++) {
            minHeap.insert(i);
        }
        return (System.nanoTime() - startTime) / 1000000;
    }

    private static long runLinkedListTest(int n) {
        java.util.LinkedList<Integer> list = new java.util.LinkedList<>();
        long startTime = System.nanoTime();
        for (int i = 0; i < n; i++) {
            list.add(i);
        }
        return (System.nanoTime() - startTime) / 1000000;
    }
}
