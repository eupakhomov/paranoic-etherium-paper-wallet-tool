# Paper Wallet Generator for Ethereum

## Application Description

Command line tool to create (offline) Ethereum paper wallets.
Paranoic version of [Matthias Zimmermann tool](https://github.com/matthiaszimmermann/ethereum-paper-wallet)
with cut off dependencies using ported fragments of [web3j](https://github.com/web3j/web3j) related to cryptographic functionality.

## Demo Output

The output of the tool is a HTML page that can be viewed in any browser. 
An example output is provided below.
![HTML Page](/screenshots/paper_wallet_html.png)

As we want to create paper wallets, the CSS is prepared make the HTML printable.

![Printed Wallet](/screenshots/paper_wallet_printed.png)

## Run the Application

After cloning this repo build the command line tool using Maven.

```
mvn clean package
```

The result of the Maven build is an executable JAR file.

### Creating a Paper Wallet
 
Use the following command to create a paper wallet (underlying format is BIP39).

```
java -jar target/epwg-0.4.0-SNAPSHOT.jar -d C:\Users\Admin\AppData\Local\Temp -p TestPassPhrase
```

This will lead to some information on the console

```
Creating wallet ...
Wallet file successfully created
Wallet pass phrase: TestPassPhrase
Wallet file location: C:\Users\Admin\AppData\Local\Temp\UTC--2017-01-14T11-34-23.830000000Z--b86bab51c139f9662ccea6547a5e34e13d144bb0.json
Writing additional output files ...
Html wallet: C:\Users\Admin\AppData\Local\Temp\UTC--2017-01-14T11-34-23.830000000Z--b86bab51c139f9662ccea6547a5e34e13d144bb0.html
Address qr code: C:\Users\Admin\AppData\Local\Temp\UTC--2017-01-14T11-34-23.830000000Z--b86bab51c139f9662ccea6547a5e34e13d144bb0.png
```

Three file are created by the tool as indicated in the output above
* The actual wallet file (UTC--2017-01-14T11-34-23.83... .json)
* The HTML file for printing (UTC--2017-01-14T11-34-23.83... .html)
* The image file with the QR code for the paper wallet address (UTC--2017-01-14T11-34-23.83... .png)

### Verifying a (Paper) Wallet

The tool also allows to verify a provided wallet file against a provided pass phrase.

```
java -jar target/epwg-0.4.0-SNAPSHOT.jar -p TestPassPhrase -w  "C:\Users\Admin\AppData\Local\Temp\UTC--2017-01-14T11-34-23.830000000Z--b86bab51c139f9662ccea6547a5e34e13d144bb0.json" -v
```

This will lead to some information on the console

```
Veriying wallet file ...
Wallet file successfully verified
Wallet file: C:\Users\Admin\AppData\Local\Temp\UTC--2017-01-14T11-34-23.830000000Z--b86bab51c139f9662ccea6547a5e34e13d144bb0.json
Pass phrase: TestPassPhrase
```

### Creating an offline Transaction

The tool further allows to create an offline EIP-1559 transaction for provided wallet details

```
java -jar target/epwg-0.4.0-SNAPSHOT.jar -p TestPassPhrase -w  "C:\Users\Admin\AppData\Local\Temp\UTC--2017-01-14T11-34-23.830000000Z--b86bab51c139f9662ccea6547a5e34e13d144bb0.json" -t 0x025403ff4c543c660423543a9c5a3cc2a02e2f1f -a 0.0123
```

leading to the following output.

```
Target address:     0x025403ff4c543c660423543a9c5a3cc2a02e2f1f
Amount [Ether]:     0.0123
Nonce:              0
Gas limit [Wei]:    21000
Max prio fee [Wei]: 3000000000
Max fee [Wei]:      185000000000
Transaction body: 0xf86b808504a817c80082520894025403ff4c543c660423543a9c5a3cc2a02e2f1f872bb2c8eabcc000801ba08c5b25a10edb8e72518f4e6f51527df718d090f80cefcf024669340fe29cf78aa0124a95546dc897b6987c2b05efd2be7ed976318174a6bf9300d6f11c1d5d2da1
```

The last line may be used to send the transaction to the Etherem network (via https://etherscan.io/pushTx). 

## Dependencies

The project is developed using Java 8. Building the project is done with Maven. 
To create QR codes the [ZXing](https://github.com/zxing/zxing) library is used.
To handle json conversions the [Jackson](https://github.com/FasterXML/jackson) library is used.
To support cryptography the [Bouncy Castle](https://www.bouncycastle.org/) library is used.
Most of the crypto functionality implemented using ported fragments of [web3j](https://github.com/web3j/web3j)

