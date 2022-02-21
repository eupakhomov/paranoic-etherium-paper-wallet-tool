package io.betelgeuse.ethereum.pwg;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;


public class PaperWallet {

	public static final int PHRASE_SIZE_DEFAULT = 8;
	public static final String WALLET_OK = "OK";
	public static final String WALLET_ERROR = "ERROR";

	// 21'000 gas - cost of simple transaction.
	// Check https://ethereum.stackexchange.com/questions/5845/how-are-ethereum-transaction-costs-calculated
	public static BigInteger GAS_LIMIT_DEFAULT = BigInteger.valueOf(150_000L);

	private static PassPhraseUtility passPhraseUtility = new PassPhraseUtility();

	private Credentials credentials = null;
	private String fileName;
	private String mnemonic;
	private String pathToFile;
	private String passPhrase;
	
	public PaperWallet(String passPhrase, File walletFile) {
		// check if provided file exists
		if(!walletFile.exists() || walletFile.isDirectory()) { 
			System.err.println(String.format("%s file does not exist or is a directory", WALLET_ERROR));
		}
		
		try {
			credentials = loadCredentials(passPhrase, walletFile);
		} 
		catch (Exception e) {
			System.err.println(String.format("%s failed to load credentials with provided password", WALLET_ERROR));
		}
	}

	public PaperWallet(String passPhrase, String pathToFile) throws Exception {
		this.passPhrase = setPassPhrase(passPhrase);
		this.pathToFile = setPathToFile(pathToFile);

		try {
			Bip39Wallet wallet = generateWalletFile(this.passPhrase, new File(this.pathToFile));
			fileName = wallet.getFilename();
			mnemonic = wallet.getMnemonic();
			credentials = getCredentials(this.passPhrase);
		}
		catch (Exception e) {
			throw new Exception("Failed to create account", e);
		}
	}

	/**
	 * EIP-1559 changed how Ethereum transaction fees are calculated and where those fees go.
	 * Instead of a singular Gas Price, you now have to pay attention to three separate values:
	 * 		- Base Fee, which is determined by the network itself. And is subsequently burned.
	 * 		- Max Priority Fee, which is optional, determined by the user, and is paid directly to miners.
	 * 		- Max Fee Per Gas, which is the absolute maximum you are willing to pay per unit of gas to get your transaction included in a block.
	 *
	 * 	The Max Priority Fee — also often referred to as the miner tip — is an 'optional' additional fee that is paid directly to miners
	 * 	in order to incentivize them to include your transaction in a block. While the Max Priority Fee is technically optional,
	 * 	at the moment most network participants estimate that transactions generally require a minimum 2.0 GWEI
	 * 	tip to be candidates for inclusion. With that said, specific mining pools may choose to set alternative minimums for inclusion.
	 *
	 * For 'typical' transactions that are submitted under normal, not-congested network conditions,
	 * the Max Priority Fee will need to be close to 2.0 GWEI. But, for transactions where order or inclusion in the next block is important,
	 * or when the network is highly congested, a higher Max Priority Fee may be necessary to prioritize your transaction.
	 *
	 * A somewhat subtle nuance to the Max Priority Fee is that it represents the maximum tip you are willing to pay to a miner.
	 * However, if the Base Fee plus the Max Priority Fee exceeds the Max Fee (see below), the Max Priority Fee will be reduced
	 * in order to maintain the upper bound of the Max Fee. This means the actual tip may need to be smaller than your Max Priority Fee and,
	 * under such circumstances, your transaction may become less attractive to miners.
	 *
	 * The Max Fee is the absolute maximum amount you are willing to pay per unit of gas to get your transaction confirmed.
	 * And here is where things can get a little confusing – given that, under most circumstances,
	 * your actual transaction fee will be less than the Max Fee you specify up front. Here is why:
	 * 		- The minimum gas price for your transactions is the current Base Fee.
	 * 		- However, what if the Base Fee increases while your transaction is pending? Your transaction will become underpriced and be at risk of becoming stuck. Or failing. Or getting dropped. This is undesirable for all of the reasons you might expect.
	 * 		- Hence, for predictable transaction settlement under EIP-1559, it is currently considered best practice to set a Max Fee that anticipates such an increase in the Base Fee. But by how much? And why?
	 * One can use the following simple heuristic to calculate the recommended Max Fee for any given Base Fee and Max Priority Fee combination:
	 * 		  Max Fee = (2 * Base Fee) + Max Priority Fee
	 * Doubling the Base Fee when calculating  the Max Fee ensures that your transaction will remain marketable for six consecutive 100%
	 * full blocks.
	 *
	 * Please use Gas Estimator:
	 * https://www.blocknative.com/gas-estimator
	 */
	public String createOfflineTx(String toAddress,
								  BigInteger gasLimit,
								  BigInteger amountWei,
								  BigInteger nonce,
								  BigInteger maxPriorityFeePerGas,
								  BigInteger maxFeePerGas) {

		RawTransaction rawTransaction  = RawTransaction.createEtherTransaction(
				ChainIdLong.MAINNET,
				nonce,
				gasLimit,
				toAddress,
				amountWei,
				maxPriorityFeePerGas,
				maxFeePerGas
				);

		byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, ChainIdLong.MAINNET, credentials);
		return Numeric.toHexString(signedMessage);
	}

	public static String getPathToFileDefault() {
		return getDefaultKeyDirectory();
	}
	
	public static String checkWalletFileStatus(File sourceFile, String passPhrase) {
		// check if provided file exists
		if(!sourceFile.exists() || sourceFile.isDirectory()) { 
			return String.format("%s file does not exist or is a directory", WALLET_ERROR);
		}
		
		WalletFile walletFile;

		// try to create wallet file object
		try {
	        ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
	        walletFile = objectMapper.readValue(sourceFile, WalletFile.class);
		} 
		catch (Exception e) {
			String message = e.getMessage();
			
			if(message == null) {
				message = "general wallet file format error";
			}
			
			return String.format("%s %s", WALLET_ERROR, message);
		}
		
		// try to decrypt wallet file object
		ECKeyPair keyPair;
		
		try {
	        keyPair = Wallet.decrypt(passPhrase, walletFile);
		} 
		catch (Exception e) {
			String message = e.getMessage();
			
			if(message == null) {
				message = "general wallet file decryption error";
			}
			
			return String.format("%s %s", WALLET_ERROR, message);
		}
		
		String privateK = encode(keyPair.getPrivateKey());
		String publicK = encode(keyPair.getPublicKey());
		return String.format("%s\npublic key: %s\nprivate key: %s", WALLET_OK, publicK, privateK);
	}

	public Credentials getCredentials() {
		return credentials;
	}

	public Credentials getCredentials(String passPhrase) throws Exception {
		if (credentials != null) {
			return credentials;
		}

		try {
			String fileWithPath = getFile().getAbsolutePath();
			credentials = loadCredentials(passPhrase, fileWithPath);

			return credentials;
		}
		catch (Exception e) {
			throw new Exception ("Failed to access credentials in file '" + getFile().getAbsolutePath() + "'", e);
		}
	}

	public String getAddress() {
		return credentials.getAddress();
	}

	public String getPassPhrase() {
		return passPhrase;
	}

	public String getPathToFile() {
		return pathToFile;
	}

	public String getFileName() {
		return fileName;
	}

	public String getMnemonic() {
		return mnemonic;
	}

	public String getFileContent() throws Exception {
		try {
			return String.join("", Files.readAllLines(getFile().toPath()));
		} 
		catch (IOException e) {
			throw new Exception ("Failed to read content from file '" + getFile().getAbsolutePath() + "'", e);
		}
	}

	public File getFile() {
		return new File(pathToFile, fileName);
	}

	private String setPassPhrase(String passPhrase) {
		if(passPhrase == null || passPhrase.isEmpty()) {
			return passPhraseUtility.getPassPhrase(PHRASE_SIZE_DEFAULT);
		}

		return passPhrase;
	}

	private String setPathToFile(String pathToFile) {
		if(pathToFile == null || pathToFile.isEmpty()) {
			return getPathToFileDefault();
		}

		return pathToFile;
	}

	public String getBaseName() {
		if(fileName == null) {
			return null;
		}
		
		int pos = fileName.lastIndexOf(".");
		
		if(pos >= 0) {
			return fileName.substring(0, pos);
		}
		
		return fileName;
	}
	
	private static String encode(BigInteger number) {
		return Keys.getAddress(number);
	}

	public static Credentials loadCredentials(String password, String source)
			throws IOException, CipherException {
		return loadCredentials(password, new File(source));
	}

	public static Credentials loadCredentials(String password, File source)
			throws IOException, CipherException {
		ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
		WalletFile walletFile = objectMapper.readValue(source, WalletFile.class);
		return Credentials.create(Wallet.decrypt(password, walletFile));
	}

	public static Bip39Wallet generateWalletFile(
			String password, File destinationDirectory)
			throws CipherException, IOException {
		return WalletUtils.generateBip39Wallet(password, destinationDirectory);
	}

	public static String getDefaultKeyDirectory() {
		return getDefaultKeyDirectory(System.getProperty("os.name"));
	}

	static String getDefaultKeyDirectory(String osNameIn) {
		String osName = osNameIn.toLowerCase();

		if (osName.startsWith("mac")) {
			return System.getProperty("user.home") + "/Library/Ethereum";
		} else if (osName.startsWith("win")) {
			return System.getenv("APPDATA") + "/Ethereum";
		} else {
			return System.getProperty("user.home") + "/.ethereum";
		}
	}

}
