import java.util.ArrayList;

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

// This is the "concrete" Iterator for thread collection.
class ThreadIterator implements AbstractThreadIterator {
    private final ThreadCollection threadCollection;
    private int current;

    @Override
    public void first() {
        current = 0;
    }

    @Override
    public void next() {
        current++;
    }

    @Override
    public Thread currentThread() {
        return isDone() ? null : threadCollection.get(current);
    }

    @Override
    public Boolean isDone() {
        return current >= threadCollection.getCount();
    }

    public ThreadIterator(ThreadCollection threadCollection) {
        this.threadCollection = threadCollection;
        current = 0;
    }
}

// This is the "concrete" Iterator for task collection.
class TaskIterator implements AbstractTaskIterator {
    private final TaskCollection taskCollection;
    private int current;

    @Override
    public void first() {
        current = 0;
    }

    @Override
    public void next() {
        current++;
    }

    @Override
    public Task currentTask() {
        return isDone() ? null : taskCollection.get(current);
    }

    @Override
    public Boolean isDone() {
        return current >= taskCollection.getCount();
    }

    public TaskIterator(TaskCollection taskCollection) {
        this.taskCollection = taskCollection;
        current = 0;
    }
}

// This is the abstract "Aggregate" for thread collection.
interface AbstractThreadAggregate {
    AbstractThreadIterator createIterator();
    void add(Thread thread); // Not needed for iteration.
    Thread remove(Thread thread); // Not needed for iteration.
    int getCount(); // Needed for iteration.
    Thread get(int index); // Needed for iteration.
}

// This is the abstract "Aggregate" for task collection.
interface AbstractTaskAggregate {
    AbstractTaskIterator createIterator();
    void add(Task task); // Not needed for iteration.
    Task remove(Task task); // Not needed for iteration.
    int getCount(); // Needed for iteration.
    Task get(int index); // Needed for iteration.
}

// This is the concrete Aggregate for threads.
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

// This is the concrete Aggregate for tasks.
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
