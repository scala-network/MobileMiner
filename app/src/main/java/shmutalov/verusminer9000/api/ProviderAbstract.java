// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package shmutalov.verusminer9000.api;

import android.os.AsyncTask;

import shmutalov.verusminer9000.Config;

public abstract class ProviderAbstract extends AsyncTask<Void, Void, Void> {

    protected final String LOG_TAG = "MiningSvc";

    final public ProviderData getBlockData() {
        return ProviderManager.data;
    }

    public IProviderListener mListener;

    protected final PoolItem mPoolItem;

    public ProviderAbstract(PoolItem poolItem) {
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
