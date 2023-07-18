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
	private static String keyLocation = "keys";
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
		this.localLedger = ledger;
		return true;
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