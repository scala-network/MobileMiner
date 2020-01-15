package io.scalaproject.androidminer.api.providers;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;

import io.scalaproject.androidminer.network.Json;
import io.scalaproject.androidminer.api.Data;
import io.scalaproject.androidminer.api.ProviderAbstract;
import io.scalaproject.androidminer.api.PoolItem;

import static io.scalaproject.androidminer.Tools.getReadableHashRateString;
import static io.scalaproject.androidminer.Tools.parseCurrency;
import static io.scalaproject.androidminer.Tools.tryParseLong;

public class CryptonoteNodejsPool extends ProviderAbstract {

    public CryptonoteNodejsPool(PoolItem pi){
        super(pi);
    }

    @@Override
    void onBackgroundFetchData() {
        PrettyTime pTime = new PrettyTime();

        try {
            String url = mPoolItem.getApiUrl() + "/stats";
            String dataStatsNetwork  = Json.fetch(url);
            Log.i(LOG_TAG, dataStatsNetwork);

            JSONObject joStats = new JSONObject(dataStatsNetwork);
            JSONObject joStatsConfig = joStats.getJSONObject("config");
            JSONObject joStatsNetwork = joStats.getJSONObject("network");

            mBlockData.getCoin().name = joStatsConfig.optString("coin").toUpperCase();
            long coinUnits = mBlockData.getCoin().units = tryParseLong(joStatsConfig.optString("coinUnits"), 1L);
            String symbol = mBlockData.getCoin().symbol = joStatsConfig.optString("symbol");
            long denominationUnit = mBlockData.getCoin().denominationUnit = tryParseLong(joStatsConfig.optString("denominationUnit"), 1L);

            mBlockData.getNetwork().lastBlockHeight = joStatsNetwork.optString("height");
            mBlockData.getNetwork().difficulty = getReadableHashRateString(joStatsNetwork.optLong("difficulty"));
            mBlockData.getNetwork().lastBlockTime = pTime.format(new Date(joStatsNetwork.optLong("timestamp") * 1000));
            mBlockData.getNetwork().lastRewardAmount = parseCurrency(joStatsNetwork.optString("reward", "0"), coinUnits, denominationUnit, symbol);


        } catch (JSONException e) {
            Log.i(LOG_TAG, e.toString());
            e.printStackTrace();
        }

        try {
            String url = mPoolItem.getApiUrl() + "/stats_address?address=" + getWalletAddress();

            JSONObject joStatsAddress = new JSONObject(url);
            JSONObject joStatsAddressStats = joStatsAddress.getJSONObject("stats");

            Data.Coin coin = mBlockData.getCoin();
            String hashRate = joStatsAddressStats.optString("hashrate", "0 H") + "/s";
            String balance = parseCurrency(joStatsAddressStats.optString("balance", "0"), coin.units, coin.denominationUnit, coin.symbol);
            String paid = parseCurrency(joStatsAddressStats.optString("paid", "0"), coin.units, coin.denominationUnit, coin.symbol);
            String lastShare = pTime.format(new Date(joStatsAddressStats.optLong("lastShare") * 1000));
            String blocks = String.valueOf(tryParseLong(joStatsAddressStats.optString("blocks"), 0L));

            mBlockData.getPool().hashrate = hashRate;
            mBlockData.getMiner().balance = balance;
            mBlockData.getMiner().paid = paid;
            mBlockData.getMiner().lastShare = lastShare;
            mBlockData.getMiner().blocks = blocks;
        } catch (JSONException e) {
            Log.i(LOG_TAG, e.toString());
            e.printStackTrace();
        }
    }
}
