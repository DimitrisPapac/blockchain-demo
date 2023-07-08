import java.util.ArrayList;
import java.util.Calendar;
import java.io.Serializable;
import java.util.ArrayList;

public class Block implements Serializable {
    private static final long serialVersionUID = 1L;
    private int difficultyLevel = 25;
    private ArrayList<Transaction> transactions = new ArrayList<Transaction>();
    private long timestamp;
    private String previousBlockHashID;
    private int nonce = 0;
    private String hashID;

    public final static int TRANSACTION_UPPER_LIMIT = 2;

    // Constructor
    public Block(String previousBlockHashID, int difficultyLevel) {
	this.previousBlockHashID = previousBlockHashID;
	this.timestamp = UtilityMethods.getTimeStamp();
	this.difficultyLevel = difficultyLevel;
    }

    protected String computeHashID() {
	StringBuilder sb = new StringBuilder();
	sb.append(this.previousBlockHashID + Long.toHexString(this.timestamp));

	for (Transaction t : transactions)
	    sb.append(t.getHashID());

	sb.append(Integer.toHexString(this.difficultyLevel) + nonce);
	byte[] b = UtilityMethods.messageDigestSHA256_toBytes(sb.toString());

	return UtilityMethods.toBinaryString(b);
    }

    public boolean addTransaction(Transaction t) {
	// For security, at most one block can be fetched at a time
	if (this.getTotalNumberOfTransactions() >= Block.TRANSACTION_UPPER_LIMIT)
	    return false;
	this.transactions.add(t);
	return true;
    }

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

    protected boolean mineTheBlock() {
	this.hashID = this.computeHashID();

	while (!UtilityMethods.hashMeetsDifficultyLevel(this.hashID, this.difficultyLevel)) {
	    this.nonce++;
	    this.hashID = this.computeHashID();
	}

	return true;
    }

    public int getDifficultyLevel() {
	return this.difficultyLevel;
    }

    public int getTotalNumberOfTransactions() {
	return this.transactions.size();
    }

    public Transaction getTransaction(int index) {
	return this.transactions.get(index);
    }
}
