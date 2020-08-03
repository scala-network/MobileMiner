// Copyright (c) 2019, Mine2Gether.com
//
// Please see the included LICENSE file for more information.
//
// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer.api.providers;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;

import io.scalaproject.androidminer.api.ProviderData;
import io.scalaproject.androidminer.api.ProviderAbstract;
import io.scalaproject.androidminer.api.PoolItem;
import io.scalaproject.androidminer.network.Json;

import static io.scalaproject.androidminer.Tools.getReadableHashRateString;
import static io.scalaproject.androidminer.Tools.parseCurrency;
import static io.scalaproject.androidminer.Tools.parseCurrencyFloat;
import static io.scalaproject.androidminer.Tools.tryParseLong;

public final class NodejsPool extends ProviderAbstract {

    public NodejsPool(PoolItem poolItem){
        super(poolItem);
    }

    @Override
    protected void onBackgroundFetchData() {
        ProviderData mBlockData = getBlockData();
        String url = "";

        long denominationUnit = 100L;

        // Config
        if(mBlockData.isNew) {
            mBlockData.isNew = false;
            try {
                url = mPoolItem.getApiUrl() + "/config";
                String aConfig  = Json.fetch(url);
                JSONObject joConfig =  new JSONObject(aConfig);

                mBlockData.coin.units = tryParseLong(joConfig.optString("coin_code"), 1L);
                mBlockData.coin.name = joConfig.optString("coin_code").toUpperCase();

                String symbol = mBlockData.coin.symbol = joConfig.optString("coin_code");
                mBlockData.coin.denominationUnit = denominationUnit;

                mBlockData.pool.minPayout =  parseCurrency(joConfig.optString("min_wallet_payout", "0"), denominationUnit, denominationUnit, symbol);
            } catch (Exception e) {
                Log.i(LOG_TAG, "COIN\n" + e.toString());
                e.printStackTrace();
            }
        }

        PrettyTime pTime = new PrettyTime();

        // Network
        try {
            url = mPoolItem.getApiUrl() + "/network/stats";
            String dataStatsNetwork  = Json.fetch(url);
            JSONObject joNetworkStats = new JSONObject(dataStatsNetwork);

            mBlockData.network.lastBlockHeight = joNetworkStats.optString("height");
            mBlockData.network.difficulty = joNetworkStats.optString("difficulty");
            mBlockData.network.lastBlockTime = pTime.format(new Date(joNetworkStats.optLong("ts") * 1000));
            mBlockData.network.lastRewardAmount =  parseCurrency(joNetworkStats.optString("value", "0"), denominationUnit, denominationUnit, "XLA");
        } catch (JSONException e) {
            Log.i(LOG_TAG, "NETWORK\n" + e.toString());
            e.printStackTrace();
        }

        // Pool
        try {
            url = mPoolItem.getApiUrl() + "/pool/stats";
            String dataStatsPool  = Json.fetch(url);
            JSONObject joPoolStats = new JSONObject(dataStatsPool).getJSONObject("pool_statistics");

            mBlockData.pool.lastBlockHeight = joPoolStats.optString("lastBlockFound");
            //mBlockData.pool.difficulty = getReadableHashRateString(joPoolStats.optLong("totalDiff"));
            mBlockData.pool.lastBlockTime = pTime.format(new Date(joPoolStats.optLong("lastBlockFoundTime") * 1000));
            //mBlockData.pool.lastRewardAmount = parseCurrency(joPoolStats.optString("reward", "0"), mBlockData.coin.units, denominationUnit, mBlockData.coin.symbol);
            mBlockData.pool.hashrate = String.valueOf(tryParseLong(joPoolStats.optString("hashRate"),0L) / 1000L);
            mBlockData.pool.blocks = joPoolStats.optString("totalBlocksFound", "0");
        } catch (JSONException e) {
            Log.i(LOG_TAG, "POOL\n" + e.toString());
            e.printStackTrace();
        }

        // Address
        String wallet = getWalletAddress();
        if(wallet.equals("")) {
            return;
        }

        try {
            url = mPoolItem.getApiUrl() + "/miner/" + getWalletAddress() + "/stats";
            String symbol = mBlockData.coin.symbol;
            String dataStatsAddress  = Json.fetch(url);
            JSONObject joStatsAddress = new JSONObject(dataStatsAddress);

            String hashRate = getReadableHashRateString(joStatsAddress.optLong("hash"));
            String balance = parseCurrency(joStatsAddress.optString("amtDue", "0"), denominationUnit, denominationUnit, symbol);
            String paid = parseCurrency(joStatsAddress.optString("amtPaid", "0"), denominationUnit, denominationUnit, symbol);
            String lastShare = pTime.format(new Date(joStatsAddress.optLong("lastHash") * 1000));
            String blocks = String.valueOf(tryParseLong(joStatsAddress.optString("validShares"), 0L));

            mBlockData.miner.hashrate = hashRate;
            mBlockData.miner.balance = balance;
            mBlockData.miner.paid = paid;
            mBlockData.miner.lastShare = lastShare;
            mBlockData.miner.blocks = blocks;

            // Payments
            url = mPoolItem.getApiUrl() + "/miner/" + getWalletAddress() +"/payments?page=0&limit=100";
            String dataMinerPayments  = Json.fetch(url);
            JSONArray  joMinerPayments = new JSONArray(dataMinerPayments);

            final int n = joMinerPayments.length();
            for (int i = 0; i < n; ++i) {
                ProviderData.Payment payment = new ProviderData.Payment();
                payment.amount = parseCurrencyFloat(joMinerPayments.getJSONObject(i).optString("amount", "0"), denominationUnit, denominationUnit);
                payment.timestamp = new Date(joMinerPayments.getJSONObject(i).optLong("ts") * 1000);
                mBlockData.miner.payments.add(payment);
            }

        } catch (JSONException e) {
            Log.i(LOG_TAG, "ADDRESS :" + url + "\n"+e.toString());
            e.printStackTrace();
        }
    }
}