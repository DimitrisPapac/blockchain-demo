import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.PrivateKey;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.Key;
import java.security.Signature;
import java.security.NoSuchAlgorithmException;
import java.security.AlgorithmParameters;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import java.util.Base64;
import java.util.Calendar;
import java.io.PrintStream;

public class UtilityMethods {

	private static long uniqueNumber = 0;

	private static final String SIGNING_ALGORITHM = "SHA256withRSA";

	public static byte[] messageDigestSHA256_toBytes(String message) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(message.getBytes());
			return md.digest();
		}
		catch(NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public static String messageDigestSHA256_toString(String message) {
		return Base64.getEncoder().encodeToString(messageDigestSHA256_toBytes(message));
	}

	public static long getTimeStamp() {
		return Calendar.getInstance().getTimeInMillis();
	}

	public static boolean hashMeetsDifficultyLevel(String hash, int difficultyLevel) {
		char[] c = hash.toCharArray();
		for (int i=0; i<difficultyLevel; i++)
			if (c[i] != '0')
				return false;
		return true;
	}

	public static String toBinaryString(byte[] hash) {
		StringBuilder sb = new StringBuilder();
		for (int i=0; i<hash.length; i++) {
			// Transform a byte into an unsigned integer.
			int x = ((int) hash[i]) + 128;
			String s = Integer.toBinaryString(x);
			while (s.length() < 8)
				s = "0" + s;  // prepend a leading zero
			sb.append(s);
		}
		return sb.toString();
	}

	public static long getUniqueNumber() {
		return UtilityMethods.uniqueNumber++;
	}

	public static KeyPair generateKeyPair() {
		try {
			KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
			kpg.initialize(2048);
			KeyPair pair = kpg.generateKeyPair();
			return pair;
		}
		catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	public static byte[] generateSignature(PrivateKey privateKey, String message) {
		try {
			Signature sig = Signature.getInstance(UtilityMethods.SIGNING_ALGORITHM);
			sig.initSign(privateKey);
			sig.update(message.getBytes());
			return sig.sign();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public static boolean verifySignature(PublicKey publicKey, byte[] signature, String message) {
		try {
			Signature sig2 = Signature.getInstance(UtilityMethods.SIGNING_ALGORITHM);
			sig2.initVerify(publicKey);
			sig2.update(message.getBytes());
			return sig2.verify(signature);
		}
		catch (Exception e) {
			e.printStackTrace();
			return false;
		}
	}

	// Utility method for transforming a Key instance from a byte array into a readable String.
	public static String getKeyString(Key key) {
		return Base64.getEncoder().encodeToString(key.getEncoded());
	}

	public static void displayTab(PrintStream out, int level, String s) {
		for (int i=0; i<level; i++)
			out.print("\t");
		out.println(s);
	}

	public static void displayUTXO(UTXO ux, PrintStream out, int level) {
		displayTab(out, level, "Fund: " + ux.getFundTransferred()
				+ ", Receiver: " + UtilityMethods.getKeyString(ux.getReceiver()));
	}

	public static void displayTransaction(Transaction tx, PrintStream out, int level) {
		displayTab(out, level, "Transaction{");
		displayTab(out, level + 1, "ID: " + tx.getHashID());
		displayTab(out, level + 1, "Sender: " + UtilityMethods.getKeyString(tx.getSender()));
		displayTab(out, level + 1, "Total fund to be transferred: " + tx.getTotalFundToTransfer());
		displayTab(out, level + 1, "Input:");
		for (int i=0; i<tx.getNumberOfInputUTXOs(); i++) {
			UTXO ui = tx.getInputUTXO(i);
			displayUTXO(ui, out, level + 2);
		}

		displayTab(out, level + 1, "Output:");
		for (int i=0; i<tx.getNumberOfOutputUTXOs() - 1; i++) {
			UTXO ut = tx.getOutputUTXO(i);
			displayUTXO(ut, out, level + 2);
		}
		UTXO change = tx.getOutputUTXO(tx.getNumberOfOutputUTXOs() - 1);
		displayTab(out, level + 2, "Change: " + change.getFundTransferred());
		displayTab(out, level + 1, "Transaction fee: " + Transaction.TRANSACTION_FEE);
		boolean b = tx.verifySignature();
		displayTab(out, level + 1, "Signature verification: " + b);
		displayTab(out, level, "}");
	}

	public static byte[] encryptionByXOR(byte[] key, String password) {
		byte[] pwds = UtilityMethods.messageDigestSHA256_toBytes(password);
		byte[] result = new byte[key.length];

		for (int i=0; i<key.length; i++) {
			int j = i % pwds.length;
			result[i] = (byte) ((key[i] ^ pwds[j]) & 0xFF);
		}

		return result;
	}

	public static byte[] decryptionByXOR(byte[] key, String password) {
		return encryptionByXOR(key, password);
	}

	public static byte[] intToBytes(int v) {
		byte[] b = new byte[Integer.BYTES];
		for (int i=b.length - 1; i>=0; i--) {
			b[i] = (byte) (v & 0xFF);
			v >>= Byte.SIZE;
		}
		return b;
	}

	public static int bytesToInt(byte[] b) {
		int v = 0;
		for (int i=0; i<b.length; i++) {
			v <<= Byte.SIZE;
			v |= b[i] & 0xFF;
		}
		return v;
	}

	// Utility method for performing AES encryption.
	public static byte[] encryptionByAES(byte[] key, String password) {
		try {
			byte[] salt = new byte[8];
			SecureRandom rand = new SecureRandom();

			// Generate salt
			rand.nextBytes(salt);

			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

			// Apply password and salt to prepare a KeySpec instance
			KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 1024, 128);

			// Create temporary key based on spec
			SecretKey tmp = factory.generateSecret(spec);

			// Use temporary key's information to obtain a SecretKey instance compatible
			// with AES encryption
			SecretKey secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

			// Create and initialize a Cipher object with the AES secret key
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);

			// Record Cipher instance's algorithm parameters for future decryption
			AlgorithmParameters params = cipher.getParameters();
			byte[] iv = params.getParameterSpec(IvParameterSpec.class).getIV();

			// Encrypt data (key)
			byte[] output = cipher.doFinal(key);

			// Organize data required for successful decryption
			byte[] outputSizeBytes = UtilityMethods.intToBytes(output.length);
			byte[] ivSizeBytes = UtilityMethods.intToBytes(iv.length);
			byte[] data = new byte[Integer.BYTES * 2
					+ salt.length + iv.length + output.length];

			// The order of the data is arranged as follows:
			// int_forOutputSize + int_forIVSize + 8_byte_salt + iv_bytes + output_bytes
			int z = 0;
			for (int i=0; i<outputSizeBytes.length; i++, z++)
				data[z] = outputSizeBytes[i];

			for (int i=0; i<ivSizeBytes.length; i++, z++)
				data[z] = ivSizeBytes[i];

			for (int i=0; i<salt.length; i++, z++)
				data[z] = salt[i];

			for (int i=0; i<iv.length; i++, z++)
				data[z] = iv[i];

			for (int i=0; i<output.length; i++, z++)
				data[z] = output[i];

			return data;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// Utility method for performing AES decryption.
	public static byte[] decryptionByAES(byte[] key, String password) {
		try {
			// Divide the input data key[] into proper values.
			// Order of data:
			// int_forOutputSize + int_forIVSize + 8_byte_salt + iv_bytes + output_bytes
			int z = 0;

			byte[] lengthByte = new byte[Integer.BYTES];
			for (int i=0; i<lengthByte.length; i++, z++)
				lengthByte[i] = key[z];

			int dataSize = bytesToInt(lengthByte);
			for (int i=0; i<lengthByte.length; i++, z++)
				lengthByte[i] = key[z];

			int ivSize = bytesToInt(lengthByte);
			byte[] salt = new byte[8];
			for (int i=0; i<salt.length; i++, z++)
				salt[i] = key[z];

			// IV bytes
			byte[] ivBytes = new byte[ivSize];
			for (int i=0; i<ivBytes.length; i++, z++)
				ivBytes[i] = key[z];

			// Real data bytes
			byte[] dataBytes = new byte[dataSize];
			for (int i=0; i<dataBytes.length; i++, z++)
				dataBytes[i] = key[z];

			// Once data is ready, reconstruct the key and cipher
			PBEKeySpec pbeKeySpec =
					new PBEKeySpec(password.toCharArray(), salt, 1024, 128);
			SecretKeyFactory secretKeyFactory =
					SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
			SecretKey tmp = secretKeyFactory.generateSecret(pbeKeySpec);
			SecretKey secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");
			Cipher cipher2 = Cipher.getInstance("AES/CBC/PKCS5Padding");

			// Algorithm parameters (ivBytes) are necessary to initiate cipher
			cipher2.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(ivBytes));

			// Decrypt data
			byte[] data = cipher2.doFinal(dataBytes);

			return data;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// Method for applying the recursive algorithm for computing the Merkle tree root
	// from a given array of hashes.
	public static String computeMerkleTreeRootHash(String[] hashes) {
		return computeMerkleTreeRootHash(hashes, 0, hashes.length - 1);
	}

	// Method for recursively building the Merkle tree root hash.
	private static String computeMerkleTreeRootHash(String[] hashes, int start, int end) {
		if (end - start + 1 == 1)        // for a single hash, the output is the hash itself
			return hashes[end];
		else if (end - start + 1 == 2)   // for two nodes, the output is their hash
			return messageDigestSHA256_toString(hashes[start] + hashes[end]);
		else {   // more than 2 nodes
			// Split array in two halves
			int mid = (start + end) >> 1;   // (start + end) / 2
			String msg = computeMerkleTreeRootHash(hashes, start, mid)
					+ computeMerkleTreeRootHash(hashes, mid + 1, end);
			return messageDigestSHA256_toString(msg);
		}
	}

	// Method for displaying the contents of a block.
	public static void displayBlock(Block block, PrintStream out, int level) {
		displayTab(out, level, "Block{");
		displayTab(out, level, "\tID: " + block.getHashID());
		// Display the transactions contained inside the block
		for (int i=0; i<block.getTotalNumberOfTransactions(); i++)
			displayTransaction(block.getTransaction(i), out, level + 1);
		// Display reward transaction
		if (block.getRewardTransaction() != null) {
			displayTab(out, level, "\tReward Transaction:");
			displayTransaction(block.getRewardTransaction(), out, level + 1);
		}
		displayTab(out, level, "}");
	}

	// Method for displaying the contents of a blockchain
	public static void displayBlockchain(Blockchain ledger, PrintStream out, int level) {
		displayTab(out, level, "Blockchain{ Number of blocks: " + ledger.size());
		// Display the contents of each block in the chain
		for (int i=0; i<ledger.size(); i++) {
			Block block = ledger.getBlock(i);
			displayBlock(block, out, level + 1);
		}
		displayTab(out, level, "}");
	}

}