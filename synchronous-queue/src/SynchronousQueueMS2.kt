import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.*

class SynchronousQueueMS2<E> : SynchronousQueue<E> {
    private val head: AtomicReference<Node<E>>
    private val tail: AtomicReference<Node<E>>

    init {
        val dummy: Node<E> = Node()
        head = AtomicReference(dummy)
        tail = AtomicReference(dummy)
    }

    open class Node<T> {
        val next: AtomicReference<Node<T>?> = AtomicReference(null)
        var resumed = false
    }

    class Receiver<T>(continuation: Continuation<T>): Node<T>() {
        val cont: Continuation<T> = continuation
    }

    class Sender<T>(val data: T, continuation: Continuation<Unit>): Node<T>() {
        val cont: Continuation<Unit> = continuation
    }

    override suspend fun send(element: E) {
        var offer: Node<E>? = null

        suspendCoroutine<Unit> sc@ { cont ->
            offer = Sender(element, cont)

            while (true) {
                val head: Node<E> = this.head.get()
                val tail: Node<E> = this.tail.get()

                if (head == tail || tail is Sender<E>) {
                    val next: Node<E>? = tail.next.get()
                    if (tail == this.tail.get()) {
                        if (next != null) {
                            this.tail.compareAndSet(tail, next)
                        } else if (tail.next.compareAndSet(next, offer)) {
                            // если не получится, придет другая корутина и сделает это на строчку выше
                            this.tail.compareAndSet(tail, offer)

                            return@sc
                        }
                    }
                } else {
                    val next: Receiver<E>? = head.next.get() as Receiver<E>
                    if (tail != this.tail.get() || head != this.head.get() || next == null) {
                        continue
                    }

                    if (this.head.compareAndSet(head, next)) {
                        next.cont.resume(element)
                        cont.resume(Unit)
                    }
                }
            }
        }

        // попытка выкинуть себя же из очереди
        val head = this.head.get()
        if (head.next.get() == offer) {
            this.head.compareAndSet(head, offer)
        }
    }

    override suspend fun receive(): E {
        var offer: Node<E>? = null

        val result = suspendCoroutine<E> sc@ { cont ->
            offer = Receiver(cont)

            while (true) {
                val head: Node<E> = this.head.get()
                val tail: Node<E> = this.tail.get()

                if (head == tail || tail is Receiver<E>) {
                    val next: Node<E>? = tail.next.get()
                    if (tail == this.tail.get()) {
                        if (next != null) {
                            this.tail.compareAndSet(tail, next)
                        } else if (tail.next.compareAndSet(next, offer)) {
                            // если не получится, придет другая корутина и сделает это на строчку выше
                            if (this.tail.compareAndSet(tail, offer))

                            return@sc
                        }
                    }
                } else {
                    val next: Sender<E>? = head.next.get() as Sender<E>
                    if (tail != this.tail.get() || head != this.head.get() || next == null) {
                        continue
                    }

                    if (this.head.compareAndSet(head, next)) {
                        next.cont.resume(Unit)
                        cont.resume(next.data)
                    }
                }
            }
        }

        // попытка выкинуть себя же из очереди
        val head = this.head.get()
        if (head.next.get() == offer) {
            this.head.compareAndSet(head, offer)
        }

        return result
    }
}
