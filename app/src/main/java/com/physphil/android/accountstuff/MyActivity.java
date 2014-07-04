package com.physphil.android.accountstuff;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;

import java.io.IOException;


public class MyActivity extends ActionBarActivity {

    private AccountManager mAccountManager;
    private Account[] accounts;
    private TextView mTv;
    static final int REQUEST_CODE_PICK_ACCOUNT = 1000;
    static final int REQUEST_CODE_RECOVER_FROM_AUTH_ERROR = 1001;
    static final int REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR = 1002;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        mTv = (TextView) findViewById(R.id.textview);

        String[] names = getAccountNames();

        StringBuilder sb = new StringBuilder();
        for(String name : names){
            sb.append(name + "\n");
        }

        mTv.setText(sb.toString());

        accounts = getAccounts();
        getToken(accounts[0]);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("PS", "in oAR, requestCode = " + requestCode + ", result code = " + resultCode);
        if(requestCode == REQUEST_CODE_RECOVER_FROM_AUTH_ERROR){

            if(resultCode == RESULT_OK){
                Log.d("PS", "result ok");
                getToken(accounts[0]);
            }
            else if(resultCode == RESULT_CANCELED){
                Log.d("PS", "result canceled");
            }
            else{
                Log.d("PS", "result = " + resultCode);
            }
        }
    }

    private Account[] getAccounts(){
        mAccountManager = AccountManager.get(this);
        Account[] accounts = mAccountManager.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
        return accounts;
    }

    private String[] getAccountNames(){
        mAccountManager = AccountManager.get(this);
        Account[] accounts = mAccountManager.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
        String[] names = new String[accounts.length];
        for(int i = 0; i < names.length; i++){
            names[i] = accounts[i].name;
        }

        return names;
    }

    public Bundle getAuthToken(boolean invalidate)
            throws AuthenticatorException, OperationCanceledException, IOException {

        Bundle authTokenBundle = new Bundle();
        AccountManager am = AccountManager.get(this);
        Account[] accounts = am.getAccountsByType("com.google");

        if (accounts.length > 0) {
                AccountManagerFuture<Bundle> accountManagerFuture;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
                    accountManagerFuture = am.getAuthToken(
                            accounts[0], "android", null, false, null, null);
                }
                else {
                    accountManagerFuture = am.getAuthToken(
                            accounts[0], "android", false, null, null);
                }

                authTokenBundle = accountManagerFuture.getResult();
//                token = authTokenBundle.getString(AccountManager.KEY_AUTHTOKEN);
            }

        return authTokenBundle;
    }

    private void handleException(TokenResult result){

        if(result.getException() instanceof UserRecoverableAuthException){

            // Start intent to get authorization
            Intent i = ((UserRecoverableAuthException)result.getException()).getIntent();
            startActivityForResult(i, REQUEST_CODE_RECOVER_FROM_PLAY_SERVICES_ERROR);
        }
        else{
            mTv.setText(result.getException().toString());
        }
    }

    @TargetApi(14)
    private void getToken(Account account){

        mAccountManager.getAuthToken(account, "android", null, false, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> future) {

                try {
                    Bundle bundle = future.getResult();

                    if(bundle.containsKey(AccountManager.KEY_INTENT)){
                        Log.d("PS", "intent returned");
                        Intent i = bundle.getParcelable(AccountManager.KEY_INTENT);
                        i.setFlags(i.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivityForResult(i, REQUEST_CODE_RECOVER_FROM_AUTH_ERROR);
                    }
                    else if(bundle.containsKey(AccountManager.KEY_AUTHTOKEN)){
                        Log.d("PS", "token returned");
                        String s = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                        mTv.setText(s);
                    }
                    else{
                        Log.d("PS", "other problem");
                    }
                }
                catch (OperationCanceledException e) {
                    Log.d("PS", e.toString());
                    e.printStackTrace();
                }
                catch (IOException e) {
                    Log.d("PS", e.toString());
                    e.printStackTrace();
                }
                catch (AuthenticatorException e) {
                    Log.d("PS", e.toString());
                    e.printStackTrace();
                }
            }
        }, null);
    }

    @TargetApi(14)
    private class TokenTask extends AsyncTask<Account, Void, TokenResult>{

        private Activity activity;

        public TokenTask(Activity activity){
            this.activity = activity;
        }

        @Override
        protected TokenResult doInBackground(Account... accounts) {

            TokenResult result;
//            try {
//                Bundle tokenBundle = getAuthToken(false);
//                Log.d("PS", "has token - " + tokenBundle.containsKey(AccountManager.KEY_AUTHTOKEN));
//                Log.d("PS", "has intent - " + tokenBundle.containsKey(AccountManager.KEY_INTENT));

                mAccountManager.getAuthToken(accounts[0], "android", null, false, new AccountManagerCallback<Bundle>() {
                    @Override
                    public void run(AccountManagerFuture<Bundle> future)
                    {
                        try {
                            Bundle bundle = future.getResult();
                        }
                        catch (OperationCanceledException e) {
                            e.printStackTrace();
//                            result = new TokenResult(e);
                        }
                        catch (IOException e) {
                            e.printStackTrace();

                        }
                        catch (AuthenticatorException e) {
                            e.printStackTrace();

                        }
                    }
                }, null);

//            }
//            catch (IOException e) {
//                e.printStackTrace();
//                Log.e("PS", e.toString());
//                return new TokenResult(e);
//            }
//            catch (AuthenticatorException e) {
//                e.printStackTrace();
//                Log.e("PS", e.toString());
//                return new TokenResult(e);
//            }
//            catch (OperationCanceledException e){
//                Log.e("PS", e.toString());
//                return new TokenResult(e);
//            }
            return null;
        }

        @Override
        protected void onPostExecute(TokenResult result) {

            if(result.isSuccessful()){

                if(result.getTokenInfo().containsKey(AccountManager.KEY_AUTHTOKEN)) {
                    mTv.setText(result.getTokenInfo().getString(AccountManager.KEY_AUTHTOKEN));
                }
                else if(result.getTokenInfo().containsKey(AccountManager.KEY_INTENT)){
                    Intent i = result.getTokenInfo().getParcelable(AccountManager.KEY_INTENT);
                    startActivityForResult(i, REQUEST_CODE_RECOVER_FROM_AUTH_ERROR);
                }
            }
            else{
                handleException(result);
            }
        }
    }

    private class TokenResult{
        private String token;
        private Exception exception;
        private Bundle tokenInfo;

        public TokenResult(String token){
            this.token = token;
        }

        public TokenResult(Bundle tokenInfo){
            this.tokenInfo = tokenInfo;
        }

        public TokenResult(Exception ex){
            this.exception = ex;
        }

        public boolean isSuccessful(){
            return (exception == null);
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public Exception getException() {
            return exception;
        }

        public void setException(Exception exception) {
            this.exception = exception;
        }

        public Bundle getTokenInfo() {
            return tokenInfo;
        }

        public void setTokenInfo(Bundle tokenInfo) {
            this.tokenInfo = tokenInfo;
        }
    }

}
