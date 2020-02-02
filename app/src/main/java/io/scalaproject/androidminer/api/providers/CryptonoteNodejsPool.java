package io.scalaproject.androidminer.api.providers;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;

import io.scalaproject.androidminer.api.ProviderData;
import io.scalaproject.androidminer.network.Json;
import io.scalaproject.androidminer.api.ProviderAbstract;
import io.scalaproject.androidminer.api.PoolItem;

import static io.scalaproject.androidminer.Tools.getReadableHashRateString;
import static io.scalaproject.androidminer.Tools.parseCurrency;
import static io.scalaproject.androidminer.Tools.tryParseLong;

public class CryptonoteNodejsPool extends ProviderAbstract {

    public CryptonoteNodejsPool(PoolItem pi){
        super(pi);
    }

    @Override
    protected void onBackgroundFetchData() {
        PrettyTime pTime = new PrettyTime();
        ProviderData mBlockData = getBlockData();
        try {
            String url = mPoolItem.getApiUrl() + "/stats";
            String dataStatsNetwork  = Json.fetch(url);
            Log.i(LOG_TAG, dataStatsNetwork);

            JSONObject joStats = new JSONObject(dataStatsNetwork);
            JSONObject joStatsConfig = joStats.getJSONObject("config");
            JSONObject joStatsNetwork = joStats.getJSONObject("network");

            mBlockData.coin.name = joStatsConfig.optString("coin").toUpperCase();
            long coinUnits = mBlockData.coin.units = tryParseLong(joStatsConfig.optString("coinUnits"), 1L);
            String symbol = mBlockData.coin.symbol = joStatsConfig.optString("symbol");
            long denominationUnit = mBlockData.coin.denominationUnit = tryParseLong(joStatsConfig.optString("denominationUnit"), 1L);
            mBlockData.pool.minPayout=  parseCurrency(joStats.optString("value", "0"), coinUnits, denominationUnit, symbol);


            mBlockData.network.lastBlockHeight = joStatsNetwork.optString("height");
            mBlockData.network.difficulty = getReadableHashRateString(joStatsNetwork.optLong("difficulty"));
            mBlockData.network.lastBlockTime = pTime.format(new Date(joStatsNetwork.optLong("timestamp") * 1000));
            mBlockData.network.lastRewardAmount = parseCurrency(joStatsNetwork.optString("reward", "0"), coinUnits, denominationUnit, symbol);
        } catch (JSONException e) {
            Log.i(LOG_TAG, "NETWORK\n"+e.toString());
            e.printStackTrace();
        }

        String wallet = getWalletAddress();
        if(wallet.equals("")) {
            return;
        }
        try {
            String url = mPoolItem.getApiUrl() + "/stats_address?address=" + getWalletAddress();

            JSONObject joStatsAddress = new JSONObject(url);
            JSONObject joStatsAddressStats = joStatsAddress.getJSONObject("stats");

            ProviderData.Coin coin = mBlockData.coin;
            String hashRate = joStatsAddressStats.optString("hashrate", "0 H") + "/s";
            String balance = parseCurrency(joStatsAddressStats.optString("balance", "0"), coin.units, coin.denominationUnit, coin.symbol);
            String paid = parseCurrency(joStatsAddressStats.optString("paid", "0"), coin.units, coin.denominationUnit, coin.symbol);
            String lastShare = pTime.format(new Date(joStatsAddressStats.optLong("lastShare") * 1000));
            String blocks = String.valueOf(tryParseLong(joStatsAddressStats.optString("blocks"), 0L));

            mBlockData.pool.hashrate = hashRate;
            mBlockData.miner.balance = balance;
            mBlockData.miner.paid = paid;
            mBlockData.miner.lastShare = lastShare;
            mBlockData.miner.blocks = blocks;
        } catch (JSONException e) {
            Log.i(LOG_TAG, "ADDRESS\n"+e.toString());
            e.printStackTrace();
        }
    }
}
