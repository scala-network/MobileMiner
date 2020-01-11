package scala.androidminer;
import java.util.regex.*;

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

}
