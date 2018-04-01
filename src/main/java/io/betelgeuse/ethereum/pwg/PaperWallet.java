package io.betelgeuse.ethereum.pwg;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.databind.ObjectMapper;

public class PaperWallet {

	public static final int PHRASE_SIZE_DEFAULT = 8;
	public static final String WALLET_OK = "OK";
	public static final String WALLET_ERROR = "ERROR";
	
	// 20 GWei as of august '17. check http://ethgasstation.info/ or similar
	public static BigInteger GAS_PRICE_DEFAULT = BigInteger.valueOf(20_000_000_000L);
	
	// 21'000 gas. check https://ethereum.stackexchange.com/questions/5845/how-are-ethereum-transaction-costs-calculated
	public static BigInteger GAS_LIMIT_DEFAULT = BigInteger.valueOf(21_000L);

	private static PassPhraseUtility passPhraseUtility = new PassPhraseUtility();

	private Credentials credentials = null;
	private String fileName;
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
			fileName = generateNewWalletFile(this.passPhrase, new File(this.pathToFile));
			credentials = getCredentials(this.passPhrase);
		}
		catch (Exception e) {
			throw new Exception("Failed to create account", e);
		}
	}
	
	public String createOfflineTx(String toAddress, BigInteger gasPrice, BigInteger gasLimit, BigInteger amount, BigInteger nonce) {
		RawTransaction rawTransaction  = RawTransaction.createEtherTransaction(nonce, gasPrice, gasLimit, toAddress, amount);
		byte[] signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials);
		String hexValue = Numeric.toHexString(signedMessage);
		
		return hexValue;
	}

	public static String getPathToFileDefault() {
		return getDefaultKeyDirectory();
	}
	
	public static String checkWalletFileStatus(File sourceFile, String passPhrase) {
		// check if provided file exists
		if(!sourceFile.exists() || sourceFile.isDirectory()) { 
			return String.format("%s file does not exist or is a directory", WALLET_ERROR);
		}
		
		WalletFile walletFile = null;

		// try to create wallet file object
		try {
	        ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
	        walletFile = objectMapper.readValue(sourceFile, WalletFile.class);
		} 
		catch (Exception e) {
			String message = e.getLocalizedMessage();
			
			if(message == null) {
				message = "general wallet file format error";
			}
			
			return String.format("%s %s", WALLET_ERROR, message);
		}
		
		// try to decrypt wallet file object
		ECKeyPair keyPair = null;
		
		try {
	        keyPair = Wallet.decrypt(passPhrase, walletFile);
		} 
		catch (Exception e) {
			String message = e.getLocalizedMessage();
			
			if(message == null) {
				message = "general wallet file decryption error";
			}
			
			return String.format("%s %s", WALLET_ERROR, message);
		}
		
		if(keyPair != null) {
			String privateK = encode(keyPair.getPrivateKey());
			String publicK = encode(keyPair.getPublicKey());
			return String.format("%s\npublic key: %s\nprivate key: %s", WALLET_OK, publicK, privateK);
		}
		else {
			return WALLET_OK;
		}
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
		// TODO check that this is the desired encoding...
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

	public static String generateNewWalletFile(String password, File destinationDirectory)
			throws CipherException, IOException, InvalidAlgorithmParameterException,
			NoSuchAlgorithmException, NoSuchProviderException {

		ECKeyPair ecKeyPair = Keys.createEcKeyPair();
		return generateWalletFile(password, ecKeyPair, destinationDirectory);
	}

	public static String generateWalletFile(
			String password, ECKeyPair ecKeyPair, File destinationDirectory)
			throws CipherException, IOException {
		WalletFile walletFile = Wallet.create(password, ecKeyPair);

		String fileName = getWalletFileName(walletFile);
		File destination = new File(destinationDirectory, fileName);

		ObjectMapper objectMapper = ObjectMapperFactory.getObjectMapper();
		objectMapper.writeValue(destination, walletFile);

		return fileName;
	}

	private static String getWalletFileName(WalletFile walletFile) {
		DateTimeFormatter format = DateTimeFormatter.ofPattern(
				"'UTC--'yyyy-MM-dd'T'HH-mm-ss.nVV'--'");
		ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);

		return now.format(format) + walletFile.getAddress() + ".json";
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
