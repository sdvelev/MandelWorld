import java.util.concurrent.BlockingQueue;

public class WorkerDynamicCustomThreadPool implements Runnable {

    private Thread thread = null;
    private BlockingQueue<Runnable> taskQueue;
    private boolean isStopped;
    private int countTasks;

    public void interrupt(){
        this.thread.interrupt();
    }

    public WorkerDynamicCustomThreadPool(BlockingQueue<Runnable> queue) {
        this.taskQueue = queue;
        this.isStopped = false;
        this.countTasks = 0;
    }

    public void run() {
        this.thread = Thread.currentThread();
        while(!isStopped()) {
            try{
                Runnable runnable = (Runnable)taskQueue.take();
                ++countTasks;
                runnable.run();
            } catch(Exception e) { }
        }
    }

    public synchronized void doStop(boolean quiet) {
        isStopped = true;

        if(!quiet) {
            System.out.println(countTasks + " tasks done by this thread.");
        }
    }

    public synchronized boolean isStopped() {
        return isStopped;
    }
}
