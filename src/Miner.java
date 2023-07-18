public class Miner extends Wallet {
    // Constructor
    public Miner(String minerName, String password) {
        super(minerName, password);
    }

    // This constructor is redundant
    public Miner(String minerName) {
        super(minerName);
    }

    public boolean mineBlock(Block block) {
        return block.mineTheBlock();
    }
}