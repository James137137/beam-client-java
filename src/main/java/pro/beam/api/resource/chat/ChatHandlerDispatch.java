package pro.beam.api.resource.chat;

import com.google.common.collect.Multimap;
import com.google.common.collect.Queues;
import com.google.gson.JsonObject;
import pro.beam.api.BeamAPI;
import pro.beam.api.resource.chat.events.EventHandler;
import pro.beam.api.util.QueueWorker;
import pro.beam.api.util.SingleDispatchQueueWorker;

import java.util.Map;
import java.util.Queue;
import java.util.function.Function;

public class ChatHandlerDispatch {
    protected final BeamAPI beam;

    protected final Map<Integer, BeamChatConnectable.ReplyPair> replyHandlers;
    protected final Multimap<Class<? extends AbstractChatEvent>, EventHandler> eventHandlers;

    protected final Queue<AbstractChatReply> replyQueue = Queues.newArrayDeque();
    protected final Queue<AbstractChatEvent> eventQueue = Queues.newArrayDeque();

    public ChatHandlerDispatch(BeamAPI beam,
                               final Map<Integer, BeamChatConnectable.ReplyPair> replyHandlers,
                               final Multimap<Class<? extends AbstractChatEvent>, EventHandler> eventHandlers) {
        this.beam = beam;

        this.replyHandlers = replyHandlers;
        this.eventHandlers = eventHandlers;
    }

    public void accept(int id, JsonObject o) {
        BeamChatConnectable.ReplyPair pair = this.replyHandlers.remove(id);
        if (pair != null) {
            Class<? extends AbstractChatDatagram> type = pair.type;
            AbstractChatDatagram reply = this.beam.gson.fromJson(o.toString(), type);

            this.replyQueue.offer((AbstractChatReply) reply);
        }
    }

    public <T extends AbstractChatEvent> void accept(T event) {
        this.eventQueue.offer(event);
    }

    public void attachReplyHandler(final int id, final BeamChatConnectable.ReplyPair pair) {
        this.replyHandlers.put(id, pair);

        new Thread(new SingleDispatchQueueWorker<>(this.replyQueue, new Function<AbstractChatReply, Boolean>() {
            @Override public Boolean apply(AbstractChatReply reply) {
                if (reply.id == id) {
                    pair.handler.onSuccess(reply);
                    return true;
                } else return false;
            }
        })).start();
    }

    public <T extends AbstractChatEvent> void attachEventHandler(final Class<T> eventType, final EventHandler<T> handler) {
        new Thread(new QueueWorker<>(this.eventQueue, new Function<AbstractChatEvent, Boolean>() {
            @Override public Boolean apply(AbstractChatEvent event) {
                try {
                    if (event.getClass() == eventType) {
                        handler.onEvent((T) event);
                        return true;
                    } else return false;
                } catch (Exception e) {
                    e.printStackTrace();
                    return false;
                }
            }
        })).start();
    }
}
