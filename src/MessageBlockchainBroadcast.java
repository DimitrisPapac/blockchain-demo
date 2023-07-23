import java.security.PublicKey;

public class MessageBlockchainBroadcast extends Message {
    private static final long serialVersionUID = 1L;
    private Blockchain ledger = null;
    private PublicKey sender = null;
    private int initialSize = 0;

    // Constructor
    public MessageBlockchainBroadcast(Blockchain ledger, PublicKey sender) {
        this.ledger = ledger;
        this.sender = sender;
        this.initialSize = this.ledger.size();
    }

    public int getInfoSize() {
        return this.initialSize;
    }

    public int getMessageType() {
        return Message.BLOCKCHAIN_BROADCAST;
    }

    public Blockchain getMessageBody() {
        return this.ledger;
    }

    public boolean isForBroadcast() {
        return true;
    }

    public PublicKey getSender() {
        return this.sender;
    }

}
