/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package tarumtresort.adt;

/**
 *
 * @author Brian
 * @param <T>
 */
public interface LinkedListInterface<T extends Comparable<T>> {
    // LIST

    void addFront(T element);

    void addBack(T element);

    void addSorted(T element);

    T removeFront();

    T removeBack();

    T getFirst();

    T getLast();

    T get(int index);

    void addAtIndex(int index, T element);

    void set(int index, T element);

    boolean removeElement(T element);

    T removeIndex(int index);

    // HELPER

    boolean contains(T element);

    int indexOf(T element);

    int size();

    boolean isEmpty();

    void clear();
}
