package pro.beam.api.util;

import java.util.Queue;
import java.util.function.Function;

public class SingleDispatchQueueWorker<T> extends AbstractQueueWorker<T> {
    private boolean workWasDone = false;

    public SingleDispatchQueueWorker(Queue<T> queue, Function<T, Boolean> applicant) {
        super(queue, applicant);
    }

    @Override protected boolean shouldWork() {
        return !this.workWasDone;
    }

    @Override public void notifyWorkOn(T element) {
        this.workWasDone = true;
    }
}
