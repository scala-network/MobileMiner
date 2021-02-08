// Copyright (c) 2019, Mine2Gether.com
//
// Please see the included LICENSE file for more information.
//
// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.content.SharedPreferences;

import java.util.HashMap;

public class Config {

    public final static int STATE_STOPPED = 0;
    public final static int STATE_MINING = 1;
    public final static int STATE_PAUSED = 2;
    public final static int STATE_COOLING = 3;
    public final static int STATE_CALCULATING = 4;

    public final static String[] SUPPORTED_ARCHITECTURES = {"arm64-v8a", "armeabi-v7a", "x86_64"};

    public final static String CONFIG_USERDEFINED_POOLS = "userdefined_pools";
    public final static String CONFIG_SELECTED_POOL = "selected_pool";
    public final static String CONFIG_POOL_PORT = "custom_port";
    public final static String CONFIG_TEMPERATURE_UNIT = "temperature_unit";
    public final static String CONFIG_TEMPERATURE_SENSOR_SHOW_WARNING = "temp_sensor_warning";

    private static Config mSettings;
    private SharedPreferences preferences;

    public final static int DefaultMaxCPUTemp = 70; // 60,65,70,75,80
    public final static int DefaultMaxBatteryTemp = 40; // 30,35,40,45,50
    public final static int DefaultCooldownTheshold = 10; // 5,10,15,20,25

    static final int DefaultPoolIndex = 1;
    public static final Long statsDelay = 30000L;
    public static final String miner_xlarig = "xlarig";
    static final String algo = "panthera";
    public static final String githubAppJson = "https://raw.githubusercontent.com/scala-network/MobileMiner/2.0.0/app.json";

    public static final String CONFIG_KEY_CONFIG_VERSION = "config_version";
    public static final String version = "4";
    static final Integer logMaxLength = 50000;
    static final Integer logPruneLength = 1000;
    static final String debugAddress = "Ssy2HXpWZ9RhXbb9uNFTeHjaYfexa3suDbGJDSfUWSEpSajSmjQXwLh2xqCAAUQfZrdiRkvpUZvBceT8d6zKc6aV9NaZVYXFsY";

    private HashMap<String,String> mConfigs = new HashMap<String, String>();

    static void initialize(SharedPreferences preferences) {
        mSettings = new Config();
        mSettings.preferences = preferences;
    }

    public static void write(String key, String value) {
        if(!key.startsWith("system:")) {
            mSettings.preferences.edit().putString(key, value).apply();
        }

        if(value.isEmpty()) {
            return;
        }

        mSettings.mConfigs.put(key, value);
    }

    static void clear() {
        mSettings.preferences.edit().clear().apply();
        mSettings.mConfigs.clear();
    }

    public static String read(String key) {
        return read(key, "");
    }

    public static String read(String key, String fallback) {
        if(!key.startsWith("system:")) {
            return mSettings.preferences.getString(key, fallback);
        }

        if(!mSettings.mConfigs.containsKey(key) || mSettings.mConfigs.get(key).isEmpty()) {
            return fallback;
        }

        return mSettings.mConfigs.get(key);
    }

    public Config() {
    }
}
