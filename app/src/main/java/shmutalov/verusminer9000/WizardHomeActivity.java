// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package shmutalov.verusminer9000;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.res.ResourcesCompat;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

import shmutalov.verusminer9000.miner.VerusBinMiningService;

public class WizardHomeActivity extends BaseActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            // Activity was brought to front and not created,
            // Thus finishing this will get us to the last viewed activity
            finish();
            return;
        }

        // check, if our device is supported
        String abi = Tools.getABI();
        VerusBinMiningService miner = new VerusBinMiningService();
        boolean isSupported = Arrays.asList(miner.getSupportedArchitectures()).contains(abi);
        if (isSupported) {
            setContentView(R.layout.fragment_wizard_home);
        } else {
            setContentView(R.layout.fragment_wizard_platform_not_supported);
        }

        View view = findViewById(android.R.id.content).getRootView();

        if (!isSupported) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(BuildConfig.BUILD_TIME);
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH);
            String build_time_debug = formatter.format(calendar.getTime());

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

            String sDebugInfo = "Version Code: " + BuildConfig.VERSION_CODE + "\n" +
                    "Version Name: " + BuildConfig.VERSION_NAME + "\n" +
                    "Build Time: " + build_time_debug + "\n\n" +
                    "Device Name: " + Tools.getDeviceName(view.getContext()) + "\n" +
                    "CPU Info: " + cpuinfo;

            Button btnDebugInfo = view.findViewById(R.id.btnDebugInfo);
            btnDebugInfo.setOnClickListener(view1 -> {
                Utils.copyToClipboard(view.getContext(),"Verus Miner 9000 Debug Info", sDebugInfo);
                Toast.makeText(view.getContext(), view.getResources().getString(R.string.debuginfo_copied), Toast.LENGTH_SHORT).show();
            });

            return;
        }

        String sDisclaimerText = getResources().getString(R.string.disclaimer_agreement);
        String sDiclaimer = getResources().getString(R.string.disclaimer);

        SpannableString ss = new SpannableString(sDisclaimerText);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                showDisclaimer();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
            }
        };

        int iStart = sDisclaimerText.indexOf(sDiclaimer);
        int iEnd = iStart + sDiclaimer.length();
        ss.setSpan(clickableSpan, iStart, iEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        TextView tvDisclaimer = view.findViewById(R.id.disclaimer);
        tvDisclaimer.setText(ss);
        tvDisclaimer.setMovementMethod(LinkMovementMethod.getInstance());
        tvDisclaimer.setLinkTextColor(ResourcesCompat.getColor(getResources(), R.color.c_blue, getTheme()));
        tvDisclaimer.setHighlightColor(Color.TRANSPARENT);
    }

    public void onEnterAddress(View view) {
        startActivity(new Intent(WizardHomeActivity.this, WizardAddressActivity.class));
        finish();

        Config.write("hide_setup_wizard", "1");
    }

    public void onCreateWallet(View view) {
        Uri uri = Uri.parse(getResources().getString(R.string.mobile_wallet_url));
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(intent);
    }

    public void onSkip(View view) {
        startActivity(new Intent(WizardHomeActivity.this, MainActivity.class));
        finish();

        Config.write("hide_setup_wizard", "1");
    }

    private void showDisclaimer() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.disclaimer);
        dialog.setTitle("Disclaimer");
        dialog.setCancelable(false);

        Button btnOK = dialog.findViewById(R.id.btnAgree);

        btnOK.setOnClickListener(v -> {
            Config.write("disclaimer_agreed", "1");
            dialog.dismiss();
        });

        dialog.show();
    }
}