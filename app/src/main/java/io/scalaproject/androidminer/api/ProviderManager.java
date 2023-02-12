// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import io.scalaproject.androidminer.Config;
import io.scalaproject.androidminer.R;
import io.scalaproject.androidminer.SettingsFragment;
import io.scalaproject.androidminer.Tools;
import io.scalaproject.androidminer.Utils;
import io.scalaproject.androidminer.network.Json;

public final class ProviderManager {

    // Increment the version number when the pool json structure changes
    static private final String version = "1";

    static private final String DEFAULT_POOLS_REPOSITORY = "https://raw.githubusercontent.com/scala-network/MobileMiner/master/app.json";

    // USAGE: When the DEFAULT_POOLS_REPOSITORY file is modified, we need to upload the new file
    //        to the IPNS gateway as well. This is to avoid having to release a new version of the app
    //        every time the data changes.
    static private final String IPNS_NAME = "pool-list.scalaproject.io";
    static private final String[] POOLS_REPOSITORY_IPNS_GATEWAYS = {
            "https://dweb.link/ipns/",
            "https://ipfs.io/ipns/",
            "https://gateway.ipfs.io/ipns/",
            "https://cloudflare-ipfs.com/ipns/"
    };

    static private final String DEFAULT_POOL = "{\n" +
            "\"pools\": [\n" +
            "{\n" +
            "\"key\": \"Scala Project (Official Pool)\",\n" +
            "\"pool\": \"mine.scalaproject.io\",\n" +
            "\"port\": \"3333\",\n" +
            "\"ports\": [\"3333\", \"5555\", \"7777\", \"8888\"],\n" +
            "\"type\": 3,\n" +
            "\"url\": \"https://pool.scalaproject.io\",\n" +
            "\"ip\": \"95.111.237.231\"\n" +
            "} ]\n" +
            "}";

    static public boolean useDefaultPool = false;

    static private final ArrayList<PoolItem> mPools = new ArrayList<>();

    static public void add(PoolItem poolItem) {
        mPools.add(poolItem);
    }
    static public void delete(PoolItem poolItem) {
        mPools.remove(poolItem);
    }

    static public PoolItem add(String key, String pool, String port, ArrayList<String> ports, int poolType, String poolUrl, String poolIP) {
        PoolItem pi = new PoolItem(key, pool, port, ports, poolType, poolUrl, poolIP);
        mPools.add(pi);

        return pi;
    }

    static public PoolItem add(String key, String pool, String port, ArrayList<String> ports, int poolType, String poolUrl, String poolIP, String poolApi) {
        PoolItem pi = new PoolItem(key, pool, port, ports, poolType, poolUrl, poolIP, poolApi, "","");
        mPools.add(pi);

        return pi;
    }

    static public Bitmap getDefaultPoolIcon(Context context, PoolItem poolItem) {
        if(poolItem != null && poolItem.isOfficial())
            return Utils.getCroppedBitmap(Utils.getBitmap(context, R.mipmap.ic_logo_blue));

        return Utils.getCroppedBitmap(Utils.getBitmap(context, R.drawable.ic_pool_default));
    }

    static public void loadPools(Context context) {
        loadDefaultPools();

        loadUserdefinedPools(context);

        // Selected pool
        boolean selectedFound = false;
        String sp = SettingsFragment.selectedPoolTmp == null ? Config.read(Config.CONFIG_SELECTED_POOL).trim() : SettingsFragment.selectedPoolTmp.getKey();
        for(int i = 0; i < mPools.size(); i++) {
            PoolItem pi = mPools.get(i);

            if(pi.getKey().equals(sp)) {
                selectedFound = true;
                pi.setIsSelected(true);
            } else {
                pi.setIsSelected(false);
            }
        }

        if(!selectedFound && !mPools.isEmpty()) {
            Config.write(Config.CONFIG_SELECTED_POOL, mPools.get(0).getKey().trim());
            mPools.get(0).setIsSelected(true);
        }
    }

    static public PoolItem[] getPools(Context context) {
        loadPools(context);

        Collections.sort(mPools, PoolItem.PoolComparator);

        return mPools.toArray(new PoolItem[0]);
    }

    static public PoolItem[] getAllPools() {
        return mPools.toArray(new PoolItem[0]);
    }

    static final public ProviderData data = new ProviderData();

    static public PoolItem getSelectedPool() {
        if(request.mPoolItem != null) {
            return request.mPoolItem;
        }

        // Selected pool
        PoolItem selectedPool = null;
        if(SettingsFragment.selectedPoolTmp != null) {
            for(int i = 0; i < mPools.size(); i++) {
                selectedPool = mPools.get(i);

                if(selectedPool.getKey().equals(SettingsFragment.selectedPoolTmp.getKey())) {
                    return selectedPool;
                }
            }
        } else {
            String sp = Config.read(Config.CONFIG_SELECTED_POOL).trim();
            for (int i = 0; i < mPools.size(); i++) {
                PoolItem pi = mPools.get(i);

                if (pi.getKey().equals(sp)) {
                    selectedPool = pi;
                    pi.setIsSelected(true);
                } else {
                    pi.setIsSelected(false);
                }
            }
        }

        if(!mPools.isEmpty() && selectedPool == null) {
            selectedPool = mPools.get(0);
            selectedPool.setIsSelected(true);
        }

        return selectedPool;
    }

    static public void afterSave() {
        if(request.mPoolItem != null)  {
            return;
        }

        PoolItem pi = getSelectedPool();
        if(pi == null) {
            return;
        }

        request.mPoolItem = pi;
        data.isNew = true;
        request.start();
    }

    static final public ProviderRequest request = new ProviderRequest();

    static public void fetchStats() {
        request.run();
    }

    static public void loadDefaultPools() {
        request.stop();
        request.mPoolItem = null;
        mPools.clear();

        boolean forceReload = false;
        String poolVersion = Config.read(Config.CONFIG_KEY_POOLS_VERSION);
        if(!poolVersion.equals(ProviderManager.version)) {
            forceReload = true;
            Config.write(Config.CONFIG_KEY_POOLS_VERSION, ProviderManager.version);
        }

        String lastFetched = Config.read(Config.CONFIG_POOLS_REPOSITORY_LAST_FETCHED);
        String jsonString = "";
        long now = System.currentTimeMillis() / 1000L;

        // Use cached pools data
        if(!lastFetched.isEmpty() && Long.parseLong(lastFetched) > now && !forceReload) {
            jsonString = Config.read(Config.CONFIG_POOLS_REPOSITORY_JSON);
        }

        if(jsonString.isEmpty()) {
            // Load Pools data from repository
            if(Tools.isURLReachable(DEFAULT_POOLS_REPOSITORY))
                jsonString  = Json.fetch(DEFAULT_POOLS_REPOSITORY);

            // If GitHub is not available or is blocked by firewalls, use IPFS gateways
            if(jsonString.isEmpty()) {
                for (String strPoolURLDir : POOLS_REPOSITORY_IPNS_GATEWAYS) {
                    String strPoolURL = strPoolURLDir + IPNS_NAME;
                    if(Tools.isURLReachable(strPoolURL)) {
                        jsonString = Json.fetch(strPoolURL);
                        if (!jsonString.isEmpty())
                            break;
                    }
                }
            }

            // None of the URL can be reached. Load default data but don't cache it.
            if(jsonString.isEmpty()) {
                useDefaultPool = true;
                jsonString = DEFAULT_POOL;
            } else {
                useDefaultPool = false;
                Config.write(Config.CONFIG_POOLS_REPOSITORY_JSON, jsonString);
                Config.write(Config.CONFIG_POOLS_REPOSITORY_LAST_FETCHED, String.valueOf(now + 3600)); //Cached time is 1 hour for now
            }
        }

        try {
            JSONObject data = new JSONObject(jsonString);
            JSONArray pools = data.getJSONArray("pools");

            for(int i = 0; i < pools.length(); i++) {
                JSONObject pool = pools.getJSONObject(i);

                PoolItem poolItem;

                ArrayList<String> listPort = new ArrayList<>();
                if(pool.has("ports")) {
                    JSONArray portsArray = pool.getJSONArray("ports");
                    for (int j = 0; j < portsArray.length(); j++){
                        listPort.add(portsArray.getString(j));
                    }
                }

                if(!pool.has("apiUrl")) {
                    poolItem = add(pool.getString("key"), pool.getString("pool"), pool.getString("port"), listPort, pool.getInt("type"), pool.getString("url"), pool.getString("ip"));
                } else {
                    poolItem = add(pool.getString("key"), pool.getString("pool"), pool.getString("port"), listPort, pool.getInt("type"), pool.getString("url"), pool.getString("ip"), pool.getString("apiUrl"));
                }

                // Icon
                if(pool.has("icon")) {
                    String iconURL = pool.getString("icon");
                    if (!iconURL.isEmpty()) {
                        Bitmap icon = Utils.getBitmapFromURL(iconURL);
                        if(icon != null)
                            poolItem.setIcon(Utils.getCroppedBitmap(icon));
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    static public void loadUserdefinedPools(Context context) {
        Map<String, ?> pools = context.getSharedPreferences(Config.CONFIG_USERDEFINED_POOLS, Context.MODE_PRIVATE).getAll();
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

    static public void saveUserDefinedPools(Context context) {
        SharedPreferences.Editor editor = context.getSharedPreferences(Config.CONFIG_USERDEFINED_POOLS, Context.MODE_PRIVATE).edit();
        editor.clear();

        for(int i = 0; i < mPools.size(); i++) {
            PoolItem pi = mPools.get(i);

            if(pi.isUserDefined()) {
                String poolString = pi.toString();
                editor.putString(Integer.toString(i), poolString);
            }
        }

        editor.apply();
    }
}
