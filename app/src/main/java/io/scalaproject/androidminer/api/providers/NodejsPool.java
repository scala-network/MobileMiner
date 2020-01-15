package io.scalaproject.androidminer.api.providers;

import io.scalaproject.androidminer.api.ProviderAbstract;
import io.scalaproject.androidminer.api.PoolItem;

public final class NodejsPool extends ProviderAbstract {

    public NodejsPool(PoolItem poolItem){
        super(poolItem);
    }

    @@Override
    void onBackgroundFetchData() {

    }

}
