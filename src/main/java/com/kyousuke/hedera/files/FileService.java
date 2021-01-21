package com.kyousuke.hedera.files;

import com.hedera.hashgraph.sdk.*;
import com.kyousuke.hedera.utilities.EnvUtils;
import com.kyousuke.hedera.utilities.HederaClient;
import com.kyousuke.hedera.utilities.Utils;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeoutException;

import static com.kyousuke.hedera.utilities.Utils.FILE_PART_SIZE;

public class FileService {
    public Client client;

    public FileService() {
        client = HederaClient.getHederaClientInstance();
    }

    public FileService(Client client) {
        this.client = client;
    }

    public FileId createFileFromEmptyHead(String text) throws TimeoutException, HederaPreCheckStatusException, HederaReceiptStatusException {
        byte[] textToAdd = text.getBytes();
        int numParts = textToAdd.length / FILE_PART_SIZE;
        int remainder = textToAdd.length % FILE_PART_SIZE;
        // add in 5k chunks
        byte[] firstPartBytes;
        if (textToAdd.length <= FILE_PART_SIZE) {
            firstPartBytes = textToAdd;
            remainder = 0;
        } else {
            firstPartBytes = Utils.copyBytes(0, FILE_PART_SIZE, textToAdd);
        }

        // create the file
        TransactionResponse fileTxId =
                new FileCreateTransaction()
                        .setExpirationTime(Instant.now().plus(Duration.ofMillis(7890000000L)))
                        // Use the same key as the operator to "own" this file
                        .setKeys(client.getOperatorPublicKey())
                        .setContents("")
                        .setMaxTransactionFee(new Hbar(5))
                        .execute(client);

        TransactionReceipt fileReceipt = fileTxId.getReceipt(client);
        FileId newFileId = Objects.requireNonNull(fileReceipt.fileId);
        System.out.println("file: " + newFileId);

        // add remaining chunks (append rest of the parts)
        // append the rest of the parts
        for (int i = 0; i < numParts; i++) {
            byte[] partBytes = Utils.copyBytes(i * FILE_PART_SIZE, FILE_PART_SIZE, textToAdd);
            appendToFile(newFileId, partBytes);
        }
        // appending remaining data
        if (remainder > 0) {
            byte[] partBytes = Utils.copyBytes(numParts * FILE_PART_SIZE, remainder, textToAdd);
            appendToFile(newFileId, partBytes);
        }

         return newFileId;
    }

    /**
     * Create a hedera file with the text
     * Handles case with byte[] of text > 5000 (max size of hedera packet).
     *
     * @return fileId
     */
    public String createFile(String text) throws TimeoutException, HederaPreCheckStatusException, HederaReceiptStatusException {

        byte[] textToAdd = text.getBytes();
        int numParts = textToAdd.length / FILE_PART_SIZE;
        int remainder = textToAdd.length % FILE_PART_SIZE;
        // add in 5k chunks
        byte[] firstPartBytes;
        if (textToAdd.length <= FILE_PART_SIZE) {
            firstPartBytes = textToAdd;
            remainder = 0;
        } else {
            firstPartBytes = Utils.copyBytes(0, FILE_PART_SIZE, textToAdd);
        }

        // create the file
        TransactionResponse fileTxId =
                new FileCreateTransaction()
                        .setExpirationTime(Instant.now().plus(Duration.ofMillis(7890000000L)))
                        // Use the same key as the operator to "own" this file
                        .setKeys(EnvUtils.getOperatorKey().getPublicKey())
                        .setContents(firstPartBytes)
                        .setMaxTransactionFee(new Hbar(5))
                        .execute(client);

        TransactionReceipt fileReceipt = fileTxId.getReceipt(client);
        FileId newFileId = fileReceipt.fileId;
        System.out.println("file: " + newFileId);

        // add remaining chunks (append rest of the parts)
        // append the rest of the parts
        for (int i = 1; i < numParts; i++) {
            byte[] partBytes = Utils.copyBytes(i * FILE_PART_SIZE, FILE_PART_SIZE, textToAdd);
            appendToFile(newFileId, partBytes);
        }
        // appending remaining data
        if (remainder > 0) {
            byte[] partBytes = Utils.copyBytes(numParts * FILE_PART_SIZE, remainder, textToAdd);
            appendToFile(newFileId, partBytes);
        }

        return newFileId.toString();
    }

    //Helper method to append file.
    //get contents in a file
    //delete a file
    public boolean deleteFile(String fileId) throws TimeoutException, HederaPreCheckStatusException, HederaReceiptStatusException {
        new FileDeleteTransaction()
                .setFileId(FileId.fromString(fileId))
                .execute(client)
                .getReceipt(client); // if this doesn't throw then the transaction was a success
        System.out.println("deleted file " + fileId);
        return true;
    }

    //get info of a file
    /*public FileInfo getFileInfo(String fileId) throws TimeoutException, HederaPreCheckStatusException {
        long cost = new FileInfoQuery()
                .setFileId(FileId.fromString(fileId))
                .getCost(client);
        FileInfo info = new FileInfoQuery()
                .setFileId(FileId.fromString(fileId))
                .setQueryPayment(cost + cost / 50) //add 2% to estimated cost
                .execute(client);
        return info;
    }*/

    /**
     * Append content to a File.
     * Handles case with byte[] of text > 5000 (max size of hedera packet).
     *
     * @return success or failure
     */
    public boolean appendToFile(String fileIdStr, String text) throws TimeoutException, HederaPreCheckStatusException {
        System.out.println("adding to file");
        byte[] textToAdd = text.getBytes();
        FileId fileId = FileId.fromString(fileIdStr);
        if (textToAdd.length < FILE_PART_SIZE) {
            return appendToFile(fileId, textToAdd);
        }
        //else: divide file into chunks of FILE_PART_SIZE and append them individually.
        int numParts = textToAdd.length / FILE_PART_SIZE;
        int remainder = textToAdd.length % FILE_PART_SIZE;

        for (int i = 0; i < numParts; i++) {
            byte[] partBytes = Utils.copyBytes(i * FILE_PART_SIZE, FILE_PART_SIZE, textToAdd);
            appendToFile(fileId, partBytes);
        }
        // appending remaining data
        if (remainder > 0) {
            byte[] partBytes = Utils.copyBytes(numParts * FILE_PART_SIZE, remainder, textToAdd);
            appendToFile(fileId, partBytes);
        }
        return true;
    }

    private boolean appendToFile(FileId fileId, byte[] contents) throws TimeoutException, HederaPreCheckStatusException {
        new FileAppendTransaction()
                .setFileId(fileId)
                .setMaxTransactionFee(new Hbar(5))
                .setContents(contents)
                .execute(client);
        return true;
    }

    /*public String queryFileContent(String fileId) throws TimeoutException, HederaPreCheckStatusException {
        Hbar cost = new FileContentsQuery()
                .setFileId(FileId.fromString(fileId))
                .getCost(client);
        byte[] contents = new FileContentsQuery()
                .setFileId(FileId.fromString(fileId))
                .setQueryPayment(cost + cost / 50) //add 2% to estimated cost
                .execute(client);
        String contentsStr = new String(contents);
        // Prints query results to console
        System.out.println("File content query results: " + contentsStr);
        return contentsStr;
    }*/
}