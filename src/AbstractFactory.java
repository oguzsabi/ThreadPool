// Abstract product class
abstract class Processable {
    abstract public void setState(ThreadState state);
    abstract public void increaseMemoryUse(int memoryAmount);
    abstract public void decreaseMemoryUse(int memoryAmount);
    abstract public void resetMemoryUse();
    abstract public ThreadState getState();
    abstract public int getPriority();
    abstract public int getMemoryUse();
}

// Base class for our concrete products
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

// One of our concrete products
class HThread extends Thread {
    public HThread(int priority) {
        this.priority = priority;
        this.currentState = ThreadState.IDLE;
        this.maxMemory = 512;
        this.memoryUse = 0;
        System.out.println("HThread is created...");
    }
}

// One of our concrete products
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