package io.scalaproject.androidminer.api.providers;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;

import io.scalaproject.androidminer.api.Data;
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

        if(getBlockData().getCoin().name == null) {
            try{
                String aurl = mPoolItem.getApiUrl() + "/config";
                String aConfig  = Json.fetch(aurl);

                JSONObject joStatss =  new JSONObject(aConfig);

                mBlockData.getCoin().name = joStatss.optString("coin_code").toUpperCase();
                mBlockData.getCoin().units = tryParseLong(joStatss.optString("coin_code"), 1L);
                String symbol = mBlockData.getCoin().symbol = joStatss.optString("symbol");
                long denominationUnit = mBlockData.getCoin().denominationUnit = 100L;

                mBlockData.getPool().minPayout =  parseCurrency(joStatss.optString("min_wallet_payout", "0"),denominationUnit, denominationUnit, symbol);

            } catch (Exception e) {

            }
        }
        PrettyTime pTime = new PrettyTime();
//
//
//        long denominationUnit = mBlockData.getCoin().denominationUnit = tryParseLong(joStatsConfig.optString("denominationUnit"), 1L);
        try {
            String url = mPoolItem.getApiUrl() + "/network/stats";
            String dataStatsNetwork  = Json.fetch(url);
//            Log.i(LOG_TAG, dataStatsNetwork);

            JSONObject joStats = new JSONObject(dataStatsNetwork);

            mBlockData.getNetwork().lastBlockHeight = joStats.optString("height");
            mBlockData.getNetwork().difficulty = joStats.optString("difficulty");
            mBlockData.getNetwork().lastBlockTime = pTime.format(new Date(joStats.optLong("ts") * 1000));
            mBlockData.getNetwork().lastRewardAmount =  parseCurrency(joStats.optString("value", "0"), 100, 100, "XLA");
        }
        catch (JSONException e) {
            Log.i(LOG_TAG, "NETWORK\n"+e.toString());
            e.printStackTrace();
        }

        String wallet = getWalletAddress();
        if(wallet.equals("")) {
            return;
        }

        String surl = mPoolItem.getApiUrl() + "/miner/" + getWalletAddress() +"/stats";
        try {
            String dataStatsNetwork  = Json.fetch(surl);
            Log.i(LOG_TAG, dataStatsNetwork);

            JSONObject joStatsAddress = new JSONObject(dataStatsNetwork);

            Data.Coin coin = mBlockData.getCoin();
            String hashRate = getReadableHashRateString(joStatsAddress.optLong("hash"));
            String balance = parseCurrency(joStatsAddress.optString("amtDue", "0"), 100, 100, "XLA");
            String paid = parseCurrency(joStatsAddress.optString("amtPaid", "0"), 100, 100, "XLA");
            String lastShare = pTime.format(new Date(joStatsAddress.optLong("lastHash") * 1000));
            String blocks = String.valueOf(tryParseLong(joStatsAddress.optString("validShares"), 0L));

            mBlockData.getMiner().hashrate = hashRate;
            mBlockData.getMiner().balance = balance;
            mBlockData.getMiner().paid = paid;
            mBlockData.getMiner().lastShare = lastShare;
            mBlockData.getMiner().blocks = blocks;
        }
        catch (JSONException e) {
            Log.i(LOG_TAG, "ADDRESS :" +surl+ "\n"+e.toString());
            e.printStackTrace();
        }
    }
}
