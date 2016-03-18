package com.centralink.account.framework;

/**
 * Created by davidliu on 3/18/15.
 */
public class LoginCredential {
    private boolean success;
    private String session;
    private String token;

    public boolean isSuccess() {
        return success;
    }

    public String getSession() {
        return session;
    }

    public String getToken() {
        return token;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder("LoginCredential: \n").
                append("  success = ").append(success).append("\n").
                append("  session = ").append(session).append("\n").
                append("  token = ").append(token);
        return builder.toString();
    }
}
