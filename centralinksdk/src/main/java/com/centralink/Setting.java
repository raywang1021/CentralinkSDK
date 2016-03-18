package com.centralink;

/**
 * Created by davidliu on 9/28/15.
 */

import android.content.Context;
import android.provider.Settings;

public class Setting {
    public static String getUUID(Context context) {
        String androidId = Settings.Secure.getString(context.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        return androidId;
    }
}
