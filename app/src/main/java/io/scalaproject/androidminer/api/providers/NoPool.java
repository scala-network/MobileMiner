// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer.api.providers;

import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.toolbox.StringRequest;

import org.json.JSONObject;

import java.text.DecimalFormat;

import io.scalaproject.androidminer.R;
import io.scalaproject.androidminer.Utils;
import io.scalaproject.androidminer.WizardPoolActivity;
import io.scalaproject.androidminer.api.ProviderAbstract;
import io.scalaproject.androidminer.api.PoolItem;
import io.scalaproject.androidminer.widgets.PoolBannerWidget;

public final class NoPool extends ProviderAbstract {

    public NoPool(PoolItem poolItem){
        super(poolItem);
    }
    public StringRequest getStringRequest(WizardPoolActivity activity, PoolBannerWidget view) {
        return new StringRequest(Request.Method.GET, mPoolItem.getStatsURL(),
                response -> {

                }
                , WizardPoolActivity::parseVolleyError);
    }
    @Override
    protected void onBackgroundFetchData() {}
}
