// Copyright (c) 2019, Mine2Gether.com
//
// Please see the included LICENSE file for more information.
//
// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package shmutalov.verusminer9000;

import android.content.SharedPreferences;

import java.util.HashMap;

public class Config {
    private static Config mSettings;
    private SharedPreferences preferences;

    public static final int DefaultPoolIndex = 1;
    public static final Long statsDelay = 30000L;

    public static final String version = "4";
    public static final Integer logMaxLength = 50000;
    public static final Integer logPruneLength = 1000;

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

    public static void clear() {
        mSettings.preferences.edit().clear().apply();
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
    }
}
