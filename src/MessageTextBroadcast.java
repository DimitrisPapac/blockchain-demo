import jdk.jshell.execution.Util;

import java.security.PrivateKey;
import java.security.PublicKey;

public class MessageTextBroadcast extends MessageSigned {
    private static final long serialVersionUID = 1L;
    private String info = null;
    private byte[] signature = null;
    private PublicKey pubkey = null;
    private String name = null;

    // Constructor
    public MessageTextBroadcast(String text, PrivateKey key,
                                PublicKey pubkey, String name) {
        this.info = text;
        this.signature = UtilityMethods.generateSignature(key, text);
        this.pubkey = pubkey;
        this.name = name;
    }

    public String getMessageBody() {
        return this.info;
    }

    public boolean isValid() {
        return UtilityMethods.verifySignature(this.pubkey, this.signature, this.info);
    }

    public int getMessageType() {
        return Message.TEXT_BROADCAST;
    }

    public boolean isForBroadcast() {
        return true;
    }

    public PublicKey getSenderKey() {
        return this.pubkey;
    }

    public String getSenderName() {
        return this.name;
    }

}
