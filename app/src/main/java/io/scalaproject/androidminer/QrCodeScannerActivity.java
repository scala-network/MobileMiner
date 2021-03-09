// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.ResultPoint;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class QrCodeScannerActivity extends BaseActivity {
    public static final String XLA_SCHEME = "scala:";

    private DecoratedBarcodeView barcodeView;
    private Button btnFlashlight;
    private String lastText;

    private final BarcodeCallback callbackBarcode = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if(result.getText() == null || result.getText().equals(lastText)) {
                // Prevent duplicate scans
                return;
            }

            lastText = result.getText();
            if(lastText.startsWith(XLA_SCHEME)) {
                lastText = lastText.substring(XLA_SCHEME.length());
            }

            barcodeView.setStatusText(lastText + System.getProperty("line.separator") + System.getProperty("line.separator") + System.getProperty("line.separator"));

            if(Utils.verifyAddress(lastText)) {
                Log.d("CONSOLE:QRCODE", "Barcode read: " + lastText);

                Config.write(Config.CONFIG_ADDRESS, lastText);
                barcodeView.setTorchOff();
                finish();

                return;
            }

            Utils.showToast(getApplicationContext(), "Invalid Scala address", Toast.LENGTH_SHORT);
        }

        @Override
        public void possibleResultPoints(List<ResultPoint> resultPoints) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_code_scanner);

        findViewById(R.id.btnCancel).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    barcodeView.setTorchOff();
                    finish();
                } catch (Exception ignored) {

                }
            }
        });

        btnFlashlight = findViewById(R.id.btnFlashlight);

        // if the device does not have flashlight in its camera, then remove the switch flashlight button
        if (!hasFlash()) {
            btnFlashlight.setVisibility(View.GONE);
        }

        barcodeView = findViewById(R.id.barcodeView);
        Collection<BarcodeFormat> formats = Arrays.asList(BarcodeFormat.QR_CODE, BarcodeFormat.CODE_39);
        barcodeView.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(formats));
        barcodeView.initializeFromIntent(getIntent());
        barcodeView.decodeContinuous(callbackBarcode);
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        return barcodeView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event);
    }

    private boolean hasFlash() {
        return this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    public void switchFlashlight(View view) {
        if (getResources().getString(R.string.turn_on_flashlight).contentEquals(btnFlashlight.getText())) {
            barcodeView.setTorchOn();
            btnFlashlight.setText(getResources().getString(R.string.turn_off_flashlight));
        } else {
            barcodeView.setTorchOff();
            btnFlashlight.setText(getResources().getString(R.string.turn_on_flashlight));
        }
    }
}