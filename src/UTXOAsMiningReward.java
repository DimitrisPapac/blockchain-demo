import java.security.PublicKey;
public class UTXOAsMiningReward extends UTXO {
    private static final long serialVersionUID = 1L;

    // Constructor
    public UTXOAsMiningReward(String parentTransactionUID, PublicKey sender,
                              PublicKey receiver, double fundToTransfer) {
        super(parentTransactionUID, sender, receiver, fundToTransfer);   //call super
    }

    public boolean isMiningReward() {
        return true;
    }
}
