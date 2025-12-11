package heap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TreeMap;

public class TimeSeriesEvaluation {

    public static void main(String[] args) {
        System.out.println("Time Series Storage Evaluation (95% Sequential, 5% Out-of-Order)");
        System.out.println("----------------------------------------------------------------");

        // Testing different sizes. Limiting SortedList because O(N^2) behavior on 5%
        // inserts will be slow.
        int[] sizes = { 10000, 50000, 100000, 200000 };

        System.out.printf("%-10s %-20s %-20s %-20s%n", "N", "Structure", "Insert Time (ms)", "Extract Time (ms)");

        for (int n : sizes) {
            int[] data = generateTimeSeriesData(n, 0.05);

            // 1. Min Heap
            evaluateMinHeap(n, data);

            // 2. BST (TreeMap)
            evaluateBST(n, data);

            // 3. Sorted List (Binary Search)
            evaluateSortedList(n, data);

            System.out.println("----------------------------------------------------------------");
        }
    }

    private static int[] generateTimeSeriesData(int n, double outOfOrderRatio) {
        int[] data = new int[n];
        Random rand = new Random(42); // Fixed seed for reproducibility
        int currentMax = 0;

        for (int i = 0; i < n; i++) {
            if (rand.nextDouble() < outOfOrderRatio && i > 0) {
                // Out of order: generate a timestamp earlier than currentMax
                data[i] = rand.nextInt(currentMax);
            } else {
                // In order: increment
                data[i] = currentMax + 1;
                currentMax++;
            }
        }
        return data;
    }

    private static void evaluateMinHeap(int n, int[] data) {
        MinHeap<Integer> heap = new MinHeap<>();

        long startInsert = System.nanoTime();
        for (int val : data) {
            heap.insert(val);
        }
        long endInsert = System.nanoTime();

        long startExtract = System.nanoTime();
        while (!heap.isEmpty()) {
            heap.extractMin();
        }
        long endExtract = System.nanoTime();

        printResult(n, "Min Heap", (endInsert - startInsert), (endExtract - startExtract));
    }

    private static void evaluateBST(int n, int[] data) {
        // TreeMap handles duplicates by ... wait, TreeMap DOES NOT handle duplicates.
        // It overwrites values for same key.
        // For simulation, we can use a counter or just minor perturbations to make them
        // unique?
        // Or we can use <Integer, List<Integer>>?
        // The user asked for "Binary Search Tree".
        // Since we are simulating timestamps, unique timestamps are rare in high
        // frequency but possible.
        // However, for complexity analysis, the STRUCTURAL cost is what matters.
        // Optimally, I will use a tolerance strategy: if exists, add tiny delta?
        // Or better: Use a wrapper object or just accept uniqueness constraint for this
        // specific test
        // assuming the generator produces mostly unique things.
        // My generator: `rand.nextInt(currentMax)` might collide.
        // I will use a simple workaround: If collision, ignore? No, that changes N.
        // I'll use a `TreeMap<Integer, Integer>` counting occurrences, or just assume
        // distinct for now
        // by generating spread out numbers?
        // Actually, let's use a MultiSet approach simulation: TreeMap<Integer, Integer>
        // (Key -> Count).
        // BUT, that makes the tree smaller (fewer nodes).
        // BETTER: Use `TreeMap<Long, Integer>` and pack index into the key to ensure
        // uniqueness: (value << 32) | index.
        // This ensures the tree size is exactly N.

        TreeMap<Long, Integer> tree = new TreeMap<>();
        long startInsert = System.nanoTime();
        for (int i = 0; i < n; i++) {
            long uniqueKey = ((long) data[i] << 32) | i;
            tree.put(uniqueKey, data[i]);
        }
        long endInsert = System.nanoTime();

        long startExtract = System.nanoTime();
        while (!tree.isEmpty()) {
            tree.pollFirstEntry();
        }
        long endExtract = System.nanoTime();

        printResult(n, "BST (TreeMap)", (endInsert - startInsert), (endExtract - startExtract));
    }

    private static void evaluateSortedList(int n, int[] data) {
        ArrayList<Integer> list = new ArrayList<>(n);

        long startInsert = System.nanoTime();
        for (int val : data) {
            if (list.isEmpty() || val >= list.get(list.size() - 1)) {
                list.add(val);
            } else {
                // Binary search
                int pos = Collections.binarySearch(list, val);
                if (pos < 0) {
                    pos = -(pos) - 1;
                }
                list.add(pos, val); // This is the expensive O(N) shift
            }
        }
        long endInsert = System.nanoTime();

        long startExtract = System.nanoTime();
        // Emulate window slide: removing oldest (smallest)
        // In sorted list, smallest is at 0.
        // remove(0) is O(N).
        while (!list.isEmpty()) {
            list.remove(0);
        }
        long endExtract = System.nanoTime();

        printResult(n, "Sorted List", (endInsert - startInsert), (endExtract - startExtract));
    }

    private static void printResult(int n, String method, long insertNs, long extractNs) {
        System.out.printf("%-10d %-20s %-20d %-20d%n",
                n,
                method,
                insertNs / 1_000_000,
                extractNs / 1_000_000);
    }
}
