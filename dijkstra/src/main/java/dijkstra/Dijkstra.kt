package dijkstra

import java.util.*
import java.util.concurrent.Phaser
import java.util.concurrent.atomic.AtomicInteger
import kotlin.Comparator
import kotlin.concurrent.thread

private val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> Integer.compare(o1!!.distance, o2!!.distance) }

private class MultiPriorityQueue(val workers: Int) {
    val list = ArrayList<PriorityQueue<Node>>()
    val random : Random = Random()

    init {
        for (i in 1..workers) {
            list.add(PriorityQueue(NODE_DISTANCE_COMPARATOR))
        }
    }

    fun add(node: Node) {
        val i : Int = random.nextInt(workers)
        synchronized(list.get(i)) {
            list.get(i).add(node)
        }
    }

    fun poll() : Node? {
        var i : Int = random.nextInt(workers)
        var j : Int = random.nextInt(workers)
        if (i > j) {
            val c = i
            i = j
            j = c
        }
        synchronized(list.get(i)) {
            synchronized(list.get(j)) {
                val iIsEmpty : Boolean = list.get(i).isEmpty()
                val jIsEmpty : Boolean = list.get(j).isEmpty()
                if (!iIsEmpty && !jIsEmpty) {
                    return if (list.get(i).peek().distance < list.get(j).peek().distance) {
                        list.get(i).poll()
                    } else {
                        list.get(j).poll()
                    }
                } else {
                    if (iIsEmpty && jIsEmpty)
                        return null

                    if (jIsEmpty)
                        return list.get(i).poll()

                    return list.get(j).poll()
                }
            }
        }
    }
}

// Returns `Integer.MAX_VALUE` if a path has not been found.
fun shortestPathParallel(start: Node) {
    val workers = Runtime.getRuntime().availableProcessors()
    // The distance to the start node is `0`
    start.distance = 0
    // Create a priority (by distance) queue and add the start node into it
    val workIsDone = AtomicInteger(1);
    val q = MultiPriorityQueue(workers)
    q.add(start)
    // Run worker threads and wait until the total work is done
    val onFinish = Phaser(workers + 1) // `arrive()` should be invoked at the end by each worker
    repeat(workers) {
        thread {
            while (true) {
                val cur: Node = q.poll() ?: if (workIsDone.compareAndSet(0, 0)) break else continue

                for (e in cur.outgoingEdges) {
                    while (true) {
                        val curDis = e.to.distance
                        val newDis = cur.distance + e.weight
                        if (curDis > newDis) {
                            if (e.to.casDistance(curDis, newDis)) {
                                q.add(e.to)
                                workIsDone.getAndIncrement()
                                break
                            }
                        } else {
                            break
                        }
                    }
                }
                workIsDone.getAndDecrement();
            }
            onFinish.arrive()
        }
    }
    onFinish.arriveAndAwaitAdvance()
}