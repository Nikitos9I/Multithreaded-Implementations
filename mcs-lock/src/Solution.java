import java.util.concurrent.atomic.AtomicReference;

/**
 * 2019-10-27 : 12:19
 *
 * @author Nikita Savinov
 */

public class Solution implements Lock<Solution.Node> {

    private final Environment env;
    private final AtomicReference<Node> tail;

    public Solution(Environment env) {
        this.env = env;
        this.tail = new AtomicReference<>(null);
    }

    @Override
    public Node lock() {
        Node my = new Node(); // сделали узел
        // залочились, чтобы ждать, когда предыдущий поток нас разлочит
        my.lock.set(true);
        Node pred = tail.getAndSet(my);
        // проверка, что мы не самый первый поток
        if (pred != null) {
            pred.next.set(my);
            // ждем, пока предыдущий поток разлочит нас
            while (my.lock.get()) env.park();
        }

        return my; // вернули узел
    }

    @Override
    public void unlock(Node node) {
        if (node.next.get() == null) {
            if (tail.compareAndSet(node, null))
                // Если мы последние в очереди
                return;
            else
                // Если мы застряли в локе между получением элемента и деланием его некстом к предыдущему
                while (node.next.get() == null);
        }
        node.next.get().lock.set(false);
        env.unpark(node.next.get().thread);
    }

    static class Node {
        final Thread thread = Thread.currentThread(); // запоминаем поток, которые создал узел
        final AtomicReference<Boolean> lock = new AtomicReference<>(false);
        final AtomicReference<Node> next = new AtomicReference<>(null);
    }

}
