// Copyright (c) 2020, Scala Project
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.content.SharedPreferences;

import java.util.HashMap;
import io.scalaproject.androidminer.api.PoolManager;

public class Config {
    public final static String[] SUPPORTED_ARCHITECTURES = {"arm64-v8a", "armeabi-v7a"};

    private static Config mSettings ;
    private SharedPreferences preferences;

    public static final int DefaultPoolIndex = 1;
    public static final String DefaultWallet = "";
    public static final String DefaultPassword = "ScalaMobileMiner";

    public static final String miner_xlarig = "xlarig";
    public static final String algo = "defyx";

    public static final String version = "0.0.3";
    public static final Integer logMaxLength = 50000;
    public static final Integer logPruneLength = 1000;

    private HashMap<String,String> mConfigs = new HashMap<String, String>();

    public static void initialize(SharedPreferences preferences) {
        mSettings = new Config();
        mSettings.preferences = preferences;
    }

    public static void write(String key, String value) {
        if(!key.startsWith("system:")) {
            mSettings.preferences.edit().putString(key, value).commit();
        }
        if(value.isEmpty()) {
            return;
        }

        mSettings.mConfigs.put(key, value);
    }

    public static void clear() {
        mSettings.preferences.edit().clear().commit();
        mSettings.mConfigs.clear();
    }

    public static String read(String key) {

        if(!key.startsWith("system:")) {
            return mSettings.preferences.getString(key, "");
        }
        if(!mSettings.mConfigs.containsKey(key)) {
            return "";
        }
        return mSettings.mConfigs.get(key);
    }


    public Config() {

        //User Defined
        PoolManager.add("custom", "custom","3333",0, "");

        // Scala Official pool
        PoolManager.add(
            "Scalaproject.io (Official Pool)",
            "mine.scalaproject.io",
            "3333",
            1,
            "https://pool.scalaproject.io"
        );

        // Another Scala pool : Miner.Rocks
        PoolManager.add(
            "Miner.Rocks",
            "stellite.miner.rocks",
            "5003",
            2,
            "https://stellite.miner.rocks"
        );

        // Another Scala pool : HeroMiners
        PoolManager.add(
            "HeroMiners",
            "scala.herominers.com",
            "10130",
            2,
            "https://scala.herominers.com"
        );
        // Another Scala pool : GNTL
        PoolManager.add(
            "GNTL",
            "xla.pool.gntl.co.uk",
            "3333",
            1,
            "https://xla.pool.gntl.co.uk"
        );
    }



}
