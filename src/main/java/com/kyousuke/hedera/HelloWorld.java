package com.kyousuke.hedera;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hedera.hashgraph.sdk.*;
import io.github.cdimascio.dotenv.Dotenv;
import org.jetbrains.annotations.Contract;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    public static Client createNewClient(Client client) throws TimeoutException, HederaPreCheckStatusException, HederaReceiptStatusException {
        PrivateKey privateKey = PrivateKey.generate();
        PublicKey publicKey = privateKey.getPublicKey();

        AccountId accountId = new AccountCreateTransaction()
                .setKey(publicKey)
                .setInitialBalance(new Hbar(1000))
                .execute(client)
                .getReceipt(client)
                .accountId;

        Client newClient = Client.forTestnet().setOperator(Objects.requireNonNull(accountId), privateKey);

        Hbar accountBalance = new AccountBalanceQuery().setAccountId(accountId).execute(newClient).hbars;
        System.out.println(accountBalance);

        return newClient;
    }

    public static void main(String[] args) throws TimeoutException, HederaPreCheckStatusException, HederaReceiptStatusException, IOException {
        ClassLoader cl = HelloWorld.class.getClassLoader();

        Gson gson = new Gson();

        JsonObject jsonObject;

        try (InputStream jsonStream = cl.getResourceAsStream("hello_world.json")) {
            if (jsonStream == null) {
                throw new RuntimeException("Failed to get .json file");
            }

            jsonObject = gson.fromJson(new InputStreamReader(jsonStream, StandardCharsets.UTF_8), JsonObject.class);
        }

        String byteCodeHex = jsonObject.getAsJsonPrimitive("object")
                .getAsString();

        // Create your Hedera testnet client
        // Client client = Client.forTestnet();
        // client.setOperator(myAccountId, myPrivateKey);

        Client client;

        if (HEDERA_NETWORK != null && HEDERA_NETWORK.equals("previewnet")) {
            client = Client.forPreviewnet();
        } else {
            try {
                client = Client.fromConfigFile(CONFIG_FILE != null ? CONFIG_FILE : "");
            } catch (Exception e) {
                client = Client.forTestnet();
            }
        }

        // Defaults the operator account ID and key such that all generated transactions will be paid for
        // by this account and be signed by this key
        client.setOperator(OPERATOR_ID, OPERATOR_KEY);

        Hbar hbar = new AccountBalanceQuery().setAccountId(OPERATOR_ID).execute(client).hbars;
        System.out.println("Operator account balance: " + hbar);

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
                .setGas(500)
                .setBytecodeFileId(fileId)
                // set an admin key so we can delete the contract later
                .setAdminKey(OPERATOR_KEY)
                .setMaxTransactionFee(new Hbar(16))
                .execute(client);

        TransactionReceipt contractReceipt = contractTxResponse.getReceipt(client);

        System.out.println(contractReceipt);

        ContractId contractId = Objects.requireNonNull(contractReceipt.contractId);

        System.out.println("new contract ID: " + contractId);

        ContractFunctionResult contractFunctionResult = new ContractCallQuery()
                .setGas(6000)
                .setContractId(contractId)
                .setFunction("greet")
                .setMaxQueryPayment(new Hbar(1))
                .execute(client);

        if (contractFunctionResult.errorMessage != null) {
            System.out.println("Error calling contract: " + contractFunctionResult.errorMessage);
            return;
        }

        System.out.println("Contract message: " + contractFunctionResult.getString(0));

        Client newClient = createNewClient(client);

        ContractFunctionResult newContractFunctionResult = new ContractCallQuery()
                .setGas(6000)
                .setContractId(contractId)
                .setFunction("greet")
                .setMaxQueryPayment(new Hbar(1))
                .execute(newClient);

        System.out.println(newContractFunctionResult.getString(0));

        // Delete the contract
        TransactionReceipt contractDeleteResult = new ContractDeleteTransaction()
                .setContractId(contractId)
                .setMaxTransactionFee(new Hbar(1))
                .execute(client)
                .getReceipt(client);

        if (contractDeleteResult.status != Status.SUCCESS) {
            System.out.println("Error deleting contract: " + contractDeleteResult.status);
        } else {
            System.out.println("Contract successfully deleted.");
        }
    }
}
