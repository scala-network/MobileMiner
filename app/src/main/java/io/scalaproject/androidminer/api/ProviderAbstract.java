package io.scalaproject.androidminer.api;

import android.os.AsyncTask;
import android.util.Log;


import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import io.scalaproject.androidminer.Config;

public abstract class ProviderAbstract extends AsyncTask<Void, Void, Void> {

    protected String LOG_TAG = "MiningSvc";
    protected Timer timer;

    final public ProviderData getBlockData() {
        return ProviderManager.data;
    }

    public IProviderListener mListener;

    protected PoolItem mPoolItem;

    public ProviderAbstract(PoolItem poolItem){
        mPoolItem = poolItem;
    }

    final public String getWalletAddress(){
        return Config.read("address");
    }


    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        if(mListener == null) {
            return;
        }
        if(!mListener.onEnabledRequest()) {
            return;
        }
        mListener.onStatsChange(getBlockData());
    }

    abstract protected void onBackgroundFetchData();

    @Override
    protected Void doInBackground(Void... voids) {
        try {
            onBackgroundFetchData();
        } catch (Exception e) {

        }

        getBlockData().pool.type = mPoolItem.getPoolType();
        return null;
    }

}
