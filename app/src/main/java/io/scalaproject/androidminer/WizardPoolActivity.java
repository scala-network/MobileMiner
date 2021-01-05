// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;
import java.security.Provider;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import io.scalaproject.androidminer.api.PoolItem;
import io.scalaproject.androidminer.api.ProviderManager;
import io.scalaproject.androidminer.widgets.PoolBannerWidget;
import io.scalaproject.androidminer.widgets.PoolInfoAdapter;

public class WizardPoolActivity extends BaseActivity
        implements PoolInfoAdapter.OnSelectPoolListener{
    private static final String LOG_TAG = "WizardPoolActivity";

    private int selectedPoolIndex = 0;

    private SwipeRefreshLayout pullToRefresh;
    private RecyclerView rvPools;

    private Set<PoolItem> allPools = new HashSet<>();
    //private Set<PoolItem> userdefinedPools = new HashSet<>();

    private PoolInfoAdapter poolsAdapter;

    private View selectedPoolView = null;
    private PoolItem selectedPool = null;

    private Context mContext = null;
    RequestQueue mPoolQueue = null;

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

        mPoolQueue = Volley.newRequestQueue(this);
        View view = findViewById(android.R.id.content).getRootView();

        rvPools = view.findViewById(R.id.rvPools);
        poolsAdapter = new PoolInfoAdapter(this, this);
        rvPools.setAdapter(poolsAdapter);

        rvPools.post(() -> updateSelectedPoolLayout());

        pullToRefresh = view.findViewById(R.id.pullToRefresh);
        pullToRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });

        Utils.hideKeyboard(this);

        allPools = new HashSet<>(Arrays.asList(ProviderManager.getPools()));

        refresh();
    }

    private void updateSelectedPoolLayout() {
        // If recycler view has not been rendered yet
        if(rvPools.getLayoutManager().getItemCount() <= 0)
            return;

        String selectedPoolName = Config.read("selected_pool");

        PoolItem[] allPools = ProviderManager.getPools();

        if(selectedPoolName.isEmpty()) {
            selectedPoolName = allPools[0].getKey();
        }

        if(!selectedPoolName.isEmpty()) {
            for (int i = 0; i < allPools.length; i++ ) {
                PoolItem poolItem = allPools[i];
                Boolean bSelected = selectedPoolName.equals(poolItem.getKey());
                View itemView = rvPools.getChildAt(i);
                //View itemView = rvPools.getLayoutManager().findViewByPosition(i);
                setItemPoolLayout(itemView, bSelected);

                if(bSelected) {
                    selectedPoolView = itemView;
                    selectedPool = poolItem;
                }
            }
        }
    }

    private void setItemPoolLayout(View itemView, Boolean selected) {
        if(itemView != null) {
            RelativeLayout rlItemNode = (RelativeLayout) itemView;
            int bottom = rlItemNode.getPaddingBottom();
            int top = rlItemNode.getPaddingTop();
            int right = rlItemNode.getPaddingRight();
            int left = rlItemNode.getPaddingLeft();
            rlItemNode.setBackgroundResource(selected ? R.drawable.corner_radius_lighter_border_blue : R.drawable.corner_radius_lighter);
            rlItemNode.setPadding(left, top, right, bottom);
        }
    }

    private AsyncLoadPools asyncLoadPools = null;

    private void refresh() {
        if (asyncLoadPools != null) return; // ignore refresh request as one is ongoing

        asyncLoadPools = new AsyncLoadPools();
        asyncLoadPools.execute();
    }

    @Override
    public void onSelectPool(final View view, final PoolItem poolItem) {
        setItemPoolLayout(selectedPoolView, false);
        selectedPoolView = view;
        selectedPool = poolItem;
        setItemPoolLayout(selectedPoolView, true);
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

    private class AsyncLoadPools extends AsyncTask<Void, PoolItem, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            poolsAdapter.setPools(null);
            poolsAdapter.allowClick(false);

            setItemPoolLayout(selectedPoolView, false);
            selectedPoolView = null;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            Set<PoolItem> seedList = new HashSet<>();
            seedList.addAll(allPools);
            allPools.clear();

            /*Dispatcher d = new Dispatcher(new Dispatcher.Listener() {
                @Override
                public void onGet(PoolItem info) {
                    publishProgress(info);
                }
            });*/

            PoolItem[] pools = ProviderManager.getPools();
            for(int i = 0; i < pools.length; i++) {
                PoolItem poolItem = pools[i];

                if(poolItem.getApiUrl() != null) {
                    StringRequest stringRequest = poolItem.getInterface().getStringRequest(poolsAdapter);
                    mPoolQueue.add(stringRequest);
                }

                allPools.add(poolItem);
            }

            return true;
        }

        @Override
        protected void onProgressUpdate(PoolItem... values) {
            if (!isCancelled())
                if (values != null)
                    poolsAdapter.addPool(values[0]);
                else
                    poolsAdapter.setPools(null);
        }

        @Override
        protected void onPostExecute(Boolean result) {
            complete();
        }

        @Override
        protected void onCancelled(Boolean result) {
            complete();
        }

        private void complete() {
            asyncLoadPools = null;
            //if (!isAdded()) return;

            pullToRefresh.setRefreshing(false);

            poolsAdapter.setPools(allPools);
            poolsAdapter.allowClick(true);

            rvPools.post(() -> updateSelectedPoolLayout());
        }
    }
}