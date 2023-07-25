import java.security.PublicKey;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MinerMessageTaskManager extends WalletMessageTaskManager implements Runnable {
    private boolean miningAction = true;
    private ArrayList<Transaction> existingTransactions = new ArrayList<Transaction>();
    private WalletConnectionAgent agent;

    // Constructor
    public MinerMessageTaskManager(WalletConnectionAgent agent, Miner miner,
                                   ConcurrentLinkedQueue<Message> messageQueue) {
        super(agent, miner, messageQueue);
        this.agent = agent;
    }

    protected synchronized void resetMiningAction() {
        this.miningAction = true;
    }

    protected synchronized boolean getMiningAction() {
        return this.miningAction;
    }

    protected synchronized void raiseMiningAction() {
        this.miningAction = false;
    }

    // A miner must respond to a query for blockchain.
    protected void receiveQueryForBlockchainBroadcast(MessageAskForBlockchainBroadcast mabcb) {
        PublicKey receiver = mabcb.getSenderKey();
        Blockchain bc = myWallet().getLocalLedger().copy_NotDeepCopy();
        MessageBlockchainPrivate message = new MessageBlockchainPrivate(bc,
                myWallet().getPublicKey(), receiver);
        boolean b = this.agent.sendMessage(message);
        if (b)
            System.out.println(myWallet().getName()
                        + ": sent local blockchain to the requester, chain size = "
                        + message.getMessageBody().size() + "|" + message.getInfoSize());
        else
            System.out.println(myWallet().getName()
                    + ": failed to send local blockchain to the requester");
    }

    // A miner needs to collect the transactions for block mining.
    // TODO: Disallow miners from collecting transactions temporarily if
    // they're currently mining a block and have won a number of consecutive
    // mining competitions.
    protected void receiveMessageTransactionBroadcast(MessageTransactionBroadcast mtb) {
        Transaction tx = mtb.getMessageBody();
        // tx should not be already present in the current transaction pool
        for (int i=0; i<this.existingTransactions.size(); i++)
            if (tx.equals(this.existingTransactions.get(i)))
                return;

        // Only add this transaction into the existing storage if it is valid.
        // Otherwise, ignore it.
        if (!myWallet().validateTransaction(tx)) {
            System.out.println("Miner " + myWallet().getName()
                            + " found an invalid transaction.");
            return;
        }
        this.existingTransactions.add(tx);

        // Assess if it is good to start building a block
        if (this.existingTransactions.size() >= Block.TRANSACTION_LOWER_LIMIT
            && this.getMiningAction()) {
            this.raiseMiningAction();
            System.out.println(myWallet().getName() + " has enough transactions "
                            + "to mine the block now, miningAction requirement met. "
                            + "Start mining a new block...");
            // Create a MinerTheWorker to mine the block
            MinerTheWorker worker = new MinerTheWorker(myWallet(),
                    this, this.agent, this.existingTransactions);
            Thread miningThread = new Thread(worker);
            miningThread.start();
            // Once the mining starts, pool new incoming Transactions
            this.existingTransactions = new ArrayList<Transaction>();
        }
    }

    @Override
    protected Miner myWallet() {
        return (Miner) super.myWallet();
    }
}
