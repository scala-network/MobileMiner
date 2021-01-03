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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.Provider;
import java.text.DecimalFormat;

import io.scalaproject.androidminer.api.PoolItem;
import io.scalaproject.androidminer.api.ProviderManager;
import io.scalaproject.androidminer.widgets.PoolBannerWidget;

public class WizardPoolActivity extends BaseActivity {
    private static final String LOG_TAG = "WizardPoolActivity";

    private int selectedPoolIndex = 0;

    private Context mContext = null;

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
        View view = findViewById(android.R.id.content).getRootView();

        PoolItem[] pools = ProviderManager.getPools();

        PoolBannerWidget[] lls = new PoolBannerWidget[pools.length];

        LinearLayout parentLayout = view.findViewById(R.id.buttonContainer);

        selectedPoolIndex = ProviderManager.getSelectedPoolIndex();

        for(int i = 0; i < pools.length; i++) {
            PoolItem poolItem = pools[i];

            LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            mContext = this;
            PoolBannerWidget poolBannerWidget =  new PoolBannerWidget(this, poolItem, i == selectedPoolIndex);

            lls[i] = poolBannerWidget;

            parentLayout.addView(poolBannerWidget);

            poolBannerWidget.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    for(int i = 0; i < lls.length; i++) {
                        View ll = lls[i];

                        if(ll == null) {
                            continue;
                        }

                        PoolBannerWidget pwv = (PoolBannerWidget)ll;

                        int bottom = ll.getPaddingBottom();
                        int top = ll.getPaddingTop();
                        int right = ll.getPaddingRight();
                        int left = ll.getPaddingLeft();

                        pwv.setVisibility(View.GONE);

                        if(view == ll) {
                            // inflate view with new layout
                            pwv.setSelected(mContext, true);
                            //ll.setBackgroundResource(R.drawable.corner_radius_lighter_border_blue);
                            selectedPoolIndex = i;
                        } else {
                            // inflate view with new layout
                            pwv.setSelected(mContext, false);
                            //ll.setBackgroundResource(R.drawable.corner_radius_lighter);
                        }
                        pwv.setVisibility(View.VISIBLE);
                        //ll.setPadding(left, top, right, bottom);
                    }

                    parentLayout.invalidate();

                    Log.i("MININGPOOL", "SelectedPoolIndex : " + selectedPoolIndex);
                }
            });

            if(poolItem.getApiUrl() != null) {
                StringRequest stringRequest = poolItem.getInterface().getStringRequest(this, poolBannerWidget);
                queue.add(stringRequest);
            }
        }
    }

    public void onNext(View view) {
        Config.write("selected_pool", Integer.toString(selectedPoolIndex));

        startActivity(new Intent(WizardPoolActivity.this, WizardSettingsActivity.class));
        finish();
    }

    static public void parseVolleyError(VolleyError error) {
        String message = "";
        try {
            String responseBody = new String(error.networkResponse.data, StandardCharsets.UTF_8);
            JSONObject data = new JSONObject(responseBody);
            JSONArray errors = data.getJSONArray("errors");
            JSONObject jsonMessage = errors.getJSONObject(0);

            message = "VolleyError: " + jsonMessage.getString("message");
        } catch (JSONException e) {
            message = "JSONException: " + e.getMessage();
        } catch (Exception e) {
            message = "Exception: " + e.getMessage();
        } finally {
            Log.i("parseVolleyError:", message);
        }
    }
}