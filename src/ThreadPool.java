import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

// Utku Işıl
// Doruk Maltepe
// Avni Yunus Demirel
// Oğuz Sabitay
// Thread Pool

// Enum used for thread states instead of String
enum ThreadState {
    IDLE,
    BUSY
}

public class ThreadPool {
    private static ThreadPool instance = null;
    private static ThreadCreationProcess facade = null;
    private final ThreadCollection threads = new ThreadCollection();
    private final TaskCollection tasks = new TaskCollection();
    private final ThreadIterator threadIterator = threads.createIterator();
    private final TaskIterator taskIterator = tasks.createIterator();

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

        for (threadIterator.first(); !threadIterator.isDone(); threadIterator.next()) {
            Thread currentThread = threadIterator.currentThread();

            if (currentThread.priority == priority && currentThread.currentState == ThreadState.IDLE) {
                currentThread.currentState = ThreadState.BUSY;
                selectedThread = threads.remove(currentThread);
                selectedThread.memoryUse = memoryRequirement;
                selectedThread.memoryManager.recordMemoryChange(memoryRequirement);
                break;
            }
        }

        if (selectedThread == null) {
            System.out.println("All priority: " + priority + " threads are busy.");
        }

        return selectedThread;
    }

    public void returnThread(Thread thread) {
        thread.currentState = ThreadState.IDLE;
        thread.resetMemoryUse();
        threads.add(thread);
        Notify();
    }

    public void attach(Task task) {
        tasks.add(task);
    }

    //Unregister from the list of Observers.
    public void detach(Task task) {
        tasks.remove(task);
    }

    //notify the Observers.
    public void Notify() {
        // set argument to something that helps
        // tell the Observers what happened
        for (taskIterator.first(); !taskIterator.isDone(); taskIterator.next()) {
            Task currentTask = taskIterator.currentTask();

            if (!currentTask.hasThread()) {
                taskIterator.currentTask().update();
            }
        }
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
    abstract public void resetMemoryUse();
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
    public void resetMemoryUse() {
        memoryManager.recordMemoryChange(-this.memoryUse);
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
    private static FileHandler fileHandler;


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

    private void logMemoryLimitExceed() {
        try {
            // This block configure the logger with handler and formatter
            if (fileHandler == null) {
                fileHandler = new FileHandler( System.getProperty("user.dir") + "/memory_manager.log");
                logger.addHandler(fileHandler);
                logger.setUseParentHandlers(false);
            }

            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);

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
    private final ThreadCollection threadTable;

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

interface AbstractThreadIterator {
    void first();
    void next();
    Boolean isDone();
    Thread currentThread();
}

interface AbstractTaskIterator {
    void first();
    void next();
    Boolean isDone();
    Task currentTask();
}

//This is the "concrete" Iterator for collection.
//		CollectionIterator

class ThreadIterator implements AbstractThreadIterator {
    private final ThreadCollection threadCollection;
    private int current;

    public void first() {
        current = 0;
    }

    public void next() {
        current++;
    }

    public Thread currentThread() {
        return isDone() ? null : threadCollection.get(current);
    }

    public Boolean isDone() {
        return current >= threadCollection.getCount();
    }

    public ThreadIterator(ThreadCollection threadCollection) {
        this.threadCollection = threadCollection;
        current = 0;
    }
}

class TaskIterator implements AbstractTaskIterator {
    private final TaskCollection taskCollection;
    private int current;

    public void first() {
        current = 0;
    }

    public void next() {
        current++;
    }

    public Task currentTask() {
        return isDone() ? null : taskCollection.get(current);
    }

    public Boolean isDone() {
        return current >= taskCollection.getCount();
    }

    public TaskIterator(TaskCollection taskCollection) {
        this.taskCollection = taskCollection;
        current = 0;
    }
}

//This is the abstract "Aggregate".
//			AbstractAggregate

interface AbstractThreadAggregate {
    AbstractThreadIterator createIterator();
    void add(Thread thread); // Not needed for iteration.
    Thread remove(Thread thread); // Not needed for iteration.
    int getCount(); // Needed for iteration.
    Thread get(int index); // Needed for iteration.
}

interface AbstractTaskAggregate {
    AbstractTaskIterator createIterator();
    void add(Task task); // Not needed for iteration.
    Task remove(Task task); // Not needed for iteration.
    int getCount(); // Needed for iteration.
    Task get(int index); // Needed for iteration.
}

//This is the concrete Aggregate.
//			Collection

class ThreadCollection implements AbstractThreadAggregate {
    private final ArrayList<Thread> threads = new ArrayList<>();

    @Override
    public ThreadIterator createIterator() {
        return new ThreadIterator(this);
    }

    @Override
    public int getCount() {
        return threads.size();
    }

    @Override
    public void add(Thread thread) {
        threads.add(thread);
    }

    @Override
    public Thread remove(Thread thread) {
        threads.remove(thread);
        return thread;
    }

    @Override
    public Thread get(int index) {
        return threads.get(index);
    }
}

class TaskCollection implements AbstractTaskAggregate {
    private final ArrayList<Task> tasks = new ArrayList<>();

    @Override
    public TaskIterator createIterator() {
        return new TaskIterator(this);
    }

    @Override
    public int getCount() {
        return tasks.size();
    }

    @Override
    public void add(Task task) {
        tasks.add(task);
    }

    @Override
    public Task remove(Task task) {
        tasks.remove(task);
        return task;
    }

    @Override
    public Task get(int index) {
        return tasks.get(index);
    }
}

class MemoryException extends Exception {
    public MemoryException(String message) {
        super(message);
    }
}

interface Observer {
    void update();
}

class Task implements Observer {
    private final ThreadPool threadPool;
    private final String taskName;
    private final int taskPriority;
    private final int initialMemoryRequirement;
    private Thread thread;

    // Constructor
    public Task(String taskName, int taskPriority, int initialMemoryRequirement) {
        this.threadPool = ThreadPool.getThreadPool();
        threadPool.attach(this);
        this.taskName = taskName;
        this.taskPriority = taskPriority;
        this.initialMemoryRequirement = initialMemoryRequirement;
    }

    public void update() {
        System.out.println("NOTIFIED the following task: " + taskName);
        this.thread = threadPool.getThread(taskPriority, initialMemoryRequirement);
    }

    public String getTaskName() {
        return taskName;
    }

    public void assignThread(Thread thread) {
        this.thread = thread;
    }

    public void returnThread() {
        if (this.thread != null) {
            threadPool.detach(this);
            threadPool.returnThread(this.thread);
            this.thread = null;
        }
    }

    public boolean hasThread() {
        return this.thread != null;
    }

    public int getTaskPriority() {
        return taskPriority;
    }

    public int getInitialMemoryRequirement() {
        return initialMemoryRequirement;
    }
}

class Main {
    public static void main(String[] args) {
        ThreadPool threadPool = ThreadPool.getThreadPool();
        TaskCollection taskCollection = new TaskCollection();
        TaskIterator taskIterator = taskCollection.createIterator();

        Task task1 = new Task("task1", 1, 260);
        Task task2 = new Task("task2", 5, 150);
        Task task3 = new Task("task3", 5, 50);
        Task task4 = new Task("task4", 1, 450);
        Task task5 = new Task("task5", 5, 11);
        Task task6 = new Task("task6", 1, 389);
        Task task7 = new Task("task7", 1, 500);
        Task task8 = new Task("task8", 5, 200);
        Task task9 = new Task("task9", 5, 250);
        Task task10 = new Task("task10", 1, 400);

        taskCollection.add(task1);
        taskCollection.add(task2);
        taskCollection.add(task3);
        taskCollection.add(task4);
        taskCollection.add(task5);
        taskCollection.add(task6);
        taskCollection.add(task7);
        taskCollection.add(task8);
        taskCollection.add(task9);
        taskCollection.add(task10);

        for (taskIterator.first(); !taskIterator.isDone(); taskIterator.next()) {
            Task currentTask = taskIterator.currentTask();
            currentTask.assignThread(threadPool.getThread(currentTask.getTaskPriority(), currentTask.getInitialMemoryRequirement()));
        }

        System.out.println("1 -------");
        task1.returnThread();
        System.out.println("2 -------");
        task2.returnThread();
        System.out.println("3 -------");
        task3.returnThread();
        System.out.println("4 -------");
        task4.returnThread();
        System.out.println("5 -------");
        task5.returnThread();
        System.out.println("6 -------");
        task6.returnThread();
        System.out.println("7 -------");
        task7.returnThread();
    }
}
