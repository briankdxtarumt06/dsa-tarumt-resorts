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

    boolean removeElement(T element);

    T removeIndex(int index);

    // QUEUE

    void enqueue(T element);

    T dequeue();

    // STACK

    void push(T element);

    T pop();

    T peek();

    // HELPER

    boolean contains(T element);

    int size();

    boolean isEmpty();

    void clear();
}
