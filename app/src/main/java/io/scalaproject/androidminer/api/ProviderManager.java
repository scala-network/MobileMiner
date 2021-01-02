// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer.api;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import io.scalaproject.androidminer.Config;
import io.scalaproject.androidminer.R;
import io.scalaproject.androidminer.Tools;
import io.scalaproject.androidminer.network.Json;

import static io.scalaproject.androidminer.Tools.getReadableHashRateString;
import static io.scalaproject.androidminer.Tools.parseCurrency;
import static io.scalaproject.androidminer.Tools.tryParseLong;

public final class ProviderManager {

    static private ArrayList<PoolItem> mPools = new ArrayList<PoolItem>();

    static public void add(PoolItem poolItem) {
        mPools.add(poolItem);
    }

    static public void add(String key, String pool,String port, int poolType, String poolUrl, String poolIP) {
        mPools.add(new PoolItem(key, pool, port, poolType, poolUrl, poolIP));
    }

    static public void add(String key, String pool, String port, int poolType, String poolUrl, String poolIP, String poolApi) {
        mPools.add(new PoolItem(key, pool, port, poolType, poolUrl, poolIP, poolApi, "",""));
    }

    static public PoolItem[] getPools() {
        return mPools.toArray(new PoolItem[mPools.size()]);
    }

    static public PoolItem getPoolById(int idx) {
        return mPools.get(idx);
    }

    static public PoolItem getPoolById(String idx) {
        int index = Integer.parseInt(idx);

        if (idx.equals("") || mPools.size() < index || mPools.size() == 0) {
            return null;
        }

        return mPools.get(index);
    }

    static final public ProviderData data = new ProviderData();

    static public PoolItem getSelectedPool() {
        if(request.mPoolItem != null) {
            return request.mPoolItem;
        }

        String sp = Config.read("selected_pool");
        if (sp.equals("")) {
            return null;
        }

        return getPoolById(sp);
    }

    static public int getSelectedPoolIndex() {
        String sp = Config.read("selected_pool");
        if (sp.equals("")) {
            return 0;
        }

        int index = Integer.parseInt(sp);

        return index;
    }

    static public void afterSave() {
        if(request.mPoolItem != null)  {
            return;
        }

        PoolItem pi = getSelectedPool();
        if(pi == null) {
            return;
        }

        //mPools.clear();
        request.mPoolItem = pi;
        data.isNew = true;
        request.start();
    }

    static final public ProviderRequest request = new ProviderRequest();

    static public void generate() {
        request.stop();
        request.mPoolItem = null;
        //mPools.clear();

        if(!mPools.isEmpty())
            return;

        //add("custom", "custom", "3333", 0, "", "");

        String lastFetched = Config.read("RepositoryLastFetched");
        String jsonString = "";
        if(!lastFetched.isEmpty()) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
            Date todayDate = Calendar.getInstance().getTime();
            String todayString = formatter.format(todayDate);

            if(lastFetched.equals(todayString)){
                jsonString = Config.read("RepositoryJson");
            }
        }

        if(jsonString.isEmpty()) {
            String url = Config.githubAppJson;
            jsonString  = Json.fetch(url);
        }

        try {
            JSONObject data = new JSONObject(jsonString);
            JSONArray pools = data.getJSONArray("pools");

            for(int i=0; i< pools.length(); i++) {
                JSONObject pool = pools.getJSONObject(i);
                if(!pool.has("apiUrl")) {
                    add(pool.getString("key"), pool.getString("pool"), pool.getString("port"), pool.getInt("poolType"), pool.getString("poolUrl"), pool.getString("poolIp"));
                } else {
                    add(pool.getString("key"), pool.getString("pool"), pool.getString("port"), pool.getInt("poolType"), pool.getString("poolUrl"), pool.getString("poolIp"), pool.getString("apiUrl"));
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
