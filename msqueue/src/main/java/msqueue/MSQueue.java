package msqueue;

import kotlinx.atomicfu.*;

public class MSQueue implements Queue {

    private static class Node {
        final int x;
        AtomicRef<Node> next = new AtomicRef<>(null);

        Node(int x) {
            this.x = x;
        }
    }

    private AtomicRef<Node> head;
    private AtomicRef<Node> tail;

    public MSQueue() {
        Node initNode = new Node(0);
        this.head = new AtomicRef<>(initNode);
        this.tail = new AtomicRef<>(initNode);
    }

    @Override
    public void enqueue(int x) {
        AtomicRef<Node> newTail = new AtomicRef<>(new Node(x));
        AtomicRef<Node> currentTail;
        while (true) {
            currentTail = new AtomicRef<>(this.tail.getValue());
            AtomicRef<Node> tailNext = currentTail.getValue().next;

            if (currentTail.getValue() == this.tail.getValue()) {
                if (tailNext.getValue() == null) {
                    if (currentTail.getValue().next.compareAndSet(null, newTail.getValue())) {
                        break;
                    }
                } else {
                    this.tail.compareAndSet(currentTail.getValue(), tailNext.getValue());
                }
            }
        }
        this.tail.compareAndSet(currentTail.getValue(), newTail.getValue());
    }

    @Override
    public int dequeue() {
        while (true) {
            AtomicRef<Node> curHead = new AtomicRef<>(this.head.getValue());
            AtomicRef<Node> curTail = new AtomicRef<>(this.tail.getValue());
            AtomicRef<Node> headNext = curHead.getValue().next;
            if (curHead.getValue() == this.head.getValue()) {
                if (curHead.getValue() == curTail.getValue()) {
                    if (headNext.getValue() == null) {
                        return Integer.MIN_VALUE;
                    } else {
                        this.tail.compareAndSet(curTail.getValue(), curTail.getValue().next.getValue());
                    }
                } else {
                    if (this.head.compareAndSet(curHead.getValue(), headNext.getValue())) {
                        return headNext.getValue().x;
                    }
                }
            }
        }
    }

    @Override
    public int peek() {
        AtomicRef<Node> curHead = new AtomicRef<>(this.head.getValue());
        AtomicRef<Node> nextHead = curHead.getValue().next;
        AtomicRef<Node> curTail = new AtomicRef<>(this.tail.getValue());
        if (curHead.getValue() == curTail.getValue())
            return Integer.MIN_VALUE;
        return nextHead.getValue().x;
    }

    @Override
    public String toString() {
        AtomicRef<Node> head = new AtomicRef<>(this.head.getValue());
        AtomicRef<Node> tail = new AtomicRef<>(this.tail.getValue());
        StringBuffer sb = new StringBuffer("[");

        while (head.getValue() != tail.getValue()) {
            head.setValue(head.getValue().next.getValue());
            sb.append(head.getValue().x).append(", ");
        }

        sb.append("]");

        return sb.toString();
    }

}