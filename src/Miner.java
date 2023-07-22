import jdk.jshell.execution.Util;

public class Miner extends Wallet {
    // Constructor
    public Miner(String minerName, String password) {
        super(minerName, password);
    }

    // This constructor is redundant
    public Miner(String minerName) {
        super(minerName);
    }

    // Method for mining a block.
    public boolean mineBlock(Block block) {
        if ((block.mineTheBlock(this.getPublicKey()))) {
            // miner signs the block
            byte[] signature = UtilityMethods.generateSignature(this.getPrivateKey(),
                    block.getHashID());
            return block.signTheBlock(this.getPublicKey(), signature);
        }
        else
           return false;
    }

    // Method for validating a transaction before adding it to a block.
    public boolean addTransaction(Transaction tx, Block block) {
        if (this.validateTransaction(tx))
            return block.addTransaction(tx, this.getPublicKey());
        else
            return false;
    }

    // Method for deleting a transaction before the block has been
    // mined and signed. Only the block creator is authorized to
    // perform the deletion.
    public boolean deleteTransaction(Transaction tx, Block block) {
        return block.deleteTransaction(tx, this.getPublicKey());
    }

    public boolean generateRewardTransaction(Block block) {
        double amount = Blockchain.MINING_REWARD + block.getTransactionFeeAmount();
        Transaction tx = new Transaction(this.getPublicKey(),
                this.getPublicKey(), amount, null);
        UTXO utxo = new UTXOAsMiningReward(tx.getHashID(), tx.getSender(),
                this.getPublicKey(), amount);
        tx.addOutputUTXO(utxo);
        tx.signTheTransaction(this.getPrivateKey());
        return block.generateRewardTransaction(this.getPublicKey(), tx);
    }

    // Method for allowing miners to create new blocks.
    public Block createNewBlock(Blockchain ledger, int difficultyLevel) {
        Block block = new Block(ledger.getLastBlock().getHashID(),
                difficultyLevel, this.getPublicKey());
        return block;
    }
}