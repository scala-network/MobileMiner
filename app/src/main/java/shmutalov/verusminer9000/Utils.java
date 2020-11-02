// Copyright (c) 2020, Scala
//
// Please see the included LICENSE file for more information.

package shmutalov.verusminer9000;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Objects;
import java.text.DecimalFormat;
import java.text.ParseException;

import static android.content.ClipDescription.MIMETYPE_TEXT_PLAIN;

final class Utils {
    static public final Integer INCREMENT = 5;
    static public final Integer MIN_CPU_TEMP = 55;
    static public final Integer MIN_BATTERY_TEMP = 30;
    static public final Integer MIN_COOLDOWN = 10;

    static String SCALA_BTC_ADDRESS = "1XTLY5LqdBXRW6hcHtnuMU7c68mAyW6qm";
    static String SCALA_ETH_ADDRESS = "0x133a15dF7177823Dd407ca87A190bbE4585a379e";
    static final String VERUS_DONATION_ADDRESS = "RKE5YdseSU6becMtpHKn4z9N4ahRkqm1cV";

    static String ADDRESS_REGEX_MAIN = "^S+([1-9A-HJ-NP-Za-km-z]{96})$";
    static String ADDRESS_REGEX_SUB = "^Ss+([1-9A-HJ-NP-Za-km-z]{96})$";

//    static boolean verifyAddress(String input) {
//        Pattern p = Pattern.compile(Utils.ADDRESS_REGEX_MAIN);
//        Matcher m = p.matcher(input.trim());
//        if(m.matches()) {
//            return true;
//        }
//
//        p = Pattern.compile(Utils.ADDRESS_REGEX_SUB);
//        m = p.matcher(input.trim());
//        return m.matches();
//    }

    // simple Verus wallet address check
    static boolean verifyAddress(String input) {
        return input.length() > 32 && input.length() < 43;
    }

    static float convertStringToFloat(String sNumber) {
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

    static String getDateTime() {
        Calendar date = Calendar.getInstance(Locale.getDefault());
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        return dateFormat.format(date.getTime());
    }

    static void showPopup(View view, LayoutInflater inflater, View popupView) {
        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        boolean focusable = true; // lets taps outside the popup also dismiss it
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, focusable);

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window tolken
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);

        // dismiss the popup window when touched
        popupView.setOnTouchListener((v, event) -> {
            popupWindow.dismiss();
            v.performClick();
            return true;
        });
    }

    static void copyToClipboard(Context context, String label, String text) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
    }

    static String pasteFromClipboard(Context appContext) {
        ClipboardManager clipboard = (ClipboardManager) appContext.getSystemService(Context.CLIPBOARD_SERVICE);

        String pasteData = "";

        // If it does contain data
        assert clipboard != null;
        if (!clipboard.hasPrimaryClip()) {
            // Ignore
        } else if (!(Objects.requireNonNull(clipboard.getPrimaryClipDescription()).hasMimeType(MIMETYPE_TEXT_PLAIN))) {
            // Ignore, since the clipboard has data but it is not plain text
        } else {
            ClipData.Item item = Objects.requireNonNull(clipboard.getPrimaryClip()).getItemAt(0);
            pasteData = item.getText().toString();
        }

        return pasteData;
    }

    static Bitmap getBitmapFromVectorDrawable(Context context, int drawableId) {
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

    /**
     * Try to parse string input into integer value, or return default value
     * @param input string to be parsed as integer
     * @param defaultValue value will be returned if input parsing fails
     * @return
     */
    static int parseIntOrDefault(String input, int defaultValue) {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }
}