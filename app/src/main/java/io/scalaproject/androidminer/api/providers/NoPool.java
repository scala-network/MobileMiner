package io.scalaproject.androidminer.api.providers;

import io.scalaproject.androidminer.api.ProviderAbstract;
import io.scalaproject.androidminer.api.PoolItem;

public final class NoPool extends ProviderAbstract {

    public NoPool(PoolItem poolItem){
        super(poolItem);
    }
    @Override
    protected void onBackgroundFetchData() {

    }
}
