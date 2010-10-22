/*    
    This file is part of Gmail Notifier.
    Copyright 2010 Jonas Larsson, jonas@hallerud.se

    Gmail Notifier is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Gmail Notifier is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Gmail Notifier. If not, see <http://www.gnu.org/licenses/>.

 */

package com.jonasl.gmailnotifier2;

import java.lang.reflect.Method;

import com.jonasl.gmailnotifier2.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;
import android.widget.CompoundButton.OnCheckedChangeListener;

public class MainActivity extends Activity implements OnClickListener {

    // Activity started and is shown to user
    static boolean sStarted = false;

    // Async request for accounts running in Android < 2.0
    private boolean mWaitingForAccounts = false;

    // Detected accounts
    private String mAccounts[] = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        // Load about page
        WebView browser = (WebView) findViewById(R.id.browser);
        browser.loadUrl("file:///android_asset/about.html");

        boolean isEnabled = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(
                "enabled", true);

        // Register onClick handlers
        Button prefsButton = (Button) findViewById(R.id.prefsButton);
        prefsButton.setOnClickListener(this);

        Button testButton = (Button) findViewById(R.id.testButton);
        testButton.setOnClickListener(this);

        Button donateButton = (Button) findViewById(R.id.donateButton);
        donateButton.setOnClickListener(this);

        CheckBox enableCheckBox = (CheckBox) findViewById(R.id.enableCheckBox);
        enableCheckBox.setChecked(isEnabled);
        enableCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putBoolean(
                        "enabled", isChecked).commit();
                if (isChecked || mAccounts == null || mAccounts.length == 0) {
                    return;
                }
                for (String account : mAccounts) {
                    GmailReceiver.clearNotification(MainActivity.this, account);
                }
            }
        });

        // Load accounts.
        // On Android >= 2.0 we use the AccountManager. Since we want to compile
        // against 1.5 SDK, we use reflection to get the accounts from it.
        // If AccountManager isn't found, we use the embedded framework.jar

        this.mAccounts = null;
        this.mWaitingForAccounts = false;

        try {
            Class<?> cAccMgr = Class.forName("android.accounts.AccountManager");
            Class<?> cAcc = Class.forName("android.accounts.Account");
            Method mGet = cAccMgr.getMethod("get", new Class[] {
                Context.class
            });
            Object accmgr = mGet.invoke(null, this);
            Method mGetAccountsByType = cAccMgr.getMethod("getAccountsByType", new Class[] {
                String.class
            });
            Object[] accs = (Object[]) mGetAccountsByType.invoke(accmgr, new Object[] {
                "com.google"
            });
            mAccounts = new String[accs.length];
            for (int i = 0; i < mAccounts.length; i++) {
                String name = (String) cAcc.getField("name").get(accs[i]);
                mAccounts[i] = name;
            }
        } catch (ClassNotFoundException ex) {
            // Ignore. We find the account below by other means
        } catch (Exception ex) {
            Log.d(GmailReceiver.TAG, ex.toString());
        }

        if (mAccounts == null) {
            // AccountManager not found or failed. Try other method.
            prefsButton.setEnabled(false); // Disable while waiting for result.
            this.mWaitingForAccounts = true;
            try {
                for (Method ele : Class.forName(
                        "com.google.android.googlelogin.GoogleLoginServiceHelper").getMethods()) {
                    if (ele.getName().equals("getAccount")) {
                        this.mWaitingForAccounts = true;
                        ele.invoke(null, this, 1337, false);
                    }
                }
            } catch (Exception ex) {
                this.mWaitingForAccounts = false;
                prefsButton.setEnabled(true);
                Log.d(GmailReceiver.TAG, ex.toString());
            }
        }
    }

    // 1 Select account for settings, 2 select account for test
    @Override
    public Dialog onCreateDialog(int id) {
        if (id == 1 || id == 2) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.selectaccount));
            if (id == 1) {
                builder.setItems(mAccounts, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        showPrefs(mAccounts[item]);
                    }
                });
            } else if (id == 2) {
                builder.setItems(mAccounts, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int item) {
                        performTest(mAccounts[item]);
                    }
                });
            }
            return builder.create();
        }
        return null;
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.donateButton:
                Intent i = new Intent(Intent.ACTION_VIEW, Uri
                        .parse("http://android.hallerud.se/donate.html"));
                startActivity(i);
                break;
            case R.id.prefsButton:
                if (mAccounts == null || mAccounts.length == 0) {
                    showPrefs(null); // Shoudn't happen
                } else if (mAccounts.length == 1) {
                    // No dialog if only one account
                    showPrefs(mAccounts[0]);
                } else {
                    showDialog(1); // Select account for preferences
                }
                break;
            case R.id.testButton:
                if (mAccounts == null || mAccounts.length == 0) {
                    performTest(null); // Shoudn't happen
                } else if (mAccounts.length == 1) {
                    // No dialog if only one account
                    performTest(mAccounts[0]);
                } else
                    showDialog(2);// Select account for test
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1337) {
            // Accounts loaded callback from framework.jar
            mAccounts = data.getExtras().getStringArray("accounts");
            mWaitingForAccounts = false;
            Button prefsButton = (Button) findViewById(R.id.prefsButton);
            prefsButton.setEnabled(true);
        }
    }

    private void showPrefs(String account) {
        PrefsActivity.sStarted = true;
        Intent intent = new Intent(getBaseContext(), PrefsActivity.class);
        if (account != null) {
            intent.putExtra("account", account);
        }
        startActivity(intent);
    }

    private void performTest(String account) {
        String text = "";
        if (GmailReceiver.createNotification(this, account, -1, null, null)) {
            text += getString(R.string.testsuccess);
        } else {
            text += getString(R.string.testfailed);
        }
        if (account != null) {
            text += " for " + account;
        }

        Toast.makeText(getBaseContext(), text, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!this.mWaitingForAccounts) {
            sStarted = false;
            GmailReceiver.exitAsNeeded();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sStarted = true;
    }

    @Override
    public void onStart() {
        super.onStart();
        sStarted = true;
    }

    @Override
    protected void onStop() {
        super.onStop();
        sStarted = false;
        GmailReceiver.exitAsNeeded();
    }
}
