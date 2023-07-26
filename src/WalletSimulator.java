import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Container;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Random;
import java.util.Scanner;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

public class WalletSimulator extends JFrame {
    // MessageFrame is an inner class for displaying balance or blockchain content.
    protected static MessageFrame messageFrame = new MessageFrame();

    // FrameHelp is an inner class for displaying help messages only.
    protected static FrameHelp help = new FrameHelp();

    // For determining whether to display public keys along with balances.
    private boolean balanceShowPublicKey = false;

    private JTextArea textInput;
    private JButton sentButton;

    // The board for displaying chat messages.
    private JTextArea displayArea;
    private GridBagLayout mgr = null;
    private GridBagConstraints gcr = null;

    // The wallet underlying this GUI.
    private Wallet wallet = null;

    // The agent dedicated to network connection.
    private WalletConnectionAgent connectionAgent = null;

    // The message task manager dedicated to message processing.
    private WalletMessageTaskManager taskManager = null;

    // For displaying date and time.
    private Calendar calendar = Calendar.getInstance();

    // Constructor
    public WalletSimulator(Wallet wallet, WalletConnectionAgent agent,
                                WalletMessageTaskManager manager) {
        super(wallet.getName());
        this.wallet = wallet;
        this.connectionAgent = agent;
        this.taskManager = manager;
        setupGUI();

        // When the GUI window is closed, inform network server, and close
        // network connection.
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent we) {
                try {
                    connectionAgent.sendMessage(
                            new MessageTextPrivate(Message.TEXT_CLOSE,
                                    wallet.getPrivateKey(), wallet.getPublicKey(),
                                    wallet.getName(), connectionAgent.getServerAddress())
                    );
                }
                catch (Exception e) {
                    // do nothing
                }
                try {
                    connectionAgent.activeClose();
                    taskManager.close();
                }
                catch (Exception e) {
                    // do nothing
                }
                dispose();
                System.exit(2);
            }
        });
    }

    // Method for setting up the GUI.
    private void setupGUI() {
        // Set default size of the frame
        this.setSize(500, 600);

        // Set menu bar
        setBar();
        Container c = getContentPane();

        // Use GridBagLayout manager
        mgr = new GridBagLayout();
        gcr = new GridBagConstraints();
        c.setLayout(mgr);

        JLabel lblInput = new JLabel("                              Message Board");
        lblInput.setForeground(Color.GREEN);
        this.displayArea = new JTextArea(50, 100);
        this.textInput = new JTextArea(5, 100);
        this.sentButton = new JButton("Click me or hit Enter to send the message below");
        this.sentButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                try {
                    MessageTextBroadcast mtb = new MessageTextBroadcast(textInput.getText(),
                            wallet.getPrivateKey(), wallet.getPublicKey(), wallet.getName());
                    connectionAgent.sendMessage(mtb);
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
                textInput.setText("");
            }
        });

        this.gcr.fill = GridBagConstraints.BOTH;
        this.gcr.weightx = 1;
        this.gcr.weighty = 0.;
        this.gcr.gridx = 0;
        this.gcr.gridy = 0;
        this.gcr.gridwidth = 1;
        this.gcr.gridheight = 1;
        this.mgr.setConstraints(lblInput, this.gcr);

        c.add(lblInput);
        this.gcr.weighty = 0.9;
        this.gcr.gridx = 0;
        this.gcr.gridy = 1;
        this.gcr.gridheight = 9;

        // Make message area scrollable
        JScrollPane scroll = new JScrollPane(this.displayArea);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        this.mgr.setConstraints(scroll, this.gcr);
        c.add(scroll);

        this.displayArea.setEditable(false);
        this.displayArea.setBackground(Color.LIGHT_GRAY);
        this.displayArea.setLineWrap(true);
        this.displayArea.setWrapStyleWord(true);

        this.gcr.weighty = 0.;
        this.gcr.gridx = 0;
        this.gcr.gridy = 11;
        this.gcr.gridheight = 1;
        this.mgr.setConstraints(this.sentButton, this.gcr);
        c.add(this.sentButton);

        this.gcr.weighty = 0.1;
        this.gcr.gridx = 0;
        this.gcr.gridy = 12;
        this.gcr.gridheight = 2;
        // Make text input area scrollable
        JScrollPane scroll2 = new JScrollPane(this.textInput);
        scroll2.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scroll2.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        this.mgr.setConstraints(scroll2, this.gcr);
        c.add(scroll2);
        this.textInput.setLineWrap(true);
        this.textInput.setWrapStyleWord(true);

        // Add a key listener to the text area. Hitting Enter sends the message.
        this.textInput.addKeyListener(new KeyListener()
        {
            @Override
            public void keyTyped(KeyEvent e) {}

            @Override
            public void keyPressed(KeyEvent e) {
                int key = e.getKeyCode();
                if (key == KeyEvent.VK_ENTER) {
                    if (e.isShiftDown() || e.isControlDown())
                        textInput.append(System.getProperty("line.separator"));
                    else {
                        try {
                            MessageTextBroadcast mtb = new MessageTextBroadcast(
                                    textInput.getText(), wallet.getPrivateKey(),
                                    wallet.getPublicKey(), wallet.getName()
                            );
                            connectionAgent.sendMessage(mtb);
                        }
                        catch (Exception e2) {
                            throw new RuntimeException(e2);
                        }
                        // Consume the ENTER so that the cursor stays at the beginning
                        e.consume();
                        textInput.setText("");
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {}
        });
        this.setVisible(true);
    }

    // Method for toggling the display of public keys of wallets along with balances.
    private void setBalanceShowPublicKey(boolean showPK) {
        this.balanceShowPublicKey = showPK;
    }

    public boolean showPublicKeyInBalance() {
        return this.balanceShowPublicKey;
    }

    // Method for setting up the menu bar.
    private void setBar() {
        JMenuBar bar = new JMenuBar();
        setJMenuBar(bar);

        // Menu for asking for information
        JMenu askMenu = new JMenu("Ask For");

        // Add an item to display help message
        JMenuItem helpItem = new JMenuItem("Click here for help");
        helpItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showHelpMessage("1. When you 'update blockchain', "
                    + "a broadcast message is sent for the latest "
                    + "blockchain so as to update the local copy. "
                    + "This becomes necessary if your local copy "
                    + "is outdated.\n"
                    + "2. When you click 'update users', "
                    + "the service provider will update your user list.\n"
                    + "3. Clicking 'show balance' will display your "
                    + "balance on the display board.\n"
                    + "4. Clicking 'display blockchain' will display "
                    + "your local blockchain on the display board.");
            }
        });

        // Add an item to allow updating local blockchain copy. Clicking this
        // item will send a message requesting for the latest blockchain.
        JMenuItem askBlockChainItem = new JMenuItem("update blockchain");
        askBlockChainItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MessageAskForBlockchainBroadcast mabb =
                        new MessageAskForBlockchainBroadcast("Please",
                                wallet.getPrivateKey(), wallet.getPublicKey(),
                                wallet.getName());
                connectionAgent.sendMessage(mabb);
            }
        });

        // Add an item to allow updating the list of wallets. Clicking
        // this item will send a message asking for wallets online.
        JMenuItem askAddressesItem = new JMenuItem("update users");
        askAddressesItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                MessageTextPrivate mtp =
                        new MessageTextPrivate(Message.TEXT_ASK_ADDRESSES,
                                wallet.getPrivateKey(), wallet.getPublicKey(),
                                wallet.getName(), connectionAgent.getServerAddress());
                connectionAgent.sendMessage(mtp);
            }
        });

        // Add an item to allow showing the balance of this wallet.
        JMenuItem askBalanceItem = new JMenuItem("show balance");
        askBalanceItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                displayBalance(wallet);
            }
        });

        // Add an item to allow showing the content of the local blockchain copy.
        JMenuItem displayBlockchain = new JMenuItem("display blockchain");
        displayBlockchain.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                displayBlockchain(wallet);
            }
        });

        askMenu.add(helpItem);
        askMenu.add(askBlockChainItem);
        askMenu.add(askAddressesItem);
        askMenu.add(askBalanceItem);
        askMenu.add(displayBlockchain);
        bar.add(askMenu);

        // Add another menu "To Send" for sending messages.
        JMenu sendMenu = new JMenu("To Send");
        // Add an item to display help message.
        JMenuItem helpItem2 = new JMenuItem("Click here for help");
        helpItem2.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                showHelpMessage("1. When you start a transaction, "
                    + "you need to choose the recipient(s) and "
                    + "the amount to send to each recipient.\n"
                    + "2. The private message you send to a "
                    + "user will be displayed on the message "
                    + "board, but only the recipient will be "
                    + "able to see it.");
            }
        });

        // Add an item to initiate and broadcast a transaction.
        JMenuItem sendTransactionItem = new JMenuItem("start a transaction");
        sendTransactionItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FrameTransaction ft = new FrameTransaction(
                        connectionAgent.getAllStoredAddresses(), connectionAgent
                );
            }
        });

        // Add an item to send a private message to another wallet.
        JMenuItem sendPrivateMessageItem = new JMenuItem("to send a private message");
        sendPrivateMessageItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                FramePrivateMessage fpm = new FramePrivateMessage(
                        connectionAgent.getAllStoredAddresses(),
                        connectionAgent, WalletSimulator.this
                );
            }
        });

        sendMenu.add(helpItem2);
        sendMenu.add(sendTransactionItem);
        sendMenu.add(sendPrivateMessageItem);
        bar.add(sendMenu);
    }

    // Method for automatically adding a new line at the end on the GUI.
    protected void appendMessageLineOnBoard(String s) {
        String time = calendar.getTime().toString();
        this.displayArea.append("(" +  time + ") "
                        + s + System.getProperty("line.separator"));
        this.displayArea.setCaretPosition(this.displayArea.getText().length());
    }

    // Method for displaying the content of the wallet's local copy of the
    // blockchain on the MessageFrame.
    protected void displayBlockchain(Wallet w) {
        StringBuilder sb = new StringBuilder();
        UtilityMethods.displayBlockchain(w.getLocalLedger(), sb, 0);
        messageFrame.setMessage(sb.toString());
    }

    // Method for displaying the balance of a wallet on the MessageFrame.
    protected void displayBalance(Wallet w) {
        StringBuilder sb = new StringBuilder();
        Blockchain ledger = w.getLocalLedger();
        ArrayList<UTXO> all = new ArrayList<UTXO>();
        ArrayList<UTXO> spent = new ArrayList<UTXO>();
        ArrayList<UTXO> unspent = new ArrayList<UTXO>();
        ArrayList<Transaction> sentTx = new ArrayList<Transaction>();
        ArrayList<UTXO> rewards = new ArrayList<UTXO>();
        double balance = ledger.findRelatedUTXOs(w.getPublicKey(), all,
                                            spent, unspent, sentTx, rewards);
        int level = 0;
        this.displayTab(sb, level, w.getName() + "{");
        this.displayTab(sb, level + 1, "All UTXOs:");
        this.displayUTXOs(sb, all, level + 2);
        this.displayTab(sb, level + 1, "Spent UTXOs:");
        this.displayUTXOs(sb, spent, level + 2);
        this.displayTab(sb, level + 1, "Unspent UTXOs:");
        this.displayUTXOs(sb, unspent, level + 2);
        // For Miners, also display the mining rewards
        if (w instanceof Miner) {
            this.displayTab(sb, level + 1, "Mining Rewards:");
            this.displayUTXOs(sb, rewards, level + 2);
        }
        this.displayTab(sb, level + 1, "Balance = " + balance);
        this.displayTab(sb, level, "}");
        String s = sb.toString();
        this.messageFrame.setMessage(s);
    }

    // Prepare the content of UTXOs into a text storage: StringBuilder.
    private void displayUTXOs(StringBuilder sb, ArrayList<UTXO> utxos, int level) {
        for (int i=0; i<utxos.size(); i++) {
            UTXO utxo = utxos.get(i);
            if (this.showPublicKeyInBalance()) {
                this.displayTab(sb, level, "Fund: "
                    + utxo.getFundTransferred() + ", receiver: "
                    + UtilityMethods.getKeyString(utxo.getReceiver())
                    + ", Sender: " + UtilityMethods.getKeyString(utxo.getSender()));
            }
            else {
                String ss = "Fund: " + utxo.getFundTransferred() + ", Receiver: "
                        + connectionAgent.getNameFromAddress(utxo.getReceiver())
                        + ", Sender: "
                        + connectionAgent.getNameFromAddress(utxo.getSender());
                this.displayTab(sb, level, ss);
            }
        }
    }

    // Prepare a text message into a text storage: StringBuilder.
    private void displayTab(StringBuilder sb, int level, String msg) {
        for (byte i=0; i<level; i++)
            sb.append("\t");
        sb.append(msg);
        sb.append(System.getProperty("line.separator"));
    }

    // Show the help message in a frame.
    protected static void showHelpMessage(String msg) {
        help.setMessage(msg);
    }

    // Main method
    public static void main(String[] args) throws Exception {
        Random rand = new Random();
        // Let Pr[Wallet being a Miner] = 3/4
        int chance = rand.nextInt(4);
        Scanner in = new Scanner(System.in);
        System.out.print("Please enter your name: ");
        String wname = in.nextLine();
        System.out.print("Please provide your password: ");
        String wpassword = in.nextLine();
        System.out.println("When showing balance, "
                    + "by default the public key is not shown as the address.\n"
                    + "This is for simplicity. "
                    + "Do you like to show the public key as address (Yes/No)?");
        String yesno = in.nextLine();
        boolean show = false;
        if (yesno.toUpperCase().startsWith("Y"))
            show = true;
        System.out.print("To join the blockchain network, enter the service provider's IP address: ");
        String ipAddress = in.nextLine();
        // If no input, assume it is localhost
        if (ipAddress.length() < 5)
            ipAddress = "localhost";

        if (chance == 0) {
            System.out.println("Your assigned role is: Wallet (i.e., general user).");
            Wallet wallet = new Wallet(wname, wpassword);
            System.out.printf("Welcome %s, your blockchain wallet is ready.%n", wname);
            WalletConnectionAgent agent = new WalletConnectionAgent(ipAddress,
                    Configuration.networkPort(), wallet);
            Thread agentThread = new Thread(agent);
            WalletMessageTaskManager manager = new WalletMessageTaskManager(agent, wallet,
                    agent.getMessageQueue());
            Thread managerThread = new Thread(manager);
            WalletSimulator simulator = new WalletSimulator(wallet, agent, manager);
            manager.setSimulator(simulator);
            agentThread.start();
            System.out.println("Wallet connection agent started...");
            managerThread.start();
            System.out.println("Wallet task manager started...");
            simulator.setBalanceShowPublicKey(show);
        }
        else {
            System.out.println("Your assigned role is: Miner (i.e., you can mine blocks).");
            Miner miner = new Miner(wname, wpassword);
            System.out.printf("Welcome %s, blockchain miner created for you.%n", wname);
            WalletConnectionAgent agent = new WalletConnectionAgent(ipAddress,
                    Configuration.networkPort(), miner);
            Thread agentThread = new Thread(agent);
            MinerMessageTaskManager manager = new MinerMessageTaskManager(agent, miner,
                    agent.getMessageQueue());
            Thread managerThread = new Thread(manager);
            WalletSimulator simulator = new WalletSimulator(miner, agent, manager);
            manager.setSimulator(simulator);
            agentThread.start();
            System.out.println("Miner connection agent started...");
            managerThread.start();
            System.out.println("Miner task manager started...");
            simulator.setBalanceShowPublicKey(show);
        }
    }
}

// Inner class for displaying the wallet balance or the blockchain content.
class MessageFrame extends JFrame {
    Container c = this.getContentPane();
    JTextArea msg = new JTextArea();
    JScrollPane pane = new JScrollPane();

    // Constructor
    public MessageFrame() {
        super("Information Board");
        this.setBounds(0, 0, 600, 450);
        JScrollPane pane = new JScrollPane(this.msg);
        pane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        pane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        c.add(pane);
        msg.setLineWrap(false);
        msg.setRows(100);
        msg.setColumns(80);
        this.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                // do nothing
            }
        });
    }

    public void setMessage(String message) {
        msg.setText(message);
        this.validate();
        this.setVisible(true);
    }

    public void appendMessage(String message) {
        msg.append(message);
        this.validate();
        this.setVisible(true);
    }
}

// Inner class for displaying the help messages.
class FrameHelp extends JFrame {
    JTextPane msg = new JTextPane();

    // Constructor
    public FrameHelp() {
        super("Help Message");
        Container c = this.getContentPane();
        this.setBounds(500, 500, 300, 220);
        msg.setBounds(0, 0, this.getWidth(), this.getHeight());
        c.add(msg);
    }

    public void setMessage(String message) {
        msg.setText(message);
        this.validate();
        this.setVisible(true);
    }
}

// Inner class for initiating a transaction.
class FrameTransaction extends JFrame implements ActionListener {
    private ArrayList<KeyNamePair> users = null;
    private WalletConnectionAgent agent = null;

    // Constructor
    public FrameTransaction(ArrayList<KeyNamePair> users, WalletConnectionAgent agent) {
        super("Prepare Transaction");
        this.users = users;
        this.agent = agent;
        setup();
    }

    // Method for setting up the Frame.
    private void setup() {
        Container c = this.getContentPane();
        this.setSize(300, 120);
        GridLayout layout = new GridLayout(3, 2, 5, 5);
        JLabel je = new JLabel("Please select a user");
        JLabel jf = new JLabel("Transaction amount");
        JButton js = new JButton("Submit");
        JButton jc = new JButton("Cancel");
        c.setLayout(layout);
        c.add(je);
        c.add(jf);
        JComboBox<String> candidates = new JComboBox<String>();
        for (int i=0; i<users.size(); i++)
            candidates.addItem(users.get(i).getName());
        c.add(candidates);
        JTextField input = new JTextField();
        c.add(input);
        c.add(js);
        c.add(jc);
        js.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = candidates.getSelectedIndex();
                double amount = -1.;
                String text = input.getText();
                if (text != null && text.length() > 0) {
                    try {
                        amount = Double.parseDouble(text);
                    }
                    catch (Exception ex) {
                        amount = -1;
                    }
                    // Transaction amount must be positive
                    if (amount <= 0) {
                        input.setText("Amount must be positive.");
                        return;
                    }
                    boolean b = agent.sendTransaction(
                            users.get(selectedIndex).getKey(), amount);
                    if (!b)
                        input.setText("Failed to send transaction.");
                    else
                        input.setText("Transaction sent.");
                }
            }
        });
        jc.addActionListener(this);
        this.setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        this.dispose();
    }
}

// Inner class for sending private chat messages.
class FramePrivateMessage extends JFrame implements ActionListener {
    private ArrayList<KeyNamePair> users = null;
    private WalletConnectionAgent agent = null;
    private JTextArea board = null;
    private WalletSimulator simulator;

    // Constructor
    public FramePrivateMessage(ArrayList<KeyNamePair> users,
                               WalletConnectionAgent agent, WalletSimulator simulator) {
        super("Send a private message");
        this.users = users;
        this.agent = agent;
        this.simulator = simulator;
        setup();
    }

    // Method for setting up the private chat messages frame.
    private void setup() {
        Container c = getContentPane();
        this.setSize(300, 200);
        GridBagLayout mgr = new GridBagLayout();
        GridBagConstraints gcr = new GridBagConstraints();
        c.setLayout(mgr);
        JLabel ja = new JLabel("Please choose:");
        gcr.fill = GridBagConstraints.BOTH;
        gcr.weightx = .5;
        gcr.weighty = 0.;
        gcr.gridx = 0;
        gcr.gridy = 0;
        gcr.gridwidth = 1;
        gcr.gridheight = 1;
        mgr.setConstraints(ja, gcr);
        c.add(ja);
        JComboBox<String> candidates = new JComboBox<String>();
        for (int i=0; i<users.size(); i++)
            candidates.addItem(users.get(i).getName());

        gcr.weightx = .5;
        gcr.weighty = 0.9;
        gcr.gridx = 1;
        gcr.gridy = 0;
        gcr.gridwidth = 1;
        gcr.gridheight = 1;
        mgr.setConstraints(candidates, gcr);
        c.add(candidates);

        gcr.weightx = 1.;
        gcr.weighty = 0.9;
        gcr.gridx = 0;
        gcr.gridy = 1;
        gcr.gridwidth = 2;
        gcr.gridheight = 2;
        JTextArea input = new JTextArea(2, 30);
        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        mgr.setConstraints(input, gcr);
        c.add(input);

        gcr.weighty = 0.;
        gcr.gridx = 0;
        gcr.gridy = 3;
        gcr.gridwidth = 1;
        gcr.gridheight = 1;
        JButton js = new JButton("Send");
        mgr.setConstraints(js, gcr);
        c.add(js);
        js.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int selectedIndex = candidates.getSelectedIndex();
                String text = input.getText();
                if (text != null && text.length() > 0) {
                    PublicKey key = users.get(selectedIndex).getKey();
                    boolean b = agent.sendPrivateMessage(key, text);
                    if (b) {
                        input.setText("Message sent!");
                        simulator.appendMessageLineOnBoard("private-->"
                            + agent.getNameFromAddress(key) + "]: " + text);
                    }
                    else
                        input.setText("ERROR: Failed to send message.");
                }
            }
        });

        gcr.weighty = 0.;
        gcr.gridx = 1;
        gcr.gridy = 3;
        gcr.gridwidth = 1;
        gcr.gridheight = 1;
        JButton jc = new JButton("Cancel");
        mgr.setConstraints(jc, gcr);
        c.add(jc);
        jc.addActionListener(this);
        this.setVisible(true);
    }

    public void actionPerformed(ActionEvent e) {
        this.dispose();
    }
}