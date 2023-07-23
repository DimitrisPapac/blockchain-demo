public class MessageTransactionBroadcast extends Message {
    private static final long serialVersionUID = 1L;
    private Transaction transaction = null;

    // Constructor
    public MessageTransactionBroadcast(Transaction transaction) {
        this.transaction = transaction;
    }

    public int getMessageType() {
        return Message.TRANSACTION_BROADCAST;
    }

    public Transaction getMessageBody() {
        return this.transaction;
    }

    public boolean isForBroadcast() {
        return true;
    }

}
