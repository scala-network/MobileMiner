package io.scalaproject.androidminer;
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

}
