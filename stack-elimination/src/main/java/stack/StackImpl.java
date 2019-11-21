package stack;

import kotlinx.atomicfu.AtomicArray;
import kotlinx.atomicfu.AtomicRef;

import java.util.Random;

public class StackImpl implements Stack {

    private static final int ELIMINATION_SIZE = 8;
    private static final int TIMEOUT = 50;
    private static final Random RANDOM = new Random();

    private static class MyInteger {
        private int value;
        private boolean isNull;
        private boolean isDone;

        private MyInteger(int value, boolean isNull, boolean isDone) {
            this.value = value;
            this.isNull = isNull;
            this.isDone = isDone;
        }
    }

    private static class Node {
        final AtomicRef<Node> next;
        final int x;

        Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }
    }

    // head pointer
    private AtomicRef<Node> head = new AtomicRef<>(null);

    // stack-elimination array
    private static final AtomicArray<MyInteger> eliminationArray = new AtomicArray<>(ELIMINATION_SIZE);

    static {
        for (int i = 0; i < ELIMINATION_SIZE; ++i) {
            eliminationArray.get(i).setValue(new MyInteger(0, true, false));
        }
    }

    @Override
    public void push(int x) {
        int cellAddress = pushEliminationOptimization(x, -1, -1, 3);
        if (cellAddress != -1) {
            int operationCounter = -1;
            while (++operationCounter < TIMEOUT) {
                if (eliminationArray.get(cellAddress).getValue().isDone) {
                    return;
                }
            }

            if (!eliminationArray.get(cellAddress).getAndSet(new MyInteger(0, false, false)).isNull) {
                pushImpl(x);
            }
        } else {
            pushImpl(x);
        }
    }

    private void pushImpl(int x) {
        while (true) {
            AtomicRef<Node> head = new AtomicRef<>(this.head.getValue());
            AtomicRef<Node> newHead = new AtomicRef<>(new Node(x, head.getValue()));
            if (CAS(this.head, head, newHead)) {
                return;
            }
        }
    }

    @Override
    public int pop() {
        MyInteger element = popEliminationOptimization(-1, -1, 3);
        if (!element.isNull) {
            return element.value;
        } else {
            return popImpl();
        }
    }

    private int popImpl() {
        while (true) {
            AtomicRef<Node> curHead = new AtomicRef<>(this.head.getValue());
            if (curHead.getValue() == null)
                return Integer.MIN_VALUE;

            AtomicRef<Node> nextNode = curHead.getValue().next;
            if (CAS(this.head, curHead, nextNode)) {
                return curHead.getValue().x;
            }
        }
    }

    private int pushEliminationOptimization(int x, int randomCell, int previous, int rest) {
        if (rest == 0)
            return -1;

        while (randomCell == previous)
            randomCell = RANDOM.nextInt(ELIMINATION_SIZE);

        if (eliminationArray.get(randomCell).compareAndSet(new MyInteger(x, true, false), new MyInteger(x, false, false))) {
            return randomCell;
        } else {
            return pushEliminationOptimization(x, randomCell, randomCell, rest - 1);
        }
    }

    private MyInteger popEliminationOptimization(int randomCell, int previous, int rest) {
        if (rest == 0)
            return new MyInteger(0, true, false);

        while (randomCell == previous)
            randomCell = RANDOM.nextInt(ELIMINATION_SIZE);

        MyInteger result = eliminationArray.get(randomCell).getAndSet(new MyInteger(0, true, true));
        if (result != null) {
            return result;
        } else {
            return popEliminationOptimization(randomCell, randomCell, rest - 1);
        }
    }

    private boolean CAS(AtomicRef<Node> headAddress, AtomicRef<Node> oldHead, AtomicRef<Node> newHead) {
        return headAddress.compareAndSet(oldHead.getValue(), newHead.getValue());
    }

    @Override
    public String toString() {
        AtomicRef<Node> head = new AtomicRef<>(this.head.getValue());
        StringBuffer sb = new StringBuffer("[");

        while (head.getValue() != null) {
            sb.append(head.getValue().x).append(", ");
            head.setValue(head.getValue().next.getValue());
        }

        sb.append("]");

        return sb.toString();
    }

}
