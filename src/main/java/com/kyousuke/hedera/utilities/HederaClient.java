package com.kyousuke.hedera.utilities;

import com.hedera.hashgraph.sdk.*;
import com.kyousuke.hedera.utilities.HederaAccount;

import java.util.concurrent.TimeoutException;

public class HederaClient {
    /**
     * Create Singleton client instead of recreating everywhere
     */
    private static Client client = null;
    // private static MirrorClient mirrorClient = null;

    private HederaClient() {
        client = makeNewClient(EnvUtils.getOperatorId(), EnvUtils.getOperatorKey());
        // mirrorClient = new MirrorClient(EnvUtils.getMirrorNodeAddress());
    }

    public static Client getHederaClientInstance() {
        if(client == null) {
            new HederaClient();
        }
        return client;
    }

    public static Client makeNewClient(String accountId, String privateKey) {
        return makeNewClient(AccountId.fromString(accountId), PrivateKey.fromString(privateKey));
    }

    public static Client makeNewClient(AccountId accountId, PrivateKey privateKey) {
        Client client = (EnvUtils.getHederaEnvironment() == EnvUtils.Hedera_Environment.TESTNET) ?
                Client.forTestnet() : Client.forMainnet();
        // Defaults the operator account ID and key such that all generated transactions will be
        // paid for by this account and be signed by this key
        client.setOperator(accountId, privateKey);
        client.setMaxTransactionFee(new Hbar(6));
        client.setMaxQueryPayment(new Hbar(3));
        return client;
    }

    public static Client makeNewClientFromExistedClient(Client client) throws HederaReceiptStatusException, TimeoutException, HederaPreCheckStatusException {
        HederaAccount hederaAccount = HederaAccount.getHederaAccountInstance(client);

        return makeNewClient(hederaAccount.getAccountId(), hederaAccount.getHederaKeyPair().getPrivateKey());
    }

    /*public static MirrorClient getMirrorClient() {
        if (mirrorClient == null) {
            mirrorClient = new MirrorClient(EnvUtils.getMirrorNodeAddress());
        }
        return mirrorClient;
    }*/
}

