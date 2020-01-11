import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.*

class SynchronousQueueMS<E> : SynchronousQueue<E> {
    private val head: AtomicReference<Node<E>>
    private val tail: AtomicReference<Node<E>>

    init {
        val dummy: Node<E> = Node(null, true)
        head = AtomicReference(dummy)
        tail = AtomicReference(dummy)
    }

    open class Node<T>(data: T?, val type: Boolean) {
        val next: AtomicReference<Node<T>?> = AtomicReference(null)
        val data = AtomicReference(data)
        var cont: Continuation<Unit>? = null
    }

    override suspend fun send(element: E) {
        val offer: Node<E> = Node(element, true)

        while (true) {
            var head: Node<E> = this.head.get()
            val tail: Node<E> = this.tail.get()

            if (head == tail || tail.type) {
                val next: Node<E>? = tail.next.get()
                if (tail == this.tail.get()) {
                    if (next != null) {
                        this.tail.compareAndSet(tail, next)
                    } else if (tail.next.compareAndSet(next, offer)) {
                        // если не получится, придет другая корутина и сделает это на строчку выше
                        this.tail.compareAndSet(tail, offer)

                        suspendCoroutine<Unit> { cont ->
                            offer.cont = cont
                        }

                        // попытка выкинуть себя же из очереди
                        head = this.head.get()
                        if (head.next.get() == offer) {
                            this.head.compareAndSet(head, offer)
                        }

                        return
                    }
                }
            } else {
                val next: Node<E>? = head.next.get()
                if (tail != this.tail.get() || head != this.head.get() || next == null) {
                    continue
                }

                if (next.cont != null && this.head.compareAndSet(head, next)) {
                    next.data.compareAndSet(null, element)
                    next.cont!!.resume(Unit)
                    return
                }
            }
        }
    }

    override suspend fun receive(): E {
        val offer: Node<E> = Node(null, false)

        while (true) {
            var head: Node<E> = this.head.get()
            val tail: Node<E> = this.tail.get()

            if (head == tail || !tail.type) {
                val next: Node<E>? = tail.next.get()
                if (tail == this.tail.get()) {
                    if (next != null) {
                        this.tail.compareAndSet(tail, next)
                    } else if (tail.next.compareAndSet(next, offer)) {
                        // если не получится, придет другая корутина и сделает это на строчку выше
                        this.tail.compareAndSet(tail, offer)

                        suspendCoroutine<Unit> { cont ->
                            offer.cont = cont
                        }

                        // попытка выкинуть себя же из очереди
                        head = this.head.get()
                        if (head.next.get() == offer) {
                            this.head.compareAndSet(head, offer)
                        }

                        return offer.data.get()!!
                    }
                }
            } else {
                val next: Node<E>? = head.next.get()
                if (tail != this.tail.get() || head != this.head.get() || next == null) {
                    continue
                }

                val elem = next.data.get()
                if (next.cont != null && this.head.compareAndSet(head, next) && elem != null) {
                    next.data.compareAndSet(elem, null)
                    next.cont!!.resume(Unit)
                    return elem!!
                }
            }
        }
    }
}
