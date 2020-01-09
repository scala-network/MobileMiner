// Copyright (c) 2020, Scala Project
//
// Please see the included LICENSE file for more information.

package scala.androidminer;

import android.support.v4.util.Pools;

import java.util.ArrayList;
import java.util.HashMap;

public class Config {

    protected static final Config settings = new Config();

    public static final int defaultPoolIndex = 1;
    public static final String defaultWallet = "";
    public static final String defaultPassword = "ScalaMobileMiner";

    public static final String miner_xlarig = "xlarig";
    public static final String algo = "defyx";

    public static final String version = "0.0.2";
    public static final Integer logMaxLength = 50000;
    public static final Integer logPruneLength = 1000;

    private ArrayList<PoolItem> mPools = new ArrayList<PoolItem>();

    private static HashMap<String,String> mConfigs = new HashMap<String, String>();

    public static void write(String key, String value) {
        if(value.isEmpty()) {
            return;
        }

        mConfigs.put(key, value);
    }


    public static String read(String key) {
        if(!mConfigs.containsKey(key)) {
            return "";
        }

        return mConfigs.get(key);
    }


    public Config() {


        //User Defined
        mPools.add(new PoolItem("custom", "custom",3333,0, ""));

        // Scala Official pool
        mPools.add(new PoolItem(
                        "Scalaproject.io (Official Pool)",
                        "mine.scalaproject.io",
                        3333,
                        1,
                        "https://pool.scalaproject.io"
                )
        );

        // Another Scala pool : Miner.Rocks
        mPools.add(new PoolItem(
                        "Miner.Rocks",
                        "stellite.miner.rocks",
                        5003,
                        2,
                        "https://stellite.miner.rocks"
                )
        );

        // Another Scala pool : HeroMiners
        mPools.add(new PoolItem(
                        "HeroMiners",
                        "scala.herominers.com",
                        10130,
                        2,
                        "https://scala.herominers.com"
                )
        );
        // Another Scala pool : GNTL
        mPools.add(new PoolItem(
                        "GNTL",
                        "xla.pool.gntl.co.uk",
                        3333,
                        1,
                        "https://xla.pool.gntl.co.uk"
                )
        );

    }

    static public PoolItem[] getPools() {

        return settings.mPools.toArray(new PoolItem[settings.mPools.size()]);
    }

    static public PoolItem getPoolById(int idx) {
        return settings.mPools.get(idx);
    }

    static public PoolItem getPoolById(String idx) {
        if(idx.equals("")){
            return null;
        }
        return settings.mPools.get(Integer.valueOf(idx));
    }

//    static public int getIdForPool(PoolItem poolItem) {
//        int i = settings.mPools.indexOf(poolItem);
//        return i;
//    }

    static public PoolItem getSelectedPool() {
        String sp = PreferenceHelper.getName("selected_pool");
        if(sp.equals("")){
            return null;
        }

        PoolItem pi = getPoolById(sp);

        return pi;
    }

}
