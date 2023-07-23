import java.io.Serializable;

public abstract class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    public static final int ID = 0;

    // Types of messages
    public static final int TEXT_BROADCAST = 1;
    public static final int TEXT_PRIVATE = 2;
    public static final int TRANSACTION_BROADCAST = 10;
    public static final int BLOCK_BROADCAST = 20;
    public static final int BLOCK_PRIVATE = 21;
    public static final int BLOCK_ASK_PRIVATE = 22;
    public static final int BLOCK_ASK_BROADCAST = 23;
    public static final int BLOCKCHAIN_BROADCAST = 3;
    public static final int BLOCKCHAIN_PRIVATE = 31;
    public static final int BLOCKCHAIN_ASK_BROADCAST = 33;
    public static final int ADDRESS_BROADCAST = 4;
    public static final int ADDRESS_PRIVATE = 41;

    // Test string used to testify if the message has been tampered
    public static final String JCOIN_MESSAGE = "This package is from mdsky.";

    public abstract int getMessageType();

    // Text phrase asking the server to close the connection
    public static final String TEXT_CLOSE = "CLOSE_me";
    public static final String TEXT_ASK_ADDRESSES = "TEXT_QUERY_ADDRESSES";

    // Message body
    public abstract Object getMessageBody();

    // To differentiate between broadcast and private messages
    public abstract boolean isForBroadcast();
}
