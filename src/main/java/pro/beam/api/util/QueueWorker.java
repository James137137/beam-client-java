package pro.beam.api.util;

import java.util.Queue;
import java.util.function.Function;

public class QueueWorker<T> extends AbstractQueueWorker<T> {
    public QueueWorker(Queue<T> queue, Function<T, Boolean> applicant) {
        super(queue, applicant);
    }

    @Override protected boolean shouldWork() {
        return true;
    }
}
