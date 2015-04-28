package pro.beam.api.util;

import java.util.Queue;
import java.util.function.Function;

public class QueueWorker<T> implements Runnable {
    protected final Function<T, Boolean> applicant;
    protected final Queue<T> queue;

    public QueueWorker(Queue<T> queue, Function<T, Boolean> applicant) {
        this.queue = queue;
        this.applicant = applicant;
    }

    @Override public void run() {
        for (;;) {
            T next = this.queue.poll();
            if (next != null) {
                this.applicant.apply(next);
            }
        }
    }
}
