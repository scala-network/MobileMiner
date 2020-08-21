// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;
import java.text.DecimalFormat;

import io.scalaproject.androidminer.api.PoolItem;
import io.scalaproject.androidminer.api.ProviderManager;
import io.scalaproject.androidminer.widgets.PoolBannerWidget;

public class WizardPoolActivity extends BaseActivity {
    private static final String LOG_TAG = "WizardPoolActivity";

    private int selectedPoolIndex = 1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            // Activity was brought to front and not created,
            // Thus finishing this will get us to the last viewed activity
            finish();
            return;
        }

        setContentView(R.layout.fragment_wizard_pool);

        RequestQueue queue = Volley.newRequestQueue(this);

        // Scala
        PoolItem[] pools = ProviderManager.getPools();
        View[] lls = new View[pools.length];
        LinearLayout parentLayout = (LinearLayout)findViewById(R.id.buttonContainer);

        for(int i=0;i<pools.length;i++) {
            PoolItem pool = pools[i];
            if(pool.getApiUrl() == null) {
                continue;
            }
            LayoutInflater vi = (LayoutInflater) getApplicationContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            final PoolBannerWidget poolBannerWidget =  new PoolBannerWidget(this);

            lls[i] = poolBannerWidget;
            parentLayout.addView(poolBannerWidget);
            poolBannerWidget.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    int bottom = poolBannerWidget.getPaddingBottom();
                    int top = poolBannerWidget.getPaddingTop();
                    int right = poolBannerWidget.getPaddingRight();
                    int left = poolBannerWidget.getPaddingLeft();
                    poolBannerWidget.setBackgroundResource(R.drawable.corner_radius_lighter);
                    poolBannerWidget.setPadding(left, top, right, bottom);
                    for(int o = 0;o< lls.length;o++) {
                        View ll = lls[o];
                        if(ll == null) {
                            continue;
                        }
                        if(poolBannerWidget != ll) {
                            bottom = ll.getPaddingBottom();
                            top = ll.getPaddingTop();
                            right = ll.getPaddingRight();
                            left = ll.getPaddingLeft();
                            ll.setBackgroundResource(R.drawable.corner_radius_lighter_border_blue);
                             ll.setPadding(left, top, right, bottom);
                        } else {
                            selectedPoolIndex = o+1;
                        }
                    }
                }
            });

            StringRequest stringRequest = pool.getInterface().getStringRequest(this, poolBannerWidget);
            queue.add(stringRequest);
        }

    }

    static public void parseVolleyError(VolleyError error) {
        // Do nothing
    }


    public void onNext(View view) {
        Config.write("selected_pool", Integer.toString(selectedPoolIndex));

        startActivity(new Intent(WizardPoolActivity.this, WizardSettingsActivity.class));
        finish();
    }
}