import java.security.PublicKey;
import java.io.Serializable;

// Class UTXO represents spendable funds
public class UTXO implements Serializable {
    private static final long serialVersionUID = 1L;
    private String hashID;
    private String parentTransactionID;
    private PublicKey sender;
    private PublicKey receiver;
    private long timestamp;
    private double fundTransferred;
    private long sequentialNumber = 0;

    // Constructor
    public UTXO(String parentTransactionID, PublicKey sender,
                PublicKey receiver, double fundToTransfer) {
        this.sequentialNumber = UtilityMethods.getUniqueNumber();
        this.parentTransactionID = parentTransactionID;
        this.sender = sender;
        this.receiver = receiver;
        this.fundTransferred = fundToTransfer;
        this.timestamp = UtilityMethods.getTimeStamp();
        this.hashID = computeHashID();
    }

    protected String computeHashID() {
        String message = this.parentTransactionID
                + UtilityMethods.getKeyString(this.sender)
                + UtilityMethods.getKeyString(this.receiver)
                + Double.toHexString(this.fundTransferred)
                + Long.toHexString(this.timestamp)
                + Long.toHexString(this.sequentialNumber);

        return UtilityMethods.messageDigestSHA256_toString(message);
    }

    public String getHashID() {
        return this.hashID;
    }

    public String getParentTransactionID() {
        return this.parentTransactionID;
    }

    public PublicKey getSender() {
        return this.sender;
    }

    public PublicKey getReceiver() {
        return this.receiver;
    }

    public long getTimeStamp() {
        return this.timestamp;
    }

    public long getSequentialNumber() {
        return this.sequentialNumber;
    }

    public double getFundTransferred() {
        return this.fundTransferred;
    }

    // Two UTXOs are equal if they have the same hash ID.
    public boolean equals(UTXO utxo) {
        return this.getHashID().equals(utxo.getHashID());
    }

    public boolean isMiningReward() {
        return false;
    }
}