// Copyright (c) 2021 Scala
//
// Please see the included LICENSE file for more information.

package io.scalaproject.androidminer;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.*;
import java.text.DecimalFormat;
import java.text.ParseException;

import io.scalaproject.androidminer.widgets.CustomToast;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

public final class Utils {
    static public Integer INCREMENT = 5;
    static public Integer MIN_CPU_TEMP = 60;
    static public Integer MIN_BATTERY_TEMP = 30;
    static public Integer MIN_COOLDOWN = 5;

    static public String SCALA_BTC_ADDRESS = "1XTLY5LqdBXRW6hcHtnuMU7c68mAyW6qm";
    static public String SCALA_ETH_ADDRESS = "0x133a15dF7177823Dd407ca87A190bbE4585a379e";
    static public String SCALA_XLA_ADDRESS = "SvkFLjR4DST5bAG8SSHWfta4MsCzRrDEPNx72cTetqcoPfkwi7cFA2sYGG2Tf51rQ9EMSPHVuzxeoS4Y7ieicg5A1M24A8TTW";
    static public String SCALA_LTC_ADDRESS = "LeLK5hopvfArVyKtkZBzF3B5wj6rGrNUGk";

    static public String ADDRESS_REGEX_MAIN = "^S+([1-9A-HJ-NP-Za-km-z]{96})$";
    static public String ADDRESS_REGEX_SUB = "^Ss+([1-9A-HJ-NP-Za-km-z]{96})$";

    static public boolean verifyAddress(String input) {
        Pattern p = Pattern.compile(Utils.ADDRESS_REGEX_MAIN);
        Matcher m = p.matcher(input.trim());
        if(m.matches()) {
            return true;
        }

        p = Pattern.compile(Utils.ADDRESS_REGEX_SUB);
        m = p.matcher(input.trim());

        return m.matches();
    }

    static public float convertStringToFloat(String sNumber) {
        float total = (float) -1;
        try
        {
            total = Float.parseFloat(sNumber);
        }
        catch(NumberFormatException ex)
        {
            DecimalFormat df = new DecimalFormat();
            Number n = null;
            try
            {
                n = df.parse(sNumber);
            }
            catch(ParseException ignored){ }
            if(n != null)
                total = n.floatValue();
        }

        return total;
    }

    static public String getDateTime() {
        Calendar date = Calendar.getInstance(Locale.getDefault());
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        return dateFormat.format(date.getTime());
    }

    static public void showPopup(View view, LayoutInflater inflater, View popupView) {
        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true; // lets taps outside the popup also dismiss it
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window tolken
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);

        // dismiss the popup window when touched
        popupView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                popupWindow.dismiss();
                return true;
            }
        });
    }

    static public void copyToClipboard(String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) MainActivity.getContextOfApplication().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
    }

    static public String pasteFromClipboard(Context appContext) {
        ClipboardManager clipboard = (ClipboardManager) appContext.getSystemService(Context.CLIPBOARD_SERVICE);

        String pasteData = "";

        // If it does contain data
        assert clipboard != null;
        if (!(clipboard.hasPrimaryClip())) {
            // Ignore
        } else if (!(Objects.requireNonNull(clipboard.getPrimaryClipDescription()).hasMimeType(MIMETYPE_TEXT_PLAIN))) {
            // Ignore, since the clipboard has data but it is not plain text
        } else {
            ClipData.Item item = Objects.requireNonNull(clipboard.getPrimaryClip()).getItemAt(0);
            pasteData = item.getText().toString();
        }

        return pasteData;
    }

    static public Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            drawable = (DrawableCompat.wrap(drawable)).mutate();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    static public void hideKeyboardFrom(Context context, View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Activity.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    static public void hideKeyboard(Activity act) {
        if (act == null) return;
        if (act.getCurrentFocus() == null) {
            act.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        } else {
            InputMethodManager imm = (InputMethodManager) act.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow((null == act.getCurrentFocus()) ? null : act.getCurrentFocus().getWindowToken(),
                    InputMethodManager.HIDE_NOT_ALWAYS);
        }
    }

    static public Bitmap getBitmap(VectorDrawable vectorDrawable) {
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return bitmap;
    }

    static public Bitmap getBitmap(VectorDrawableCompat vectorDrawable) {
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(),
                vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        vectorDrawable.draw(canvas);
        return bitmap;
    }

    static public Bitmap getBitmap(Context context, int drawableId) {
        Drawable drawable = ContextCompat.getDrawable(context, drawableId);
        if (drawable instanceof BitmapDrawable) {
            return BitmapFactory.decodeResource(context.getResources(), drawableId);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && drawable instanceof VectorDrawable) {
            return getBitmap((VectorDrawable) drawable);
        }  else if (drawable instanceof VectorDrawableCompat) {
            return getBitmap((VectorDrawableCompat) drawable);
        }

        throw new IllegalArgumentException("unsupported drawable type");
    }

    static public Bitmap getCroppedBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawCircle(bitmap.getWidth() / 2, bitmap.getHeight() / 2, bitmap.getWidth() / 2, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    static public Bitmap getBitmapFromURL(String src) {
        try {
            java.net.URL url = new java.net.URL(src);
            HttpURLConnection connection = (HttpURLConnection) url
                    .openConnection();
            connection.setDoInput(true);
            connection.connect();
            InputStream input = connection.getInputStream();
            return BitmapFactory.decodeStream(input);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    static public int getDimPixels(View v, int dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, v.getResources().getDisplayMetrics());
    }

    static public String getBuildTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(BuildConfig.BUILD_TIME);
        return DateFormat.getDateInstance(DateFormat.LONG).format(calendar.getTime());
    }

    static public String getPrettyTx(String text) {
        return text.substring(0, 7) + "..." + text.substring(text.length() - 7);
    }

    static public String formatTimestamp(Date d) {
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy");
        return sdf.format(d);
    }

    static public Date getDate(long time) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(time * 1000L);
        return c.getTime();
    }

    static public void showToast(Context context, String text, int length) {
        CustomToast ct = new CustomToast(context, text, length);
        ct.show();
    }

    static public void showToast(Context context, String text, int length, int YOffset) {
        CustomToast ct = new CustomToast(context, text, length, YOffset);
        ct.show();
    }

    // Converts to celcius
    static public int convertFahrenheitToCelcius(int fahrenheit) {
        return ((fahrenheit - 32) * 5 / 9);
    }

    // Converts to fahrenheit
    static public int convertCelciusToFahrenheit(int celsius) {
        return ((celsius * 9) / 5) + 32;
    }
}