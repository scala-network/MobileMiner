package io.scalaproject.androidminer.api;

import android.os.AsyncTask;


import io.scalaproject.androidminer.Config;

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

        } catch (Exception e) {

        }



        return null;
    }

}
