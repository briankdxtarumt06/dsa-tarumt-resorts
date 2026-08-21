package tarumtresort.adt;

// Author: Brian Kam Ding Xian, Chai Chee Tong, Fong Wen Ling, Imam Mahdi Ali Ang Attuko, Lee Boon Yew
public interface LinkedListInterface<T extends Comparable<T>> extends Iterable<T> {
    // LIST

    void addFront(T element);

    void addBack(T element);

    void addSorted(T element);

    T removeFront();

    T removeBack();

    T getFront();

    T getBack();

    T get(int index);

    void addAtIndex(int index, T element);

    void set(int index, T element);

    boolean removeElement(T element);

    T removeIndex(int index);

    boolean merge(LinkedListInterface<T> other);

    // HELPER

    boolean contains(T element);

    int indexOf(T element);

    boolean isSorted();

    int size();

    boolean isEmpty();

    void clear();
}
