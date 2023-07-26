import jdk.jshell.execution.Util;

import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Scanner;

// Class BlockchainPlatform simulates a blockchain system.
public class BlockchainPlatform {

    protected static Scanner keyboard = null;

    public static void main(String[] args) {
        keyboard = new Scanner(System.in);

        // Begin by starting the genesis miner
        MinerGenesisSimulator genesisSimulator = new MinerGenesisSimulator();
        Thread simulator = new Thread(genesisSimulator);
        simulator.start();
        System.out.println("Genesis simulator is up...");
        System.out.println("Starting the blockchain message service provider...");
        BlockchainMessageServiceProvider server =
                new BlockchainMessageServiceProvider();
        // Obtain the genesis blockchain from the genesis miner
        Blockchain ledger = genesisSimulator.getGenesisLedger();
        // Set up the genesis blockchain as a permanent asset
        BlockchainMessageServiceProvider.updateGenesisBlock(ledger);

        // Make sure that the genesisMiner's blockchain and the ledger here
        // are the same
        Blockchain ledger2 = genesisSimulator.getGenesisMiner().getLocalLedger();
        if (ledger.size() != ledger2.size()) {
            System.out.println("ERROR!!! The two genesis blockchains have "
                + "different sizes: " + ledger.size() + "|" + ledger.size());
            System.exit(1);
        }
        if (!ledger.getLastBlock().getHashID().equals(
                ledger2.getLastBlock().getHashID())) {
            System.out.println("Error!!! The two genesis blockchains have different hashcodes!");
            System.out.println(ledger.getLastBlock().getPreviousBlockHashID() + "\n"
                            + ledger2.getLastBlock().getPreviousBlockHashID());
            System.exit(2);
        }
        System.out.println("**************************************************");
        System.out.println("Blockchain message service provider is now ready to work.");
        System.out.println("**************************************************");
        // Start server
        server.startWorking();
        // Reaching this point means the server is down
        System.out.println("========== Blockchain platform shuts down... ==========");
    }
}

// Inner class for simulating the genesis miner.
final class MinerGenesisSimulator implements Runnable {
    private Blockchain genesisLedger = null;
    private Miner genesisMiner;

    protected synchronized Blockchain getGenesisLedger() {
        if (this.genesisLedger == null) {
            System.out.println("Blockchain platform starts...");
            System.out.println("Creating genesis miner, genesis transaction, "
                + "and genesis block...");
            // Create a genesis miner to start a blockchain
            genesisMiner = this.getGenesisMiner();
            // Create a genesis block
            Block genesisBlock =
                    new Block("0", Configuration.blockMiningDifficultyLevel(),
                            genesisMiner.getPublicKey());
            UTXO u1 = new UTXO("0", genesisMiner.getPublicKey(),
                    genesisMiner.getPublicKey(), 1_000_001.0);
            UTXO u2 = new UTXO("0", genesisMiner.getPublicKey(),
                    genesisMiner.getPublicKey(), 1_000_000.0);
            ArrayList<UTXO> inputs = new ArrayList<UTXO>();
            inputs.add(u1);
            inputs.add(u2);
            Transaction gt = new Transaction(genesisMiner.getPublicKey(),
                    genesisMiner.getPublicKey(), 1_000_000.0, inputs);
            boolean b = gt.prepareOutputUTXOs();
            if (!b) {
                System.out.println("Genesis transaction failed...");
                System.exit(1);
            }
            gt.signTheTransaction(genesisMiner.getPrivateKey());
            b = genesisBlock.addTransaction(gt, genesisMiner.getPublicKey());
            if (!b) {
                System.out.println("Failed to add the genesis transaction to "
                    + "the genesis block. System quit...");
                System.exit(2);
            }
            // Genesis miner mines the genesis block
            System.out.println("Genesis miner is now mining the genesis block...");
            b = genesisMiner.mineBlock(genesisBlock);
            if (b) {
                System.out.println("Genesis block is successfully mined!");
                System.out.println(genesisBlock.getHashID());
            }
            else {
                System.out.println("Failed to mine genesis block. System exit...");
                System.exit(1);
            }
            Blockchain ledger = new Blockchain(genesisBlock);
            System.out.println("Blockchain genesis successful!");
            // The genesis miner copies the blockchain into its local copy
            genesisMiner.setLocalLedger(ledger);
            // Set up the genesis blockchain
            this.genesisLedger = ledger.copy_NotDeepCopy();
            System.out.printf("Genesis miner balance: %.2f%n",
                    genesisMiner.getCurrentBalance(genesisMiner.getLocalLedger()));
        }
        return this.genesisLedger;
    }

    protected synchronized Miner getGenesisMiner() {
        if (this.genesisMiner == null)
            genesisMiner = new Miner("genesis", "satoshi");
        return genesisMiner;
    }

    public void run() {
        System.out.println("Important! You are the genesis miner, you must "
            + "start before any other miners or wallet!");
        Miner miner = getGenesisMiner();
        this.getGenesisLedger();
        System.out.printf("You name = %s%n", miner.getName());
        System.out.println("===== Important! Has the ServiceRelayProvider " +
                "started? ===== (1 = yes, 0 = no)");
        int yesno = UtilityMethods.guaranteeIntegerInputByScanner(
                BlockchainPlatform.keyboard, 0, 1);
        while (yesno == 0) {
            System.out.println("===== Important! Has the ServiceRelayProvider " +
                    "started? ===== (1 = yes, 0 = no)");
            yesno = UtilityMethods.guaranteeIntegerInputByScanner(
                    BlockchainPlatform.keyboard, 0, 1);
        }
        double balance = miner.getCurrentBalance(miner.getLocalLedger());
        System.out.printf("Checking genesis miner balance: %.2f%n", balance);
        System.out.print("To join the blockchain network, please enter "
            + "the service provider IP address: ");
        System.out.println("If this simulator runs on the same computer as "
            + "the Service Provider, please enter 127.0.0.1 or hit Enter.");
        String ipAddress = BlockchainPlatform.keyboard.nextLine();
        if (ipAddress == null || ipAddress.length() < 5)
            ipAddress = "localhost";
        // Start the connection agent for the genesis miner
        WalletConnectionAgent agent = new WalletConnectionAgent(ipAddress,
                Configuration.networkPort(), miner);
        Thread athread = new Thread(agent);
        athread.start();
        // Start the message task manager for the genesis miner
        MinerGenesisMessageTaskManager taskManager =
                new MinerGenesisMessageTaskManager(agent, miner, agent.getMessageQueue());
        Thread tThread = new Thread(taskManager);
        tThread.start();
    }
}