package pro.beam.api.example;

import pro.beam.api.BeamAPI;
import pro.beam.api.resource.BeamUser;
import pro.beam.api.resource.chat.BeamChat;
import pro.beam.api.resource.chat.BeamChatBridge;
import pro.beam.api.resource.chat.events.EventHandler;
import pro.beam.api.resource.chat.events.IncomingMessageEvent;
import pro.beam.api.resource.chat.methods.AuthenticateMessage;
import pro.beam.api.services.impl.ChatService;
import pro.beam.api.services.impl.UsersService;

import java.util.concurrent.ExecutionException;

/**
 * This class serves as a demo to show off basic functionality of the Beam Java Client.
 * The following snippet of code connects to a user's own channel (provided a username
 * and password), and then outputs all messages to stdout via an EventHandler.
 *
 * The source is annotated:
 */
public class Application {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // Construct an instance of the Beam API. This holds all of the services that are
        // used to make various requests and do certain things.
        BeamAPI beam = new BeamAPI();

        // Construct a "user" object that represents the user corresponding to the <username>
        // and <password> provided below.
        BeamUser user = beam.use(UsersService.class).login("<username>", "<password>").get();
        // Fetch the chat object that belongs to the user's channel.
        BeamChat chat = beam.use(ChatService.class).findOne(user.channel.id).get();

        // Construct a bridge to make that chat interactive, and then connect to it.
        BeamChatBridge bridge = chat.bridge(beam).connect();
        // Once the bridge is constructed, send the authentication packet to verify that
        // we are who we say we are, and that we have the rights to chat.
        bridge.send(AuthenticateMessage.from(user.channel, user, chat.authkey));

        // Finally, attach an EventHandler to the IncomingMessageEvent class such that
        // when the event is sent down, the message is printed to stdout.
        bridge.on(IncomingMessageEvent.class, new EventHandler<IncomingMessageEvent>() {
            @Override public void onEvent(IncomingMessageEvent event) {
                System.out.println(event.data.getMessage());
            }
        });
    }
}
