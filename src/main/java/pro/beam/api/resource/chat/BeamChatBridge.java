package pro.beam.api.resource.chat;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import pro.beam.api.BeamAPI;
import pro.beam.api.resource.chat.events.EventHandler;
import pro.beam.api.resource.chat.events.data.IncomingMessageData;
import pro.beam.api.resource.chat.methods.AuthenticateMessage;
import pro.beam.api.resource.chat.replies.ReplyHandler;

import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.util.Map;

public class BeamChatBridge {
    private static final SSLSocketFactory SSL_SOCKET_FACTORY = (SSLSocketFactory) SSLSocketFactory.getDefault();

    protected final BeamAPI beam;
    protected final BeamChat chat;

    public BeamChatConnectable connectable;

    protected AuthenticateMessage authenticate;
    protected Multimap<Class<? extends AbstractChatEvent>, EventHandler> eventHandlers = HashMultimap.create();

    public BeamChatBridge(BeamAPI beam, BeamChat chat) {
        this.beam = beam;
        this.chat = chat;
    }

    public BeamChatBridge connect() {
        BeamChatConnectable newConnectable = new BeamChatConnectable(this, this.chat.selectEndpoint());

        // Re-attach event handlers to the new connectable object.
        for (Map.Entry<Class<? extends AbstractChatEvent>, EventHandler> entry : this.eventHandlers.entries()) {
            newConnectable.on(entry.getKey(), entry.getValue());
        }

        try {
            newConnectable.setSocket(SSL_SOCKET_FACTORY.createSocket());

            newConnectable.connectBlocking();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        this.connectable = newConnectable;

        return this;
    }

    ///////////////////////////////////////////////////////////////////////////////
    /////                       Delegate Methods Below                        /////
    ///////////////////////////////////////////////////////////////////////////////
    public <T extends AbstractChatEvent> boolean on(Class<T> eventType, EventHandler<T> handler) {
        return this.connectable.on(eventType, handler);
    }

    public void send(AbstractChatMethod method) {
        this.send(method, null);
    }

    public <T extends AbstractChatReply> void send(AbstractChatMethod method, ReplyHandler<T> handler) {
        if (method.getClass() == AuthenticateMessage.class) {
            this.authenticate = (AuthenticateMessage) method;
        }

        this.connectable.send(method, handler);
    }

    public void delete(IncomingMessageData message) {
        this.connectable.delete(message);
    }

    protected void notifyClose(int i, String s, boolean b) {
        try {
            // HACK: Wait for 500ms because the WSS client doesn't correctly report it's state on
            // whether or not it's closed, so we make sure that it is before opening a new connection.
            Thread.sleep(500);
        } catch (InterruptedException ignored) { }

        // Cache the event-handlers.
        this.eventHandlers = HashMultimap.create(this.connect().eventHandlers);

        this.connect();
        if (this.authenticate != null) {
            this.send(this.authenticate);
        }
    }

    public void disconnect() {
        this.connectable.closeConnection(1000, "BEAM_JAVA_DISCONNECT");
    }
}
