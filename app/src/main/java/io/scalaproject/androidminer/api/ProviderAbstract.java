package io.scalaproject.androidminer.api;

import android.os.AsyncTask;


import io.scalaproject.androidminer.Config;

public abstract class ProviderAbstract extends AsyncTask<Void, Void, Void> {

    protected String LOG_TAG = "MiningSvc";

    protected Data mBlockData = new Data();

    final public Data getBlockData() {
        return mBlockData;
    }

    private ProviderListenerInterface statslistener = null;
    private ProviderListenerInterface payoutlistener = null;

    protected PoolItem mPoolItem;

    public ProviderAbstract(PoolItem poolItem){
        mPoolItem = poolItem;
    }

    final public String getWalletAddress(){
        return Config.read("address");
    }

    final public void setStatsChangeListener(ProviderListenerInterface listener) {
        if (this.statslistener != null) {
            this.statslistener = null;
        }

        this.statslistener = listener;
    }

    final public void setPayoutChangeListener(ProviderListenerInterface listener) {
        if (this.payoutlistener != null) {
            this.payoutlistener = null;
        }

        this.payoutlistener = listener;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);

        if (statslistener != null){
            statslistener.onStatsChange(mBlockData);
        }

        if (payoutlistener != null){
            payoutlistener.onStatsChange(mBlockData);
        }
    }

    abstract protected void onBackgroundFetchData();

    @Override
    protected Void doInBackground(Void... voids) {
        try {
            onBackgroundFetchData();

        } catch (Exception e) {

        }

        return null;
    }
}
