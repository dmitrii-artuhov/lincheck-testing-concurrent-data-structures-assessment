import java.util.concurrent.atomic.AtomicReference


// Note: no memory-allocation ABA problem possible in programming language with garbage collection,
// here no need to implement counters as stated in the paper (http://www.cs.rochester.edu/~scott/papers/1996_PODC_queues.pdf)
// explanation is here: https://stackoverflow.com/questions/42854116/why-does-automatic-garbage-collection-eliminate-aba-problems
// So we don't need `AtomicStampedReference` class because we are running CAS on `Node` addresses and not on `Node::element` values

class MSQueue<E> : Queue<E> {
    private val head: AtomicReference<Node<E>>
    private val tail: AtomicReference<Node<E>>

    init {
        val dummyNode = Node<E>(null)

        head = AtomicReference(dummyNode)
        tail = AtomicReference(dummyNode)
    }

    override fun enqueue(element: E) {
        val newNode: Node<E> = Node(element)

        while (true) {
            val currentTail: Node<E> = tail.get()
            val tailNext: Node<E>? = currentTail.next.get()

            if (currentTail == tail.get()) {
                if (tailNext != null) {
                    tail.compareAndSet(currentTail, tailNext)
                }
                else {
                    if (currentTail.next.compareAndSet(null, newNode)) {
                        tail.compareAndSet(currentTail, newNode)
                        return
                    }
                }
            }
        }
    }

    override fun dequeue(): E? {
        while (true) {
            val first: Node<E> = head.get()
            val last: Node<E> = tail.get()
            val next: Node<E>? = first.next.get()

            if (first == head.get()) {
                if (first == last) {
                    if (next == null) {
                        return null
                    }
                    tail.compareAndSet(last, next)
                }
                else {
                    val result: E? = next?.element
                    if (head.compareAndSet(first, next)) {
                        if (result != null) {
                            next.element = null
                        }
                        return result
                    }
                }
            }
        }
    }

    // FOR TEST PURPOSE, DO NOT CHANGE IT.
    override fun validate() {
        check(tail.get().next.get() == null) {
            "At the end of the execution, `tail.next` must be `null`"
        }
        check(head.get().element == null) {
            "At the end of the execution, the dummy node shouldn't store an element"
        }
    }

    private class Node<E>(
        var element: E?
    ) {
        val next = AtomicReference<Node<E>?>(null)
    }
}
