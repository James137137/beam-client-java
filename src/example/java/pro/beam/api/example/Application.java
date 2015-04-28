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

public class Application {
    public static void main(String[] args) throws ExecutionException, InterruptedException {
        BeamAPI beam = new BeamAPI();

        BeamUser user = beam.use(UsersService.class).login("<username>", "<password>").get();
        BeamChat chat = beam.use(ChatService.class).findOne(user.channel.id).get();

        BeamChatBridge bridge = chat.bridge(beam).connect();
        bridge.send(AuthenticateMessage.from(user.channel, user, chat.authkey));

        bridge.on(IncomingMessageEvent.class, new EventHandler<IncomingMessageEvent>() {
            @Override public void onEvent(IncomingMessageEvent event) {
                System.out.println(event.data.getMessage());
            }
        });
    }
}
