package pro.beam.api.resource.chat;

import com.google.common.collect.Multimap;
import com.google.common.collect.Queues;
import com.google.gson.JsonObject;
import pro.beam.api.BeamAPI;
import pro.beam.api.resource.chat.events.EventHandler;
import pro.beam.api.util.QueueWorker;

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

        this.startWorkers();
    }

    public <T extends AbstractChatReply> void accept(int id, JsonObject o) {
        BeamChatConnectable.ReplyPair pair = this.replyHandlers.get(id);
        if (pair != null) {
            Class<? extends AbstractChatDatagram> type = pair.type;
            AbstractChatDatagram reply = this.beam.gson.fromJson(o.toString(), type);

            this.replyQueue.offer((AbstractChatReply) reply);
        }
    }

    public <T extends AbstractChatEvent> void accept(T event) {
        this.eventQueue.offer(event);
    }

    private void startWorkers() {
        new Thread(new QueueWorker<>(this.replyQueue, new Function<AbstractChatReply, Boolean>() {
            @Override public Boolean apply(AbstractChatReply reply) {
                BeamChatConnectable.ReplyPair replyPair = replyHandlers.remove(reply.id);
                if (replyPair != null) {
                    replyPair.handler.onSuccess(reply);
                }

                return true;
            }
        })).run();

        new Thread(new QueueWorker<>(this.eventQueue, new Function<AbstractChatEvent, Boolean>() {
            @Override public Boolean apply(AbstractChatEvent event) {
                for (EventHandler handler : eventHandlers.get(event.getClass())) {
                    handler.onEvent(event);
                }

                return true;
            }
        })).run();
    }
}
