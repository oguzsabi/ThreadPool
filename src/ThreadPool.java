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

// Singleton class. Subject for observer pattern. Client for abstract factory pattern.
public class ThreadPool {
    private static ThreadPool instance = null;
    private static ThreadCreationProcess facade = null;
    private final ThreadCollection threads = new ThreadCollection();
    private final TaskCollection tasks = new TaskCollection();
    private final ThreadIterator threadIterator = threads.createIterator();
    private final TaskIterator taskIterator = tasks.createIterator();

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

    public static ThreadPool getThreadPool() {
        if (instance == null) {
            facade = new ThreadCreationProcess();
            instance = new ThreadPool();
        }

        return instance;
    }

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

    public void reclaimThread(Thread thread) {
        thread.currentState = ThreadState.IDLE;
        thread.resetMemoryUse();
        threads.add(thread);
        Notify();
    }

    //Register to the list of Observers.
    public void attach(Task task) {
        tasks.add(task);
    }

    //Unregister from the list of Observers.
    public void detach(Task task) {
        tasks.remove(task);
    }

    //notify the Observers.
    public void Notify() {
        for (taskIterator.first(); !taskIterator.isDone(); taskIterator.next()) {
            Task currentTask = taskIterator.currentTask();

            if (!currentTask.hasThread()) {
                currentTask.update();
            }
        }
    }

    private Thread createThread(ThreadFactory threadFactory) {
        Thread thread = threadFactory.createThread();
        boolean canAddThreadToPool = facade.processThreadCreation(thread);

        if (canAddThreadToPool) {
            return thread;
        }

        return null;
    }
}

// Our custom exception class
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

    public Task(String taskName, int taskPriority, int initialMemoryRequirement) {
        this.threadPool = ThreadPool.getThreadPool();
        threadPool.attach(this);
        this.taskName = taskName;
        this.taskPriority = taskPriority;
        this.initialMemoryRequirement = initialMemoryRequirement;
    }

    @Override
    public void update() {
        System.out.println("NOTIFIED the following task: " + taskName);
        assignThread(threadPool.getThread(taskPriority, initialMemoryRequirement));
    }

    public String getTaskName() {
        return taskName;
    }

    public void assignThread(Thread thread) {
        if (thread != null) {
            System.out.println("Assigned thread of priority " + thread.priority + " to " + taskName + "\n");
        } else {
            System.out.println("Could not assign thread to " + taskName + "\n");
        }

        this.thread = thread;
    }

    public Thread getThread() {
        return this.thread;
    }

    public void returnThread() {
        if (this.thread != null) {
            threadPool.detach(this);
            threadPool.reclaimThread(this.thread);
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
        // We get our ThreadPool instance and create TaskCollection and TaskIterator objects.
        ThreadPool threadPool = ThreadPool.getThreadPool();
        TaskCollection taskCollection = new TaskCollection();
        TaskIterator taskIterator = taskCollection.createIterator();

        // Creating new tasks
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
        Task task11 = new Task("task11", 1, 400);
        Task task12 = new Task("task12", 5, 133);
        Task task13 = new Task("task13", 5, 99);

        // Adding tasks to the taskCollection
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
        taskCollection.add(task11);
        taskCollection.add(task12);
        taskCollection.add(task13);

        System.out.println();

        // Assigning IDLE threads to suitable tasks
        for (taskIterator.first(); !taskIterator.isDone(); taskIterator.next()) {
            Task currentTask = taskIterator.currentTask();
            currentTask.assignThread(threadPool.getThread(currentTask.getTaskPriority(), currentTask.getInitialMemoryRequirement()));
        }

        System.out.println();

        // Memory increase and decrease example
        System.out.println("Memory use of task1's thread: " + task1.getThread().memoryUse);
        System.out.println("Increasing memory use of task1's thread by: 200");
        task1.getThread().increaseMemoryUse(200);
        System.out.println("Memory use of task1's thread: " + task1.getThread().memoryUse);
        System.out.println("Decreasing memory use of task1's thread by: 300");
        task1.getThread().decreaseMemoryUse(300);
        System.out.println("Memory use of task1's thread: " + task1.getThread().memoryUse);
        System.out.println("Increasing memory use of task1's thread by: 100");
        task1.getThread().increaseMemoryUse(100);
        System.out.println("Memory use of task1's thread: " + task1.getThread().memoryUse);

        System.out.println();

        // Finished tasks are returning their threads so that other tasks can start execution.
        System.out.println("------- Return Thread of task1 -------");
        task1.returnThread();
        System.out.println();

        System.out.println("------- Return Thread of task2 -------");
        task2.returnThread();
        System.out.println();

        System.out.println("------- Return Thread of task3 -------");
        task3.returnThread();
        System.out.println();

        System.out.println("------- Return Thread of task4 -------");
        task4.returnThread();
        System.out.println();

        System.out.println("------- Return Thread of task5 -------");
        task5.returnThread();
        System.out.println();

        System.out.println("------- Return Thread of task6 -------");
        task6.returnThread();
        System.out.println();

        System.out.println("------- Return Thread of task7 -------");
        task7.returnThread();
        System.out.println();

        System.out.println("------- Return Thread of task8 -------");
        task8.returnThread();
        System.out.println();

        System.out.println("------- Return Thread of task9 -------");
        task9.returnThread();
        System.out.println();

        System.out.println("------- Return Thread of task10 -------");
        task10.returnThread();
        System.out.println();
    }
}
