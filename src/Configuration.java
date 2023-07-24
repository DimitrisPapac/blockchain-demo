public class Configuration {
    private static String KEY_LOCATION = "keys";

    public static final String KeyLocation() {
        return Configuration.KEY_LOCATION;
    }

    private static String HASH_ALGORITHM = "SHA-256";
    public static final String hashAlgorithm() {
        return Configuration.HASH_ALGORITHM;
    }

    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    public static final String signatureAlgorithm() {
        return Configuration.SIGNATURE_ALGORITHM;
    }

    private static final String KEYPAIR_ALGORITHM = "RSA";
    public static final String keypairAlgorithm() {
        return Configuration.KEYPAIR_ALGORITHM;
    }

    private static final int PORT = 9001;
    public static final int networkPort() {
        return Configuration.PORT;
    }

    private static final int BLOCK_MINING_DIFFICULTY_LEVEL = 20;
    public static final int blockMiningDifficultyLevel() {
        return Configuration.BLOCK_MINING_DIFFICULTY_LEVEL;
    }
}