import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;

public class Wallet {
	private KeyPair keyPair;
	private String walletName;
	private static String keyLocation = Configuration.KeyLocation();
	private Blockchain localLedger = null;  // wallet's local copy of the blockchain

	// Constructor
	public Wallet(String walletName, String password) {
		this.walletName = walletName;
		this.keyPair = UtilityMethods.generateKeyPair();
		try {
			populateExistingWallet(walletName, password);
			System.out.println("A wallet exists with the same name "
					+ "and password. Loaded the existing wallet.");
		}
		catch (Exception e) {   // wallet does not exist
			try {
				this.prepareWallet(password);
				System.out.println("Created a new wallet based on "
						+ "the name and password.");
			}
			catch (IOException ioe) {
				throw new RuntimeException(ioe);
			}
		}
	}

	// Constructor with only the wallet's name as input
	public Wallet(String walletName) {
		this.walletName = walletName;
		this.keyPair = UtilityMethods.generateKeyPair();
	}

	public String getName() {
		return this.walletName;
	}

	public PublicKey getPublicKey() {
		return this.keyPair.getPublic();
	}

	public PrivateKey getPrivateKey() {
		return this.keyPair.getPrivate();
	}

	// Method for preparing a new wallet.
	private void prepareWallet(String password)
			throws IOException, FileNotFoundException {
		ByteArrayOutputStream bo = new ByteArrayOutputStream();
		ObjectOutputStream out = new ObjectOutputStream(bo);

		// Write key pair into the ByteArrayOutputStream object
		out.writeObject(this.keyPair);

		// Fetch, encrypt, and store encrypted data
		byte[] keyBytes =
				UtilityMethods.encryptionByAES(bo.toByteArray(), password);

		File f = new File(Wallet.keyLocation);
		// If directory for storing keys does not exist, create directory
		if (!f.exists())
			f.mkdir();

		String path = String.format("%s/%s_keys", Wallet.keyLocation,
				this.getName().replace(' ', '_'));
		FileOutputStream fout = new FileOutputStream(path);

		// Write encrypted data to file
		fout.write(keyBytes);

		// Close output streams
		fout.close();
		bo.close();
	}

	// Method for populating and existing wallet.
	private void populateExistingWallet(String walletName, String password)
			throws IOException, FileNotFoundException, ClassNotFoundException {
		this.walletName = walletName;

		// Create a FileInputStream to read the encrypted key pair from file
		String path = String.format("%s/%s_keys", Wallet.keyLocation,
				this.getName().replace(' ', '_'));
		FileInputStream fin = new FileInputStream(path);

		// Read encrypted key pair bytes into a byte array
		byte[] bb = new byte[4096];
		int size = fin.read(bb);   // number of bytes loaded into bb

		// Close input stream
		fin.close();

		// Copy bytes from byte array bb into byte array data
		byte[] data = new byte[size];
		for (int i=0; i<data.length; i++)
			data[i] = bb[i];

		// Decrypt data
		byte[] keyBytes = UtilityMethods.decryptionByAES(data, password);

		// Deserialize bytes back into and object
		ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(keyBytes));

		// Cast into KeyPair object
		this.keyPair = (KeyPair) (in.readObject());
	}

	// Getter method for retrieving the local ledger.
	public synchronized Blockchain getLocalLedger() {
		return this.localLedger;
	}

	// Setter method for updating the wallet's local ledger to the input ledger.
	public synchronized boolean setLocalLedger(Blockchain ledger) {
		// Validate incoming ledger
		boolean b = Blockchain.validateBlockchain(ledger);
		if (!b) {
			System.out.printf("%s] Warning: incoming blockchain does not validate!%n", this.getName());
			return false;
		}
		// If the wallet does not have a local ledger initialized yet
		if (this.localLedger == null) {
			this.localLedger = ledger;
			return true;
		}
		else {
			// Incoming ledger must be strictly longer than local ledger
			// and both must share the same genesis block
			if (ledger.size() > this.localLedger.size() &&
						ledger.getGenesisMiner().equals(this.localLedger.getGenesisMiner())) {
				this.localLedger = ledger;
				return true;
			}
			else if (ledger.size() <= this.localLedger.size()) {
				System.out.println(this.getName() + "] Warning: incoming blockchain "
						+ "is shorter than local blockchain.");
				System.out.printf("Incoming blockchain size: %d blocks%n", ledger.size());
				System.out.printf("Local blockchain size: %d blocks%n", this.localLedger.size());
				return false;
			}
			else {
				System.out.println(this.getName() + "] Warning: incoming blockchain "
						+ "and local blockchain have different genesis miners.");
				return false;
			}
		}
	}

	//
	public synchronized boolean updateLocalLedger(ArrayList<Blockchain> chains) {
		// If input ArrayList is empty, no action is needed
		if (chains.size() == 0)
			return false;
		if (this.localLedger == null) {   // local ledger has not been initialized
			// Pick the longest, successfully validating chain
			Blockchain max = null;
			int currentLength = 0;
			for (int i=0; i<chains.size(); i++) {
				Blockchain bc = chains.get(i);
				boolean b = Blockchain.validateBlockchain(bc);
				if (b && bc.size() > currentLength) {
					max = bc;
					currentLength = max.size();
				}
			}
			if (max != null) {
				this.localLedger = max;
				return true;
			}
			else return false;
		}
		else {   // local ledger is already initialized
			// Find the longest validating blockchain
			Blockchain max = this.localLedger;
			for (int i=0; i<chains.size(); i++) {
				Blockchain bc = chains.get(i);
				boolean b = bc.getGenesisMiner().equals(this.localLedger.getGenesisMiner());
				if (b && bc.size() > max.size() && Blockchain.validateBlockchain(bc))
					max = bc;
			}
			this.localLedger = max;
			return true;
		}
	}

	// When a new block is received, it must be verified before being
	// added to the local blockchain.
	public synchronized boolean updateLocalLedger(Block block) {
		if (verifyGuestBlock(block))
			return this.localLedger.addBlock(block);
		return false;
	}

	// Method for verifying an incoming block against a blockchain.
	public boolean verifyGuestBlock(Block block, Blockchain ledger) {
		// Verify signature
		if (!block.verifySignature(block.getCreator())) {
			System.out.printf("\tWarning: Block(%s) has an invalid signature!%n", block.getHashID());
			return false;
		}
		// Verify PoW including recomputation of the block's hash
		if (!UtilityMethods.hashMeetsDifficultyLevel(block.getHashID(),
						block.getDifficultyLevel()) ||
						!block.computeHashID().equals(block.getHashID())) {
			System.out.printf("\tWarning: Block(%s) mining is not successful!%n",
					block.getHashID());
			return false;
		}
		// Ensure that the block is built upon the last block
		if (!ledger.getLastBlock().getHashID().equals(block.getPreviousBlockHashID())) {
			System.out.printf("\tWarning: Block(%s) mining is not linked to the last block!%n",
					block.getHashID());
			return false;
		}
		// Examine if all transactions in the block are valid.
		int size = block.getTotalNumberOfTransactions();
		for (int i=0; i<size; i++) {
			Transaction tx = block.getTransaction(i);
			if (!validateTransaction(tx)) {
				System.out.println("\tWarning: Block(" + block.getHashID()
						+ ") transaction " + i + " is invalid either because "
						+ "its signature has been tampered, or it already "
						+ "exists in the blockchain.");
				return false;
			}
		}
		Transaction tr = block.getRewardTransaction();
		if (tr.getTotalFundToTransfer() > Blockchain.MINING_REWARD + block.getTransactionFeeAmount()) {
			System.out.printf("\tWarning: Block(%s) overrewarded%n", block.getHashID());
			return false;
		}
		return true;
	}

	// Method for verifying a newly arrived guest block.
	public boolean verifyGuestBlock(Block block) {
		return this.verifyGuestBlock(block, this.getLocalLedger());
	}

	// Method for validating transactions before collecting them into a block.
	public boolean validateTransaction(Transaction tx) {
		if (tx == null)
			return false;
		if (!tx.verifySignature()) {
			System.out.println("WARNING: transaction ID = " + tx.getHashID() + " from "
								+ UtilityMethods.getKeyString(tx.getSender())
								+ " is invalid. Tampering detected!");
			return false;
		}

		// Ensure that transaction does not already exist in the ledger.
		// TODO: Optimize since this is time-consuming.
		boolean exists;
		if (this.getLocalLedger() == null)
			exists = false;
		else
			exists = this.getLocalLedger().transactionExists(tx);
		return !exists;
	}


	// Method for computing the wallet's balance from the input ledger.
	public double getCurrentBalance(Blockchain ledger) {
		return ledger.checkBalance(this.getPublicKey());
	}

	// Method for transferring amounts of fund to multiple recipients.
	// The method output the signed transaction.
	public Transaction transferFund(PublicKey[] receivers, double[] fundToTransfer) {
		ArrayList<UTXO> unspent = new ArrayList<UTXO>();
		double available = this.getLocalLedger().findUnspentUTXOs(this.getPublicKey(),
				unspent);
		double totalNeeded = Transaction.TRANSACTION_FEE;
		for (int i=0; i<fundToTransfer.length; i++)
			totalNeeded += fundToTransfer[i];

		// if available funds are insufficient
		if (available < totalNeeded) {
			System.out.printf("%s balance = %.2f, not enough to make the transfer of %.2f",
					this.getName(), available, totalNeeded);
			return null;
		}

		// Create input for the transaction
		ArrayList<UTXO> inputs = new ArrayList<UTXO>();
		available = 0.0;
		for (int i=0; i<unspent.size() && available < totalNeeded; i++) {
			UTXO uxo = unspent.get(i);
			available += uxo.getFundTransferred();
			inputs.add(uxo);
		}

		// Create transaction
		Transaction tx = new Transaction(this.getPublicKey(),
				receivers, fundToTransfer, inputs);

		// Prepare output UTXO
		boolean b = tx.prepareOutputUTXOs();
		if (b) {
			tx.signTheTransaction(this.getPrivateKey());
			return tx;
		}
		else
			return null;
	}

	// Overloaded method for single receiver.
	public Transaction transferFund(PublicKey receiver, double fundToTransfer) {
		PublicKey[] receivers = { receiver };
		double[] funds = { fundToTransfer };
		return transferFund(receivers, funds);
	}
}