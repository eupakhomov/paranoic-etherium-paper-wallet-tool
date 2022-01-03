package io.betelgeuse.ethereum.pwg;

import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.Security;
import java.util.Scanner;

/**
 * -p '...' -w  "e:\DCIM\100HPAIO\3.json" -t 0x988E3CA5Ed4E0d3eEeF189f37A994b5947Df22BF -a 150 -n 2
 * https://badmofo.github.io/ethsend/
 */
public class Application {

    public static final String SWITCH_DIRECTORY = "-d";
    public static final String SWITCH_PASS_PHRASE = "-p";
    public static final String SWITCH_WALLET = "-w";
    public static final String SWITCH_ADDRESS = "-t";
    public static final String SWITCH_AMOUNT = "-a";
    public static final String SWITCH_NONCE = "-n";
    public static final String SWITCH_VERIFY = "-v";
    public static final String SWITCH_SILENT = "-s";
    public static final String SWITCH_GAS_LIMIT = "-l";
	public static final String SWITCH_MAX_TIPS = "-i";
	public static final String SWITCH_MAX_FEE = "-f";
    public static final String SWITCH_HELP = "-h";

	public static final String ARGUMENTS_ERROR = "ARGUMENTS ERROR";

	public static final String CREATE_OK = "WALLET CREATION OK";
	public static final String CRATE_ERROR = "WALLET CREATION ERROR";
	
	public static final String VERIFY_OK = "WALLET VERIFICATION OK";
	public static final String VERIFY_ERROR = "WALLET VERIFICATION ERROR";
	
	public static final String EXT_HTML = "html";
	public static final String EXT_PNG = "png";

    // target directory for new wallet file etc.
	private String targetDirectory = PaperWallet.getPathToFileDefault();

	// pass phrase for the wallet file
	private String passPhrase = null;

	// existing wallet file location
	private String walletFile = null;

	// target address for offline transaction (need to specify wallet file)
	private String targetAddress = null;

	// nonce value for offline transaction
	private Integer nonce = 0;

	// amount [ethers] for offline transaction
	private BigDecimal amount = new BigDecimal("0.01");

	// verify the specified wallet file
	private boolean verify = false;

	// silent mode, suppress command line output
	private boolean silent = false;

    // gas limit to use with offline transaction
    private BigInteger gasLimit = PaperWallet.GAS_LIMIT_DEFAULT;

    // max tips to miners (Gwei) - eg 5 Gwei
	private String maxTips = "5";

	// max overall fee (Gwei) - eg 200 Gwei
	private String maxFee = "200";

	public static void main(String[] args) {
		Application app = new Application();
		app.run(args);
	}

	private void parseArguments(String [] args) {
	    int i = 0;

	    if(args.length == 0) {
            printHelp();
	        return;
        }

	    do {
	        String key = args[i];
	        i++;

	        switch(key) {
                case SWITCH_DIRECTORY:
                    targetDirectory = args[i];
                    i++;
                    break;
                case SWITCH_PASS_PHRASE:
                    passPhrase = args[i];
                    i++;
                    break;
                case SWITCH_WALLET:
                    walletFile = args[i];
                    i++;
                    break;
                case SWITCH_ADDRESS:
                    targetAddress = args[i];
                    i++;
                    break;
                case SWITCH_AMOUNT:
                    amount = new BigDecimal(args[i]);
                    i++;
                    break;
                case SWITCH_NONCE:
                    nonce = Integer.valueOf(args[i]);
                    i++;
                    break;
				case SWITCH_MAX_TIPS:
                    maxTips = args[i];
                    i++;
                    break;
				case SWITCH_MAX_FEE:
					maxFee = args[i];
					i++;
					break;
                case SWITCH_GAS_LIMIT:
                    gasLimit = new BigInteger(args[i]);
                    i++;
                    break;
                case SWITCH_VERIFY:
                    verify = true;
                    break;
                case SWITCH_SILENT:
                    silent = true;
                    break;
                case SWITCH_HELP:
                default:
                    printHelp();
            }
        } while(i < args.length);
    }

	public String run(String [] args) {
	    parseArguments(args);

        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        if(walletFile != null) {
            if(targetAddress != null || verify) {
                if(verify) {
                    return verifyWalletFile();
                }
                else {
                    return createOfflineTx();
                }
            }
            else {
                System.err.println("Invalid arguments: for a specified wallet you need to specify -v or -t");
            }
        }else if(passPhrase != null) {
			return createWalletFile();
		}

		return ARGUMENTS_ERROR;
	}

	public String verifyWalletFile() {
		readPassPhrase();

		log("Veriying wallet file ...");
		String statusMessage = PaperWallet.checkWalletFileStatus(new File(walletFile), passPhrase);

		if(statusMessage.startsWith(PaperWallet.WALLET_OK)) {
			log("Wallet file successfully verified");
			log("Wallet file: " + walletFile);
			log("Pass phrase: " + passPhrase);
			
			return VERIFY_OK;
		}
		else {
			log("Verification failed: " + statusMessage);
			log("Wallet file: " + walletFile);
			log("Pass phrase: " + passPhrase);
			
			return String.format("%s %s", VERIFY_ERROR, statusMessage);
		}
	}

	private String createOfflineTx() {
		readPassPhrase();

		try {
			PaperWallet pw = new PaperWallet(passPhrase, new File(walletFile));
			BigInteger amountWei = Convert.toWei(amount.toPlainString(), Convert.Unit.ETHER).toBigInteger();

			BigInteger maxPriorityFeePerGas = Convert.toWei(maxTips, Convert.Unit.GWEI).toBigInteger();
			BigInteger maxFeePerGas = Convert.toWei(maxFee, Convert.Unit.GWEI).toBigInteger();

			log("Target address:     " + targetAddress);
			log("Amount [Ether]:     " + amount);
			log("Nonce:              " + nonce);
			log("Gas limit [Wei]:    " + gasLimit);
			log("Max prio fee [Wei]: " + maxPriorityFeePerGas);
			log("Max fee [Wei]:      " + maxFeePerGas);

			String txData = pw.createOfflineTx(targetAddress,
					gasLimit,
					amountWei,
					BigInteger.valueOf(nonce),
					maxPriorityFeePerGas,
					maxFeePerGas);
			String curlCmd = String.format("Transaction body: %s", txData);

			log(curlCmd);

			return curlCmd;
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	private void readPassPhrase() {
		if(passPhrase == null) {
			Scanner scanner = new Scanner(System.in);
			System.out.print("Wallet pass phrase: ");
			passPhrase = scanner.next();
			scanner.close();
		}
	}

	public String createWalletFile() {
		PaperWallet pw;
		
		log("Creating wallet ...");
		
		try {
			pw = new PaperWallet(passPhrase, targetDirectory);
		}
		catch(Exception e) {
			return String.format("%s %s", CRATE_ERROR, e.getLocalizedMessage());
		}
		
		log("Wallet file successfully created");
		log(String.format("Wallet pass phrase: %s", pw.getPassPhrase()));
		log(String.format("Wallet file location: %s", pw.getFile().getAbsolutePath()));

		String html = WalletPageUtility.createHtml(pw);
		byte [] qrCode = QrCodeUtility.contentToPngBytes(pw.getAddress(), 256);

		String path = pw.getPathToFile();
		String baseName = pw.getBaseName();
		String htmlFile = String.format("%s%s%s.%s", path, File.separator, baseName, EXT_HTML);
		String pngFile = String.format("%s%s%s.%s", path, File.separator, baseName, EXT_PNG);

		log("Writing additional output files ...");
		FileUtility.saveToFile(html, htmlFile);
		FileUtility.saveToFile(qrCode, pngFile);
		log(String.format("Html wallet: %s", htmlFile));
		log(String.format("Address qr code: %s", pngFile));
		
		return String.format("%s %s", CREATE_OK, pw.getFile().getAbsolutePath());
	}

	private void printHelp() {
        System.out.print("Usage: java -jar target/epwg.jar ");
        System.out.print("[-d path]");
        System.out.print("[-p password]");
        System.out.print("[-w location]");
        System.out.print("[-t address]");
        System.out.println("[-a amount]");
        System.out.print("[-n nonce]");
        System.out.print("[-v]");
        System.out.print("[-s]");
        System.out.print("[-l limit]");
		System.out.print("[-i max tips]");
		System.out.print("[-f max fee]");
        System.out.println("[-h]");
        System.out.println();

        System.out.println("Options: ");
        System.out.println("  -d              Target directory for new wallet file");
        System.out.println("  -p              Pass phrase for the wallet file");
        System.out.println("  -w              Existing wallet file location");
        System.out.println("  -t              Target address for offline transaction (need to specify wallet file)");
        System.out.println("  -a              Amount [ethers] for offline transaction");
        System.out.println("  -n              Nonce value for offline transaction");
        System.out.println("  -v              Verify the specified wallet file");
        System.out.println("  -s              Silent mode, suppress command line output");
        System.out.println("  -l              Gas limit for offline transaction");
		System.out.println("  -i              Max tips to miners (priority fee) - check https://www.blocknative.com/gas-estimator");
		System.out.println("  -f              Max overall fee - check https://www.blocknative.com/gas-estimator");
        System.out.println("  -h              Show help");

    }

	private void log(String message) {
		if(!silent) {
			System.out.println(message);
		}
	}
}
