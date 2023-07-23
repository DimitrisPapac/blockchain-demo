import javax.xml.crypto.dsig.keyinfo.KeyName;
import java.util.ArrayList;

public class MessageAddressPrivate extends Message {
    private static final long serialVersionUID = 1L;
    private ArrayList<KeyNamePair> addresses;

    // Constructor
    public MessageAddressPrivate(ArrayList<KeyNamePair> addresses) {
        this.addresses = addresses;
    }

    public int getMessageType() {
        return Message.ADDRESS_PRIVATE;
    }

    public ArrayList<KeyNamePair> getMessageBody() {
        return this.addresses;
    }

    public boolean isForBroadcast() {
        return false;
    }

}