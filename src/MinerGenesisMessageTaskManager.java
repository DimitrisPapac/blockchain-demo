import jdk.jshell.execution.Util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

public final class MinerGenesisMessageTaskManager extends
                    MinerMessageTaskManager implements Runnable {
    // Record how many sign-in bonus blocks have been mined.
    private int blocksMined = 0;
    // Maximum sign-in bonuses the genesis miner will send out.
    public static final int SIGN_IN_BONUS_USERS_LIMIT = 1_000;
    // List of wallets that the genesis miner has collected.
    private HashMap<String, KeyNamePair> users = new HashMap<String, KeyNamePair>();
    private WalletConnectionAgent agent;
    private final int signInBonus = 1_000;
    // List of wallets to which the genesis miner needs to send sign-in bonuses.
    private ArrayList<KeyNamePair> waitingListForSignInBonus = new ArrayList<KeyNamePair>();

    // Constructor
    public MinerGenesisMessageTaskManager(WalletConnectionAgent agent,
                                          Miner miner, ConcurrentLinkedQueue<Message> messageQueue) {
        super(agent, miner, messageQueue);
        this.agent = agent;
    }

    // Genesis miner polls the message service provider for new users by sending
    // a MessageTextPrivate until it has enough new users for sign-in bonuses.
    public void whatToDo() {
        try {
            Thread.sleep(10 * agent.sleepTime);
            if (waitingListForSignInBonus.size() == 0
                && users.size() < SIGN_IN_BONUS_USERS_LIMIT) {
                MessageTextPrivate mtp = new MessageTextPrivate(Message.TEXT_ASK_ADDRESSES,
                        myWallet().getPrivateKey(), myWallet().getPublicKey(),
                        myWallet().getName(), this.agent.getServerAddress());
                agent.sendMessage(mtp);
                Thread.sleep(10 * agent.sleepTime);
            }
            else
                sendSignInBonus();
        }
        catch (Exception e) {}
    }

    // Method for initiating an instance of MinerTheWorker to construct
    // and mine a block that is a sign-in bonus for a new coming blockchain
    // participant. If however the genesis miner has mined enough sign-in
    // bonus blocks, the sign-in bonus will be broadcast as a transaction
    // s.t. other miners can collect it.
    private void sendSignInBonus() {
        if (waitingListForSignInBonus.size() == 0)
            return;
        KeyNamePair pk = waitingListForSignInBonus.remove(0);
        // Sign-in bonuses are prepared as Transactions
        Transaction tx = myWallet().transferFund(pk.getKey(), signInBonus);
        if (tx != null && tx.verifySignature()) {
            System.out.printf("%s is sending %s as a sign-in bonus of %s.%n",
                    myWallet().getName(), pk.getName(), signInBonus);
            if (blocksMined < Configuration.SELF_BLOCKS_TO_MINE_LIMIT && this.getMiningAction()) {
                blocksMined++;
                this.raiseMiningAction();
                System.out.printf("%s is mining the sign-in bonus block for %s by himself.%n",
                        myWallet().getName(), pk.getName());
                ArrayList<Transaction> tss = new ArrayList<Transaction>();
                tss.add(tx);
                MinerTheWorker worker = new MinerTheWorker(myWallet(),
                        this, this.agent, tss);
                Thread miningThread = new Thread(worker);
                miningThread.start();
            }
            else {
                // Broadcast this transaction
                System.out.printf("%s is broadcasting the transaction of sign-in bonus for %s.%n",
                        myWallet().getName(), pk.getName());
                MessageTransactionBroadcast mtbc = new MessageTransactionBroadcast(tx);
                this.agent.sendMessage(mtbc);
            }
        }
        else
            waitingListForSignInBonus.add(0, pk);   // in theory, this will never occur
    }

    // Method specifying the genesis miner's behavior when receiving a block.
    protected void receiveMessageBlockBroadcast(MessageBlockBroadcast mbb) {
        Block block = mbb.getMessageBody();
        boolean b = myWallet().verifyGuestBlock(block, myWallet().getLocalLedger());
        boolean c = false;
        if (b)
            this.myWallet().updateLocalLedger(block);

        if (b && c) {
            System.out.println("New block is added to the local blockchain, "
                                + "blockchain size = " + this.myWallet().getLocalLedger().size());
            // Display the balance of the genesis miner in detail for
            // verification purposes.
            displayWallet_MinerBalance(myWallet());
        }
        else {
            System.out.println("New block is rejected.");
            // Check if this block is a sign-in bonus block; if it is, mine it again.
            if (block.getCreator().equals(myWallet().getPublicKey())) {
                System.out.println("Genesis miner needs to re-mine a sign-in bonus block.");
                String id = UtilityMethods.getKeyString(
                        block.getTransaction(0).getOutputUTXO(0).getReceiver());
                KeyNamePair pk = users.get(id);
                if (pk != null) {
                    // Add at the beginning of the waiting list
                    waitingListForSignInBonus.add(pk);
                }
                else {
                    // Should never occur
                    System.out.println("ERROR: An existing user for "
                        + "sign-in bonus is not found. Program error!");
                }
            }
        }
    }

    protected void receiveMessageTransactionBroadcast(MessageTransactionBroadcast mtbc) {
        // Ignore such messages
    }

    // MessageAddressPrivate messages must originate from the message service provider.
    protected void receiveMessageAddressPrivate(MessageAddressPrivate map) {
        ArrayList<KeyNamePair> all = map.getMessageBody();
        for (int i=0; i<all.size(); i++) {
            KeyNamePair pk = all.get(i);
            String id = UtilityMethods.getKeyString(pk.getKey());
            if (!pk.getKey().equals(myWallet().getPublicKey())
                && !users.containsKey(id)) {
                users.put(id, pk);
                if (users.size() <= SIGN_IN_BONUS_USERS_LIMIT)
                    this.waitingListForSignInBonus.add(pk);
            }
        }
    }

    protected void receivePrivateChatMessage(MessageTextPrivate mtp) {
        // Do nothing for the genesis miner
    }

    protected void receiveMessageTextBroadcast(MessageTextBroadcast mtb) {
        // Do nothing for the genesis miner
    }

    protected void askForLatestBlockChain() {
        // Do nothing for the genesis miner
    }

    // Method for displaying the balance of the genesis miner in detail for
    // verification purposes.
    public static final void displayWallet_MinerBalance(Wallet miner) {
        ArrayList<UTXO> all = new ArrayList<UTXO>();
        ArrayList<UTXO> spent = new ArrayList<UTXO>();
        ArrayList<UTXO> unspent = new ArrayList<UTXO>();
        ArrayList<Transaction> ts = new ArrayList<Transaction>();
        double balance = miner.getLocalLedger().findRelatedUTXOs(
                miner.getPublicKey(), all, spent, unspent, ts);
        System.out.println("{");
        System.out.println("\t" + miner.getName() + ": Balance = " + balance
                    + ", Local blockchain size = " + miner.getLocalLedger().size());

        double income = 0;
        System.out.println("\tAll UTXOs:");
        for (int i=0; i<all.size(); i++) {
            UTXO utxo = all.get(i);
            System.out.println("\t\t" + utxo.getFundTransferred() + "|" + utxo.getHashID()
                + "|from = " + UtilityMethods.getKeyString(utxo.getSender())
                + "|to = " + UtilityMethods.getKeyString(utxo.getReceiver()));
            income += utxo.getFundTransferred();
        }
        System.out.printf("\t----- Total Income = %.2f ----------%n", income);

        System.out.println("\tSpent UTXOs:");
        income = 0;
        for (int i=0; i<spent.size(); i++) {
            UTXO utxo = spent.get(i);
            System.out.println("\t\t" + utxo.getFundTransferred() + "|" + utxo.getHashID()
                    + "|from = " + UtilityMethods.getKeyString(utxo.getSender())
                    + "|to = " + UtilityMethods.getKeyString(utxo.getReceiver()));
            income += utxo.getFundTransferred();
        }
        System.out.printf("\t----- Total Spending = %.2f ----------%n", income);

        double tsFee = ts.size() * Transaction.TRANSACTION_FEE;
        if (tsFee > 0)
            System.out.println("\t\tTransaction Fee " + tsFee + "is automatically "
                + "deducted. Please do not include it in the calculation.");

        System.out.println("\tUnspent UTXOs:");
        income = 0;
        for (int i=0; i<unspent.size(); i++) {
            UTXO utxo = unspent.get(i);
            System.out.println("\t\t" + utxo.getFundTransferred() + "|" + utxo.getHashID()
                    + "|from = " + UtilityMethods.getKeyString(utxo.getSender())
                    + "|to = " + UtilityMethods.getKeyString(utxo.getReceiver()));
            income += utxo.getFundTransferred();
        }
        System.out.printf("\t----- Total Unspent = %.2f ----------%n", income);

        System.out.println("}");
    }
}
