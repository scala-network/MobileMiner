package io.scalaproject.androidminer;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.*;
import java.text.DecimalFormat;
import java.text.ParseException;

final class Utils {

    static public boolean verifyAddress(String input) {
        Pattern p = Pattern.compile("^Se[123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]{95}$");
        Matcher m = p.matcher(input.trim());
        boolean b = m.matches();
        if(b) {
            return b;
        }
        Pattern p1 = Pattern.compile("^SE[123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz]{107}$");
        Matcher m2 = p1.matcher(input.trim());
        boolean b2 = m2.matches();
        return b2;
    }

    static public float convertStringToFloat(String sNumber) {
        Float total = Float.valueOf(-1);
        try
        {
            total = Float.valueOf(sNumber);
        }
        catch(NumberFormatException ex)
        {
            DecimalFormat df = new DecimalFormat();
            Number n = null;
            try
            {
                n = df.parse(sNumber);
            }
            catch(ParseException ex2){ }
            if(n != null)
                total = n.floatValue();
        }

        return total;
    }

    static public String getDateTime() {
        Calendar date = Calendar.getInstance(Locale.getDefault());
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        String sDate = dateFormat.format(date.getTime());

        return sDate ;
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
}
