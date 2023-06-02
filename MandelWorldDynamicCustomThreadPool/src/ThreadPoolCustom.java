import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class ThreadPoolCustom {
    private BlockingQueue<Runnable> taskQueue;
    private List<WorkerDynamicCustomThreadPool> runnables = new ArrayList<>();
    private boolean isStopped;

    public ThreadPoolCustom(int allThreadsCount, int allTasksCount) {
        this.taskQueue = new ArrayBlockingQueue<>(allTasksCount);
        this.isStopped = false;

        for (int i = 0; i < allThreadsCount; ++i) {
            this.runnables.add(new WorkerDynamicCustomThreadPool(this.taskQueue));
        }
        for (WorkerDynamicCustomThreadPool runnable : this.runnables) {
            new Thread(runnable).start();
        }
    }

    public synchronized void execute(Runnable task) throws Exception {
        if(this.isStopped) {
            throw new IllegalStateException("ThreadPoolCustom has been stopped");
        }

        this.taskQueue.offer(task);
    }

    public synchronized void stop(boolean quiet) {
        this.isStopped = true;
        for(WorkerDynamicCustomThreadPool runnable : this.runnables) {
            runnable.doStop(quiet);
        }
    }

    public synchronized void killThreads(){
        for(WorkerDynamicCustomThreadPool runnable : this.runnables) {
            runnable.interrupt();
        }
    }

    public synchronized void waitUntilAllTasksFinished() {
        while(!this.taskQueue.isEmpty()) {
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
