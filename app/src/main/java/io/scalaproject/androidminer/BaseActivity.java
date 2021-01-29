// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import android.content.Context;
import android.os.Handler;
import android.widget.Toast;

import static io.scalaproject.androidminer.MainActivity.contextOfApplication;

public abstract class BaseActivity extends AppCompatActivity implements Thread.UncaughtExceptionHandler {
    io.scalaproject.androidminer.dialogs.ProgressDialog progressDialog = null;

    private class SimpleProgressDialog extends io.scalaproject.androidminer.dialogs.ProgressDialog {

        SimpleProgressDialog(Context context, int msgId) {
            super(context);
            setCancelable(false);
            setMessage(context.getString(msgId));
        }

        @Override
        public void onBackPressed() {
            // prevent back button
        }
    }

    private static int sessionDepth = 0;

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        MainActivity.hideNotifications();
        ex.printStackTrace();
    }

    public void showProgressDialog(int msgId) {
        showProgressDialog(msgId, 250); // don't show dialog for fast operations
    }

    public void showProgressDialog(int msgId, long delayMillis) {
        dismissProgressDialog(); // just in case
        progressDialog = new SimpleProgressDialog(BaseActivity.this, msgId);
        if (delayMillis > 0) {
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    if (progressDialog != null) progressDialog.show();
                }
            }, delayMillis);
        } else {
            progressDialog.show();
        }
    }

    public void dismissProgressDialog() {
        if (progressDialog == null) return; // nothing to do

        if (progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        progressDialog = null;
    }

    @Override
    protected void onStart() {
        super.onStart();
        sessionDepth++;
        if(sessionDepth == 1){
            //app came to foreground;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (sessionDepth > 0)
            sessionDepth--;
        if (sessionDepth == 0) {
            // app went to background
            if(MainActivity.isDeviceMiningBackground())
                Toast.makeText(contextOfApplication, getResources().getString(R.string.miningbackground), Toast.LENGTH_SHORT).show();
        }
    }
}