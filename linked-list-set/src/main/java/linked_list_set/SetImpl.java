package linked_list_set;

import kotlinx.atomicfu.AtomicRef;

public class SetImpl implements Set {
    private static abstract class NodeParent {
        abstract int getX();
        abstract AtomicRef<NodeParent> nextNode();
    }

    public static class Node extends NodeParent {
        AtomicRef<NodeParent> next;
        int x;

        Node(int x, NodeParent next) {
            this.x = x;
            this.next = new AtomicRef<>(next);
        }

        @Override
        public int getX() {
            return x;
        }

        @Override
        public AtomicRef<NodeParent> nextNode() {
            return next;
        }
    }

    public static class RemovedNode extends NodeParent {
        final Node node;

        RemovedNode(Node node) {
            this.node = node;
        }

        @Override
        public int getX() {
            return node.getX();
        }

        NodeParent getNode() {
            return node;
        }

        @Override
        public AtomicRef<NodeParent> nextNode() {
            return node.nextNode();
        }
    }

    private static class Window {
        NodeParent cur, next;
    }

    private final NodeParent head = new Node(Integer.MIN_VALUE, new Node(Integer.MAX_VALUE, null));

    /**
     * Returns the {@link Window}, where cur.x < x <= next.x
     */
    private Window findWindow(int x) {
        nextIteration:
        while (true) {
            Window w = new Window();
            w.cur = head;
            w.next = w.cur.nextNode().getValue();

            while (w.next.getX() < x) {
                NodeParent node = w.next.nextNode().getValue();
                if (node instanceof RemovedNode) {
                    if (w.next instanceof Node && !w.cur.nextNode().compareAndSet(w.next, ((RemovedNode) node).getNode())) {
                        continue nextIteration;
                    }
                    w.next = ((RemovedNode) node).getNode();
                } else {
                    w.cur = w.next;
                    w.next = w.cur.nextNode().getValue();
                }
            }

            NodeParent node = w.next.nextNode().getValue();
            if (node instanceof Node || w.next.getX() == Integer.MAX_VALUE) {
                return w;
            }

            w.cur.nextNode().compareAndSet(w.next, ((RemovedNode) node).getNode());
        }
    }

    @Override
    public boolean add(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.next.getX() == x) {
                return false;
            }
            NodeParent node = new Node(x, w.next);
            if (w.next instanceof Node && w.cur.nextNode().compareAndSet(w.next, node)) {
                return true;
            }
        }
    }

    @Override
    public boolean remove(int x) {
        while (true) {
            Window w = findWindow(x);
            if (w.next.getX() != x) {
                return false;
            }
            NodeParent nextNext = w.next.nextNode().getValue();
            if (nextNext instanceof RemovedNode) {
                return false;
            }

            if (w.next.nextNode().compareAndSet(nextNext, new RemovedNode((Node) nextNext))) {
                w.cur.nextNode().compareAndSet(w.next, nextNext);
                return true;
            }
        }
    }

    @Override
    public boolean contains(int x) {
        Window w = findWindow(x);
        return w.next.getX() == x;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        AtomicRef<NodeParent> np = new AtomicRef<>(head);

        while (np.getValue() != null) {
            sb.append(np.getValue().getX()).append(", ");
            np = np.getValue().nextNode();
        }

        sb.append("]");
        return sb.toString();
    }
}