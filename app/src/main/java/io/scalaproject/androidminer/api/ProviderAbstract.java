package io.scalaproject.androidminer.api;

import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;

import io.scalaproject.androidminer.Config;
import scala.androidminer.StatsFragment;
import scala.androidminer.Tools;
import scala.androidminer.network.Json;
import io.scalaproject.androidminer.api.PoolItem;
import io.scalaproject.androidminer.api.PoolManager;

import static scala.androidminer.Tools.getReadableHashRateString;
import static scala.androidminer.Tools.parseCurrency;
import static scala.androidminer.Tools.tryParseLong;

public abstract class ProviderAbstract extends AsyncTask<Void, Void, Void> {

    protected String LOG_TAG = "MiningSvc";

    protected Data mBlockData = new Data();

    final public Data getBlockData() {
        return mBlockData;
    }

    private ProviderListenerInterface listener = null;

    protected PoolItem mPoolItem;

    public ProviderAbstract(PoolItem poolItem){
        mPoolItem = poolItem;
    }


    final public String getWalletAddress(){
        return Config.read("address");
    }


    final public void setStatsChangeListener(ProviderListenerInterface listener) {
        if (this.listener != null) {
            this.listener = null;
        }
        this.listener = listener;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        if (listener != null){
            listener.onStatsChange(mBlockData);
        }
    }


    abstract protected void onBackgroundFetchData();


    @Override
    protected Void doInBackground(Void... voids) {


        try {
            onBackgroundFetchData();

        } catch (Exception) {

        }



        return null;
    }

}
