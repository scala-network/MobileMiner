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
import java.util.Objects;

public class Config {

    public final static int STATE_STOPPED = 0;
    public final static int STATE_MINING = 1;
    public final static int STATE_PAUSED = 2;
    public final static int STATE_COOLING = 3;
    public final static int STATE_CALCULATING = 4;

    public static final int MAX_WORKERNAME_TITLE_CHARS = 25;

    public static final String URL_CHANGELOG_DIRECTORY = "https://raw.githubusercontent.com/scala-network/MobileMiner/master/fastlane/metadata/android/en-US/changelog/";
    public static final String URL_RELEASES = "https://github.com/scala-network/MobileMiner/releases";

    public final static String[] SUPPORTED_ARCHITECTURES = {"arm64-v8a", "armeabi-v7a", "x86_64"};

    public final static String CONFIG_INIT = "init";
    public final static String CONFIG_USERDEFINED_POOLS = "userdefined_pools";
    public final static String CONFIG_SELECTED_POOL = "selected_pool";
    public final static String CONFIG_CORES = "cores";
    public final static String CONFIG_CUSTOM_PORT = "custom_port";
    public final static String CONFIG_TEMPERATURE_UNIT = "temperature_unit";
    public final static String CONFIG_TEMPERATURE_SENSOR_SHOW_WARNING = "temp_sensor_warning";
    public final static String CONFIG_HASHRATE_REFRESH_DELAY = "hashrate_refresh_delay";
    public final static String CONFIG_SEND_DEBUG_INFO = "send_debug_info";
    public final static String CONFIG_ADDRESS = "address";
    public final static String CONFIG_USERNAME_PARAMETERS = "usernameparameters";
    public final static String CONFIG_WORKERNAME = "workername";
    public final static String CONFIG_MAX_CPU_TEMP = "maxcputemp";
    public final static String CONFIG_MAX_BATTERY_TEMP = "maxbatterytemp";
    public final static String CONFIG_COOLDOWN_THRESHOLD = "cooldownthreshold";
    public final static String CONFIG_DISABLE_TEMPERATURE_CONTROL = "disable_temperature_control";
    public final static String CONFIG_PAUSE_ON_BATTERY = "pauseonbattery";
    public final static String CONFIG_PAUSE_ON_NETWORK = "pauseonnetwork";
    public final static String CONFIG_KEEP_SCREEN_ON_WHEN_MINING = "keepscreenonwhenmining";
    public final static String CONFIG_HIDE_SETUP_WIZARD = "hide_setup_wizard";
    public final static String CONFIG_CPU_INFO = "cpu_info";
    public final static String CONFIG_DISCLAIMER_AGREED = "disclaimer_agreed";
    public final static String CONFIG_POOLS_REPOSITORY_LAST_FETCHED = "pools_respository_last_fetched";
    public final static String CONFIG_POOLS_REPOSITORY_JSON = "pools_respository_json";
    public final static String CONFIG_APP_PREVIOUS_VERSION = "app_previous_version";

    public final static int DefaultRefreshDelay = 30; // In seconds

    public final static int CHECK_TEMPERATURE_DELAY = 10000; // In milliseconds
    public final static int CHECK_MINING_TIME_DELAY = 60000; // In milliseconds

    private static Config mSettings;
    private SharedPreferences preferences;

    public final static int DefaultMaxCPUTemp = 70; // 60,65,70,75,80
    public final static int DefaultMaxBatteryTemp = 40; // 30,35,40,45,50
    public final static int DefaultCooldownTheshold = 10; // 5,10,15,20,25

    public static final Long statsDelay = 30000L;
    public static final String miner_xlarig = "xlarig";
    static final String algo = "panthera";

    public static final String CONFIG_KEY_CONFIG_VERSION = "config_version";
    public static final String version = "4";

    public static final String CONFIG_KEY_POOLS_VERSION = "pools_version";

    static final Integer logMaxLength = 50000;
    static final Integer logPruneLength = 1000;

    private final HashMap<String,String> mConfigs = new HashMap<>();

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

        if(!mSettings.mConfigs.containsKey(key) || Objects.requireNonNull(mSettings.mConfigs.get(key)).isEmpty()) {
            return fallback;
        }

        return mSettings.mConfigs.get(key);
    }

    public Config() {
    }
}
