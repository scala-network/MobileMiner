package io.scalaproject.androidminer.api.providers;

import android.util.Log;

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
import static io.scalaproject.androidminer.Tools.tryParseLong;

public final class NodejsPool extends ProviderAbstract {

    public NodejsPool(PoolItem poolItem){
        super(poolItem);
    }

    @Override
    protected void onBackgroundFetchData() {
        ProviderData mBlockData = getBlockData();

        if(mBlockData.isNew) {
            mBlockData.isNew = false;
            try{
                String aurl = mPoolItem.getApiUrl() + "/config";
                String aConfig  = Json.fetch(aurl);

                JSONObject joStatss =  new JSONObject(aConfig);

            mBlockData.coin.units = tryParseLong(joStatss.optString("coin_code"), 1L);
            mBlockData.coin.name = joStatss.optString("coin_code").toUpperCase();
            String symbol = mBlockData.coin.symbol = joStatss.optString("symbol");
                long denominationUnit = mBlockData.coin.denominationUnit = 100L;
                mBlockData.pool.minPayout =  parseCurrency(joStatss.optString("min_wallet_payout", "0"),denominationUnit, denominationUnit, symbol);
            } catch (Exception e) {

            }
        }
        PrettyTime pTime = new PrettyTime();

        long denominationUnit = mBlockData.coin.denominationUnit;
        String url = mPoolItem.getApiUrl() + "/network/stats";

        try {
            String dataStatsNetwork  = Json.fetch(url);

            JSONObject joStats = new JSONObject(dataStatsNetwork);

            mBlockData.network.lastBlockHeight = joStats.optString("height");
            mBlockData.network.difficulty = joStats.optString("difficulty");
            mBlockData.network.lastBlockTime = pTime.format(new Date(joStats.optLong("ts") * 1000));
            mBlockData.network.lastRewardAmount =  parseCurrency(joStats.optString("value", "0"), denominationUnit, denominationUnit, "XLA");
        } catch (JSONException e) {
            Log.i(LOG_TAG, "NETWORK\n"+e.toString());
            e.printStackTrace();
        }

        String wallet = getWalletAddress();
        if(wallet.equals("")) {
            return;
        }

        String surl = mPoolItem.getApiUrl() + "/miner/" + getWalletAddress() +"/stats";
        try {
            String symbol = mBlockData.coin.symbol;
            String dataStatsNetwork  = Json.fetch(surl);
            Log.i(LOG_TAG, dataStatsNetwork);

            JSONObject joStatsAddress = new JSONObject(dataStatsNetwork);

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
        } catch (JSONException e) {
            Log.i(LOG_TAG, "ADDRESS :" +surl+ "\n"+e.toString());
            e.printStackTrace();
        }
    }
}
