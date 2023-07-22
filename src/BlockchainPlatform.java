import java.security.PublicKey;
import java.util.ArrayList;

// Class BlockchainPlatform simulates a blockchain system.
public class BlockchainPlatform {

    // The blockchain
    public static Blockchain ledger;

    // For tracking the total transaction fee that has been paid
    // private static double transactionFee = 0.0;

    public static void main(String[] args) {
        // Store all wallets/miners for later processing.
        ArrayList<Wallet> users = new ArrayList<Wallet>();

        // Set difficulty level
        int difficultyLevel = 22;
        System.out.println("\n============ Starting blockchain platform... ============\n");
        System.out.println("Creating genesis miner, genesis transaction, "
                + "and genesis block...");

        // Create a genesis miner to start a blockchain
        Miner genesisMiner = new Miner("genesis", "genesis");
        users.add(genesisMiner);

        // Create the genesis block. Its "previous block hash ID" is set
        // to "0" manually
        Block genesisBlock = new Block("0", difficultyLevel, genesisMiner.getPublicKey());

        // Manually create two UTXOs as the input of the genesis transaction
        UTXO u1 = new UTXO("0", genesisMiner.getPublicKey(),
                genesisMiner.getPublicKey(), 10_001.0);
        UTXO u2 = new UTXO("0", genesisMiner.getPublicKey(),
                genesisMiner.getPublicKey(), 10_000.0);

        // Prepare input for genesis transaction
        ArrayList<UTXO> inputs = new ArrayList<UTXO>();
        inputs.add(u1);
        inputs.add(u2);

        // Prepare genesis transaction
        Transaction gt = new Transaction(genesisMiner.getPublicKey(),
                genesisMiner.getPublicKey(), 10_000.0, inputs);
        boolean b = gt.prepareOutputUTXOs();

        // Check if output preparation is successful. If not, exit the system.
        if (!b) {
            System.out.println("Genesis transaction failed. System exit...");
            System.exit(1);
        }

        // Genesis miner signs the genesis transaction
        gt.signTheTransaction(genesisMiner.getPrivateKey());

        // Add the genesis transaction into the genesis block
        b = genesisBlock.addTransaction(gt, genesisMiner.getPublicKey());
        if (!b) {
            System.out.println("Failed to add genesis transaction to the genesis block. System exit...");
            System.exit(1);
        }

        // Genesis miner mines the genesis block
        System.out.println("Genesis miner now mining the genesis block...");
        b = genesisMiner.mineBlock(genesisBlock);

        // Check if mining is successful
        if (b)
            System.out.printf("Genesis block is successfully mined! HashID: %s%n",
                    genesisBlock.getHashID());
        else {
            System.out.println("Failed to mine genesis block. System exit...");
            System.exit(1);
        }

        // Construct a copy of the blockchain
        ledger = new Blockchain(genesisBlock);
        System.out.println("Blockchain genesis successful!");

        // Genesis miner copies the blockchain to his local ledger
        genesisMiner.setLocalLedger(ledger);

        // Manually check the balance of the genesis miner
        System.out.printf("Genesis miner balance: %.2f%n",
                genesisMiner.getCurrentBalance(genesisMiner.getLocalLedger()));

        // Create other wallets/miners
        Miner A = new Miner("Miner A", "miner A");
        Wallet B = new Wallet("Wallet B", "wallet B");
        Miner C = new Miner("Miner C", "miner C");

        // Every wallet stores a local ledger
        A.setLocalLedger(ledger.copy_NotDeepCopy());
        B.setLocalLedger(ledger.copy_NotDeepCopy());
        C.setLocalLedger(ledger.copy_NotDeepCopy());

        // Miner A starts another block
        Block b2 = A.createNewBlock(A.getLocalLedger(), difficultyLevel);
        System.out.println("\n\nBlock b2 created by A.");
        System.out.println("Genesis miner sends B: 500 + 200, C: 300 + 100");
        PublicKey[] receiver = {B.getPublicKey(), B.getPublicKey(),
                                C.getPublicKey(), C.getPublicKey()};
        double[] funds = {500, 200, 300, 100};

        Transaction t1 = genesisMiner.transferFund(receiver, funds);
        System.out.println("A is collecting Transactions...");
        if (A.addTransaction(t1, b2))
            System.out.println("t1 added to block b2.");
        else
            System.out.println("Warning: t1 cannot be added into b2!");
        System.out.println("A is generating reward Transactions...");
        if (A.generateRewardTransaction(b2))
            System.out.println("Reward transactions successfully added to b2.");
        else
            System.out.println("Reward transactions cannot be added to b2.");
        System.out.println("A is now mining block b2...");

        if (A.mineBlock(b2))
            System.out.println("b2 successfully mined and signed by A!");

        // C can verify block b2
        b = verifyBlock(C, b2, "b2");
        if (b) {
            System.out.println("All blockchain users begin updating their "
                            + "local blockchain with b2.");
            allUpdateBlockchain(users, b2);
            System.out.println("After b2 is added to the blockchain, the balances are:");
            displayAllBalances(users);
        }

        // Display balances for manual examination
        System.out.println("Total = " + (20_000 + Blockchain.MINING_REWARD)
                        + ". Adding all wallets, total = "
                        + (genesisMiner.getCurrentBalance(ledger) + A.getCurrentBalance(ledger))
                        + B.getCurrentBalance(ledger) + C.getCurrentBalance(ledger));

        // A starts another block
        Block b3 = A.createNewBlock(ledger, difficultyLevel);
        System.out.println("\n\nBlock b3 created by A.");
        System.out.println("Genesis miner sends B: 500 + 200, C: 300 + 100");
        Transaction t2 = genesisMiner.transferFund(receiver, funds);

        // Attempting to add transaction t1 to block b3 should fail
        if (A.addTransaction(t1, b3))
            System.out.println("t1 added into block b3");
        else
            System.out.println("Warning: t1 cannot be added to b3 because t1 already exists!");

        // Add transaction t2 to block b3
        if (A.addTransaction(t2, b3))
            System.out.println("t2 added to block b3");
        else
            System.out.println("Warning: t2 cannot be added to b3!");

        System.out.println("A is collecting Transactions...");
        System.out.println("A is generating reward Transactions...");
        if (A.generateRewardTransaction(b3))
            System.out.println("Reward transaction successfully added to block b3!");
        else
            System.out.println("Reward transaction cannot be added to block b3!");

        // Miner C attempts to mine b3. This should fail.
        if (C.mineBlock(b3))
            System.out.println("Block b3 was successfully mined and signed by C!");
        else
            System.out.println("C cannot mine block b3!");

        // C is not the creator of block b3, so it cannot modify it.
        if (C.deleteTransaction(b3.getTransaction(0), b3))
            System.out.println("C deleted the first transaction from b3!");
        else
            System.out.println("C is not authorized to delete the first transaction from b3!");

        // Only the creator of b3 is can mine it.
        if (A.mineBlock(b3))
            System.out.println("Block b3 was successfully mined and signed by A!");
        else
            System.out.println("ERROR: b3 could not be mined by A!");

        // A is allowed to modify block b3
        if (A.deleteTransaction(b3.getTransaction(0), b3))
            System.out.println("A deleted the first transaction from b3!");
        else
            System.out.println("A cannot delete the first transaction from b3, block already signed!");

        // C can verify block b3
        b = verifyBlock(C, b3, "b3");
        if (b) {
            System.out.println("Blockchain users begin updating their local ledgers with block b3.");
            allUpdateBlockchain(users, b3);
            System.out.println("Block b3 added. Balances now are:");
            displayAllBalances(users);
        }
        System.out.println("Total = " + (20_000 + Blockchain.MINING_REWARD * 2)
            + ". Adding all wallets, total = " + (genesisMiner.getCurrentBalance(ledger)
            + A.getCurrentBalance(ledger) + B.getCurrentBalance(ledger)
            + C.getCurrentBalance(ledger)));

        Transaction t5 = C.transferFund(C.getPublicKey(), 20);
        // Miner A attempts to add t5 into block b3. This should fail.
        if (A.addTransaction(t5, b3))
            System.out.println("A added transaction t5 to block b3.");
        else
            System.err.println("A cannot add t5 to block b3 because the block has been signed.");
        // Skip a line
        System.out.println();

        // Miner C creates a new block
        Block b4 = C.createNewBlock(ledger, difficultyLevel);
        System.out.println("C successfully created block b4.");
        if (C.addTransaction(t5, b4))
            System.out.println("C added t6 to block b4.");
        else
            System.out.println("C failed to add t5 to block b4.");

        // More transactions
        Transaction t6 = C.transferFund(A.getPublicKey(), 100);
        Transaction t7 = B.transferFund(A.getPublicKey(), 100);
        Transaction t8 = C.transferFund(B.getPublicKey(), 100);

        // This should succeed
        if (C.addTransaction(t6, b4))
            System.out.println("C added t6 to block b4.");
        else
            System.out.println("C failed to add t6 to block b4.");

        // This should also succeed
        if (C.addTransaction(t7, b4))
            System.out.println("C added t7 to block b4.");
        else
            System.out.println("C failed to add t7 to block b4.");

        // This should also succeed
        if (C.addTransaction(t8, b4))
            System.out.println("C added t8 to block b4.");
        else
            System.out.println("C failed to add t8 to block b4.");

        if (C.generateRewardTransaction(b4))
            System.out.println("C generated reward transaction in b4.");
        else
            System.out.println("C could not generate reward transaction in b4.");

        if (C.mineBlock(b4)) {
            System.out.printf("C mined block b4, HashID: %n%s%n", b4.getHashID());
            b = verifyBlock(A, b4, "b4");
            if (b) {
                System.out.println("All blockchain users now update their local ledgers...");
                allUpdateBlockchain(users, b4);
                System.out.println("Block b4 added. Balances now are:");
                displayAllBalances(users);
            }
        }

        // Attempting to add b4 twice should fail
        b = ledger.addBlock(b4);
        if (b)
            System.out.println("ERROR: b4 is added again to the ledger!!!");
        else
            System.out.println("b4 cannot be added twice to the ledger.");

        // Reassess the balance
        System.out.println("After b4 has been added, the balances are:");
        displayAllBalances(users);
        System.out.println("Total = " + (20_000 + Blockchain.MINING_REWARD * 3)
            + ". Adding all wallets, total = "
            + (genesisMiner.getCurrentBalance(ledger) + A.getCurrentBalance(ledger)
            + B.getCurrentBalance(ledger) + C.getCurrentBalance(ledger)));

        // Skip a line
        System.out.println();

        System.out.println("==================================================");
        System.out.println("Blockchain current status:\n");
        UtilityMethods.displayBlockchain(ledger, System.out, 0);
        System.out.println("========== Terminating Blockchain Platform... ==========");
    }

    public static boolean verifyBlock(Wallet w, Block b, String blockName) {
        if (w.verifyGuestBlock(b)) {
            System.out.printf("%s accepted block %s.%n", w.getName(), blockName);
            return true;
        }
        else {
            System.out.printf("%s rejected block %s.%n", w.getName(), blockName);
            return false;
        }
    }

    // Method for updating the local ledgers of all users in the input list
    // with a new Block.
    public static void allUpdateBlockchain(ArrayList<Wallet> users, Block b) {
        for (int i=0; i<users.size(); i++) {
            Wallet w = users.get(i);
            w.updateLocalLedger(b);
            System.out.printf("%s successfully updated its local ledger!%n", w.getName());
        }
    }

    //
    public static void displayUTXOs(ArrayList<UTXO> utxos, int level) {
        for (int i=0; i<utxos.size(); i++)
            UtilityMethods.displayUTXO(utxos.get(i), System.out, level);
    }

    //
    public static void displayBalance(Wallet w) {
        Blockchain ledger = w.getLocalLedger();
        ArrayList<UTXO> all = new ArrayList<UTXO>();
        ArrayList<UTXO> spent = new ArrayList<UTXO>();
        ArrayList<UTXO> unspent = new ArrayList<UTXO>();
        ArrayList<Transaction> sentTx =  new ArrayList<Transaction>();
        ArrayList<UTXO> rewards = new ArrayList<UTXO>();
        double balance = ledger.findRelatedUTXOs(w.getPublicKey(), all, spent,
                        unspent, sentTx, rewards);
        byte level = 0;
        UtilityMethods.displayTab(System.out, level, w.getName() + "{");
        UtilityMethods.displayTab(System.out, level + 1, "All UTXOs:");
        displayUTXOs(all, level + 2);
        UtilityMethods.displayTab(System.out, level + 1, "Spent UTXOs:");
        displayUTXOs(spent, level + 2);
        UtilityMethods.displayTab(System.out, level + 1, "Unspent UTXOs:");
        displayUTXOs(unspent, level + 2);
        if (w instanceof Miner) {
            UtilityMethods.displayTab(System.out, level + 1, "Balance = " + balance);
            displayUTXOs(rewards, level + 2);
        }
        UtilityMethods.displayTab(System.out, level + 1, "Balance = " + balance);
        UtilityMethods.displayTab(System.out, level, "}");
    }

    //
    public static void displayAllBalances(ArrayList<Wallet> users) {
        for (int i=0; i<users.size(); i++)
            displayBalance(users.get(i));
    }

}