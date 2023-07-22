import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.Serializable;

public class Blockchain implements Serializable {
	private static final long serialVersionUID = 1L;
	private LedgerList<Block> blockchain;

	public static final double MINING_REWARD = 100.0;

	// Constructor
	public Blockchain(Block genesisBlock) {
		this.blockchain = new LedgerList<Block>();
		this.blockchain.add(genesisBlock);
	}

	// Private constructor used by this class solely for the purpose of copying.
	private Blockchain(LedgerList<Block> chain) {
		this.blockchain = new LedgerList<Block>();
		int size = chain.size();
		for (int i=0; i<size; i++)
			this.blockchain.add(chain.findByIndex(i));
	}

	// Method for retrieving the blockchain's genesis block (aka: the first block).
	public Block getGenesisBlock() {
		return this.blockchain.getFirst();
	}

	// Method for retrieving the blockchain's most recently added block.
	public Block getLastBlock() {
		return this.blockchain.getLast();
	}

	// Method for retrieving the blockchain's current size.
	public int size() {
		return this.blockchain.size();
	}

	// Method for retrieving the block at a specific index.
	public Block getBlock(int index) {
		return this.blockchain.findByIndex(index);
	}

	// Method for checking the balance of an input public key/user
	public double checkBalance(PublicKey key) {
		ArrayList<UTXO> all = new ArrayList<UTXO>();
		ArrayList<UTXO> spent = new ArrayList<UTXO>();
		ArrayList<UTXO> unspent = new ArrayList<UTXO>();
		return findRelatedUTXOs(key, all, spent, unspent);
	}

	// Method for finding all UTXOs related to a particular public key.
	// Updates additional input parameters as follows:
	// all is updated to contain all UTXOs in which the input keyholder
	// acts as the receiving party (recipient),
	// spent is updated to contain all UTXOs in which the input keyholder
	// acts as the sending party (sender),
	// unspent is updated to cointain all UTXOs in all that remain unspent,
	// sentTransactions is updated to contain all Transactions in which the
	// input keyholder acts as a sending party.
	public double findRelatedUTXOs(PublicKey key, ArrayList<UTXO> all,
								   ArrayList<UTXO> spent, ArrayList<UTXO> unspent,
								   ArrayList<Transaction> sentTransactions, ArrayList<UTXO> rewards) {
		double gain = 0.0;
		double spending = 0.0;
		HashMap<String, UTXO> spentUTXOs = new HashMap<String, UTXO>();
		int limit = this.size();

		// for each block
		for (int a=0; a<limit; a++) {
			Block block = this.blockchain.findByIndex(a);
			int size = block.getTotalNumberOfTransactions();

			// for each transaction within the block
			for (int i=0; i<size; i++) {
				Transaction tx = block.getTransaction(i);
				int N;

				// if transaction is related to key in the role of sender
				if (a != 0 && tx.getSender().equals(key)) {
					// Compute number of input UTXOs
					N = tx.getNumberOfInputUTXOs();

					// for each input UTXO in the transaction
					for (int x=0; x<N; x++) {
						UTXO utxo = tx.getInputUTXO(x);
						spent.add(utxo);
						spentUTXOs.put(utxo.getHashID(), utxo);
						spending += utxo.getFundTransferred();
					}

					// save sending transaction
					sentTransactions.add(tx);
				}

				// Compute number of output UTXOs
				N = tx.getNumberOfOutputUTXOs();

				// For each output UTXO in the transaction
				for (int x=0; x<N; x++) {
					UTXO ux = tx.getOutputUTXO(x);
					// if key is the recipient in the UTXO
					if (ux.getReceiver().equals(key)) {
						all.add(ux);
						gain += ux.getFundTransferred();
					}
				}
			}

			// Add reward transactions. TODO: Modify code so that a miner is
			// allowed to underpay himself like in Bitcoin.
			if (block.getCreator().equals(key)) {
				Transaction rt = block.getRewardTransaction();
				if (rt != null && rt.getNumberOfOutputUTXOs() > 0) {
					UTXO ux = rt.getOutputUTXO(0);
					// A miner can only reward himself
					if (ux.getReceiver().equals(key)) {
						rewards.add(ux);
						all.add(ux);
						gain += ux.getFundTransferred();
					}
				}
			}
		}

		// For each UTXO transferring fund to the user
		for (int i=0; i<all.size(); i++) {
			UTXO ut = all.get(i);
			// if UTXO has not been spent yet
			if (!spentUTXOs.containsKey(ut.getHashID()))
				unspent.add(ut);
		}

		// return the balance for the input public key
		return gain - spending;
	}

	public double findRelatedUTXOs(PublicKey key, ArrayList<UTXO> all,
								   ArrayList<UTXO> spent, ArrayList<UTXO> unspent,
								   ArrayList<Transaction> sentTransactions) {
		ArrayList<UTXO> rewards = new ArrayList<UTXO>();
		return findRelatedUTXOs(key, all, spent, unspent, sentTransactions, rewards);
	}

	// Method for finding all UTXOs relating to a particular input key, while
	// also updating ArrayLists all, spent, and uspent.
	public double findRelatedUTXOs(PublicKey key, ArrayList<UTXO> all,
								   ArrayList<UTXO> spent, ArrayList<UTXO> unspent) {
		ArrayList<Transaction> sendingTransactions = new ArrayList<Transaction>();
		return findRelatedUTXOs(key, all, spent, unspent, sendingTransactions);
	}

	// Method for finding all unspent UTXOs related to a particular key
	public ArrayList<UTXO> findUnspentUTXOs(PublicKey key) {
		ArrayList<UTXO> all = new ArrayList<UTXO>();
		ArrayList<UTXO> spent = new ArrayList<UTXO>();
		ArrayList<UTXO> unspent = new ArrayList<UTXO>();
		findRelatedUTXOs(key, all, spent, unspent);
		return unspent;
	}

	// Method for finding all unspent UTXOs relating to an input public key.
	public double findUnspentUTXOs(PublicKey key, ArrayList<UTXO> unspent) {
		ArrayList<UTXO> all = new ArrayList<UTXO>();
		ArrayList<UTXO> spent = new ArrayList<UTXO>();
		return findRelatedUTXOs(key, all, spent, unspent);
	}

	// Method for checking whether a transaction already exists inside a blockchain.
	protected boolean transactionExists(Transaction tx) {
		int size = this.blockchain.size();
		for (int i=size-1; i>0; i--) {
			Block b = this.blockchain.findByIndex(i);
			int bs = b.getTotalNumberOfTransactions();
			for (int j=0; j<bs; j++) {
				Transaction tx2 = b.getTransaction(j);
				if (tx.equals(tx2))
					return true;
			}
		}
		return false;
	}

	// Method for retrieving the genesis miner of the blockchain.
	public PublicKey getGenesisMiner() {
		return this.getGenesisBlock().getCreator();
	}

	public static boolean validateBlockchain(Blockchain ledger) {
		int size = ledger.size();
		for (int i = size-1; i > 0; i--) {
			Block currentBlock = ledger.getBlock(i);
			boolean b = currentBlock.verifySignature(currentBlock.getCreator());
			if (!b) {
				System.out.println("validateBlockchain(): Block " + (i + 1) + " has an invalid signature!");
				return false;
			}
			b = UtilityMethods.hashMeetsDifficultyLevel(currentBlock.getHashID(),
					currentBlock.getDifficultyLevel()) &&
					currentBlock.computeHashID().equals(currentBlock.getHashID());
			if (!b) {
				System.out.println("validateBlockchain(): Block " + (i + 1) + " has a bad hash!");
				return false;
			}
			Block previousBlock = ledger.getBlock(i-1);
			b = currentBlock.getPreviousBlockHashID().equals(previousBlock.getHashID());
			if (!b)
			{
				System.out.println("validateBlockchain(): Block " + (i + 1) + " has an invalid previous block hash ID!");
				return false;
			}
		}
		Block genesisBlock = ledger.getGenesisBlock();
		// Confirm that the genesis block has been signed
		boolean b2 = genesisBlock.verifySignature(genesisBlock.getCreator());
		if (!b2) {
			System.out.println("validateBlockchain(): Genesis block is tampered!");
			return false;
		}

		b2 = UtilityMethods.hashMeetsDifficultyLevel(genesisBlock.getHashID(),
				genesisBlock.getDifficultyLevel()) &&
				genesisBlock.computeHashID().equals(genesisBlock.getHashID());
		if (!b2) {
			System.out.println("validateBlockchain(): Genesis block has a bad hash!");
			return false;
		}
		return true;
	}

	// Method for adding a new valid block to the blockchain.
	public synchronized boolean addBlock(Block block) {
		if (this.size() == 0) {
			this.blockchain.add(block);
			return true;
		} else if (block.getPreviousBlockHashID().equals(this.getLastBlock().getHashID())) {
			this.blockchain.add(block);
			return true;
		} else
			return false;
	}

	// Method for creating a "shallow copy" of the blockchain.
	// Blocks and their order are preserved.
	public synchronized Blockchain copy_NotDeepCopy() {
		return new Blockchain(this.blockchain);
	}
}