// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer.api;

import java.util.ArrayList;
import io.scalaproject.androidminer.Config;

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

        // User Defined
        add("custom", "custom", "3333", 0, "", "");

        // Scala Official pool
        add(
                "Scala Project (Official Pool)",
                "mine.scalaproject.io",
                "3333",
                3, // Scala
                "https://pool.scalaproject.io",
                "95.111.237.231"
        );

        // FastPool
        add(
                "FastPool",
                "fastpool.xyz",
                "10126",
                2, // CryptonoteNodeJS
                "https://fastpool.xyz/xla/",
                "130.185.202.159"
        );

        // GNTL
        add(
                "GNTL",
                "xla.pool.gntl.co.uk",
                "40002",
                1, // NodeJS
                "https://xla.pool.gntl.co.uk",
                "83.151.238.34"
        );

        // HeroMiners
        add(
                "HeroMiners",
                "scala.herominers.com",
                "10130",
                2, // CryptonoteNodeJS
                "https://scala.herominers.com",
                "138.201.217.40"
        );

        // LetsHashIt
        add(
                "LetsHashIt",
                "letshash.it",
                "2332",
                2,
                "https://letshash.it/xla",
                "95.111.246.231"
        );

        // LuckyPool
        add(
                "LuckyPool",
                "scala.luckypool.io",
                "6677",
                1, // NodeJS
                "https://scala.luckypool.io",
                "51.89.96.162"
        );
    }
}
