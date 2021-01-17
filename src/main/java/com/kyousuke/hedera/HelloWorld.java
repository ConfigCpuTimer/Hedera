package com.kyousuke.hedera;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hedera.hashgraph.sdk.*;
import com.hedera.hashgraph.sdk.proto.ContractCall;
import com.hedera.hashgraph.sdk.proto.ContractCallTransactionBody;
import com.kyousuke.hedera.utilities.HederaAccount;
import com.kyousuke.hedera.utilities.HederaClient;
import io.github.cdimascio.dotenv.Dotenv;
import org.jetbrains.annotations.Contract;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

public class HelloWorld {
    // Grab your Hedera testnet account ID and private key

    // Old version 1.1.5
    // AccountId myAccountId = AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("MY_ACCOUNT_ID")));
    // Ed25519PrivateKey myPrivateKey = Ed25519PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("MY_PRIVATE_KEY")));
    private static final AccountId OPERATOR_ID = AccountId.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_ID")));
    private static final PrivateKey OPERATOR_KEY = PrivateKey.fromString(Objects.requireNonNull(Dotenv.load().get("OPERATOR_KEY")));

    private static final String HEDERA_NETWORK = Dotenv.load().get("HEDERA_NETWORK");
    private static final String CONFIG_FILE = Dotenv.load().get("CONFIG_FILE");

    public static void auctionTest(Client client) throws IOException, TimeoutException, HederaPreCheckStatusException, HederaReceiptStatusException {
        JsonObject jsonObject;

        try(InputStream jsonStream = HelloWorld.class.getClassLoader().getResourceAsStream("AuctionTest.json")) {
            if(jsonStream == null) {
                throw new RuntimeException("Failed to get .json file");
            }

            jsonObject = new Gson()
                    .fromJson(new InputStreamReader(jsonStream, StandardCharsets.UTF_8), JsonObject.class);
        }

        String byteCodeHex = jsonObject.getAsJsonPrimitive("object")
                .getAsString();

        // TODO: Split byteCodeHex

        TransactionResponse fileTxResponse = new FileCreateTransaction()
                // Use the same key as the operator to "own" this file
                // .setKeys(/**/)
                .setContents(byteCodeHex.getBytes(StandardCharsets.UTF_8))
                .setMaxTransactionFee(new Hbar(2))
                .execute(client);

        TransactionResponse contractTxResponse = new ContractCreateTransaction()
                .setGas(6000)
                .setBytecodeFileId(Objects.requireNonNull(fileTxResponse.getReceipt(client).fileId))
                .setMaxTransactionFee(new Hbar(3))
                .execute(client);

        TransactionReceipt contractReceipt = contractTxResponse.getReceipt(client);

        System.out.println(contractReceipt);

        ContractId contractId = Objects.requireNonNull(contractReceipt.contractId);

        System.out.println("new contract ID: " + contractId);

        new ContractExecuteTransaction()
                .setContractId(contractId)
                .setGas(6000)
                .setFunction("submitBid", new ContractFunctionParameters().addInt256(BigInteger.valueOf(10)))
                .setMaxTransactionFee(new Hbar(10))
                .execute(client);

        new ContractExecuteTransaction()
                .setContractId(contractId)
                .setGas(6000)
                .setFunction("submitBid", new ContractFunctionParameters().addInt256(BigInteger.valueOf(20)))
                .setMaxTransactionFee(new Hbar(10))
                .execute(client);

        System.out.println(new ContractCallQuery()
                .setContractId(contractId)
                .setGas(6000)
                .setFunction("marketClear")
                .setMaxQueryPayment(new Hbar(10))
                .execute(client));
    }

    public static void main(String[] args) throws TimeoutException, HederaPreCheckStatusException, HederaReceiptStatusException, IOException {
        Client client;

        if(HEDERA_NETWORK != null && HEDERA_NETWORK.equals("previewnet")) {
            client = Client.forPreviewnet();
        } else {
            try {
                client = Client.fromConfigFile(CONFIG_FILE != null ? CONFIG_FILE : "");
            } catch(Exception e) {
                client = Client.forTestnet();
            }
        }

        // Defaults the operator account ID and key such that all generated transactions will be paid for
        // by this account and be signed by this key
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);

        Hbar hbar = new AccountBalanceQuery().setAccountId(OPERATOR_ID).execute(client).hbars;
        System.out.println("Operator account balance: " + hbar);

        auctionTest(client);

        ClassLoader cl = HelloWorld.class.getClassLoader();

        Gson gson = new Gson();

        JsonObject jsonObject;

        try(InputStream jsonStream = cl.getResourceAsStream("hello_world.json")) {
            if(jsonStream == null) {
                throw new RuntimeException("Failed to get .json file");
            }

            jsonObject = gson.fromJson(new InputStreamReader(jsonStream, StandardCharsets.UTF_8), JsonObject.class);
        }

        String byteCodeHex = jsonObject.getAsJsonPrimitive("object")
                .getAsString();

        // Create your Hedera testnet client
        // Client client = Client.forTestnet();
        // client.setOperator(myAccountId, myPrivateKey);



        TransactionResponse fileTxResponse = new FileCreateTransaction()
                // Use the same key as the operator to "own" this file
                .setKeys(OPERATOR_KEY)
                .setContents(byteCodeHex.getBytes(StandardCharsets.UTF_8))
                .setMaxTransactionFee(new Hbar(2))
                .execute(client);

        TransactionReceipt fileReceipt = fileTxResponse.getReceipt(client);
        FileId fileId = Objects.requireNonNull(fileReceipt.fileId);

        System.out.println("contract bytecode file: " + fileId);

        TransactionResponse contractTxResponse = new ContractCreateTransaction()
                .setGas(5000)
                .setBytecodeFileId(fileId)
                // set an admin key so we can delete the contract later
                .setAdminKey(OPERATOR_KEY)
                .setMaxTransactionFee(new Hbar(16))
                .setConstructorParameters(new ContractFunctionParameters().addString("Hello!"))
                .execute(client);

        TransactionReceipt contractReceipt = contractTxResponse.getReceipt(client);

        System.out.println(contractReceipt);

        ContractId contractId = Objects.requireNonNull(contractReceipt.contractId);

        System.out.println("new contract ID: " + contractId);


        /*if(contractFunctionResult.errorMessage != null) {
            System.out.println("Error calling contract: " + contractFunctionResult.errorMessage);
            return;
        }*/

        // System.out.println("Contract message: " + contractFunctionResult.getInt8(0));

        // Client newClient = HederaClient.makeNewClientFromExistedClient(client);

        System.out.println(new ContractCallQuery()
                .setGas(6000)
                .setContractId(contractId)
                .setFunction("get_message")
                .execute(client)
                .getString(0));

        new ContractExecuteTransaction()
                .setGas(6000)
                .setContractId(contractId)
                .setFunction("set_message",
                        new ContractFunctionParameters().addString("Hello Again!"))
                .execute(client);

        System.out.println(new ContractCallQuery()
                .setGas(6000)
                .setContractId(contractId)
                .setFunction("get_message")
                // .setMaxQueryPayment(new Hbar(1))
                .execute(HederaClient.makeNewClientFromExistedClient(client))
                .getString(0));


        // Delete the contract
        TransactionReceipt contractDeleteResult = new ContractDeleteTransaction()
                .setContractId(contractId)
                .setMaxTransactionFee(new Hbar(1))
                .execute(client)
                .getReceipt(client);

        if(contractDeleteResult.status != Status.SUCCESS) {
            System.out.println("Error deleting contract: " + contractDeleteResult.status);
        } else {
            System.out.println("Contract successfully deleted.");
        }
    }
}
