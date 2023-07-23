import java.security.PrivateKey;
import java.security.PublicKey;

public class MessageAskForBlockchainBroadcast extends MessageTextBroadcast {
    private static final long serialVersionUID = 1L;

    // Constructor
    public MessageAskForBlockchainBroadcast(String text, PrivateKey sk,
                                            PublicKey sender, String name) {
        super(text, sk, sender, name);
    }

    public int getMessageType() {
        return Message.BLOCKCHAIN_ASK_BROADCAST;
    }

}