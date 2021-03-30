package com.kyousuke.hedera;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.hedera.hashgraph.sdk.*;
import com.kyousuke.hedera.files.FileService;
import com.kyousuke.hedera.market.BuyerTradeAgent;
import com.kyousuke.hedera.market.SellerTradeAgent;
import com.kyousuke.hedera.market.TradeAgent;
import com.kyousuke.hedera.utilities.HederaClient;
import io.github.cdimascio.dotenv.Dotenv;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.distribution.WeibullDistribution;

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

    public static void auctionTest() throws IOException, TimeoutException, HederaPreCheckStatusException, HederaReceiptStatusException {
        ClassLoader cl = HelloWorld.class.getClassLoader();

        Gson gson = new Gson();

        JsonObject jsonObject;

        try(InputStream jsonStream = cl.getResourceAsStream("AuctionTest.json")) {
            if(jsonStream == null) {
                throw new RuntimeException("Failed to get .json file");
            }

            jsonObject = gson.fromJson(new InputStreamReader(jsonStream, StandardCharsets.UTF_8), JsonObject.class);
        }

        String byteCodeHex = jsonObject.getAsJsonPrimitive("object")
                .getAsString();

        Client client = Client.forTestnet();

        client.setOperator(OPERATOR_ID, OPERATOR_KEY);

        TransactionResponse fileTxResponseEmpty = new FileCreateTransaction()
                // Use the same key as the operator to "own" this file
                .setKeys(OPERATOR_KEY)
                .setContents(""/*"".getBytes(StandardCharsets.UTF_8)*/)
                .setMaxTransactionFee(new Hbar(2))
                .execute(client);

        TransactionReceipt fileReceipt = fileTxResponseEmpty.getReceipt(client);
        FileId fileId = Objects.requireNonNull(fileReceipt.fileId);

        TransactionResponse fileAppendResponse = new FileAppendTransaction()
                .setFileId(fileId)
                .setMaxTransactionFee(new Hbar(10))
                .setContents(byteCodeHex.getBytes(StandardCharsets.UTF_8))
                .execute(client);

        /*TransactionResponse fileTxResponse = new FileCreateTransaction()
                // Use the same key as the operator to "own" this file
                .setKeys(OPERATOR_KEY)
                .setContents(byteCodeHex.getBytes(StandardCharsets.UTF_8))
                .setMaxTransactionFee(new Hbar(2))
                .execute(client);*/

        System.out.println("contract bytecode file: " + fileId);

        TransactionResponse contractTxResponse = new ContractCreateTransaction()
                .setGas(5000)
                .setBytecodeFileId(fileId)
                // set an admin key so we can delete the contract later
                .setAdminKey(OPERATOR_KEY)
                .setMaxTransactionFee(new Hbar(16))
                .execute(client);

        ContractId contractId = Objects.requireNonNull(contractTxResponse.getReceipt(client).contractId);

        System.out.println("new contract ID: " + contractId);

        new ContractExecuteTransaction()
                .setGas(6000)
                .setContractId(contractId)
                .setFunction("submitBid", new ContractFunctionParameters().addInt256(BigInteger.valueOf(10)))
                .setMaxTransactionFee(new Hbar(10))
                .execute(client);

        new ContractExecuteTransaction()
                .setGas(6000)
                .setContractId(contractId)
                .setFunction("submitBid", new ContractFunctionParameters().addInt256(BigInteger.valueOf(20)))
                .setMaxTransactionFee(new Hbar(10))
                .execute(client);

        ContractFunctionResult contractFunctionResult = new ContractCallQuery()
                .setGas(6000)
                .setContractId(contractId)
                .setFunction("marketClear")
                .setMaxQueryPayment(new Hbar(20))
                .execute(client);

        System.out.println(contractFunctionResult.getInt256(0));

        ContractFunctionResult contractFunctionResultLength = new ContractCallQuery()
                .setGas(6000)
                .setContractId(contractId)
                .setFunction("getLength")
                .setMaxQueryPayment(new Hbar(10))
                .execute(client);

        System.out.println(contractFunctionResultLength.getUint256(0));

        ContractFunctionResult contractFunctionResultTwo = new ContractCallQuery()
                .setGas(6000)
                .setContractId(contractId)
                .setFunction("marketClearTwo")
                .setMaxQueryPayment(new Hbar(10))
                .execute(client);

        System.out.println(contractFunctionResultTwo.getInt256(0));
    }

    public static void quickSortDescending(BuyerTradeAgent[] arr, int left, int right) {
        int i = left;
        int j = right;
        int pivotIndex = left + (right - left) / 2;
        BuyerTradeAgent pivot = arr[pivotIndex];

        while (i <= j) {
            while (arr[i].price.intValue() > pivot.price.intValue()) i++;
            while (arr[j].price.intValue() < pivot.price.intValue()) j--;

            if (i <= j) {
                BuyerTradeAgent tmp = arr[i];
                arr[i] = arr[j];
                arr[j] = tmp;

                i++;
                j--;
            }
        }

        if (left < j) quickSortDescending(arr, left, j);
        if (i < right) quickSortDescending(arr, i, right);
    }

    public static void quickSortAscending(SellerTradeAgent[] arr, int left, int right) {
        int i = left;
        int j = right;
        int pivotIndex = left + (right - left) / 2;
        SellerTradeAgent pivot = arr[pivotIndex];

        while (i <= j) {
            while (arr[i].price.intValue() < pivot.price.intValue()) i++;
            while (arr[j].price.intValue() > pivot.price.intValue()) j--;

            if (i <= j) {
                SellerTradeAgent tmp = arr[i];
                arr[i] = arr[j];
                arr[j] = tmp;

                i++;
                j--;
            }
        }

        if (left < j) quickSortAscending(arr, left, j);
        if (i < right) quickSortAscending(arr, i, right);
    }

    public static void doubleAuction() throws IOException, HederaReceiptStatusException, TimeoutException, HederaPreCheckStatusException {
        ClassLoader cl = HelloWorld.class.getClassLoader();

        Gson gson = new Gson();

        JsonObject jsonObject;

        try(InputStream jsonStream = cl.getResourceAsStream("DoubleAuction.json")) {
            if(jsonStream == null) {
                throw new RuntimeException("Failed to get .json file");
            }

            jsonObject = gson.fromJson(new InputStreamReader(jsonStream, StandardCharsets.UTF_8), JsonObject.class);
        }

        String byteCodeHex = jsonObject.getAsJsonPrimitive("object")
                .getAsString();

        Client client = Client.forTestnet();

        client.setOperator(OPERATOR_ID, OPERATOR_KEY);

        System.out.println("Operator account balance: " + new AccountBalanceQuery().setAccountId(OPERATOR_ID).execute(client).hbars);

        FileService fileService = new FileService(client);

        FileId fileId = fileService.createFileFromEmptyHead(byteCodeHex);

        TransactionResponse contractTxResponse = new ContractCreateTransaction()
                .setGas(5000)
                .setBytecodeFileId(fileId)
                // set an admin key so we can delete the contract later
                .setAdminKey(OPERATOR_KEY)
                .setMaxTransactionFee(new Hbar(16))
                .execute(client);

        TransactionReceipt transactionReceipt = contractTxResponse.getReceipt(client);

        ContractId contractId = Objects.requireNonNull(transactionReceipt.contractId);

        System.out.println(new ContractCallQuery()
                .setFunction("marketClearingTest")
                .setGas(1000)
                .setMaxQueryPayment(new Hbar(5))
                .setContractId(contractId)
                .execute(client)
                .getString(0));

        WeibullDistribution quantities = new WeibullDistribution(120, 200);
        UniformRealDistribution prices = new UniformRealDistribution(500, 1000);
//        BetaDistribution prices = new BetaDistribution(30, 30);

        BuyerTradeAgent[] buyerAgents = new BuyerTradeAgent[50];
        SellerTradeAgent[] sellerAgents = new SellerTradeAgent[50];

        for (int i = 0; i < buyerAgents.length; i++) {
            buyerAgents[i] = new BuyerTradeAgent();
        }

        for (int i = 0; i < sellerAgents.length; i++) {
            sellerAgents[i] = new SellerTradeAgent();
        }

        quickSortDescending(buyerAgents, 0, buyerAgents.length - 1);
        quickSortAscending(sellerAgents, 0, sellerAgents.length - 1);

        System.out.println("Buyers' bids: ");

        for (int i = 0; i < buyerAgents.length; i++) {
            System.out.println(buyerAgents[i].getQuantity() + ", " + buyerAgents[i].getPrice());
        }

        System.out.println("Sellers' bids: ");

        for (int i = 0; i < sellerAgents.length; i++) {
            System.out.println(sellerAgents[i].getQuantity() + ", " + sellerAgents[i].getPrice());
        }

        for (int i = 0; i < 50; i++) {
            for (BuyerTradeAgent x : buyerAgents) {
                new ContractExecuteTransaction()
                        .setFunction("consumptionBid",
                                new ContractFunctionParameters()
                                        .addInt256(x.getQuantity()).addInt256(x.getPrice()))
                        .setGas(10000)
                        .setMaxTransactionFee(new Hbar(10))
                        .setContractId(contractId)
                        .execute(client);
            }

            for (SellerTradeAgent x : sellerAgents) {
                new ContractExecuteTransaction()
                        .setFunction("generationBid",
                                new ContractFunctionParameters()
                                        .addInt256(x.getQuantity()).addInt256(x.getPrice()))
                        .setGas(10000)
                        .setMaxTransactionFee(new Hbar(10))
                        .setContractId(contractId)
                        .execute(client);
            }

            System.out.println("ConsumptionsLength: " + new ContractCallQuery()
                    .setFunction("getConsumptionsLength")
                    .setGas(1000)
                    .setMaxQueryPayment(new Hbar(5))
                    .setContractId(contractId)
                    .execute(client)
                    .getUint256(0));

            System.out.println("GenerationsLength: " + new ContractCallQuery()
                    .setFunction("getGenerationsLength")
                    .setGas(1000)
                    .setMaxQueryPayment(new Hbar(5))
                    .setContractId(contractId)
                    .execute(client)
                    .getUint256(0));

            new ContractExecuteTransaction()
                    .setFunction("marketClearing")
                    .setGas(299999)
                    .setMaxTransactionFee(new Hbar(10))
                    .setContractId(contractId)
                    .execute(client);

            int marketClearingPrice = new ContractCallQuery()
                    .setFunction("getClearingPrice")
                    .setGas(299999)
                    .setMaxQueryPayment(new Hbar(5))
                    .setContractId(contractId)
                    .execute(client)
                    .getInt256(0)
                    .intValue();

            int marketClearingQuantity = new ContractCallQuery()
                    .setFunction("getClearingQuantity")
                    .setGas(299999)
                    .setMaxQueryPayment(new Hbar(5))
                    .setContractId(contractId)
                    .execute(client)
                    .getInt256(0)
                    .intValue();

            System.out.println("Round " + i + "marketClearingPrice: " + marketClearingPrice);

            System.out.println("Round " + i + "marketClearingQuantity: " + marketClearingQuantity);

            for (BuyerTradeAgent x : buyerAgents) {
                x.adjust(marketClearingPrice);
            }

            for (SellerTradeAgent x : sellerAgents) {
                x.adjust(marketClearingPrice);
            }

            quickSortDescending(buyerAgents, 0, buyerAgents.length - 1);
            quickSortAscending(sellerAgents, 0, sellerAgents.length - 1);
        }
    }

    public static void main(String[] args) throws TimeoutException, HederaPreCheckStatusException, HederaReceiptStatusException, IOException {
        doubleAuction();

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
                .setFunction("bidding", new ContractFunctionParameters().addInt8((byte) 15))
                .setMaxQueryPayment(new Hbar(1))
                .execute(client);

        if(contractFunctionResult.errorMessage != null) {
            System.out.println("Error calling contract: " + contractFunctionResult.errorMessage);
            return;
        }

        System.out.println("Contract message: " + contractFunctionResult.getInt8(0));


        Client newClient = HederaClient.makeNewClientFromExistedClient(client);

        ContractFunctionResult newContractFunctionResult = new ContractCallQuery()
                .setGas(6000)
                .setContractId(contractId)
                .setFunction("bidding", new ContractFunctionParameters().addInt8((byte) 5))
                .setMaxQueryPayment(new Hbar(1))
                .execute(client);

        System.out.println(newContractFunctionResult.getInt8(0));

        System.out.println(new ContractCallQuery()
                .setGas(6000)
                .setContractId(contractId)
                .setFunction("getResult")
                .setMaxQueryPayment(new Hbar(1))
                .execute(HederaClient.makeNewClientFromExistedClient(client))
                .getInt8(0));



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
