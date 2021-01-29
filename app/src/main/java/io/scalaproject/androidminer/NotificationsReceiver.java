// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class NotificationsReceiver extends BroadcastReceiver {
    private static final String LOG_TAG = "NotificationsReceiver";
    static public MainActivity activity = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        if(MainActivity.OPEN_ACTION.equals(action)) {
            Log.v(LOG_TAG,"OPEN_ACTION");
        } else if(MainActivity.STOP_ACTION.equals(action)) {
            Log.v(LOG_TAG,"STOP_ACTION");
            activity.stopMining();
        }
    }
}
