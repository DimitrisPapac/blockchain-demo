import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Calendar;
import java.io.Serializable;

public class Transaction implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final double TRANSACTION_FEE = 1.0;
    private String hashID;
    private PublicKey sender;
    private PublicKey[] receivers;
    private double[] fundToTransfer;
    private long timestamp;
    private ArrayList<UTXO> inputs = null;
    private ArrayList<UTXO> outputs = new ArrayList<UTXO>(4);
    private byte[] signature = null;
    private boolean signed = false;
    private long mySequentialNumber;

    // Constructor for single receiver
    public Transaction(PublicKey sender, PublicKey receiver,
			    double fundToTransfer, ArrayList<UTXO> inputs) {
	PublicKey[] pks = new PublicKey[1];
	pks[0] = receiver;
	double[] funds = new double[1];
	funds[0] = fundToTransfer;
	this.setUp(sender, pks, funds, inputs);
    }

    // Constructor for multiple receivers
    public Transaction(PublicKey sender, PublicKey[] receivers,
			    double[] fundToTransfer, ArrayList<UTXO> inputs) {
	this.setUp(sender, receivers, fundToTransfer, inputs);
    }

    public boolean prepareOutputUTXOs() {
	if (this.receivers.length != this.fundToTransfer.length)
	    return false;

	double totalCost = this.getTotalFundToTransfer() + Transaction.TRANSACTION_FEE;
	double availableAmount = 0.0;
	for (int i=0; i<this.inputs.size(); i++)
	    availableAmount += this.inputs.get(i).getFundTransferred();

	if (availableAmount < totalCost)
	    return false;

	this.outputs.clear();
	for (int i=0; i<receivers.length; i++) {
	    UTXO utxo = new UTXO(this.getHashID(), this.sender,
			receivers[i], this.fundToTransfer[i]);
	    this.outputs.add(utxo);
	}
	UTXO change = new UTXO(this.getHashID(), this.sender,
				this.sender, availableAmount - totalCost);
	this.outputs.add(change);
	return true;
    }

    private void setUp(PublicKey sender, PublicKey[] receivers,
			    double[] fundToTransfer, ArrayList<UTXO> inputs) {
	this.mySequentialNumber = UtilityMethods.getUniqueNumber();
	this.sender = sender;
	this.receivers = new PublicKey[1];
	this.receivers = receivers;
	this.fundToTransfer = fundToTransfer;
	this.inputs = inputs;
	this.timestamp = Calendar.getInstance().getTimeInMillis();
	computeHashID();
    }

    public void signTheTransaction(PrivateKey privateKey) {
	if (this.signature == null && !signed) {
	    this.signature = UtilityMethods.generateSignature(privateKey, getMessageData());
	    signed = true;
	}
    }

    public boolean verifySignature() {
	String message = getMessageData();
	return UtilityMethods.verifySignature(this.sender, this.signature, message);
    }

    private String getMessageData() {
	StringBuilder sb = new StringBuilder();
	sb.append(UtilityMethods.getKeyString(sender)
		    + Long.toHexString(this.timestamp)
		    + Long.toString(this.mySequentialNumber));

	for (int i=0; i<this.receivers.length; i++)
	    sb.append(UtilityMethods.getKeyString(this.receivers[i])
		+ Double.toHexString(this.fundToTransfer[i]));

	for (int i=0; i<this.getNumberOfInputUTXOs(); i++) {
	    UTXO utxo = this.getInputUTXO(i);
	    sb.append(utxo.getHashID());
	}

	return sb.toString();
    }

    protected void computeHashID() {
	String message = getMessageData();
	this.hashID = UtilityMethods.messageDigestSHA256_toString(message);
    }

    public String getHashID() {
	return this.hashID;
    }

    public PublicKey getSender() {
	return this.sender;
    }

    public long getTimeStamp() {
	return this.timestamp;
    }

    public long getSequentialNumber() {
	return this.mySequentialNumber;
    }

    public double getTotalFundToTransfer() {
	double total = 0;
	for (double amount : this.fundToTransfer)
	    total += amount;
	return total;
    }

    protected void addOutputUTXO(UTXO utxo) {
	if (!signed)
	    outputs.add(utxo);
    }

    public int getNumberOfOutputUTXOs() {
	return this.outputs.size();
    }

    public UTXO getOutputUTXO(int i) {
	return this.outputs.get(i);
    }

    public int getNumberOfInputUTXOs() {
	if (this.inputs == null)
	    return 0;
	return this.inputs.size();
    }

    public UTXO getInputUTXO(int i) {
	return this.inputs.get(i);
    }

    // Transactions are considered equal if they have the same hash ID.
    public boolean equals(Transaction tx) {
	return this.getHashID().equals(tx.getHashID());
    }
}