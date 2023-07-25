import java.util.ArrayList;

// Class MinerTheWorker must have access to the miner it works for,
// the message task manager, the network connection agent, and the
// queue containing the transactions for a block.
public class MinerTheWorker implements Runnable {
    private Miner miner;
    private WalletConnectionAgent agent;
    private MinerMessageTaskManager manager;
    private boolean resume = true;
    private ArrayList<Transaction> existingTransactions = null;

    // Constructor
    public MinerTheWorker(Miner miner, MinerMessageTaskManager manager,
                          WalletConnectionAgent agent, ArrayList<Transaction> existingTransactions) {
        this.miner = miner;
        this.agent = agent;
        this.existingTransactions = existingTransactions;
        this.manager = manager;
    }

    public void run() {
        final long breakTime = 2;
        System.out.printf("Miner %s begins mining a block...%n", miner.getName());
        Block block = miner.createNewBlock(miner.getLocalLedger(),
                Configuration.blockMiningDifficultyLevel());
        for (int i=0; i<this.existingTransactions.size(); i++)
            miner.addTransaction(this.existingTransactions.get(i), block);

        // Reward miner
        miner.generateRewardTransaction(block);
        try {
            Thread.sleep(breakTime);
        }
        catch (Exception e) {
            // do nothing
        }

        // Check if mining task should be aborted
        if (!resume) {
            manager.resetMiningAction();
            return;
        }

        // Mine block
        boolean b = miner.mineBlock(block);
        if (b) {
            System.out.printf("%s mined and signed the block, hashID is:%n", miner.getName());
            System.out.println(block.getHashID());
        }
        else {
            System.out.printf("%s failed to mine the block. Aborting...%n", miner.getName());
            manager.resetMiningAction();
            return;
        }

        try {
            Thread.sleep(breakTime);
        }
        catch (Exception e) {
            // do nothing
        }

        // Check if mining task should be aborted
        if (!resume) {
            manager.resetMiningAction();
            return;
        }

        // For fairness' sake, miner has to announce the block first.
        // Its local ledger is only updated if it receives the block
        // back first from the public pool, in which case it knows
        // that it won the competition.
        MessageBlockBroadcast mbbc = new MessageBlockBroadcast(block);
        this.agent.sendMessage(mbbc);
        manager.resetMiningAction();
    }

    // Method for providing a mechanism to abort the mining process.
    protected void abort() {
        this.resume = false;
    }
}
