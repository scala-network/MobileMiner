// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class WizardAddressActivity extends BaseActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ((getIntent().getFlags() & Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) != 0) {
            // Activity was brought to front and not created,
            // Thus finishing this will get us to the last viewed activity
            finish();
            return;
        }

        setContentView(R.layout.fragment_wizard_address);
    }

    public void onPaste(View view) {
        View view2 = findViewById(android.R.id.content).getRootView();

        TextInputEditText etAddress = view2.findViewById(R.id.addressWizard);
        etAddress.setText(Utils.pasteFromClipboard(WizardAddressActivity.this));
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
        }
        else {
            Toast.makeText(appContext, "This version of Android does not support Qr Code.", Toast.LENGTH_LONG).show();
        }
    }

    private void startQrCodeActivity() {
        View view2 = findViewById(android.R.id.content).getRootView();

        Context appContext = WizardAddressActivity.this;

        try {
            Intent intent = new Intent(appContext, QrCodeScannerActivity.class);
            startActivity(intent);

            TextView tvAddress = view2.findViewById(R.id.address);
            tvAddress.setText(Config.read("address"));
        } catch (Exception e) {
            Toast.makeText(appContext, e.getMessage(), Toast.LENGTH_SHORT).show();
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
                Toast.makeText(appContext,"Camera Permission Denied.", Toast.LENGTH_LONG).show();
            }
        }
    }

    public void onNext(View view) {
        View view2 = findViewById(android.R.id.content).getRootView();

        TextView tvAddress = view2.findViewById(R.id.addressWizard);
        String strAddress = tvAddress.getText().toString();

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

        startActivity(new Intent(WizardAddressActivity.this, WizardPoolActivity.class));
        finish();
    }

    private void requestFocus(View view) {
        if (view.requestFocus()) {
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        }
    }

    public void onMineScala(View view) {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.mine_scala);
        dialog.setCancelable(false);

        Button btnYes = dialog.findViewById(R.id.btnYes);
        btnYes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                View view2 = findViewById(android.R.id.content).getRootView();
                TextView tvAddress = view2.findViewById(R.id.addressWizard);
                tvAddress.setText(Utils.SCALA_XLA_ADDRESS);

                dialog.dismiss();
            }
        });

        Button btnNo = dialog.findViewById(R.id.btnNo);
        btnNo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }
}