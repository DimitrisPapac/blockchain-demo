import javax.xml.crypto.dsig.keyinfo.KeyName;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class WalletConnectionAgent implements Runnable {
    private Wallet wallet;
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    // The message service provider has a pair of public and private key.
    // The provider's address is represented by its public key.
    private PublicKey serverAddress;

    // For storing unprocessed messages.
    private ConcurrentLinkedQueue<Message> messageQueue =
                new ConcurrentLinkedQueue<Message>();

    // Local list of wallets in the system.
    private Hashtable<String, KeyNamePair> allAddresses =
                new Hashtable<String, KeyNamePair>();

    private boolean forever = true;
    public final long sleepTime = 100;

    // Constructor
    public WalletConnectionAgent(String host, int port, Wallet wallet) {
        this.wallet = wallet;
        System.out.println("Now creating agent for network communication...");
        try {
            socket = new Socket(host, port);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            // The agent gets a MessageForID from the message service provider
            MessageID fromServer = (MessageID) in.readObject();

            // Ensure that the message is valid
            if (fromServer.isValid())
                this.serverAddress = fromServer.getPublicKey();
            else
                throw new Exception("MessageID from service provider is invalid.");

            // If the message was valid, the agent replies to the server with a
            // MessageForID message
            System.out.println("Obtained and stored server address. Now sending "
                                + "wallet public key to the server.");
            System.out.printf("name = %s%n", this.wallet.getName());
            MessageID mid = new MessageID(this.wallet.getPrivateKey(), this.wallet.getPublicKey(),
                                            this.wallet.getName());
            out.writeObject(mid);

            // Expect blockchain genesis
            MessageBlockchainPrivate mbcp = (MessageBlockchainPrivate) in.readObject();
            this.wallet.setLocalLedger(mbcp.getMessageBody());
            System.out.println("Genesis blockchain set!");
        }
        catch (Exception e) {
            System.out.printf("WalletConnectionAgent: creation failed because| %s", e.getMessage());
            System.out.println("Please restart.");
            System.exit(1);
        }
    }

    public void run() {
        try {
            Thread.sleep(this.sleepTime);
        }
        catch (Exception e) {
            // do nothing
        }

        while (forever) {
            try {
                // Accept a message and store it in the queue
                Message msg = (Message) in.readObject();
                this.messageQueue.add(msg);
                Thread.sleep(this.sleepTime);
            }
            catch (Exception e) {
                forever = false;
            }
        }
    }

    public synchronized boolean sendMessage(Message msg) {
        if (msg == null) {
            System.out.println("Null message. Cannot send.");
            return false;
        }
        try {
            this.out.writeObject(msg);
            return true;
        }
        catch (Exception e) {
            System.out.printf("Failed to send message [%s%n", e.getMessage());
            return false;
        }
    }

    // Method for enabling the wallet to initiate the closing of this connection.
    public void activeClose() {
        MessageTextPrivate mc = new MessageTextPrivate(Message.TEXT_CLOSE,
                            this.wallet.getPrivateKey(), this.wallet.getPublicKey(),
                            this.wallet.getName(), this.getServerAddress());
        this.sendMessage(mc);
        try {
            Thread.sleep(this.sleepTime);
        }
        catch (Exception e) {
            // do nothing
        }
        this.close();
    }

    // Normal closing action
    public void close() {
        this.forever = false;
        try {
            this.in.close();
            this.out.close();
        }
        catch (Exception e) {
            // do nothing
        }
    }

    // Method for obtaining the local list of wallets.
    public ArrayList<KeyNamePair> getAllStoredAddresses() {
        Iterator<KeyNamePair> iter = this.allAddresses.values().iterator();
        ArrayList<KeyNamePair> arr = new ArrayList<KeyNamePair>();
        while (iter.hasNext())
            arr.add(iter.next());
        return arr;
    }

    // Method for adding an address to the local list.
    public void addAddress(KeyNamePair address) {
        this.allAddresses.put(UtilityMethods.getKeyString(address.getKey()), address);
    }

    // Method for finding the matching name for a given public key.
    // If not found, simply returns the address.
    public String getNameFromAddress(PublicKey key) {
        if (key.equals(this.wallet.getPublicKey()))
            return this.wallet.getName();
        String address = UtilityMethods.getKeyString(key);
        KeyNamePair knp = this.allAddresses.get(address);
        if (knp != null)
            return knp.getName();
        else return address;
    }

    // Method for retrieving the server's address.
    public PublicKey getServerAddress() {
        return this.serverAddress;
    }

    // Method for retrieving the message queue.
    public ConcurrentLinkedQueue<Message> getMessageQueue() {
        return this.messageQueue;
    }

    // Method for initiating, preparing, and sending a transaction.
    protected boolean sendTransaction(PublicKey receiver, double fundToTransfer) {
        Transaction tx = this.wallet.transferFund(receiver, fundToTransfer);
        if (tx != null && tx.verifySignature()) {
            MessageTransactionBroadcast mtb = new MessageTransactionBroadcast(tx);
            this.sendMessage(mtb);
            return true;
        }
        return false;
    }

    // Method for sending a private chat message to a receiver.
    protected boolean sendPrivateMessage(PublicKey receiver, String text) {
        MessageTextPrivate mtp = new MessageTextPrivate(text,
                this.wallet.getPrivateKey(), this.wallet.getPublicKey(),
                this.wallet.getName(), receiver);
        this.sendMessage(mtp);
        return true;
    }

}
