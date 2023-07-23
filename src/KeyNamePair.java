import java.io.Serializable;
import java.security.PublicKey;

public class KeyNamePair implements Serializable {
    private static final long serialVersionUID = 1L;
    private PublicKey key;
    private String name;

    // Constructor
    public KeyNamePair(PublicKey key, String name) {
        this.key = key;
        this.name = name;
    }

    /* Getter methods */
    public String getName() {
        return this.name;
    }

    public PublicKey getKey() {
        return this.key;
    }

}
