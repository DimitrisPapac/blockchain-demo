import java.io.Serializable;
import java.security.PublicKey;

public class MessageBlockchainPrivate extends Message implements Serializable {
    private static final long serialVersionUID = 1L;
    private Blockchain ledger = null;
    private PublicKey sender = null;
    private PublicKey receiver = null;
    private int initialSize = 0;

    // Constructor
    public MessageBlockchainPrivate(Blockchain ledger, PublicKey sender,
                                    PublicKey receiver) {
        this.ledger = ledger;
        this.sender = sender;
        this.receiver = receiver;
        this.initialSize = this.ledger.size();
    }

    public int getInfoSize() {
        return this.initialSize;
    }

    public int getMessageType() {
        return Message.BLOCKCHAIN_PRIVATE;
    }

    public PublicKey getSender() {
        return this.sender;
    }

    public PublicKey getReceiver() {
        return this.receiver;
    }

    public Blockchain getMessageBody() {
        return this.ledger;
    }

    public boolean isForBroadcast() {
        return false;
    }

}
