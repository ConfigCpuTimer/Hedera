package com.kyousuke.hedera.utilities;

import com.hedera.hashgraph.sdk.*;

import java.util.concurrent.TimeoutException;

public class HederaAccount {
    public final AccountId accountId;
    public HederaKeyPair hederaKeyPair;

    private HederaAccount() {

    }

    public static HederaAccount makeNewAccount(Client client) throws TimeoutException, HederaPreCheckStatusException, HederaReceiptStatusException {
        HederaAccount hederaAccount = new HederaAccount();

        AccountId accountId = new AccountCreateTransaction()
                .setKey(hederaAccount.getHederaKeyPair().getPublicKey())
                .setInitialBalance(new Hbar(100))
                .execute(client)
                .getReceipt(client)
                .accountId;

        return hederaAccount;
    }

    public static HederaAccount getHederaAccountInstance() {
        return new HederaAccount();
    }

    public HederaKeyPair getHederaKeyPair() {
        return hederaKeyPair;
    }
}
