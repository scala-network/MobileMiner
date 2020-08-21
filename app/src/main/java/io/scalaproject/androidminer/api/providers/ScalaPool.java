// Copyright (c) 2019, Mine2Gether.com
//
// Please see the included LICENSE file for more information.
//
// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer.api.providers;

import android.util.Log;
import android.widget.TextView;
import android.view.View;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.ocpsoft.prettytime.PrettyTime;

import java.text.DecimalFormat;
import java.util.Date;

import io.scalaproject.androidminer.R;
import io.scalaproject.androidminer.Utils;
import io.scalaproject.androidminer.WizardPoolActivity;
import io.scalaproject.androidminer.api.PoolItem;
import io.scalaproject.androidminer.api.ProviderAbstract;
import io.scalaproject.androidminer.api.ProviderData;
import io.scalaproject.androidminer.network.Json;
import io.scalaproject.androidminer.widgets.PoolBannerWidget;

import static io.scalaproject.androidminer.Tools.getReadableHashRateString;
import static io.scalaproject.androidminer.Tools.parseCurrency;
import static io.scalaproject.androidminer.Tools.tryParseLong;

public class ScalaPool extends ProviderAbstract {

    public ScalaPool(PoolItem pi){
        super(pi);
    }

    public StringRequest getStringRequest(WizardPoolActivity activity, PoolBannerWidget view) {
        String url = mPoolItem.getApiUrl() + "/stats";
        return new StringRequest(Request.Method.GET, url,
                response -> {
                    try {
                        Log.i(LOG_TAG, response);
                        JSONObject obj = new JSONObject(response);
                        JSONObject objConfig = obj.getJSONObject("config");
                        JSONObject objConfigPool = obj.getJSONObject("pool");

                        float fHr = Utils.convertStringToFloat(objConfigPool.getString("hashrate")) / 1000.0f;
                        Log.d(LOG_TAG, objConfigPool.toString());
                        view.isRecommendPool(false)
                                .setPoolName("Official Pool")
                                .setMinerScala(String.format("%s %s", objConfigPool.getString("miners"), activity.getResources().getString(R.string.miners)))
                                .setHrScala(String.format("%s kH/s", new DecimalFormat("##.#").format(fHr)));

                    } catch (Exception e) {
                        //Do nothing
                    }
                }
                , WizardPoolActivity::parseVolleyError);
    }
    @Override
    protected void onBackgroundFetchData() {
        PrettyTime pTime = new PrettyTime();
        ProviderData mBlockData = getBlockData();
        mBlockData.isNew = false;

        try {
            String url = mPoolItem.getApiUrl() + "/stats";
            String dataStatsNetwork  = Json.fetch(url);
            Log.i(LOG_TAG, dataStatsNetwork);

            JSONObject joStats = new JSONObject(dataStatsNetwork);
            JSONObject joStatsConfig = joStats.getJSONObject("config");
            //JSONObject joStatsLastBlock = joStats.getJSONObject("lastblock");
            JSONObject joStatsNetwork = joStats.getJSONObject("network");
            JSONObject joStatsPool = joStats.getJSONObject("pool");
            JSONObject joStatsPoolStats = joStatsPool.getJSONObject("stats");

            mBlockData.coin.name = joStatsConfig.optString("coin").toUpperCase();
            mBlockData.coin.units = tryParseLong(joStatsConfig.optString("coinUnits"), 1L);
            mBlockData.coin.symbol = joStatsConfig.optString("symbol").toUpperCase();
            mBlockData.coin.denominationUnit = tryParseLong(joStatsConfig.optString("denominationUnit"), 1L);

            //mBlockData.pool.lastBlockHeight = joStatsPoolStats.optString("lastblock_height");
            mBlockData.pool.difficulty = getReadableHashRateString(joStatsPoolStats.optLong("totalDiff"));
            mBlockData.pool.lastBlockTime = pTime.format(new Date(joStatsPoolStats.optLong("lastBlockFound")));
            //mBlockData.pool.lastRewardAmount = parseCurrency(joStatsPoolStats.optString("lastblock_reward", "0"), mBlockData.coin.units, mBlockData.coin.denominationUnit, mBlockData.coin.symbol);
            mBlockData.pool.hashrate = String.valueOf(tryParseLong(joStatsPool.optString("hashrate"),0L) / 1000L);
            mBlockData.pool.blocks = joStatsPool.optString("roundHashes", "0");
            mBlockData.pool.minPayout = parseCurrency(joStatsConfig.optString("minPaymentThreshold", "0"), mBlockData.coin.units, mBlockData.coin.denominationUnit, mBlockData.coin.symbol);

            mBlockData.network.lastBlockHeight = joStatsNetwork.optString("height");
            mBlockData.network.difficulty = getReadableHashRateString(joStatsNetwork.optLong("difficulty"));
            mBlockData.network.lastBlockTime = pTime.format(new Date(joStatsNetwork.optLong("timestamp") * 1000));
            mBlockData.network.lastRewardAmount = parseCurrency(joStatsNetwork.optString("reward", "0"), mBlockData.coin.units, mBlockData.coin.denominationUnit, mBlockData.coin.symbol);
        } catch (JSONException e) {
            Log.i(LOG_TAG, "NETWORK\n" + e.toString());
            e.printStackTrace();
        }

        String wallet = getWalletAddress();
        Log.i(LOG_TAG, "Wallet: " + wallet);
        if(wallet.equals("")) {
            return;
        }
        try {
            String url = mPoolItem.getApiUrl() + "/stats_address?address=" + getWalletAddress();

            String dataWallet  = Json.fetch(url);

            JSONObject joStatsAddress = new JSONObject(dataWallet);
            JSONObject joStatsAddressStats = joStatsAddress.getJSONObject("stats");

            ProviderData.Coin coin = mBlockData.coin;
            String hashRate = joStatsAddressStats.optString("hashrate", "0");
            String balance = parseCurrency(joStatsAddressStats.optString("balance", "0"), coin.units, coin.denominationUnit, coin.symbol);
            String paid = parseCurrency(joStatsAddressStats.optString("paid", "0"), coin.units, coin.denominationUnit, coin.symbol);
            String lastShare = pTime.format(new Date(joStatsAddressStats.optLong("lastShare") * 1000));
            String blocks = String.valueOf(tryParseLong(joStatsAddressStats.optString("blocks"), 0L));

            Log.i(LOG_TAG, "hashRate: " + hashRate);

            mBlockData.miner.hashrate = hashRate;
            mBlockData.miner.balance = balance;
            mBlockData.miner.paid = paid;
            mBlockData.miner.lastShare = lastShare;
            mBlockData.miner.blocks = blocks;
        } catch (JSONException e) {
            Log.i(LOG_TAG, "ADDRESS\n" + e.toString());
            e.printStackTrace();
        }
    }
}
