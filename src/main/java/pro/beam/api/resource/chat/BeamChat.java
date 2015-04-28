package pro.beam.api.resource.chat;

import pro.beam.api.BeamAPI;

import java.net.URI;
import java.util.Random;

public class BeamChat {
    public String authkey;
    public String[] endpoints;
    public boolean linksAllowed;
    public boolean linksClickable;
    public String role;
    public double slowchat;

    public BeamChatBridge bridge(BeamAPI beam) {
        return new BeamChatBridge(beam, this);
    }

    public URI selectEndpoint() {
        return URI.create(this.endpoints[new Random().nextInt(this.endpoints.length)]);
    }
}
