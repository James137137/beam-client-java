package pro.beam.api.util;

import java.util.Queue;
import java.util.function.Function;

public abstract class AbstractQueueWorker<T> implements Runnable {
    protected final Function<T, Boolean> applicant;
    protected final Queue<T> queue;

    public AbstractQueueWorker(Queue<T> queue, Function<T, Boolean> applicant) {
        this.queue = queue;
        this.applicant = applicant;
    }

    @Override public void run() {
        T next;
        while (this.shouldWork()) {
            if ((next = this.queue.element()) != null) {
                boolean shouldRemove = this.applicant.apply(next);
                if (shouldRemove) {
                    this.notifyWorkOn(next);
                    this.queue.remove(next);
                }
            }
        }
    }

    protected abstract boolean shouldWork();

    protected void notifyWorkOn(T element) {
        // no-op
    }
}
