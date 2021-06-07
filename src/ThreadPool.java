import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

enum ThreadState {
    IDLE,
    BUSY
}

public class ThreadPool {
    private static ThreadPool instance = null;
    private static ThreadCreationProcess facade = null;
    private final AbstractAggregate threads = new ThreadCollection();
    private final AbstractIterator threadIterator = threads.CreateIterator();

    public static ThreadPool getThreadPool() {
        if (instance == null) {
            facade = new ThreadCreationProcess();
            instance = new ThreadPool();
        }
        return instance;
    }

    // Simple load balancer
    public Thread getThread(int priority, int memoryRequirement) {
        if (priority == 5 && memoryRequirement > 256) {
            System.out.println("Priority and memory requirement does not match. Changing priority to 1!");
            priority = 1;
        }
        else if (priority == 1 && memoryRequirement > 512) {
            System.out.println("Priority and memory requirement does not match. Changing memory requirement to 512MB!");
            memoryRequirement = 512;
        }

        Thread selectedThread = null;

        for (threadIterator.First(); !threadIterator.IsDone(); threadIterator.Next()) {
            Thread currentThread = threadIterator.CurrentThread();

            if (currentThread.priority == priority && currentThread.currentState == ThreadState.IDLE) {
                currentThread.currentState = ThreadState.BUSY;
                selectedThread = threads.remove(currentThread);
                selectedThread.memoryUse = memoryRequirement;
                break;
            }
        }

        if (selectedThread == null) {
            System.out.println("All priority: " + priority + " threads are busy.");
        }

        return selectedThread;
    }

    public void returnThread(Thread thread) {
        threads.add(thread);
    }

    // Constructor (private).
    private ThreadPool() {
        HThreadFactory hThreadFactory = new HThreadFactory();
        LThreadFactory lThreadFactory = new LThreadFactory();
        int numberOfHeavyThreads = 4;
        int numberOfLightThreads = 4;

        for (int i = 0; i < numberOfHeavyThreads; i++) {
            Thread newThread = createThread(hThreadFactory);

            if (newThread != null) {
                threads.add(newThread);
            }
        }

        for (int i = 0; i < numberOfLightThreads; i++) {
            Thread newThread = createThread(lThreadFactory);

            if (newThread != null) {
                threads.add(newThread);
            }
        }
    }

    private Thread createThread(ThreadFactory threadFactory) {
        Thread thread = threadFactory.createThread();
         // assign priority part can be inside facade as well.
        boolean canAddThreadToPool = facade.processThreadCreation(thread);

        if (canAddThreadToPool) {
            return thread;
        }

        return null;
    }
}

abstract class Processable {
    abstract public void setState(ThreadState state);
    abstract public void increaseMemoryUse(int memoryAmount);
    abstract public void decreaseMemoryUse(int memoryAmount);
    abstract public void resetMemoryUse(int memoryAmount);
    abstract public ThreadState getState();
    abstract public int getPriority();
    abstract public int getMemoryUse();
}

abstract class Thread extends Processable {
    protected int priority;
    protected ThreadState currentState;
    protected int maxMemory;
    protected int memoryUse;
    protected MemoryManager memoryManager = new MemoryManager();

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
    public int getMemoryUse() {
        return memoryUse;
    }

    @Override
    public void increaseMemoryUse(int memoryAmount) {
        try {
            if (memoryUse + memoryAmount <= maxMemory) {
                memoryUse += memoryAmount;
                memoryManager.recordMemoryChange(memoryAmount);
            } else {
                throw new MemoryException("Threads cannot exceed their maximum memory!");
            }
        } catch (MemoryException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void decreaseMemoryUse(int memoryAmount) {
        try {
            if (memoryUse - memoryAmount >= 0) {
                memoryUse -= memoryAmount;
                memoryManager.recordMemoryChange(-memoryAmount);
            } else {
                throw new MemoryException("Threads cannot have negative memory usage!");
            }
        } catch (MemoryException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void resetMemoryUse(int memoryAmount) {
        memoryUse = 0;
    }
}

class HThread extends Thread {
    public HThread(int priority) {
        this.priority = priority;
        this.currentState = ThreadState.IDLE;
        this.maxMemory = 512;
        this.memoryUse = 0;
        System.out.println("HThread is created...");
    }
}

class LThread extends Thread {
    public LThread(int priority) {
        this.priority = priority;
        this.currentState = ThreadState.IDLE;
        this.maxMemory = 256;
        this.memoryUse = 0;
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
    private static final int totalMaxMemory = 3072;
    private static int totalMemoryAllocated = 0;
    private static int totalMemoryUsed = 0;
    private static final int memoryLimitForLogging = 1024;
    private static final Logger logger = Logger.getLogger("ThreadMemoryLogger");

    public void recordMemoryChange(int memoryChange) {
        totalMemoryUsed += memoryChange;

        if (totalMemoryUsed > memoryLimitForLogging) {
            logMemoryLimitExceed();
        }
    }

    public void allocateMemory(int memorySize) throws MemoryException {
        if (totalMemoryAllocated + memorySize <= totalMaxMemory) {
            totalMemoryAllocated += memorySize;
        } else {
            throw new MemoryException("Total maximum memory limit exceeded!");
        }
    }

    private static void logMemoryLimitExceed() {
        try {
            // This block configure the logger with handler and formatter
            FileHandler fh = new FileHandler( System.getProperty("user.dir") + "/memory_manager.log");
            logger.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);

            // the following statement is used to log any messages
            logger.info("Memory Limit of " + memoryLimitForLogging + "MB is exceeded!");
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }
    }
}

// FACADE
class ThreadCreationProcess {
    private final MemoryManager memoryManager;
    private final ThreadTable threadTable;

    public ThreadCreationProcess() {
        memoryManager = new MemoryManager();
        threadTable = new ThreadTable();
    }

    public boolean processThreadCreation(Thread thread) {
        try {
            memoryManager.allocateMemory(thread.maxMemory);
            threadTable.createThreadTableEntry(thread);
            return true;
        } catch (MemoryException exception) {
            System.out.println(exception.getMessage());
            System.out.println("New thread will be destroyed...");
            return false;
        }
    }
}

class ThreadTable {
    private final AbstractAggregate threadTable;

    public ThreadTable() {
        this.threadTable = new ThreadCollection();
    }

    public void createThreadTableEntry(Thread thread) {
        switch (thread.priority) {
            case 1 -> System.out.println("Created a heavy thread table entry!");
            case 5 -> System.out.println("Created a light thread table entry!");
            default -> System.out.println("Created a thread table entry!");
        }

        threadTable.add(thread);
    }
}

interface AbstractIterator {
    void First();
    void Next();
    Boolean IsDone();
    Thread CurrentThread();
}

//This is the "concrete" Iterator for collection.
//		CollectionIterator

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

//This is the abstract "Aggregate".
//			AbstractAggregate

interface AbstractAggregate {
    AbstractIterator CreateIterator();
    void add(Thread thread); // Not needed for iteration.
    Thread remove(Thread thread); // Not needed for iteration.
    int getCount(); // Needed for iteration.
    Thread get(int idx); // Needed for iteration.
}

//This is the concrete Aggregate.
//			Collection

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

class MemoryException extends Exception {
    public MemoryException(String message) {
        super(message);
    }
}

class Main {
    public static void main(String[] args) {
        ThreadPool threadPool = ThreadPool.getThreadPool();

        // Tasks are using threads
        Thread hThread1 = threadPool.getThread(1, 260);
        hThread1.increaseMemoryUse(512);
        Thread lThread1 = threadPool.getThread(5, 150);
        Thread lThread2 = threadPool.getThread(5, 50);
        Thread hThread2 = threadPool.getThread(1, 450);
        hThread2.increaseMemoryUse(512);
        Thread lThread3 = threadPool.getThread(5, 11);
        lThread3.increaseMemoryUse(100);
        Thread hThread3 = threadPool.getThread(1, 389);
        Thread hThread4 = threadPool.getThread(1, 500);
        Thread lThread4 = threadPool.getThread(5, 200);
        threadPool.getThread(5, 250);
        threadPool.getThread(1, 400);

        // Tasks are returning threads after their jobs are done
        threadPool.returnThread(hThread1);
        threadPool.returnThread(hThread2);
        threadPool.returnThread(hThread3);
        threadPool.returnThread(hThread4);
        threadPool.returnThread(lThread1);
        threadPool.returnThread(lThread2);
        threadPool.returnThread(lThread3);
        threadPool.returnThread(lThread4);
    }
}
