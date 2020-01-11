import java.util.concurrent.atomic.*

class BlockingStackImpl<E> : BlockingStack<E> {

    init {
        val firstNode = Node<Any>()
        head = AtomicReference(firstNode)
        tail = AtomicReference(firstNode)
    }

    // ==========================
    // Internal functions
    // ==========================

    private fun getSegment(val seg: Segment, val segPosition: Int) {
        var o = -1
        var curSeg: Segment? = seg
        var prevSeg: Segment? = null

        while (++o < segPosition) {
            prevSeg = curSeg
            curSeg = curSeg.next
        }

        if (curSeg == null) {
            prevSeg.next.compareAndSet(null, Segment())
            return prevSeg.next.get()
        }

        return curSeg
    }

    private fun getSegmentAndMoveHead(val seg: Segment, val segPosition: Int) {
        if ()
    }

    // ==========================
    // Segment Queue Synchronizer
    // ==========================

    private val SEGMENT_SIZE = 16

    private val enqIdx = AtomicLong()
    private val deqIdx = AtomicLong()

    private val head: AtomicRef<Node>? = null
    private val tail: AtomicRef<Node>? = null

    class Segment() {
        private val data: AtomicArray<Any?> = AtomicArray(SEGMENT_SIZE)
        val next: AtomicReference<Segment?> = AtomicReference(null)

        fun get(index: Int): Any? = data[index].value
        fun cas(index: Int, expected: Any?, value: Any?): Boolean = data[index].compareAndSet(expected, value)
        fun getAndSet(index: Int, value: Any?) = data[index].getAndSet(value)
    }

    private suspend fun suspend2() = suspendCoriutine<E> sc@ { cont ->
        val last = this.head.get()
        val enqIdx = enqIdx.getAndIncrement()
        val segment = getSegment(last, enqIdx / SEGMENT_SIZE)
        val i = (enqIdx % SEGMENT_SIZE).toInt()
        if (segment === null || segment.get(i) === RESUMED || !segment.cas(i, null, cont)) {
            cont.resume(Unit)
            return@sc
        }
    }

    private fun resume2(element: E) {
        try_again@while (true) {
            val curHead = this.head.get()
            val deqIdx = deqIdx.getAndIncrement()
            val segment = getSegmentAndMoveHead(curHead, deqIdx / SEGMENT_SIZE) ?: continue@try_again
            val i = (deqIdx % SEGMENT_SIZE).toInt()
            val cont = segment.getAndSet(i, RESUMED)

            if (cont === null) return
            cont.resume(Unit)
            return
        }
    }

    private suspend fun suspend() = suspendCoroutine<Any> sc@ { cont ->
        while (true) {
            var tail: Segment = this.tail.get()
            val enqIdx = tail.enqIdx.getAndIncrement()
            if (enqIdx > SEGMENT_SIZE)
                continue
            if (enqIdx == SEGMENT_SIZE) {
                val newTail = Segment()
                newTail.cas(0, null, cont) // cas отработает ?
                while (true) {
                    tail = this.tail.get()
                    val tailNext: AtomicReference<Segment?> = tail.next

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
//                    println("Suspend: " + System.currentTimeMillis())
                    cont.resume(Unit)
                    return@sc
                }
            }
        }
    }

    private fun resume(element: E) {
        while (true) {
            val head = this.head.get()

            if (head.isEmpty()) {
//                println("Resume1: " + System.currentTimeMillis())
                val headNext: Segment = head.next.get() ?: return
                this.head.compareAndSet(head, headNext)
            } else {
                val deqIdx: Int = head.deqIdx.getAndIncrement()
                if (deqIdx >= SEGMENT_SIZE) {
                    continue
                }

                val res = head.getAndSet(deqIdx, RESUMED) ?: return
//                println("Resume2: " + System.currentTimeMillis())
                (res as Continuation<E>).resume(element)
            }
        }
    }

    private suspend fun suspend1() = suspendCoroutine<Any> sc@ { cont ->
        var tail: Segment<E> = this.tail.get()
        val enqIdx = tail.enqIdx.getAndIncrement()

        if (tail.get(enqIdx) === RESUMED || !tail.cas(enqIdx, null, cont)) {
            cont.resume(Unit)
            return@sc
        }

        return@sc
    }

    private fun resume1(element: E) {
        val head = this.head.get()
        val deqIdx = head.deqIdx.getAndIncrement()

        while (head.get(deqIdx) === null) {}
        val cont = head.getAndSet(deqIdx, RESUMED)

        (cont as Continuation<E>).resume(element)
    }

    // ==============
    // Blocking Stack
    // ==============

    private val head = AtomicReference<Segment<E>?>()
    private val elements = AtomicInteger()

    override fun push(element: E) {
        val elements = this.elements.getAndIncrement()
        if (elements >= 0) {
            // push the element to the top of the stack
            TODO("implement me")
        } else {
            // resume the next waiting receiver
            resume(element)
        }
    }

    override suspend fun pop(): E {
        val elements = this.elements.getAndDecrement()
        if (elements > 0) {
            // remove the top element from the stack
            TODO("implement me")
        } else {
            return suspend()
        }
    }
}

private class Node<E>() {
    private val next: Node<E>?
    private val data: AtomicArray<Any?> = AtomicArray(SEGMENT_SIZE)

    private val NODE_SIZE = 16

    private val enqIdx = AtomicLong()
    private val deqIdx = AtomicLong()

    init(val element: Any?) {
        enqIdx.setValue(1);
        data.get(0).setValue(x);
    }

    private fun isEmpty(): Boolean {
        return deqIdx.getValue() >= enqIdx.getValue() || deqIdx.getValue() >= NODE_SIZE
    }
}

private val SUSPENDED = Any()