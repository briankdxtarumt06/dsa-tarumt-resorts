package tarumtresort.adt;

import java.util.Iterator;
import tarumtresort.entity.Node;

// Author: Brian Kam Ding Xian, Chai Chee Tong, Fong Wen Ling, Imam Mahdi Ali Ang Attuko, Lee Boon Yew
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
        return removeNode(head);
    }

    @Override
    public T removeBack() {
        if (isEmpty()) {
            return null;
        }
        return removeNode(tail);
    }

    @Override
    public T getFront() {
        return isEmpty() ? null : head.getData();
    }

    @Override
    public T getBack() {
        return isEmpty() ? null : tail.getData();
    }

    @Override
    public T get(int index) {
        if (index < 0 || index >= size) {
            return null;
        }
        return nodeAt(index).getData();
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
        return removeNode(nodeAt(index));
    }

    // combine 2 sorted list into 1 sorted list
    @Override
    public boolean merge(LinkedListInterface<T> other) {
        if (other == null || other.isEmpty() || !isSorted() || !other.isSorted()) {
            return false;
        }
        for (int i = 0; i < other.size(); i++) {
            addSorted(other.get(i));
        }
        return true;
    }

    @Override
    public void addAtIndex(int index, T element) {
        if (index < 0 || index > size) {
            return;
        }
        if (index == 0) {
            addFront(element);
            return;
        }
        if (index == size) {
            addBack(element);
            return;
        }
        Node<T> prev = nodeAt(index - 1);
        Node<T> newNode = new Node<>(element);
        newNode.setNext(prev.getNext());
        newNode.setPrev(prev);
        prev.getNext().setPrev(newNode);
        prev.setNext(newNode);
        size++;
    }

    @Override
    public void set(int index, T element) {
        if (index < 0 || index >= size) {
            return;
        }
        nodeAt(index).setData(element);
    }

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
    public int indexOf(T element) {
        Node<T> current = head;
        int index = 0;
        while (current != null) {
            if (element == null ? current.getData() == null : element.equals(current.getData())) {
                return index;
            }
            current = current.getNext();
            index++;
        }
        return -1;
    }

    // condition to check if list is sorted or not
    // reason: list can become unsorted if addFront or addBack is called
    @Override
    public boolean isSorted() {
        Node<T> current = head;
        while (current != null && current.getNext() != null) {
            if (current.getData().compareTo(current.getNext().getData()) > 0) {
                return false;
            }
            current = current.getNext();
        }
        return true;
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

    // private helper method to get node at index
    private Node<T> nodeAt(int index) {
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
        return current;
    }

    // convert list to string
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

    // iterator to iterate through list
    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {
            private Node<T> current = head;

            @Override
            public boolean hasNext() {
                return current != null;
            }

            @Override
            public T next() {
                T data = current.getData();
                current = current.getNext();
                return data;
            }
        };
    }
}
