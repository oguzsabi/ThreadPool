import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

// This class is used for memory management simulation
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
            // This block configures the logger with a file handler
            if (fileHandler == null) {
                fileHandler = new FileHandler( System.getProperty("user.dir") + "/memory_manager.log");
                logger.addHandler(fileHandler);
                logger.setUseParentHandlers(false);
            }

            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);

            // the following statement is used to log our message
            logger.info("Memory Limit of " + memoryLimitForLogging + "MB is exceeded!");
        } catch (SecurityException | IOException e) {
            e.printStackTrace();
        }
    }
}

// FACADE class
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

// This class is used for thread table simulation
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