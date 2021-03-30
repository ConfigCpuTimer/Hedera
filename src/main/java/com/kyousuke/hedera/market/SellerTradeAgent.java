package com.kyousuke.hedera.market;

import com.google.common.collect.EvictingQueue;
import org.apache.commons.math3.distribution.BetaDistribution;
import org.apache.commons.math3.distribution.UniformRealDistribution;
import org.apache.commons.math3.distribution.WeibullDistribution;

import java.math.BigInteger;
import java.util.Queue;

public class SellerTradeAgent extends TradeAgent {
    public BigInteger quantity;
    public BigInteger price;

    public double shortTerm;
    public double middleTerm;
    public double longTerm;

    public double errLastRound;
    public Queue<Double> errHistory;

    public boolean checkState;

    public SellerTradeAgent() {
        this.quantity = new BigInteger(String.valueOf(Math.round(new WeibullDistribution(120, 200).sample())));
        this.price = new BigInteger(String.valueOf(Math.round(new UniformRealDistribution(500, 1000).sample())));

        this.shortTerm = 0.5d;
        this.middleTerm = 0.5d;
        this.longTerm = 0.5d;

        this.errLastRound = 0.0d;
        this.errHistory = EvictingQueue.create(5);
    }

    public void adjust(int marketClearingPrice/*,
                       int maxBuyPrice,
                       int minBuyPrice,
                       int maxSellPrice,
                       int minSellPrice,
                       int quantitiesSupplied,
                       int quantitiesCleared*/) {
        if (this.price.intValue() < marketClearingPrice) {
            this.quantity = new BigInteger(String.valueOf(Math.round(new WeibullDistribution(120, 200).sample())));
            this.price = new BigInteger(String.valueOf(Math.round(new BetaDistribution(30, 30).sample() * 1000.0d)));
        } else {
            double priceCurrent = price.intValue();
/*

            int quantitiesUncleared = quantitiesSupplied - quantitiesCleared;

            double alpha = (double) quantitiesCleared / (double) quantitiesSupplied;
            double beta = (double) quantitiesUncleared / (double) quantitiesSupplied;

            double gamma = (double) (maxBuyPrice - marketClearingPrice) / (double) (maxBuyPrice - minBuyPrice);
            double delta = (double) (maxBuyPrice - priceCurrent) / (double) (maxBuyPrice - minBuyPrice);
*/

            double err = marketClearingPrice - priceCurrent;

            double sumErr = 0;
            for (Double x : this.errHistory) {
                sumErr += x;
            }

            double diffErr = err - errLastRound;

            this.errHistory.add(err);
            this.errLastRound = err;
            priceCurrent += shortTerm * err + middleTerm * sumErr + longTerm * diffErr;
            this.price = new BigInteger(String.valueOf(Math.round(priceCurrent)));
        }
    }

    public BigInteger getQuantity() {
        return quantity;
    }

    public BigInteger getPrice() {
        return price;
    }
}
