import javax.xml.crypto.dsig.keyinfo.KeyName;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.HashMap;

public class WalletMessageTaskManager implements Runnable {
    private boolean forever = true;
    // The agent that this task manager is working with for the wallet
    private WalletConnectionAgent agent;
    private Wallet wallet;
    private ConcurrentLinkedQueue<Message> messageQueue;
    private HashMap<String, String> thankYouTransactions =
                                        new HashMap<String, String>();
    private WalletSimulator simulator = null;

    // Constructor
    public WalletMessageTaskManager(WalletConnectionAgent agent, Wallet wallet,
                                    ConcurrentLinkedQueue<Message> messageQueue) {
        this.agent = agent;
        this.wallet = wallet;
        this.messageQueue = messageQueue;
    }

    // The task manager needs to have direct access to the WalletSimulator
    // for proper message display on the GUI.
    public void setSimulator(WalletSimulator simulator) {
        this.simulator = simulator;
    }

    //
    protected void askForLatestBlockchain() {
        MessageAskForBlockchainBroadcast forLedger =
                new MessageAskForBlockchainBroadcast("Thanks",
                        this.wallet.getPrivateKey(), this.wallet.getPublicKey(),
                        this.wallet.getName());
        boolean b = this.agent.sendMessage(forLedger);
        if (b)
            System.out.println("Sent a message for latest blockchain.");
        else
            System.out.println("Error: Failed to send message for latest blockchain!");
    }

    // Method for specifying what this task manager should do when there are no available
    // message to process. It is intended to be overridden by MinerGenesisMessageTaskManager.
    public void whatToDo() {
        // do nothing
    }

    public void run() {
        try {
            Thread.sleep(2 * agent.sleepTime);
        }
        catch (Exception e) {
            // do nothing
        }

        // Start by updating the local blockchain
        askForLatestBlockchain();

        while (forever) {
            if (this.messageQueue.isEmpty()) {
                try {
                    Thread.sleep(this.agent.sleepTime);
                    whatToDo();
                }
                catch (Exception e) {
                    System.out.println("Error in sleep.");
                    e.printStackTrace();
                    this.close();
                    this.agent.activeClose();
                }
            }
            else {
                Message msg = this.messageQueue.poll();
                if (msg == null)
                    System.out.println("Message is null!");
                else {
                    // To ensure that the network connection is properly closed
                    // in case something unexpected occurs.
                    try {
                        this.processMessage(msg);
                    }
                    catch (Exception e) {
                        System.out.println("Error when processing message.");
                        e.printStackTrace();
                        this.close();
                        this.agent.activeClose();
                    }
                }
            }
        }
    }

    // Method for processing different messages as is appropriate per case.
    protected void processMessage(Message msg) {
        if (msg == null)
            return;
        if (!msg.isForBroadcast()) {
            // If the message is private for this wallet
            if (msg.getMessageType() == Message.TEXT_PRIVATE) {
                MessageTextPrivate mtp = (MessageTextPrivate) msg;

                // Confirm validity
                if (!mtp.isValid()) {
                    System.out.println("Text private message tampered!");
                    return;
                }

                // Check if message is intended for this wallet
                if (!mtp.getReceiver().equals(this.wallet.getPublicKey())) {
                    System.out.println("Text private is not for this wallet! Ignoring...");
                    return;
                }

                // Check if the message is a CLOSE connection message
                String text  = mtp.getMessageBody();
                if (mtp.getSenderKey().equals(agent.getServerAddress())
                                        && text.equals(Message.TEXT_CLOSE)) {
                    System.out.println("Sender is requesting to close the connection.");
                    this.close();
                    agent.close();
                }
                else
                    receivePrivateChatMessage(mtp);
            }
            else if (msg.getMessageType() == Message.ADDRESS_PRIVATE) {
                MessageAddressPrivate map = (MessageAddressPrivate) msg;
                receiverMessageAddressPrivate(map);
            }
            else if (msg.getMessageType() == Message.BLOCKCHAIN_PRIVATE) {
                MessageBlockchainPrivate mbcp = (MessageBlockchainPrivate) msg;
                receiveMessageBlockchainPrivate(mbcp);
            }
            else
                System.out.println("\nReceived unsupported private message. Ignoring...\n");
        }
        else if (msg.getMessageType() == Message.BLOCK_BROADCAST) {
            // Upon receiving a block, validate it and then try to update
            // the local blockchain
            System.out.println("Received a block broadcast message.");
            MessageBlockBroadcast mbb = (MessageBlockBroadcast) msg;
            this.receiveMessageBlockBroadcast(mbb);
        }
        else if (msg.getMessageType() == Message.BLOCKCHAIN_BROADCAST) {
            System.out.println("Received a blockchain broadcast message.");
            MessageBlockchainBroadcast mbcb = (MessageBlockchainBroadcast) msg;
            boolean b = this.wallet.setLocalLedger(mbcb.getMessageBody());
            if (b)
                System.out.println("Blockchain updated!");
            else
                System.out.println("New blockchain rejected. Retaining local version...");
        }
        else if (msg.getMessageType() == Message.TRANSACTION_BROADCAST) {
            // Wallets do not mine blocks or collect transactions.
            // Wallets are only interested in transactions in which it is the receiver.
            System.out.println("Received a transaction broadcast message.");
            MessageTransactionBroadcast mtb =
                                (MessageTransactionBroadcast) msg;
            this.receiveMessageTransactionBroadcast(mtb);
        }
        else if (msg.getMessageType() == Message.BLOCKCHAIN_ASK_BROADCAST) {
            MessageAskForBlockchainBroadcast mabcb =
                    (MessageAskForBlockchainBroadcast) msg;
            if (!(mabcb.getSenderKey().equals(myWallet().getPublicKey()))
                    && mabcb.isValid())
                receiveQueryForBlockchainBroadcast(mabcb);
        }
        else if (msg.getMessageType() == Message.TEXT_BROADCAST) {
            MessageTextBroadcast mtb = (MessageTextBroadcast) msg;
            receiveMessageTextBroadcast(mtb);
        }
    }

    // Method for processing a MessageTextBroadcast message.
    protected void receiveMessageTextBroadcast(MessageTextBroadcast mtb) {
        String text = mtb.getMessageBody();
        String name = mtb.getSenderName();
        this.simulator.appendMessageLineOnBoard(name + "]: " + text);
        // Automatically store the user information (can be self)
        agent.addAddress(new KeyNamePair(mtb.getSenderKey(), mtb.getSenderName()));
    }

    // Method for processing a MessageAddressPrivate message by updating the local
    // wallet list. The message must originate from the message service provider.
    protected void receiverMessageAddressPrivate(MessageAddressPrivate map) {
        ArrayList<KeyNamePair> all = map.getMessageBody();
        System.out.println("Listing all the currently available users:");
        for (int i=0; i<all.size(); i++) {
            KeyNamePair knp = all.get(i);
            if (!knp.getKey().equals(wallet.getPublicKey())) {
                agent.addAddress(knp);
                System.out.printf("%s| key = %s", knp.getName(),
                        UtilityMethods.getKeyString(knp.getKey()));
            }
        }
    }

    // Method for processing a private chat message by displaying it on the GUI.
    protected void receivePrivateChatMessage(MessageTextPrivate mtp) {
        String text = mtp.getMessageBody();
        String name = mtp.getSenderName();
        this.simulator.appendMessageLineOnBoard("private<--" + name + "]: " + text);
        // Automatically store user information
        agent.addAddress(new KeyNamePair(mtp.getSenderKey(), mtp.getSenderName()));
    }

    // Wallets do not respond to such a query.
    protected void receiveQueryForBlockchainBroadcast(MessageAskForBlockchainBroadcast mabcb) {
        System.out.println("Cannot process such a request. Ignoring...");
    }

    // When wallets receive a transaction broadcast message, they can either ignore them,
    // or respond with a "THANK YOU" message to the transaction publisher if the transaction
    // contains UTXO(s) paid to this wallet.
    // TODO: Send a "THANK YOU" message to the transaction publisher when a block is published
    // and accepted.
    protected void receiveMessageTransactionBroadcast(MessageTransactionBroadcast mtb) {
        Transaction tx = mtb.getMessageBody();
        if (!this.thankYouTransactions.containsKey(tx.getHashID())) {
            int n = tx.getNumberOfOutputUTXOs();
            int total = 0;
            for (int i=0; i<n; i++) {
                UTXO utxo = tx.getOutputUTXO(i);
                if (utxo.getReceiver().equals(this.wallet.getPublicKey()))
                    total += utxo.getFundTransferred();
            }

            // If the UTXO sender is self, do not display this message
            if (total > 0 && !tx.getSender().equals(myWallet().getPublicKey())) {
                this.thankYouTransactions.put(tx.getHashID(), tx.getHashID());
                System.out.printf("In the transaction, there is payment of %d for this wallet%n", total);
                System.out.println("Sending \"THANK YOU\" message to the payer...");
                MessageTextPrivate mtp = new MessageTextPrivate("Thank you "
                                            + "for the fund of " + total
                                            + ", awaiting its publication.",
                                            this.wallet.getPrivateKey(), this.wallet.getPublicKey(),
                                            this.wallet.getName(), tx.getSender());
                this.agent.sendMessage(mtp);
            }
        }
    }

    // When a block is rejected, transactions inside the block must be broadcast again
    // if they haven't been successfully published in order to prevent them from being lost.
    protected void receiveMessageBlockBroadcast(MessageBlockBroadcast mbb) {
        Block block = mbb.getMessageBody();
        boolean b = this.wallet.updateLocalLedger(block);
        if (b)
            System.out.println("New block added to the local blockchain.");
        else {
            int size = block.getTotalNumberOfTransactions();
            int counter = 0;
            for (int i=0; i<size; i++) {
                Transaction tx = block.getTransaction(i);
                if (!myWallet().getLocalLedger().transactionExists(tx)) {
                    MessageTransactionBroadcast mtb =
                                new MessageTransactionBroadcast(tx);
                    this.agent.sendMessage(mtb);
                }
                counter++;
            }
            System.out.printf("New block is rejected. Released %d unpublished transactions in the pool",
                                counter);
        }
    }

    // A private message of blockchain must be for this wallet alone.
    // If not, it is discared. Otherwise, we examine whether or not this
    // wallet should update its local blockchain copy.
    protected void receiveMessageBlockchainPrivate(MessageBlockchainPrivate mbcp) {
        System.out.println("Received a blockchain private message.");
        if (mbcp.getReceiver().equals(myWallet().getPublicKey())) {
            boolean b = this.myWallet().setLocalLedger(mbcp.getMessageBody());
            if (b)
                System.out.println("Blockchain updated!");
            else
                System.out.println("New blockchain rejected. Retaining current local copy...");
        }
        else
            System.out.println("ERROR: Received blockchain private message intended for someone else! "
                                + "Ignoring...");
    }

    // Method for retrieving the wallet that this task manager is working with.
    protected Wallet myWallet() {
        return this.wallet;
    }

    public void close() {
        this.forever = false;
    }
}
