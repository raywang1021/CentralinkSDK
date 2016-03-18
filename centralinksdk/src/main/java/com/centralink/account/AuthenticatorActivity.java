package com.centralink.account;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.centralink.Setting;
import com.centralink.account.framework.AccountContract;
import com.centralink.account.framework.CentralinkAccounts;
import com.centralink.account.framework.LoginCredential;
import com.centralink.utils.Utils;
import com.crashlytics.android.Crashlytics;
import com.centralink.R;
import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.TextHttpResponseHandler;

import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.util.Date;

/**
 * The Authenticator activity.
 * Called by the Authenticator and in charge of identifing the user.
 * It sends back to the Authenticator the result.
 *
 * Created by davidliu on 3/17/15.
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity {

    private static final String TAG = AuthenticatorActivity.class.getSimpleName();

    public final static String ARG_ACCOUNT_TYPE = "ACCOUNT_TYPE";
    public final static String ARG_AUTH_TYPE = "AUTH_TYPE";
    public final static String ARG_ACCOUNT_NAME = "ACCOUNT_NAME";
    public final static String ARG_IS_ADDING_NEW_ACCOUNT = "IS_ADDING_ACCOUNT";

    public static final String KEY_ERROR_MESSAGE = "ERR_MSG";

    public final static String PARAM_USER_PASS = "USER_PASS";

    private final int REQ_SIGNUP = 1;

    private AccountManager mAccountManager;
    private String mAuthTokenType;
    private LoginCredential credential;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        setTitle(R.string.add_your_account);

        mAccountManager = AccountManager.get(getBaseContext());

        String accountName = getIntent().getStringExtra(ARG_ACCOUNT_NAME);
        mAuthTokenType = getIntent().getStringExtra(ARG_AUTH_TYPE);
        if (mAuthTokenType == null)
            mAuthTokenType = AccountContract.AUTHTOKEN_TYPE_FULL_ACCESS;

        if (accountName != null) {
            ((TextView)findViewById(R.id.accountName)).setText(accountName);
        }

        findViewById(R.id.submit).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                submit();
            }
        });
        findViewById(R.id.signUp).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Since there can only be one AuthenticatorActivity, we call the sign up activity, get his results,
                // and return them in setAccountAuthenticatorResult(). See finishLogin().
                Intent signup = new Intent(getBaseContext(), SignUpActivity.class);
                signup.putExtras(getIntent().getExtras());
                startActivityForResult(signup, REQ_SIGNUP);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        // The sign up activity returned that the user has successfully created an account
        if (requestCode == REQ_SIGNUP && resultCode == RESULT_OK) {
            finishLogin(data);
        } else
            super.onActivityResult(requestCode, resultCode, data);
    }

    public void submit() {

        final String userName = ((TextView) findViewById(R.id.accountName)).getText().toString();
        final String userPass = ((TextView) findViewById(R.id.accountPassword)).getText().toString();

        final String accountType = getIntent().getStringExtra(ARG_ACCOUNT_TYPE);

        // Do login
        Toast.makeText(getApplicationContext(), "Logging onto Centralink", Toast.LENGTH_SHORT).show();
        AsyncHttpClient httpClient = new AsyncHttpClient();
        RequestParams requestParams = new RequestParams();
        requestParams.put("username", userName);
        requestParams.put("password", userPass);
        requestParams.put("Content-Type", "application/json");
        requestParams.put("Accept", "application/json");
        httpClient.post("http://dev.centralink.cc:443/login/api/v1/", requestParams, new TextHttpResponseHandler() {
            /**
             * Called when request succeeds
             *
             * @param statusCode     http response status line
             * @param headers        response headers if any
             * @param responseString string response of given charset
             */
            @Override
            public void onSuccess(int statusCode, Header[] headers, String responseString) {
                Toast.makeText(getApplicationContext(), "Login successfully. Updating controller information.", Toast.LENGTH_SHORT).show();
                Bundle data = new Bundle();
                String errorMessage = "";
                if (TextUtils.isEmpty(responseString)) {
                    errorMessage = getString(R.string.login_failed_message);
                }
                credential = CentralinkAccounts.parseLoginResponse(responseString);
                if (credential.isSuccess()) {
                    data.putString(AccountManager.KEY_ACCOUNT_NAME, userName);
                    data.putString(AccountManager.KEY_ACCOUNT_TYPE, accountType);
                    data.putString(AccountManager.KEY_AUTHTOKEN, responseString);
                    data.putString(PARAM_USER_PASS, userPass);
                } else {
                    errorMessage = getString(R.string.login_failed_message);
                }

                if (!TextUtils.isEmpty(errorMessage)) {
                    Toast.makeText(getBaseContext(), errorMessage, Toast.LENGTH_SHORT).show();
                } else {
                    final Intent intent = new Intent();
                    intent.putExtras(data);

                    // Update controller device info
                    AsyncHttpClient httpClient = new AsyncHttpClient();
                    final String curTimeString = DateFormat.getDateTimeInstance().format(new Date());
                    JSONObject jsonObject = new JSONObject();
                    StringEntity entity = null;
                    try {
                        jsonObject.put("session", credential.getSession());
                        jsonObject.put("hash", Utils.getHmacSha1Digest(credential.getToken(), curTimeString));
                        jsonObject.put("t", curTimeString);
                        jsonObject.put("device", getDeviceInfo());
                        entity = new StringEntity(jsonObject.toString());
                        Log.v(TAG, "post enity: " + entity.toString());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }

                    if (entity != null) {
                        Log.e(TAG, "update controller info with - \"device\":" + getDeviceInfo().toString());
                        httpClient.post(getApplicationContext(), "http://dev.centralink.cc:443/api/v1/ctrler/update/", entity, "application/json", new JsonHttpResponseHandler() {

                            @Override
                            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                                super.onSuccess(statusCode, headers, response);
                                finishLogin(intent);
                            }

                            @Override
                            public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                                super.onFailure(statusCode, headers, throwable, errorResponse);
                                Log.e(TAG, "Update controller info is failed - error code: " + statusCode + ", error response: " + errorResponse);
                                Toast.makeText(getApplicationContext(), "Update controller info failed.", Toast.LENGTH_SHORT);
                                Crashlytics.logException(throwable);
                            }
                        });
                    }
                }

            }

            /**
             * Called when request fails
             *
             * @param statusCode     http response status line
             * @param headers        response headers if any
             * @param responseString string response of given charset
             * @param throwable      throwable returned when processing request
             */
            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                throwable.printStackTrace();
                Crashlytics.logException(throwable);
            }
        });
    }

    private void finishLogin(Intent intent) {
        Log.v(TAG, "finishLogin");

        String accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
        String accountPassword = intent.getStringExtra(PARAM_USER_PASS);
        final Account account = new Account(accountName, intent.getStringExtra(AccountManager.KEY_ACCOUNT_TYPE));

        if (getIntent().getBooleanExtra(ARG_IS_ADDING_NEW_ACCOUNT, false)) {
            Log.v(TAG, "addAccountExplicitly");
            String authtoken = intent.getStringExtra(AccountManager.KEY_AUTHTOKEN);
            String authtokenType = mAuthTokenType;

            // Creating the account on the device and setting the auth token we got
            // (Not setting the auth token will cause another call to the server to authenticate the user)
            mAccountManager.addAccountExplicitly(account, accountPassword, null);
            mAccountManager.setAuthToken(account, authtokenType, authtoken);
        } else {
            Log.v(TAG, "setPassword");
            mAccountManager.setPassword(account, accountPassword);
        }

        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }

    private JSONObject getDeviceInfo() {
        JSONObject json = new JSONObject();
        try {
            json.put("uuid", Setting.getUUID(getApplicationContext()));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.v(TAG, "DeviceInfo: " + json);
        return json;
    }

}