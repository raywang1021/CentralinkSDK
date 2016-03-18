package com.centralink.account;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Created by davidliu on 3/17/15.
 */
public class CentralinkAuthenticatorService extends Service {
    @Override
    public IBinder onBind(Intent intent) {

        CentralinkAuthenticator authenticator = new CentralinkAuthenticator(this);
        return authenticator.getIBinder();
    }
}