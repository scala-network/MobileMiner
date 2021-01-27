// Copyright (c) 2021, Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

public class SplashActivity extends BaseActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        Config.initialize(preferences);

        /*String configversion = Config.read(Config.CONFIG_KEY_CONFIG_VERSION);
        if(!configversion.equals(Config.version)) {
            Config.clear();
            Config.write(Config.CONFIG_KEY_CONFIG_VERSION, Config.version);
        }*/

        super.onCreate(savedInstanceState);
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            // Activity was brought to front and not created,
            // Thus finishing this will get us to the last viewed activity
            finish();
            return;
        }

        setContentView(R.layout.splashscreen);

        int millisecondsDelay = 2000;
        new Handler().postDelayed(new Runnable() {
            public void run() {
                String hide_setup_wizard = Config.read("hide_setup_wizard");

                if (hide_setup_wizard.equals("")) {
                    startActivity(new Intent(SplashActivity.this, WizardHomeActivity.class));
                } else {
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                }

                finish();
            }
        }, millisecondsDelay);
    }
}