package heap;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class MinHeap<T extends Comparable<T>> {
    private List<T> heap;

    public MinHeap() {
        this.heap = new ArrayList<>();
    }

    public void insert(T val) {
        heap.add(val);
        siftUp(heap.size() - 1);
    }

    public T extractMin() {
        if (heap.isEmpty()) {
            throw new NoSuchElementException("Heap is empty");
        }
        T min = heap.get(0);
        T last = heap.remove(heap.size() - 1);
        if (!heap.isEmpty()) {
            heap.set(0, last);
            siftDown(0);
        }
        return min;
    }

    public T peek() {
        if (heap.isEmpty()) {
            throw new NoSuchElementException("Heap is empty");
        }
        return heap.get(0);
    }

    public int size() {
        return heap.size();
    }

    public boolean isEmpty() {
        return heap.isEmpty();
    }

    // Returns true if a swap occurred, false otherwise.
    // This is useful for analyzing complexity.
    // Returns true if a shift occurred, false otherwise.
    private boolean siftUp(int index) {
        boolean shifted = false;
        T target = heap.get(index);
        while (index > 0) {
            int parentIndex = (index - 1) / 2;
            T parent = heap.get(parentIndex);
            if (target.compareTo(parent) < 0) {
                heap.set(index, parent);
                index = parentIndex;
                shifted = true;
            } else {
                break;
            }
        }
        heap.set(index, target);
        return shifted;
    }

    private int siftDown(int index) {
        int size = heap.size();
        int shifts = 0;
        T target = heap.get(index);
        while (true) {
            int leftChild = 2 * index + 1;
            int rightChild = 2 * index + 2;
            int smallestChild = -1;

            // Find smaller child
            if (leftChild < size) {
                smallestChild = leftChild;
                if (rightChild < size && heap.get(rightChild).compareTo(heap.get(leftChild)) < 0) {
                    smallestChild = rightChild;
                }
            }

            if (smallestChild != -1 && heap.get(smallestChild).compareTo(target) < 0) {
                heap.set(index, heap.get(smallestChild));
                index = smallestChild;
                shifts++;
            } else {
                break;
            }
        }
        heap.set(index, target);
        return shifts;
    }

    private void swap(int i, int j) {
        T temp = heap.get(i);
        heap.set(i, heap.get(j));
        heap.set(j, temp);
    }

    // Helper for demonstration
    public int getLastSiftUpSwaps() {
        return 0;
    }

    // Overloaded insert for the purpose of the test to return shift count
    // (equivalent to swaps in overhead roughly)
    public int insertAndCountSwaps(T val) {
        heap.add(val);
        int shifts = 0;
        int index = heap.size() - 1;
        T target = heap.get(index);

        while (index > 0) {
            int parentIndex = (index - 1) / 2;
            T parent = heap.get(parentIndex);
            if (target.compareTo(parent) < 0) {
                heap.set(index, parent);
                index = parentIndex;
                shifts++;
            } else {
                break;
            }
        }
        heap.set(index, target);
        return shifts;
    }

    public int extractMinAndCountSwaps() {
        if (heap.isEmpty()) {
            throw new NoSuchElementException("Heap is empty");
        }
        // Remove logic
        T last = heap.remove(heap.size() - 1);
        int swaps = 0;
        if (!heap.isEmpty()) {
            heap.set(0, last);
            swaps = siftDown(0);
        }
        return swaps;
    }

    public static void main(String[] args) {
        MinHeap<Integer> minHeap = new MinHeap<>();
        minHeap.insert(1);
        minHeap.insert(3);
        minHeap.insert(8);
        minHeap.insert(2);
        minHeap.insert(4);
        System.out.println(minHeap.extractMin()); // 1
        System.out.println(minHeap.extractMin()); // 2
        System.out.println(minHeap.extractMin()); // 3
        System.out.println(minHeap.extractMin()); // 4
        System.out.println(minHeap.extractMin()); // 8
    }
}
