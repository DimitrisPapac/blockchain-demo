public class MessageBlockBroadcast extends Message {
    private static final long serialVersionUID = 1L;
    private Block block = null;

    // Constructor
    public MessageBlockBroadcast(Block block) {
        this.block = block;
    }

    public int getMessageType() {
        return Message.BLOCK_BROADCAST;
    }

    public Block getMessageBody() {
        return this.block;
    }

    public boolean isForBroadcast() {
        return true;
    }

}
