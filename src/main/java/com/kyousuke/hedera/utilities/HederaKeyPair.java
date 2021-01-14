package com.kyousuke.hedera.utilities;

import com.hedera.hashgraph.sdk.PrivateKey;
import com.hedera.hashgraph.sdk.PublicKey;

public class HederaKeyPair {
    private final PrivateKey privateKey;
    public final PublicKey publicKey;

    public HederaKeyPair() {
        privateKey = PrivateKey.generate();
        publicKey = privateKey.getPublicKey();
    }

    public HederaKeyPair(PrivateKey privateKey) {
        this.privateKey = privateKey;
        this.publicKey = this.privateKey.getPublicKey();
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }
}
