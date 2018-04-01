package io.betelgeuse.ethereum.pwg;

import org.bouncycastle.jcajce.provider.digest.Keccak;

import java.math.BigInteger;
import java.security.*;
import java.security.spec.ECGenParameterSpec;

/**
 * Keys util.
 */
public class Keys {

    public static final int ADDRESS_LENGTH_IN_HEX = 40;

    static final int PRIVATE_KEY_SIZE = 32;
    static final int PUBLIC_KEY_SIZE = 64;

    static final int PUBLIC_KEY_SIZE_IN_HEX = 64 << 1;

    private Keys() { }

    /**
     * Create a keypair using SECP-256k1 curve.
     *
     * Private keypairs are encoded using PKCS8
     * Private keys are encoded using X.509
     */
    static KeyPair createSecp256k1KeyPair() throws NoSuchProviderException,
            NoSuchAlgorithmException, InvalidAlgorithmParameterException {

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("ECDSA", "BC");
        ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec("secp256k1");
        keyPairGenerator.initialize(ecGenParameterSpec, new SecureRandom());
        return keyPairGenerator.generateKeyPair();
    }

    public static ECKeyPair createEcKeyPair() throws InvalidAlgorithmParameterException,
            NoSuchAlgorithmException, NoSuchProviderException {
        KeyPair keyPair = createSecp256k1KeyPair();
        return ECKeyPair.create(keyPair);
    }

    public static String getAddress(ECKeyPair ecKeyPair) {
        return getAddress(ecKeyPair.getPublicKey());
    }

    public static String getAddress(BigInteger publicKey) {
        return getAddress(
                Numeric.toHexStringWithPrefixZeroPadded(publicKey, PUBLIC_KEY_SIZE_IN_HEX));
    }

    public static String getAddress(String publicKey) {
        String publicKeyNoPrefix = Numeric.cleanHexPrefix(publicKey);

        if (publicKeyNoPrefix.length() < PUBLIC_KEY_SIZE_IN_HEX) {
            publicKeyNoPrefix = Numeric.zeros(
                    PUBLIC_KEY_SIZE_IN_HEX - publicKeyNoPrefix.length()) +
                    publicKeyNoPrefix;
        }
        String hash = sha3(publicKeyNoPrefix);
        return hash.substring(hash.length() - ADDRESS_LENGTH_IN_HEX);  // right most 160 bits
    }

    /**
     * Keccak-256 hash function
     *
     * @param hexInput hex encoded input data with optional 0x prefix
     * @return hash value as hex encoded string
     */
    public static String sha3(String hexInput) {
        byte[] bytes = Numeric.hexStringToByteArray(hexInput);
        byte[] result = sha3(bytes);
        return Numeric.toHexString(result);
    }

    /**
     * Keccak-256 hash function
     *
     * @param input binary encoded input data
     * @param offset of start of data
     * @param length of data
     * @return hash value
     */
    public static byte[] sha3(byte[] input, int offset, int length) {
        Keccak.DigestKeccak kecc = new Keccak.Digest256();
        kecc.update(input, offset, length);
        return kecc.digest();
    }

    public static byte[] sha3(byte[] input) {
        return sha3(input, 0, input.length);
    }
}
