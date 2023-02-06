// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeoutException;

import io.scalaproject.androidminer.dialogs.ProgressDialog;

public abstract class BaseActivity extends AppCompatActivity {
    private ProgressDialog progressDialog = null;

    private static class SimpleProgressDialog extends io.scalaproject.androidminer.dialogs.ProgressDialog {
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

    @Override
    protected void onDestroy() {
        dismissProgressDialog();
        super.onDestroy();
    }

    @Override
    public void onPause() {
        dismissProgressDialog();
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Thread.UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(@NotNull Thread paramThread, @NotNull Throwable paramThrowable) {
                MainActivity.hideNotifications();

                // Ignore this exception: https://stackoverflow.com/a/55999687/1046299
                if (paramThread.getName().equals("FinalizerWatchdogDaemon") && paramThrowable instanceof TimeoutException) {
                    System.exit(2); // Prevents the service/app from freezing
                    return;
                }

                if (oldHandler != null)
                    oldHandler.uncaughtException(paramThread, paramThrowable); // Delegates to Android's error handling
                else
                    System.exit(2); // Prevents the service/app from freezing
            }
        });
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
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }

        progressDialog = null;
    }
}