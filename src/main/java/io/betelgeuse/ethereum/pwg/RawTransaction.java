package io.betelgeuse.ethereum.pwg;

import java.math.BigInteger;

/**
 * <p>Transaction class used for signing transactions locally.<br>
 * For the specification, refer to p4 of the <a href="http://gavwood.com/paper.pdf">
 * yellow paper</a>.</p>
 */
public class RawTransaction {
    // default as per https://github.com/ethereum/wiki/wiki/JSON-RPC#eth_sendtransaction
    public static final String DEFAULT_GAS = Numeric.toHexStringWithPrefix(BigInteger.valueOf(9000));

    private BigInteger nonce;
    private BigInteger gasPrice;
    private BigInteger gasLimit;
    private String to;
    private BigInteger value;
    private String data;

    private RawTransaction(BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, String to,
                           BigInteger value, String data) {
        this.nonce = nonce;
        this.gasPrice = gasPrice;
        this.gasLimit = gasLimit;
        this.to = to;
        this.value = value;

        if (data != null) {
            this.data = Numeric.cleanHexPrefix(data);
        }
    }

    public static RawTransaction createContractTransaction(
            BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, BigInteger value,
            String init) {

        return new RawTransaction(nonce, gasPrice, gasLimit, "", value, init);
    }

    public static RawTransaction createEtherTransaction(
            BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, String to,
            BigInteger value) {

        return new RawTransaction(nonce, gasPrice, gasLimit, to, value, "");

    }

    public static RawTransaction createFunctionCallTransaction(
            BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, String to, String data) {
        return createFunctionCallTransaction(nonce, gasPrice, gasLimit, to, BigInteger.ZERO, data);
    }

    public static RawTransaction createFunctionCallTransaction(
            BigInteger nonce, BigInteger gasPrice, BigInteger gasLimit, String to,
            BigInteger value, String data) {

        return new RawTransaction(nonce, gasPrice, gasLimit, to, value, data);
    }

    public BigInteger getNonce() {
        return nonce;
    }

    public BigInteger getGasPrice() {
        return gasPrice;
    }

    public BigInteger getGasLimit() {
        return gasLimit;
    }

    public String getTo() {
        return to;
    }

    public BigInteger getValue() {
        return value;
    }

    public String getData() {
        return data;
    }
}
