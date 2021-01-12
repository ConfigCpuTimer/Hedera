package com.kyousuke.hedera.utilities;

import com.hedera.hashgraph.sdk.*;

import java.util.concurrent.TimeoutException;

public class HederaAccount {
    private final AccountId accountId;
    private final HederaKeyPair hederaKeyPair;

    {
        hederaKeyPair = new HederaKeyPair();
    }

    private HederaAccount(Client client) throws TimeoutException, HederaPreCheckStatusException, HederaReceiptStatusException {
        this.accountId = new AccountCreateTransaction()
                .setKey(this.getHederaKeyPair().getPublicKey())
                .setInitialBalance(new Hbar(100))
                .execute(client)
                .getReceipt(client)
                .accountId;
    }

    public static HederaAccount getHederaAccountInstance(Client client) throws HederaReceiptStatusException, TimeoutException, HederaPreCheckStatusException {
        return new HederaAccount(client);
    }

    public HederaKeyPair getHederaKeyPair() {
        return hederaKeyPair;
    }

    public AccountId getAccountId() {
        return accountId;
    }
}
