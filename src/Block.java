import java.security.PublicKey;
import java.util.ArrayList;
import java.io.Serializable;
// import java.util.Calendar;

public class Block implements Serializable {
    private static final long serialVersionUID = 1L;
    private int difficultyLevel = 20;   // default difficulty level
    private ArrayList<Transaction> transactions = new ArrayList<Transaction>();
    private long timestamp;
    private String previousBlockHashID;
    private int nonce = 0;
    private String hashID;

    // for recording the miner/creator of the block
    private PublicKey creator;

    private boolean mined = false;

    // miner's signature
    private byte[] signature = null;

    // the transaction to reward the miner
    private Transaction rewardTransaction = null;

    // maximum number of transactions per block
    public final static int TRANSACTION_UPPER_LIMIT = 100;
    // minimum number of valid transactions before starting a block
    public final static int TRANSACTION_LOWER_LIMIT = 1;

    // Constructor
    public Block(String previousBlockHashID, int difficultyLevel, PublicKey creator) {
        this.previousBlockHashID = previousBlockHashID;
        this.timestamp = UtilityMethods.getTimeStamp();
        this.difficultyLevel = difficultyLevel;
        this.creator = creator;
    }

    protected String computeHashID() {
        StringBuilder sb = new StringBuilder();   // for gathering together the hash input
        sb.append(this.previousBlockHashID + Long.toHexString(this.timestamp));

        sb.append(this.computeMerkleRoot());
        sb.append(String.valueOf(nonce));
        //sb.append(Integer.toHexString(this.difficultyLevel) + this.nonce);

        // Compute hash as an array of bytes
        byte[] b = UtilityMethods.messageDigestSHA256_toBytes(sb.toString());

        return UtilityMethods.toBinaryString(b);
    }

    // Method for adding a new transaction to the block.
    public boolean addTransaction(Transaction t, PublicKey key) {
        // For security, at most one block can be fetched at a time
        if (this.getTotalNumberOfTransactions() >= Block.TRANSACTION_UPPER_LIMIT)
            return false;

        // Only the creator of the block is allowed to add transactions and
        // only prior to the block has been mined and signed
        if (key.equals(this.getCreator()) && !this.isMined() && !this.isSigned()) {
            this.transactions.add(t);
            return true;
        }
        else
            return false;
    }

    /* Getter methods */

    public String getHashID() {
        return this.hashID;
    }

    public int getNonce() {
        return this.nonce;
    }

    public long getTimeStamp() {
        return this.timestamp;
    }

    public String getPreviousBlockHashID() {
        return this.previousBlockHashID;
    }

    public int getDifficultyLevel() {
        return this.difficultyLevel;
    }

    public int getTotalNumberOfTransactions() {
        return this.transactions.size();
    }

    // Method for retrieving a Transaction found in a particular index.
    public Transaction getTransaction(int index) {
        return this.transactions.get(index);
    }

    // Method for retrieving the reward transaction
    public Transaction getRewardTransaction() {
        return this.rewardTransaction;
    }

    public double getTransactionFeeAmount() {
        return this.transactions.size() * Transaction.TRANSACTION_FEE;
    }

    // Method for mining the block
    protected boolean mineTheBlock(PublicKey key) {
        // Only the creator of the block can mine it, provided it hasn't
        // been mined yet
        if (!this.mined && key.equals(this.getCreator())) {
            this.hashID = this.computeHashID();
            while (!UtilityMethods.hashMeetsDifficultyLevel(this.hashID, this.difficultyLevel)) {
                this.nonce++;
                this.hashID = this.computeHashID();
            }
            this.mined = true;
        }
        return this.mined;
    }

    public boolean generateRewardTransaction(PublicKey pk, Transaction rewardTransaction) {
        // A block can only generate one reward transaction. It can only be added by the
        // block's creator and cannot be changed once it has been added
        if (this.rewardTransaction == null && pk.equals(this.creator)) {
            this.rewardTransaction = rewardTransaction;
            return true;
        }
        else
            return false;
    }

}