// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer.api.providers;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;

import io.scalaproject.androidminer.PoolActivity;
import io.scalaproject.androidminer.api.ProviderAbstract;
import io.scalaproject.androidminer.api.PoolItem;
import io.scalaproject.androidminer.widgets.PoolInfoAdapter;

public final class NoPool extends ProviderAbstract {

    public NoPool(PoolItem poolItem){
        super(poolItem);
    }

    public StringRequest getStringRequest(PoolInfoAdapter poolsAdapter) {
        return new StringRequest(Request.Method.GET, mPoolItem.getStatsURL(),
                response -> {
                    mPoolItem.setIsValid(false);

                    poolsAdapter.dataSetChanged();
                }
                , PoolActivity::parseVolleyError);
    }
    @Override
    protected void onBackgroundFetchData() {}
}
