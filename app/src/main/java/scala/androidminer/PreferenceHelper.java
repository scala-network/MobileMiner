// Copyright (c) 2020, Scala Project
//
// Please see the included LICENSE file for more information.

package scala.androidminer;

public class PreferenceHelper {

    public static void setName(String key, String value) {
        MainActivity.preferences.edit().putString(key, value).commit();
    }

    public static String getName(String key) {
        return MainActivity.preferences.getString(key, "");
    }

    public static void clear() {
        MainActivity.preferences.edit().clear().commit();
    }


    public static boolean setMiner(String miner) {
        //@@TODO Verify miner variable
        setName("miner", miner);
        return true;
    }


}