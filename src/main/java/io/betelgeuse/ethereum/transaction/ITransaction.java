package io.betelgeuse.ethereum.transaction;

import io.betelgeuse.ethereum.pwg.Sign;
import io.betelgeuse.ethereum.rlp.RlpType;

import java.math.BigInteger;
import java.util.List;

public interface ITransaction {

    List<RlpType> asRlpValues(Sign.SignatureData signatureData);

    BigInteger getNonce();

    BigInteger getGasPrice();

    BigInteger getGasLimit();

    String getTo();

    BigInteger getValue();

    String getData();

    TransactionType getType();
}
