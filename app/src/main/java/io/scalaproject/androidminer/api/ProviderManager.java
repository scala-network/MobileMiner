// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer.api;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import io.scalaproject.androidminer.Config;
import io.scalaproject.androidminer.Utils;
import io.scalaproject.androidminer.network.Json;

public final class ProviderManager {

    static private ArrayList<PoolItem> mPools = new ArrayList<PoolItem>();

    //static private ArrayList<PoolItem> mUserDefinedPools = new ArrayList<PoolItem>();

    static public void add(PoolItem poolItem) {
        mPools.add(poolItem);
    }

    static public PoolItem add(String key, String pool,String port, int poolType, String poolUrl, String poolIP) {
        PoolItem pi = new PoolItem(key, pool, port, poolType, poolUrl, poolIP);
        mPools.add(pi);

        return pi;
    }

    static public PoolItem add(String key, String pool, String port, int poolType, String poolUrl, String poolIP, String poolApi) {
        PoolItem pi = new PoolItem(key, pool, port, poolType, poolUrl, poolIP, poolApi, "","");
        mPools.add(pi);

        return pi;
    }

    static public void loadPools(Context context) {
        loadDefaultPools();

        // WTF with this??
        //loadUserdefinedPools(context);

        //Collections.sort(mPools, PoolItem.PoolComparator);
    }

    static public PoolItem[] getPools(Context context) {
        loadPools(context);

        Collections.sort(mPools, PoolItem.PoolComparator);

        return mPools.toArray(new PoolItem[mPools.size()]);
    }

    static public PoolItem[] getAllPools() {
        return mPools.toArray(new PoolItem[mPools.size()]);
    }

    static public PoolItem getPoolById(int idx) {
        return mPools.get(idx);
    }

    static public PoolItem getPoolByKey(String key) {
        if (mPools.size() == 0) {
            return null;
        }

        for(int i = 0; i < mPools.size(); i++) {
            PoolItem pi = mPools.get(i);

            if(pi.getKey().equals(key))
                return pi;
        }

        return mPools.get(0);
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
            return mPools.get(0);
        }

        return getPoolByKey(sp);
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

    static public void loadDefaultPools() {
        request.stop();
        request.mPoolItem = null;
        mPools.clear();

        if(!mPools.isEmpty())
            return;

        // This line breaks the selection!!! WHY??
        add("custom", "custom", "3333", 0, "", "");

        String lastFetched = Config.read("RepositoryLastFetched");
        String jsonString = "";
        long now = System.currentTimeMillis() / 1000L;

        if(!lastFetched.isEmpty() && Long.parseLong(lastFetched) < now){
            jsonString = Config.read("RepositoryJson");
        }

        if(jsonString.isEmpty()) {
            String url = Config.githubAppJson;
            jsonString  = Json.fetch(url);
            Config.write("RepositoryJson", jsonString);
            Config.write("RepositoryLastFetched", String.valueOf(now + 3600));//Cached time is 1 hour for now
        }

        try {
            JSONObject data = new JSONObject(jsonString);
            JSONArray pools = data.getJSONArray("pools");

            for(int i = 0; i < pools.length(); i++) {
                JSONObject pool = pools.getJSONObject(i);

                PoolItem poolItem;

                if(!pool.has("apiUrl")) {
                    poolItem = add(pool.getString("key"), pool.getString("pool"), pool.getString("port"), pool.getInt("poolType"), pool.getString("poolUrl"), pool.getString("poolIp"));
                } else {
                    poolItem = add(pool.getString("key"), pool.getString("pool"), pool.getString("port"), pool.getInt("poolType"), pool.getString("poolUrl"), pool.getString("poolIp"), pool.getString("apiUrl"));
                }

                // Icon
                if(pool.has("icon")) {
                    String iconURL = pool.getString("icon");
                    if (!iconURL.isEmpty()) {
                        poolItem.setIcon(Utils.getCroppedBitmap((Utils.getBitmapFromURL(iconURL))));
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    static public void loadUserdefinedPools(Context context) {
        //mUserDefinedPools.clear();

        Map<String, ?> pools = context.getSharedPreferences(Config.POOLS_USERDEFINED_KEY, Context.MODE_PRIVATE).getAll();
        for (Map.Entry<String, ?> poolEntry : pools.entrySet()) {
            if (poolEntry != null) { // just in case, ignore possible future errors
                PoolItem pi = PoolItem.fromString((String) poolEntry.getValue());
                if (pi != null) {
                    pi.setUserDefined(true);
                    add(pi);
                }
            }
        }
    }
}
