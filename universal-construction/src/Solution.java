/**
 * 2019-10-21 : 01:26
 *
 * @author Nikita Savinov
 */

public class Solution implements AtomicCounter {
    private final Node root = new Node(0);
    private final ThreadLocal<Node> last = ThreadLocal.withInitial(() -> root);

    private static class Node {
        private final int value;
        private final Consensus<Node> next = new Consensus<>();

        private Node(int x) {
            this.value = x;
        }
    }

    public int getAndAdd(int x) {
        Node node;
        int res;

        do {
            res = last.get().value;
            node = new Node(res + x);
            last.set(last.get().next.decide(node));
        } while (node != last.get());

        return res;
    }
}
