package com.centralink.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import com.centralink.ipcammanager.framework.models.IPCamera;
import com.crashlytics.android.Crashlytics;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * Created by davidliu on 12/9/14.
 */
public class Utils {
    private static final String TAG = "Utils";

    public static boolean isActivityRunning(Activity activity) {
        return activity != null && !activity.isFinishing();
    }

    public static void dismissDialogSafely(Dialog dialog, Activity activity) {
        if (dialog != null && dialog.isShowing() && isActivityRunning(activity)) {
            try {
                dialog.dismiss();
            } catch (IllegalArgumentException ex) {
                // java.lang.IllegalArgumentException: View not attached to window manager
            } catch (NullPointerException ex) {
                // NullPointerException
                // at android.widget.PopupWindow$PopupViewContainer.dispatchKeyEvent
            }
        }
    }

    public static void closeStreamSafely(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException e) {
            }
        }
    }

    /**
     * Gets orientation of the image in MediaStore.
     * If the information is not available in MediaStore, gets from Exif information.
     * Possible values: 0, 90, 180, 270.
     * Returns 0 if can't get the information.
     */
    public static int getImageOrientation(Context context, Uri photoUri) {
        Cursor cursor = context.getContentResolver().query(photoUri,
                new String[]{MediaStore.Images.ImageColumns.ORIENTATION},
                null, null, null);

        try {
            if (cursor == null || cursor.getCount() != 1 || !cursor.moveToFirst()) {
                // Can't get info from this cursor, get from exif information
                return getExifOrientation(photoUri.getPath());
            }
            return cursor.getInt(0);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Gets orientation of the image from Exif information.
     * Possible values: 0, 90, 180, 270.
     * Returns 0 if can't get the information.
     */
    public static int getExifOrientation(String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return 0;
        }

        try {
            ExifInterface exif = new ExifInterface(filePath);
            int exifOrientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);

            switch (exifOrientation) {
                case ExifInterface.ORIENTATION_NORMAL:
                    return 0;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return 0;
            }
        } catch (IOException exc) {
            return 0;
        }
    }

    /**
     * Rotates a bitmap by the specified degrees.
     *
     * @param source
     *         the source bitmap
     * @param degrees
     *         degrees for the source bitmap to be rotated
     *
     * @return the rotated bitmap or source bitmap when degrees is 0.f.
     * @throws Exception
     */
    public static Bitmap rotateBitmap(Bitmap source, float degrees) throws IOException {
        if (degrees == 0.f) {
            return source;
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(degrees);
        try {
            Bitmap processedBitmap = Bitmap.createBitmap(
                    source,
                    0,
                    0,
                    source.getWidth(),
                    source.getHeight(),
                    matrix,
                    true);
            return processedBitmap;
        } catch (OutOfMemoryError e) {
            throw new IOException("Memory not able to be allocated.");
        }
    }

    /**
     * Util method to get hash value for authentication on each API call
     * @param token
     * @param baseString
     * @return
     */
    public static String getHmacSha1Digest(String token, String baseString) {
        String retVal = "";
        String key = token;
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            SecretKeySpec secret = new SecretKeySpec(key.getBytes("UTF-8"), mac.getAlgorithm());
            mac.init(secret);
            byte[] digest = mac.doFinal(baseString.getBytes());

            retVal = byteArrayToHexString(digest);
        } catch (Exception e) {
            Crashlytics.logException(e);
            Log.e(TAG, "getHmacSha1Digest error: " + e.getMessage());
        }
        return retVal;
    }

    private static String byteArrayToHexString(byte[] array) {
        StringBuffer hexString = new StringBuffer();
        for (byte b : array) {
            int intVal = b & 0xff;
            if (intVal < 0x10)
                hexString.append("0");
            hexString.append(Integer.toHexString(intVal));
        }
        return hexString.toString();
    }


    /***
     * Android L (lollipop, API 21) introduced a new problem when trying to invoke implicit intent,
     * "java.lang.IllegalArgumentException: Service Intent must be explicit"
     *
     * If you are using an implicit intent, and know only 1 target would answer this intent,
     * This method will help you turn the implicit intent into the explicit form.
     *
     * Inspired from SO answer: http://stackoverflow.com/a/26318757/1446466
     * @param context
     * @param implicitIntent - The original implicit intent
     * @return Explicit Intent created from the implicit original intent
     */
    public static Intent createExplicitFromImplicitIntent(Context context, Intent implicitIntent) {
        // Retrieve all services that can match the given intent
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> resolveInfo = pm.queryIntentServices(implicitIntent, 0);

        // Make sure only one match was found
        if (resolveInfo == null || resolveInfo.size() != 1) {
            return null;
        }

        // Get component info and create ComponentName
        ResolveInfo serviceInfo = resolveInfo.get(0);
        String packageName = serviceInfo.serviceInfo.packageName;
        String className = serviceInfo.serviceInfo.name;
        ComponentName component = new ComponentName(packageName, className);

        // Create a new intent. Use the old one for extras and such reuse
        Intent explicitIntent = new Intent(implicitIntent);

        // Set the component to be explicit
        explicitIntent.setComponent(component);

        return explicitIntent;
    }

    public static String getFixedIPcamStreamUrl(IPCamera camera) {
        if (camera == null) {
            return "";
        }
        String streamUrl = camera.getStreamUri();
        String username = camera.getUsername();
        String password = camera.getPassword();

        // Fix url protocol and remove unnecessary port
        streamUrl = streamUrl.replace("http", "rtsp");
        streamUrl = streamUrl.replace(":80", "");

        // Add user credential if any
        if (!TextUtils.isEmpty(username) && !TextUtils.isEmpty(password)) {
            StringBuilder sb = new StringBuilder("://");
            try {
                sb.append(URLEncoder.encode(username, "UTF-8")).append(":").append(URLEncoder.encode(password, "UTF-8")).append("@");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            streamUrl = streamUrl.replace("://", sb.toString());
        }

        return streamUrl;
    }
}
