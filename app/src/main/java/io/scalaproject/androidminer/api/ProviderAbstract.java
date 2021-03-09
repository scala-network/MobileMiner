// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer.api;

import android.os.AsyncTask;

import com.android.volley.toolbox.StringRequest;

import io.scalaproject.androidminer.Config;
import io.scalaproject.androidminer.widgets.PoolInfoAdapter;

public abstract class ProviderAbstract extends AsyncTask<Void, Void, Void> {

    protected final String LOG_TAG = "MiningSvc";

    final public ProviderData getBlockData() {
        return ProviderManager.data;
    }

    public IProviderListener mListener;

    protected final PoolItem mPoolItem;

    public ProviderAbstract(PoolItem poolItem){
        mPoolItem = poolItem;
    }

    final public String getWalletAddress(){
        return Config.read(Config.CONFIG_ADDRESS);
    }

    abstract public StringRequest getStringRequest(PoolInfoAdapter poolsAdapter);

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
            e.printStackTrace();
        }

        getBlockData().pool.type = mPoolItem.getPoolType();
        return null;
    }
}
