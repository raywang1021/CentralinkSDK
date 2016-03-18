package com.centralink.account.framework;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.gson.Gson;

/**
 * Created by davidliu on 3/30/15.
 */
public class CentralinkAccounts {

    private static final String TAG = CentralinkAccounts.class.getSimpleName();

    private static final String KEY_AUTH_SESSION = "session";
    private static final String KEY_AUTH_TOKEN = "token";
    private static boolean isAuthTokenGotten = false;

    public static void getAuthNew(final Context cxt) {
        Account[] accArray = AccountManager.get(cxt).getAccountsByType(AccountContract.ACCOUNT_TYPE);
        Account acc = null;

        Log.d(TAG, "getAuthNew: ready to add callback");

        if (accArray == null || accArray.length == 0) {
            Log.d(TAG, "getAuthNew: avoid adding callback due to no feasible accounts");
            return;
        }

        acc = accArray[0];

        AccountManager.get(cxt).getAuthToken(acc, AccountContract.AUTHTOKEN_TYPE_FULL_ACCESS, null, true, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> future) {
                try {
                    Log.d(TAG, "getAuthNew: callback: start");
                    Bundle bundle = future.getResult();
                    Log.d(TAG, "getAuthNew: callback: successfully getResult");
                    final String authtoken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                    LoginCredential credential = CentralinkAccounts.parseLoginResponse(authtoken);
                    Log.d(TAG, "getAuthNew: GetTokenForAccount Bundle is " + credential);

                    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(cxt.getApplicationContext());
                    pref.edit().putString(KEY_AUTH_SESSION, credential.getSession()).apply();
                    Log.d(TAG, "getAuthNew: add preference: " + KEY_AUTH_SESSION + ": " + credential.getSession());
                    pref.edit().putString(KEY_AUTH_TOKEN, credential.getToken()).apply();
                    Log.d(TAG, "getAuthNew: add preference: " + KEY_AUTH_TOKEN + ": " + credential.getToken());

                    isAuthTokenGotten = true;
                    Log.d(TAG, "getAuthNew: callback: end");

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, new Handler(Looper.getMainLooper()));

        Log.d(TAG, "getAuthNew: successfully add callback");
    }

    public static void getAuth(final Activity act) {
        Log.d(TAG, "getAuth: ready to add callback");

        AccountManager.get(act).getAuthTokenByFeatures(AccountContract.ACCOUNT_TYPE,
                AccountContract.AUTHTOKEN_TYPE_FULL_ACCESS, null, act, null, null, new AccountManagerCallback<Bundle>() {
                    @Override
                    public void run(AccountManagerFuture<Bundle> future) {
                        try {
                            Log.d(TAG, "getAuth: callback: start");
                            new Exception().printStackTrace();
                            Bundle bundle = future.getResult();
                            Log.d(TAG, "getAuth: callback: successfully getResult");
                            final String authtoken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                            LoginCredential credential = CentralinkAccounts.parseLoginResponse(authtoken);
                            Log.v(TAG, "getAuth: GetTokenForAccount Bundle is " + credential);

                            SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(act.getApplicationContext());
                            pref.edit().putString(KEY_AUTH_SESSION, credential.getSession()).apply();
                            Log.d(TAG, "getAuth: add preference: " + KEY_AUTH_SESSION + ": " + credential.getSession());
                            pref.edit().putString(KEY_AUTH_TOKEN, credential.getToken()).apply();
                            Log.d(TAG, "getAuth: add preference: " + KEY_AUTH_TOKEN + ": " + credential.getToken());

                            isAuthTokenGotten = true;
                            Log.d(TAG, "getAuth: callback: end");

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }, new Handler(Looper.getMainLooper()));

        Log.d(TAG, "getAuth: successfully add callback");
    }
    public static LoginCredential parseLoginResponse(String response) {
        if (response == null) {
            throw new IllegalArgumentException("input param response cannot be null");
        }
        LoginCredential credential = new Gson().fromJson(response, LoginCredential.class);
        return credential;
    }

    public static String getSession(Context context) {
        if (false == isAuthTokenGotten) {
            getAuthNew(context);
        }
        String ret = PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_AUTH_SESSION, "");

        if (ret.equals(""))
            Log.d(TAG, "getSession: " + KEY_AUTH_SESSION + " in preference is empty");

        return ret;
    }

    public static String getToken(Context context) {
        if (false == isAuthTokenGotten) {
            getAuthNew(context);
        }

        String ret = PreferenceManager.getDefaultSharedPreferences(context).getString(KEY_AUTH_TOKEN, "");

        if (ret.equals(""))
            Log.d(TAG, "getToken: " + KEY_AUTH_TOKEN + " in preference is empty");

        return ret;
    }
}
