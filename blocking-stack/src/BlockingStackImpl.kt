import java.util.concurrent.atomic.*
import kotlin.collections.ArrayList
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import java.util.*

class BlockingStackImpl<E> : BlockingStack<E> {

    // ==========================
    // Segment<E> Queue Synchronizer
    // ==========================

    private val SEGMENT_SIZE = 3
    private val head: AtomicReference<Segment<E>>
    private val tail: AtomicReference<Segment<E>>

    init {
        val dummy: Segment<E> = Segment<E>()
        head = AtomicReference<Segment<E>>(dummy)
        tail = AtomicReference<Segment<E>>(dummy)
    }

    class Segment<E>() {
        private val SEGMENT_SIZE = 3
        private val data: AtomicReferenceArray<Any?> = AtomicReferenceArray(SEGMENT_SIZE)
        
        open val next: AtomicReference<Segment<E>?> = AtomicReference(null)
        open val enqIdx = AtomicInteger(0)
        open val deqIdx = AtomicInteger(0)
        
        constructor(cont: Continuation<E>) : this() {
            enqIdx.getAndIncrement()
            data.getAndSet(0, cont)
        }

        fun get(index: Int): Any? = data.get(index)
        fun cas(index: Int, expected: Any?, value: Any?): Boolean = data.compareAndSet(index, expected, value)
        fun getAndSet(index: Int, value: Any?) = data.getAndSet(index, value)

        fun isEmpty(): Boolean {
            return deqIdx.get() >= enqIdx.get() || deqIdx.get() >= SEGMENT_SIZE
        }
    }

    private suspend fun suspend() = suspendCoroutine<E> sc@ { cont ->
        while (true) {
            var tail: Segment<E> = this.tail.get()
            val enqIdx = tail.enqIdx.getAndIncrement()
            if (enqIdx > SEGMENT_SIZE)
                continue
            if (enqIdx == SEGMENT_SIZE) {
                val newTail = Segment<E>(cont)
                while (true) {
                    tail = this.tail.get()
                    val tailNext: AtomicReference<Segment<E>?> = tail.next

                    if (tail === this.tail.get()) {
                        if (tailNext.get() == null) {
                            if (tail.next.compareAndSet(null, newTail)) {
                                break
                            }
                        } else {
                            this.tail.compareAndSet(tail, tailNext.get())
                        }
                    }
                }

                this.tail.compareAndSet(tail, newTail)
                return@sc
            } else {
                if (tail.get(enqIdx) === RESUMED || !tail.cas(enqIdx, null, cont)) {
                    continue
                }

                return@sc
            }
        }
    }

    private fun resume(element: E) {
        while (true) {
            val head = this.head.get()

            if (head.isEmpty()) {
                val headNext: Segment<E> = head.next.get() ?: continue
                this.head.compareAndSet(head, headNext)
            } else {
                val deqIdx = head.deqIdx.getAndIncrement()
                if (deqIdx >= SEGMENT_SIZE) {
                    continue;
                }

                val cont = head.getAndSet(deqIdx, RESUMED) ?: continue

                (cont as Continuation<E>).resume(element)
                return
            }
        }
    }

    // ==============
    // Blocking Stack
    // ==============

    private val stackHead = AtomicReference<Node<E>?>(null)
    private val elements = AtomicInteger()

    override fun push(element: E) {
        val elements = this.elements.getAndIncrement()
        if (elements >= 0) {
            while (true) {
                val curHead = this.stackHead.get()
                val newHead = Node<E>(element, curHead)

                if (curHead?.element === SUSPENDED && this.stackHead.compareAndSet(curHead, curHead.next)) {
                    resume(element)
                    return
                }
                
                if (this.stackHead.compareAndSet(curHead, newHead)) {
                    return
                }
            }
        } else {
            resume(element)
        }
    }

    override suspend fun pop(): E {
        val elements = this.elements.getAndDecrement()
        if (elements > 0) {
            while (true) {
                if (this.stackHead.compareAndSet(null, Node<E>(SUSPENDED, null))) {
                    return suspend() as E
                }
                
                val curHead = this.stackHead.get()
                if (curHead?.element === SUSPENDED && this.stackHead.compareAndSet(curHead, Node<E>(SUSPENDED, curHead))) {
                    return suspend() as E
                }
                
                val next = curHead?.next

                if (curHead != null && this.stackHead.compareAndSet(curHead, next))
                    return curHead?.element as E
            }
        } else {
            return suspend() as E
        }
    }
}

private class Node<E>(val element: Any?, val next: Node<E>?)

private val SUSPENDED = Any()
private val RESUMED = Any()
private val DONE = Any()