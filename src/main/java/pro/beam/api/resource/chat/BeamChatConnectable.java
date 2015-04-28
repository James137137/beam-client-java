package pro.beam.api.resource.chat;

import com.google.common.collect.HashMultimap;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import pro.beam.api.BeamAPI;
import pro.beam.api.http.ws.CookieDraft_17;

import java.net.URI;

import pro.beam.api.resource.chat.events.EventHandler;
import pro.beam.api.resource.chat.events.data.IncomingMessageData;
import pro.beam.api.resource.chat.replies.ReplyHandler;

import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

public class BeamChatConnectable extends WebSocketClient {
    protected final BeamChatBridge bridge;
    protected final ChatHandlerDispatch dispatch;

    public BeamChatConnectable(BeamChatBridge bridge, URI endpoint) {
        super(endpoint, new CookieDraft_17(bridge.beam.http));

        this.bridge = bridge;
        this.dispatch = new ChatHandlerDispatch(bridge.beam,
                                                new ConcurrentHashMap<>(new HashMap<Integer, ReplyPair>()),
                                                HashMultimap.<Class<? extends AbstractChatEvent>, EventHandler>create());
    }

    public <T extends AbstractChatEvent> boolean on(Class<T> eventType, EventHandler<T> handler) {
        return this.dispatch.eventHandlers.put(eventType, handler);
    }

    public void send(AbstractChatMethod method) {
        this.send(method, null);
    }

    public <T extends AbstractChatReply> void send(final AbstractChatMethod method, ReplyHandler<T> handler) {
        if (handler != null) {
            this.dispatch.replyHandlers.put(method.id, ReplyPair.from(handler));
        }

        this.bridge.beam.executor.submit(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                byte[] data = BeamChatConnectable.this.bridge.beam.gson.toJson(method).getBytes();
                BeamChatConnectable.this.send(data);

                return null;
            }
        });
    }

    public void delete(IncomingMessageData message) {
        String path = BeamAPI.BASE_PATH.resolve("chats/" + message.channel + "/message/" + message.id).toString();
        this.bridge.beam.http.delete(path, null);
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
    }

    @Override
    public void onMessage(String s) {
        ReplyPair replyPair = null;
        try {
            // Parse out the generic JsonObject so we can pull out the ID element from it,
            //  since we cannot yet parse as a generic class.
            JsonObject e = this.bridge.beam.gson.fromJson(s, JsonObject.class);
            if (e.has("id")) {
                this.dispatch.accept(e.get("id").getAsInt(), e);
            } else if (e.has("event")) {
                // Handles cases of Beam Widget (ex. GiveawayBot) sending ChatMessage events
                if (e.getAsJsonObject("data").has("user_id") && e.getAsJsonObject("data").get("user_id").getAsInt() == -1) {
                    Class<? extends AbstractChatEvent> type = AbstractChatEvent.EventType.fromSerializedName("WidgetMessage").getCorrespondingClass();
                    this.dispatch.accept(this.bridge.beam.gson.fromJson(e, type));
                } else {
                   // Use the default ChatMessage event handling
                   String eventName = e.get("event").getAsString();
                   Class<? extends AbstractChatEvent> type = AbstractChatEvent.EventType.fromSerializedName(eventName).getCorrespondingClass();
                   this.dispatch.accept(this.bridge.beam.gson.fromJson(e, type));
                }
            }
        } catch (JsonSyntaxException e) {
            // If an exception was called and we do have a reply handler to catch it,
            // call the #onFailure method with the throwable.
            if (replyPair != null) {
                replyPair.handler.onFailure(e);
            } else {
                throw e;
            }
        }
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        this.bridge.notifyClose(i, s, b);
    }

    @Override
    public void onError(Exception e) {
        e.printStackTrace();
    }

    protected static class ReplyPair<T extends AbstractChatReply> {
        public ReplyHandler<T> handler;
        public Class<T> type;

        private static <T extends AbstractChatReply> ReplyPair<T> from(ReplyHandler<T> handler) {
            ReplyPair<T> pair = new ReplyPair<>();

            pair.handler = handler;
            pair.type = (Class<T>) ((ParameterizedType) handler.getClass().getGenericSuperclass()).getActualTypeArguments()[0];

            return pair;
        }
    }
}
