// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;

import org.acra.*;
import org.acra.annotation.*;

@AcraCore(buildConfigClass = BuildConfig.class)
@AcraMailSender(mailTo = "hello@scalaproject.io")
public class MobileMinerApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(context);

        ACRA.init(this);

        SharedPreferences preferences = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        Config.initialize(preferences);

        ACRA.getErrorReporter().setEnabled(Config.read(Config.CONFIG_SEND_DEBUG_INFO, "0").equals("1"));
    }
}