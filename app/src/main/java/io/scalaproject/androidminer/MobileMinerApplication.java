// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.app.Application;
import android.app.job.JobInfo;
import android.content.Context;

import org.acra.*;
import org.acra.annotation.*;
import org.acra.data.StringFormat;


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

        // The following line triggers the initialization of ACRA
        ACRA.init(this);
    }
}
