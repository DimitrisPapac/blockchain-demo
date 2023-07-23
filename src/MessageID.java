import java.security.PrivateKey;
import java.security.PublicKey;

public class MessageID extends MessageSigned {
    private static final long serialVersionUID = 1L;
    private String info = null;
    private byte[] signature = null;
    private PublicKey sender = null;
    private String name = null;

    // Constructor
    public MessageID(PrivateKey sk, PublicKey sender, String name) {
        this.info = Message.JCOIN_MESSAGE;
        signature = UtilityMethods.generateSignature(sk, this.info);
        this.sender = sender;
        this.name = name;
    }

    public String getMessageBody() {
        return this.info;
    }

    public boolean isValid() {
        return UtilityMethods.verifySignature(this.getPublicKey(), signature, this.info);
    }

    public int getMessageType() {
        return Message.ID;
    }

    protected PublicKey getPublicKey() {
        return this.sender;
    }

    public boolean isForBroadcast() {
        return false;
    }

    public String getName() {
        return this.name;
    }

    public KeyNamePair getKeyNamePair() {
        KeyNamePair knp = new KeyNamePair(this.getPublicKey(), this.getName());
        return knp;
    }

}
