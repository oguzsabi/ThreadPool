import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

enum ThreadState {
    IDLE,
    BUSY
}

public class ThreadPool {
    private static ThreadPool instance = null;
    private final AbstractAggregate threads = new ThreadCollection();
    private final AbstractIterator threadIterator = threads.CreateIterator();
    private static final Lock lock = new ReentrantLock();

    public static ThreadPool getThreadPool() {
        if (instance == null) {
            lock.lock();
            System.out.println("Acquired lock");
            try {
                if (instance == null)
                    instance = new ThreadPool();
            } finally {
                lock.unlock();
                System.out.println("Released lock");
            }
        }
        return instance;
    }

    // Simple load balancer
    public Thread getThread(int priority) {
        Thread selectedThread = null;

        for (threadIterator.First(); !threadIterator.IsDone(); threadIterator.Next()) {
            Thread currentThread = threadIterator.CurrentThread();
            if (currentThread.priority == priority) {
                currentThread.currentState = ThreadState.BUSY;
                lock.lock();
                selectedThread = threads.remove(currentThread);
                lock.unlock();
                break;
            }
        }

        if (selectedThread == null) {
            System.out.println("All priority: " + priority + " threads are busy.");
        }

        return selectedThread;
    }

    // Constructor (private).
    private ThreadPool() {
        HThreadFactory hThreadFactory = new HThreadFactory();
        LThreadFactory lThreadFactory = new LThreadFactory();

        threads.add(createThread(hThreadFactory));
        threads.add(createThread(hThreadFactory));
        threads.add(createThread(lThreadFactory));
        threads.add(createThread(hThreadFactory));
        threads.add(createThread(lThreadFactory));
        threads.add(createThread(lThreadFactory));
        threads.add(createThread(hThreadFactory));
        threads.add(createThread(lThreadFactory));
    }

    private Thread createThread(ThreadFactory threadFactory) {
        return threadFactory.createThread();
    }
}

abstract class ProcessingUnit {
    abstract public void setState(ThreadState state);
    abstract public ThreadState getState();
    abstract public int getPriority();
    abstract public int getMemory();
}

abstract class Thread extends ProcessingUnit {
    protected int priority;
    protected ThreadState currentState;
    protected int memory;

    @Override
    public void setState(ThreadState state) {
        currentState = state;
    }

    @Override
    public ThreadState getState() {
        return currentState;
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public int getMemory() {
        return priority;
    }
}

class HThread extends Thread {
    public HThread(int priority) {
        this.priority = priority;
        this.currentState = ThreadState.IDLE;
        this.memory = 512;
        System.out.println("HThread is created...");
    }
}

class LThread extends Thread {
    public LThread(int priority) {
        this.priority = priority;
        this.currentState = ThreadState.IDLE;
        this.memory = 256;
        System.out.println("LThread is created...");
    }
}

abstract class ThreadFactory {
    abstract public Thread createThread();
}

class HThreadFactory extends ThreadFactory {
    @Override
    public Thread createThread() {
        return new HThread(1);
    }
}

class LThreadFactory extends ThreadFactory {
    @Override
    public Thread createThread() {
        return new LThread(5);
    }
}

class MemoryManager {

}

class ThreadTable {

}

interface  AbstractIterator {
    void First();
    void Next();
    Boolean IsDone();
    Thread CurrentThread();
}

//
//This is the "concrete" Iterator for collection.
//		CollectionIterator
//

class CollectionIterator implements AbstractIterator {
    private final ThreadCollection threadCollection;
    private int current;

    public void First() {
        current = 0;
    }

    public void Next() {
        current++;
    }

    public Thread CurrentThread() {
        return IsDone() ? null : threadCollection.get(current);
    }

    public Boolean IsDone() {
        return current >= threadCollection.getCount();
    }

    public CollectionIterator(ThreadCollection threadCollection) {
        this.threadCollection = threadCollection;
        current = 0;
    }
}


//
//This is the abstract "Aggregate".
//			AbstractAggregate
//

interface AbstractAggregate {
    AbstractIterator CreateIterator();
    void add(Thread thread); // Not needed for iteration.
    Thread remove(Thread thread); // Not needed for iteration.
    int getCount(); // Needed for iteration.
    Thread get(int idx); // Needed for iteration.
}

//
//This is the concrete Aggregate.
//			Collection
//

class ThreadCollection implements AbstractAggregate {
    private final ArrayList<Thread> threads = new ArrayList<>();

    public CollectionIterator CreateIterator() {
        return new CollectionIterator(this);
    }

    public int getCount() {
        return threads.size();
    }

    public void add(Thread thread) {
        threads.add(thread);
    }

    public Thread remove(Thread thread) {
        threads.remove(thread);
        return thread;
    }

    public Thread get(int index) {
        return threads.get(index);
    }
}

class Main {
    public static void main(String[] args) {
        ThreadPool threadPool = ThreadPool.getThreadPool();

        threadPool.getThread(1);
        threadPool.getThread(5);
        threadPool.getThread(5);
        threadPool.getThread(1);
        threadPool.getThread(5);
        threadPool.getThread(1);
        threadPool.getThread(1);
        threadPool.getThread(5);
        threadPool.getThread(5);
        threadPool.getThread(1);
    }
}
