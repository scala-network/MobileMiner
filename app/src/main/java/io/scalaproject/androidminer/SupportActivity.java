// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import io.scalaproject.androidminer.widgets.Toolbar;

public class SupportActivity extends BaseActivity {
    private static final String LOG_TAG = "SupportActivity";

    private Toolbar toolbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            // Activity was brought to front and not created,
            // Thus finishing this will get us to the last viewed activity
            finish();
            return;
        }

        setContentView(R.layout.fragment_support);

        // Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);

        toolbar.setOnButtonListener(new Toolbar.OnButtonListener() {
            @Override
            public void onButtonMain(int type) {
                switch (type) {
                    case Toolbar.BUTTON_MAIN_CLOSE:
                        onBackPressed();
                }
            }

            @Override
            public void onButtonOptions(int type) {
                // Does nothing in pool view
            }
        });

        toolbar.setTitle("Support");
        toolbar.setButtonMain(Toolbar.BUTTON_MAIN_CLOSE);
        toolbar.setButtonOptions(Toolbar.BUTTON_OPTIONS_NONE);

        ImageView ivDiscord = findViewById(R.id.ivDiscord);
        ivDiscord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse(getResources().getString(R.string.discordLink));
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        ImageView ivTelegram = findViewById(R.id.ivTelegram);
        ivTelegram.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse(getResources().getString(R.string.telegramLink));
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        ImageView ivReddit = findViewById(R.id.ivReddit);
        ivReddit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse(getResources().getString(R.string.twitterLink));
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        ImageView ivEmail = findViewById(R.id.ivEmail);
        ivEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Uri uri = Uri.parse(getResources().getString(R.string.emailLink));
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        // Set debug info
        StringBuilder cpuinfo = new StringBuilder(Config.read("CPUINFO").trim());
        if(cpuinfo.length() == 0) {
            try {
                Map<String, String> m = Tools.getCPUInfo();

                cpuinfo = new StringBuilder("ABI: " + Tools.getABI() + "\n");
                for (Map.Entry<String, String> pair : m.entrySet()) {
                    cpuinfo.append(pair.getKey()).append(": ").append(pair.getValue()).append("\n");
                }
            } catch (Exception e) {
                cpuinfo = new StringBuilder();
            }

            Config.write("CPUINFO", cpuinfo.toString().trim());
        }

        // Convert build time to readable date
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(BuildConfig.BUILD_TIME);

        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH);
        String build_time_debug = formatter.format(calendar.getTime());

        String sDebugInfo = "Version Code: " + BuildConfig.VERSION_CODE + "\n" +
                "Version Name: " + BuildConfig.VERSION_NAME + "\n" +
                "Build Time: " + build_time_debug + "\n\n" +
                "Device Name: " + Tools.getDeviceName() + "\n" +
                "CPU Info: " + cpuinfo;

        TextView tvDebugInfo = findViewById(R.id.debuginfo);
        tvDebugInfo.setText(sDebugInfo);
        tvDebugInfo.setMovementMethod(new ScrollingMovementMethod());

        Button btnDebugInfo = findViewById(R.id.btnDebugInfo);
        btnDebugInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.copyToClipboard("Scala Miner Debug Info", sDebugInfo);
                Utils.showToast(getApplicationContext(), getResources().getString(R.string.debuginfo_copied), Toast.LENGTH_SHORT, Tools.TOAST_YOFFSET_BOTTOM);
            }
        });

        Utils.hideKeyboard(this);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }
}