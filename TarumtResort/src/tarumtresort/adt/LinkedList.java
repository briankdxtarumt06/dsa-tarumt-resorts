/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package tarumtresort.adt;

import tarumtresort.entity.Node;

/**
 *
 * @author Brian
 * @param <T>
 */
public class LinkedList<T extends Comparable<T>> implements LinkedListInterface<T> {

    private Node<T> head;
    private Node<T> tail;
    private int size;

    // LIST

    @Override
    public void addFront(T element) {
        Node<T> newNode = new Node<>(element);
        if (isEmpty()) {
            head = tail = newNode;
        } else {
            newNode.setNext(head);
            head.setPrev(newNode);
            head = newNode;
        }
        size++;
    }

    @Override
    public void addBack(T element) {
        Node<T> newNode = new Node<>(element);
        if (isEmpty()) {
            head = tail = newNode;
        } else {
            tail.setNext(newNode);
            newNode.setPrev(tail);
            tail = newNode;
        }
        size++;
    }

    @Override
    public void addSorted(T element) {
        if (isEmpty() || element.compareTo(head.getData()) <= 0) {
            addFront(element);
            return;
        }
        Node<T> current = head;
        while (current.getNext() != null && element.compareTo(current.getNext().getData()) > 0) {
            current = current.getNext();
        }
        if (current.getNext() == null) {
            addBack(element);
            return;
        }
        Node<T> newNode = new Node<>(element);
        newNode.setNext(current.getNext());
        newNode.setPrev(current);
        current.getNext().setPrev(newNode);
        current.setNext(newNode);
        size++;
    }

    @Override
    public T removeFront() {
        if (isEmpty()) {
            return null;
        }
        T data = head.getData();
        if (size == 1) {
            head = tail = null;
        } else {
            head = head.getNext();
            head.setPrev(null);
        }
        size--;

        return data;
    }

    @Override
    public T removeBack() {
        if (isEmpty()) {
            return null;
        }
        T data = tail.getData();
        if (size == 1) {
            head = tail = null;
        } else {
            tail = tail.getPrev();
            tail.setNext(null);
        }
        size--;

        return data;
    }

    @Override
    public T getFirst() {
        return isEmpty() ? null : head.getData();
    }

    @Override
    public T getLast() {
        return isEmpty() ? null : tail.getData();
    }

    @Override
    public T get(int index) {
        if (index < 0 || index >= size) {
            return null;
        }
        Node<T> current;
        if (index < size / 2) {
            current = head;
            for (int i = 0; i < index; i++) {
                current = current.getNext();
            }
        } else {
            current = tail;
            for (int i = size - 1; i > index; i--) {
                current = current.getPrev();
            }
        }

        return current.getData();
    }

    @Override
    public boolean removeElement(T element) {
        if (isEmpty()) {
            return false;
        }
        Node<T> current = head;
        while (current != null) {
            if (element == null ? current.getData() == null : element.equals(current.getData())) {
                removeNode(current);
                return true;
            }
            current = current.getNext();
        }
        return false;
    }

    @Override
    public T removeIndex(int index) {
        if (index < 0 || index >= size) {
            return null;
        }
        Node<T> current;
        if (index < size / 2) {
            current = head;
            for (int i = 0; i < index; i++) {
                current = current.getNext();
            }
        } else {
            current = tail;
            for (int i = size - 1; i > index; i--) {
                current = current.getPrev();
            }
        }
        return removeNode(current);
    }

    // private helper method to remove node from list
    private T removeNode(Node<T> node) {
        if (size == 1) {
            head = tail = null;
        } else if (node == head) {
            head = head.getNext();
            head.setPrev(null);
        } else if (node == tail) {
            tail = tail.getPrev();
            tail.setNext(null);
        } else {
            node.getPrev().setNext(node.getNext());
            node.getNext().setPrev(node.getPrev());
        }
        size--;

        return node.getData();
    }

    // LIST END

    // QUEUE - sorted when enqueue

    @Override
    public void enqueue(T element) {
        addSorted(element);
    }

    @Override
    public T dequeue() {
        return removeFront();
    }

    // QUEUE END

    // STACK - unsorted when push

    @Override
    public void push(T element) {
        addFront(element);
    }

    @Override
    public T pop() {
        return removeFront();
    }

    @Override
    public T peek() {
        return getFirst();
    }

    // STACK END

    // HELPER

    @Override
    public boolean contains(T element) {
        Node<T> current = head;
        while (current != null) {
            if (element == null ? current.getData() == null : element.equals(current.getData())) {
                return true;
            }
            current = current.getNext();
        }
        return false;
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        Node<T> current = head;
        while (current != null) {
            sb.append(current.getData());
            if (current.getNext() != null) {
                sb.append(", ");
            }
            current = current.getNext();
        }
        return sb.append("]").toString();
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public void clear() {
        head = tail = null;
        size = 0;
    }

    // HELPER END
}
