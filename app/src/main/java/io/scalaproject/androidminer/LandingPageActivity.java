package io.scalaproject.androidminer;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class LandingPageActivity extends BaseActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            // Activity was brought to front and not created,
            // Thus finishing this will get us to the last viewed activity
            finish();
            return;
        }

        setContentView(R.layout.fragment_landing_page);

        View view = findViewById(android.R.id.content).getRootView();

        Button btnPaperWallet = view.findViewById(R.id.btnPaperWallet);
        btnPaperWallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse(getResources().getString(R.string.paper_wallet_url));
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        Button btnCLIWallet = view.findViewById(R.id.btnCLIwallet);
        btnCLIWallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse(getResources().getString(R.string.cli_wallet_url));
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

        Button btnGUIWallet = view.findViewById(R.id.btnGUIwallet);
        btnGUIWallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Uri uri = Uri.parse(getResources().getString(R.string.gui_wallet_url));
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(intent);
            }
        });

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
        tvDisclaimer.setLinkTextColor(getResources().getColor(R.color.c_blue));
        tvDisclaimer.setHighlightColor(Color.TRANSPARENT);
    }

    public void onSkip(View view) {
        startActivity(new Intent(LandingPageActivity.this, MainActivity.class));
        finish();

        Config.write("hide_landing_page", "1");
    }

    private void showDisclaimer() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.disclaimer);
        dialog.setTitle("Disclaimer");
        dialog.setCancelable(false);

        Config.write("disclaimer_agreed", "1");

        Button btnOK = dialog.findViewById(R.id.btnAgree);

        btnOK.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}