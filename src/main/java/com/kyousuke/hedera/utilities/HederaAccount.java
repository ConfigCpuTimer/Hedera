package com.kyousuke.hedera.utilities;

import com.hedera.hashgraph.sdk.*;

import java.util.concurrent.TimeoutException;

public class HederaAccount {
    public final AccountId accountId;
    public HederaKeyPair hederaKeyPair;

    private HederaAccount(Client client) throws TimeoutException, HederaPreCheckStatusException, HederaReceiptStatusException {
        hederaKeyPair = new HederaKeyPair();

        accountId = new AccountCreateTransaction()
                .setKey(this.getHederaKeyPair().getPublicKey())
                .setInitialBalance(new Hbar(100))
                .execute(client)
                .getReceipt(client)
                .accountId;
    }

    public static HederaAccount getHederaAccountInstance(Client client) throws HederaReceiptStatusException, TimeoutException, HederaPreCheckStatusException {
        return new HederaAccount(client);
    }

    public AccountId getAccountId() {
        return accountId;
    }

    public HederaKeyPair getHederaKeyPair() {
        return hederaKeyPair;
    }
}
