import java.util.ArrayList;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

enum ThreadState {
    IDLE,
    BUSY
}

public class ThreadPool {
    private static ThreadPool instance = null;
    private final ArrayList<Thread> threads = new ArrayList<>();
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

        for (int index = 0; index < threads.size(); index++) {
            Thread currentThread = threads.get(index);
            if (currentThread.priority == priority) {
                currentThread.currentState = ThreadState.BUSY;
                selectedThread = threads.remove(index);
            }
        }

        if (selectedThread == null) {
            System.out.println("All threads are busy.");
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

class Main {
    public static void main(String[] args) {
        ThreadPool threadPool = ThreadPool.getThreadPool();

        Thread t1 = threadPool.getThread(1);
        System.out.println(t1);

        Thread t2 = threadPool.getThread(5);
        System.out.println(t2);
    }
}
