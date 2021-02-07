// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import android.os.StrictMode;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;


import androidx.core.content.ContextCompat;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;

import io.scalaproject.androidminer.widgets.Toolbar;

public class WizardAddressActivity extends BaseActivity {
    private TextView tvAddress;
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

        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        if (SDK_INT > 8) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                    .permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        //ProviderManager.loadPools(getApplicationContext());

        setContentView(R.layout.fragment_wizard_address);
        View view2 = findViewById(android.R.id.content).getRootView();
        tvAddress = view2.findViewById(R.id.addressWizard);

        // Toolbar
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        toolbar.setOnButtonListener(new Toolbar.OnButtonListener() {
            @Override
            public void onButtonMain(int type) {
                switch (type) {
                    case Toolbar.BUTTON_MAIN_BACK:
                        //onBackPressed();
                        //startActivity(new Intent(WizardAddressActivity.this, WizardHomeActivity.class));
                        finish();
                        break;
                }
            }

            @Override
            public void onButtonOptions(int type) {
                onMineScala();
            }
        });

        toolbar.setTitle("Wallet Address");
        toolbar.setButtonMain(Toolbar.BUTTON_MAIN_BACK);
        toolbar.setButtonOptions(Toolbar.BUTTON_OPTIONS_STAR);
    }

    public void onPaste(View view) {
        tvAddress.setText(Utils.pasteFromClipboard(WizardAddressActivity.this));
    }

    public void onScanQrCode(View view) {
        Context appContext = WizardAddressActivity.this;

        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 100);
            }
            else {
                startQrCodeActivity();
            }
        } else if(Build.VERSION.SDK_INT >= 21) {
            startQrCodeActivity();
        }else {
            Utils.showToast(appContext, "This version of Android does not support Qr Code.", Toast.LENGTH_LONG);
        }
    }

    private void startQrCodeActivity() {
        Context appContext = WizardAddressActivity.this;

        try {
            Intent intent = new Intent(appContext, QrCodeScannerActivity.class);
            startActivity(intent);
        } catch (Exception e) {
            Utils.showToast(appContext, e.getMessage(), Toast.LENGTH_SHORT);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        Context appContext = WizardAddressActivity.this;

        if (requestCode == 100) {
            if (permissions[0].equals(Manifest.permission.CAMERA) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startQrCodeActivity();
            }
            else {
                Utils.showToast(appContext,"Camera Permission Denied.", Toast.LENGTH_LONG);
            }
        }
    }

    public void onNext(View view) {
        String strAddress = tvAddress.getText().toString();
        View view2 = findViewById(android.R.id.content).getRootView();

        TextInputLayout til = view2.findViewById(R.id.addressIL);

        if(strAddress.isEmpty() || !Utils.verifyAddress(strAddress)) {
            til.setErrorEnabled(true);
            til.setError(getResources().getString(R.string.invalidaddress));
            requestFocus(tvAddress);
            return;
        }

        til.setErrorEnabled(false);
        til.setError(null);

        Config.write("address", strAddress);

        Intent intent = new Intent(WizardAddressActivity.this, PoolActivity.class);
        intent.putExtra(PoolActivity.RequesterType, PoolActivity.REQUESTER_WIZARD);
        startActivity(intent);

        //finish();
    }

    private void requestFocus(View view) {
        if (view.requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        tvAddress.setText(Config.read("address"));

    }

    public void onMineScala() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(WizardAddressActivity.this, R.style.MaterialAlertDialogCustom);
        builder.setTitle(getString(R.string.supporttheproject))
                .setMessage(getString(R.string.minetoscala))
                .setCancelable(true)
                .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        View view2 = findViewById(android.R.id.content).getRootView();
                        TextView tvAddress = view2.findViewById(R.id.addressWizard);
                        tvAddress.setText(Utils.SCALA_XLA_ADDRESS);
                    }
                })
                .setNegativeButton(getString(R.string.no), null)
                .show();
    }
}