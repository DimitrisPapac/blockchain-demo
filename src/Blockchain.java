import java.security.PublicKey;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.Serializable;

public class Blockchain implements Serializable {
    public static final long serialVersionUID = 1L;
    public static final double MINING_REWARD = 100.0;
    private LedgerList<Block> blockchain;

    // Constructor
    public Blockchain(Block genesisBlock) {
	this.blockchain = new LedgerList<Block>();
	this.blockchain.add(genesisBlock);
    }

    // Method for adding a new block at the end of the blockchain.
    public synchronized void addBlock(Block block) {
	if (block.getPreviousBlockHashID().equals(this.getLastBlock().getHashID()))
	    this.blockchain.add(block);
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

    // 
    public double findRelatedUTXOs(PublicKey key, ArrayList<UTXO> all,
				ArrayList<UTXO> spent, ArrayList<UTXO> unspent,
				ArrayList<Transaction> sentTransactions) {
	double gain = 0.0;
	double spending = 0.0;
	HashMap<String, UTXO> map = new HashMap<String, UTXO>();
	int limit = this.size();
	for (int a=0; a<limit; a++) {
	    Block block = this.blockchain.findByIndex(a);
	    int size = block.getTotalNumberOfTransactions();
	    for (int i=0; i<size; i++) {
		Transaction tx = block.getTransaction(i);
		int N;
		if (a != 0 && tx.getSender().equals(key)) {
		    N = tx.getNumberOfInputUTXOs();
		    for (int x=0; x<N; x++) {
			UTXO utxo = tx.getInputUTXO(x);
			spent.add(utxo);
			map.put(utxo.getHashID(), utxo);
			spending += utxo.getFundTransferred();
		    }
		    sentTransactions.add(tx);
		}
		N = tx.getNumberOfOutputUTXOs();
		for (int x=0; x<N; x++) {
		    UTXO ux = tx.getOutputUTXO(x);
		    if (ux.getReceiver().equals(key)) {
			all.add(ux);
			gain += ux.getFundTransferred();
		    }
		}
	    }
	}

	for (int i=0; i<all.size(); i++) {
	    UTXO ut = all.get(i);
	    if (!map.containsKey(ut.getHashID()))
		unspent.add(ut);
	}

	return gain - spending;
    }

    // 
    public double checkBalance(PublicKey key) {
	ArrayList<UTXO> all = new ArrayList<UTXO>();
	ArrayList<UTXO> spent = new ArrayList<UTXO>();
	ArrayList<UTXO> unspent = new ArrayList<UTXO>();
	return findRelatedUTXOs(key, all, spent, unspent);
    }

    // 
    public double findRelatedUTXOs(PublicKey key, ArrayList<UTXO> all,
				ArrayList<UTXO> spent, ArrayList<UTXO> unspent) {
	ArrayList<Transaction> sendingTransactions = new ArrayList<Transaction>();
	return findRelatedUTXOs(key, all, spent, unspent, sendingTransactions);
    }

    // 
    public ArrayList<UTXO> findUnspentUTXOs(PublicKey key) {
	ArrayList<UTXO> all = new ArrayList<UTXO>();
	ArrayList<UTXO> spent = new ArrayList<UTXO>();
	ArrayList<UTXO> unspent = new ArrayList<UTXO>();
	findRelatedUTXOs(key, all, spent, unspent);
	return unspent;
    }

    // 
    public double findUnspentUTXOs(PublicKey key, ArrayList<UTXO> unspent) {
	ArrayList<UTXO> all = new ArrayList<UTXO>();
	ArrayList<UTXO> spent = new ArrayList<UTXO>();
	return findRelatedUTXOs(key, all, spent, unspent);
    }
}