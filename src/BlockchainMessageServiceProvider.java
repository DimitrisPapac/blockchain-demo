import jdk.jshell.execution.Util;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyPair;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class BlockchainMessageServiceProvider {
    private ServerSocket serverSocket = null;
    private boolean forever = true;
    // Hashtable for storing all network connections based on wallet public keys.
    Hashtable<String, ConnectionChannelTaskManager> connections = null;
    // All incoming messages are stored in this queue for processing.
    private ConcurrentLinkedQueue<Message> messageQueue = null;
    // To store all names/addresses so that name discovery service can
    // be provided.
    private Hashtable<String, KeyNamePair> allAddresses = null;
    // Genesis blockchain is a public asset in this system.
    private static Blockchain genesisBlockchain = null;

    // Constructor
    public BlockchainMessageServiceProvider() {
        System.out.println("BlockchainMessageServiceProvider starting up...");
        connections = new Hashtable<String, ConnectionChannelTaskManager>();
        this.messageQueue = new ConcurrentLinkedQueue<Message>();
        this.allAddresses = new Hashtable<String, KeyNamePair>();
        try {
            serverSocket = new ServerSocket(Configuration.networkPort());
        }
        catch (Exception e) {
            System.out.println("BlockchainMessageServiceProvider failed to "
                + "create server socket. Failed.");
            // If something goes wrong, the system should not start at all.
            System.exit(1);
        }
    }

    protected void startWorking() {
        System.out.println("BlockchainMessageServiceProvider is ready!");
        // Server needs a pair of public/private keys to represent the server
        KeyPair keypair = UtilityMethods.generateKeyPair();
        // Start message checking thread
        MessageCheckingTaskManager checkingAgent =
                new MessageCheckingTaskManager(this, messageQueue, keypair);
        Thread agent = new Thread(checkingAgent);
        agent.start();
        System.out.println("BlockchainMessageServiceProvider generated "
                    + "MessageCheckingTaskManager, thread working...");

        // The network server is supposed to run indefinitely
        while (forever) {
            try {
                Socket socket = serverSocket.accept();
                System.out.println("BlockchainMessageServiceProvider "
                    + "accepts one connection.");
                // Allocate a connection channel task manager for a connection
                ConnectionChannelTaskManager st =
                        new ConnectionChannelTaskManager(this, socket, keypair);
                Thread tt = new Thread(st);
                tt.start();
            }
            catch (Exception e) {
                System.out.println("BlockchainMessageServiceProvider runs into "
                    + "a problem:" + e.getMessage() + " --> exiting now...");
                System.exit(2);
            }
        }
    }

    // Method for discovering the corresponding public key based on a hashID.
    protected PublicKey findAddress(String id) {
        KeyNamePair knp = this.allAddresses.get(id);
        if (knp != null)
            return knp.getKey();
        else
            return null;
    }

    // When a connection is closed, remove this connection from storage.
    protected synchronized KeyNamePair removeAddress(String id) {
        return this.allAddresses.remove(id);
    }

    // Method for obtaining all addresses.
    protected synchronized ArrayList<KeyNamePair> getAllAddresses() {
        ArrayList<KeyNamePair> arr = new ArrayList<KeyNamePair>();
        Iterator<KeyNamePair> iter = this.allAddresses.values().iterator();
        while (iter.hasNext())
            arr.add(iter.next());
        return arr;
    }

    // Method for finding a connection channel task manager based on a connectionID.
    protected synchronized ConnectionChannelTaskManager
                    findConnectionChannelTaskManager(String connectionID) {
        return this.connections.get(connectionID);
    }

    // Method for retrieving all connection channel task managers.
    protected synchronized ArrayList<ConnectionChannelTaskManager>
                    getAllConnectionChannelTaskManager() {
        ArrayList<ConnectionChannelTaskManager> arr =
                new ArrayList<ConnectionChannelTaskManager>();
        Iterator<ConnectionChannelTaskManager> iter =
                this.connections.values().iterator();
        while (iter.hasNext())
            arr.add(iter.next());
        return arr;
    }

    // Add one address into the address collection.
    protected synchronized void addPublicKeyAddress(KeyNamePair knp) {
        this.allAddresses.put(UtilityMethods.getKeyString(knp.getKey()), knp);
    }

    // Add one connection channel task manager.
    protected synchronized void addConnectionChannel(ConnectionChannelTaskManager channel) {
        this.connections.put(channel.getConnectionChannelID(), channel);
    }

    // Remove a connection channel task manager and the related address.
    protected synchronized KeyNamePair removeConnectionChannel(String channelID) {
        this.connections.remove(channelID);
        KeyNamePair knp = this.removeAddress(channelID);
        return knp;
    }

    // Add one message into the queue.
    protected void addMessageIntoQueue(Message msg) {
        this.messageQueue.add(msg);
    }

    // A static method for updating the genesis block. It is called exactly once.
    protected static void updateGenesisBlock(Blockchain genesisBlock) {
        if (BlockchainMessageServiceProvider.genesisBlockchain == null)
            BlockchainMessageServiceProvider.genesisBlockchain = genesisBlock;
    }

    public static Blockchain getGenesisBlockchain() {
        return BlockchainMessageServiceProvider.genesisBlockchain;
    }
}

// Inner class
class ConnectionChannelTaskManager implements Runnable {
    private Socket socket;
    private ObjectInputStream in = null;
    private ObjectOutputStream out = null;
    boolean forever = true;

    // The connection clientID indicates which connection this thread is working for.
    private String ConnectionID = null;
    private BlockchainMessageServiceProvider server;

    // The private and public key pair of the server.
    private KeyPair keypair;

    // The public key of the client (wallet).
    private PublicKey delegatePublicKey;

    // The name of the client (wallet).
    private String name = null;

    // Constructor
    protected ConnectionChannelTaskManager(BlockchainMessageServiceProvider server,
                                           Socket s, KeyPair keypair) {
        this.server = server;
        this.socket = s;
        this.keypair = keypair;
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
            // The server sends the client its public key.
            MessageID toClient = new MessageID(this.keypair.getPrivate(),
                    this.keypair.getPublic(), "ServiceProvider");
            out.writeObject(toClient);
            out.flush();
            // The server will then wait for the client to send in a MessageForID
            MessageID mid = (MessageID) in.readObject();
            // Examine if the communication is securely constructed
            if (!mid.isValid())
                throw new Exception("messageID is invalid. Something went wrong...");
            // Store this connection and its ID
            this.delegatePublicKey = mid.getPublicKey();
            this.ConnectionID = UtilityMethods.getKeyString(mid.getPublicKey());
            this.name = mid.getName();
            System.out.println("Connection successfully established for "
                + this.getDelegateName() + "|" + this.ConnectionID);
            this.server.addConnectionChannel(this);
            this.server.addPublicKeyAddress(mid.getKeyNamePair());
            System.out.println("Adding address for "
                + mid.getKeyNamePair().getName() + ", now send the genesis blockchain...");
            // Let the new coming user get the genesis blockchain
            MessageBlockchainPrivate mchain = new MessageBlockchainPrivate(
                    BlockchainMessageServiceProvider.getGenesisBlockchain(),
                    BlockchainMessageServiceProvider.getGenesisBlockchain().getGenesisMiner(),
                    this.delegatePublicKey);
            out.writeObject(mchain);
        }
        catch (Exception e) {
            System.out.printf("ConnectionChannelTaskManager exception: %s%n", e.getMessage());
            System.out.println("This ConnectionChannelTaskManager connection failed.");
            System.out.println("Aborting this connection now...");
            this.activeClose();
        }
    }

    // Return the name of the client (wallet).
    public String getDelegateName() {
        return this.name;
    }

    // Return the address (public key) of the client (wallet).
    public PublicKey getDelegateAddress() {
        return this.delegatePublicKey;
    }

    // This method is synchronized so that messages are sent one at a time.
    protected synchronized boolean sendMessage(Message msg) {
        try {
            out.writeObject(msg);
            out.flush();
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }

    public void run() {
        int count = 0;
        while (forever) {
            try {
                Message msg = (Message) in.readObject();
                this.server.addMessageIntoQueue(msg);
            }
            catch (Exception ie) {
                count++;
                // If the exception occurred too many times,
                // close this thread.
                if (count >= 3)
                    this.activeClose();
            }
        }
    }

    // Method for returning the readable string of the client's public key.
    protected String getConnectionID() {
        return this.ConnectionID;
    }

    // Initiated by the server side.
    private void activeClose() {

    }
}