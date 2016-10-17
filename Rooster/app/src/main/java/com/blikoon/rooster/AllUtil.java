package com.blikoon.rooster;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by neobyte on 10/15/2016.
 */

public class AllUtil {

    public static boolean isValidId(String jid){
        final String EMAIL_PATTERN = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@"
                + "[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
        Pattern pattern = Pattern.compile(jid);
        Matcher matcher = pattern.matcher(EMAIL_PATTERN);
        return matcher.matches();
    }
}
