package io.scalaproject.androidminer;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

public class SplashActivity extends Activity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        SharedPreferences preferences = getSharedPreferences(getPackageName() + "_preferences", MODE_PRIVATE);
        Config.initialize(preferences);

        String configversion = Config.read("config_version");
        if(!configversion.equals(Config.version)) {
            Config.clear();
            Config.write("config_version", Config.version);
        }

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
                String hide_landing_page = Config.read("hide_landing_page");

                if (hide_landing_page.equals("")) {
                    startActivity(new Intent(SplashActivity.this, LandingPageActivity.class));
                } else {
                    startActivity(new Intent(SplashActivity.this, MainActivity.class));
                }

                finish();
            }
        }, millisecondsDelay);
    }
}