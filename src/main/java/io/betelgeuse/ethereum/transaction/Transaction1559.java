package io.betelgeuse.ethereum.transaction;

import io.betelgeuse.ethereum.pwg.Bytes;
import io.betelgeuse.ethereum.pwg.Numeric;
import io.betelgeuse.ethereum.pwg.Sign;
import io.betelgeuse.ethereum.rlp.RlpList;
import io.betelgeuse.ethereum.rlp.RlpString;
import io.betelgeuse.ethereum.rlp.RlpType;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static io.betelgeuse.ethereum.transaction.TransactionType.EIP1559;


/**
 * Transaction class used for signing 1559 transactions locally.<br>
 * For the specification, refer to p4 of the <a href="http://gavwood.com/paper.pdf">yellow
 * paper</a>.
 */
public class Transaction1559 extends LegacyTransaction implements ITransaction {

    private long chainId;
    private BigInteger maxPriorityFeePerGas;
    private BigInteger maxFeePerGas;

    public Transaction1559(
            long chainId,
            BigInteger nonce,
            BigInteger gasLimit,
            String to,
            BigInteger value,
            String data,
            BigInteger maxPriorityFeePerGas,
            BigInteger maxFeePerGas) {
        super(EIP1559, nonce, null, gasLimit, to, value, data);
        this.chainId = chainId;
        this.maxPriorityFeePerGas = maxPriorityFeePerGas;
        this.maxFeePerGas = maxFeePerGas;
    }

    @Override
    public List<RlpType> asRlpValues(Sign.SignatureData signatureData) {

        List<RlpType> result = new ArrayList<>();

        result.add(RlpString.create(getChainId()));

        result.add(RlpString.create(getNonce()));

        // add maxPriorityFeePerGas and maxFeePerGas if this is an EIP-1559 transaction
        result.add(RlpString.create(getMaxPriorityFeePerGas()));
        result.add(RlpString.create(getMaxFeePerGas()));

        result.add(RlpString.create(getGasLimit()));

        // an empty to address (contract creation) should not be encoded as a numeric 0 value
        String to = getTo();
        if (to != null && to.length() > 0) {
            // addresses that start with zeros should be encoded with the zeros included, not
            // as numeric values
            result.add(RlpString.create(Numeric.hexStringToByteArray(to)));
        } else {
            result.add(RlpString.create(""));
        }

        result.add(RlpString.create(getValue()));

        // value field will already be hex encoded, so we need to convert into binary first
        byte[] data = Numeric.hexStringToByteArray(getData());
        result.add(RlpString.create(data));

        // access list
        result.add(new RlpList());

        if (signatureData != null) {
            result.add(RlpString.create(Sign.getRecId(signatureData, getChainId())));
            result.add(RlpString.create(Bytes.trimLeadingZeroes(signatureData.getR())));
            result.add(RlpString.create(Bytes.trimLeadingZeroes(signatureData.getS())));
        }

        return result;
    }

    public static Transaction1559 createEtherTransaction(
            long chainId,
            BigInteger nonce,
            BigInteger gasLimit,
            String to,
            BigInteger value,
            BigInteger maxPriorityFeePerGas,
            BigInteger maxFeePerGas) {
        return new Transaction1559(
                chainId, nonce, gasLimit, to, value, "", maxPriorityFeePerGas, maxFeePerGas);
    }

    public static Transaction1559 createTransaction(
            long chainId,
            BigInteger nonce,
            BigInteger gasLimit,
            String to,
            BigInteger value,
            String data,
            BigInteger maxPriorityFeePerGas,
            BigInteger maxFeePerGas) {

        return new Transaction1559(
                chainId, nonce, gasLimit, to, value, data, maxPriorityFeePerGas, maxFeePerGas);
    }

    @Override
    public BigInteger getGasPrice() {
        throw new UnsupportedOperationException("not available for 1559 transaction");
    }

    public long getChainId() {
        return chainId;
    }

    public BigInteger getMaxPriorityFeePerGas() {
        return maxPriorityFeePerGas;
    }

    public BigInteger getMaxFeePerGas() {
        return maxFeePerGas;
    }
}
