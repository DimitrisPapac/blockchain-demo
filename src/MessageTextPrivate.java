import jdk.jshell.execution.Util;

import java.security.PrivateKey;
import java.security.PublicKey;

public class MessageTextPrivate extends MessageSigned {
    private static final long serialVersionUID = 1L;
    private String info = null;
    private byte[] signature = null;
    private PublicKey senderKey = null;
    private String senderName;
    private PublicKey receiver = null;

    // Constructor
    public MessageTextPrivate(String text, PrivateKey senderSK,
                              PublicKey senderPK, String senderName, PublicKey receiver) {
        this.info = text;
        signature = UtilityMethods.generateSignature(senderSK, this.info);
        this.senderKey = senderPK;
        this.senderName = senderName;
        this.receiver = receiver;
    }

    public String getMessageBody() {
        return this.info;
    }

    public boolean isValid() {
        return UtilityMethods.verifySignature(senderKey, signature, this.info);
    }

    public int getMessageType() {
        return Message.TEXT_PRIVATE;
    }

    public PublicKey getReceiver() {
        return this.receiver;
    }

    public PublicKey getSenderKey() {
        return this.senderKey;
    }

    public String getSenderName() {
        return this.senderName;
    }

    public KeyNamePair getSenderKeyNamePair() {
        return new KeyNamePair(this.getSenderKey(), this.senderName);
    }

    public boolean isForBroadcast() {
        return false;
    }

}
