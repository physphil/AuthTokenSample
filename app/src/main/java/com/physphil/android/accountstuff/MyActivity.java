package com.physphil.android.accountstuff;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.auth.GoogleAuthUtil;

import java.io.IOException;


public class MyActivity extends ActionBarActivity {

    private final String TAG = MyActivity.class.getName();

    private AccountManager mAccountManager;
    private Account[] accounts;
    private TextView mTv;

    static final int REQUEST_CODE_RECOVER_FROM_AUTH_ERROR = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my);
        mTv = (TextView) findViewById(R.id.textview);
        accounts = getAccounts();

        // Display account names on screen
        String[] names = getAccountNames();
        StringBuilder sb = new StringBuilder();
        for(String name : names){
            sb.append(name + "\n");
        }

        mTv.setText(sb.toString());

        // Get token from AccountManager
        getToken(accounts[0]);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == REQUEST_CODE_RECOVER_FROM_AUTH_ERROR){

            if(resultCode == RESULT_OK){

                getToken(accounts[0]);
            }
            else if(resultCode == RESULT_CANCELED){

                finish();
            }
        }
    }

    private Account[] getAccounts(){
        mAccountManager = AccountManager.get(this);
        Account[] accounts = mAccountManager.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
        return accounts;
    }

    private String[] getAccountNames(){

        String[] names = new String[accounts.length];
        for(int i = 0; i < names.length; i++){
            names[i] = accounts[i].name;
        }

        return names;
    }

    private void getToken(Account account){

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            mAccountManager.getAuthToken(account, "android", null, false, mAccountManagerCallback, null);
        }
        else{
            mAccountManager.getAuthToken(account, "android", false, mAccountManagerCallback, null);
        }
    }

    /**
     * Called when result of AccountManagerFuture is available. Contains a bundle with either a
     * token or an intent to launch if the user needs to authorize before getting a token.
     */
    private AccountManagerCallback<Bundle> mAccountManagerCallback =
            new AccountManagerCallback<Bundle>() {

        @Override
        public void run(AccountManagerFuture<Bundle> future) {

            try {
                Bundle bundle = future.getResult();

                // Display token if received, show authorization intent if user needs to act
                if(bundle.containsKey(AccountManager.KEY_AUTHTOKEN)){

                    mTv.setText(bundle.getString(AccountManager.KEY_AUTHTOKEN));
                }
                else if(bundle.containsKey(AccountManager.KEY_INTENT)){

                    // Need to set FLAG_NEW_TASK to ensure result isn't returned prematurely, bug in Android
                    Intent i = bundle.getParcelable(AccountManager.KEY_INTENT);
                    i.setFlags(i.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivityForResult(i, REQUEST_CODE_RECOVER_FROM_AUTH_ERROR);
                }
            }
            catch (OperationCanceledException e) {
                Log.e(TAG, e.toString());
            }
            catch (IOException e) {
                Log.e(TAG, e.toString());
            }
            catch (AuthenticatorException e) {
                Log.e(TAG, e.toString());
            }
        }
    };
}
