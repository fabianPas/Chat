package com.pas.chat;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.os.AsyncTask;

import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.pas.chat.client.Client;

import java.util.HashMap;

import javax.security.auth.login.LoginException;


/**
 * A login screen that offers login via email/password.
 */
public class RegisterActivity extends Activity implements LoaderCallbacks<Cursor> {

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserRegisterTask mRegisterTask = null;
    private ChatApplication mApplication = null;

    // UI references.
    private View mProgressView;
    private View mRegisterFormView;

    private EditText mUsernameView;
    private EditText mEmailView;
    private EditText mPasswordView;
    private EditText mConfirmPasswordView;

    private Client mClient;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mApplication = ((ChatApplication)getApplicationContext());

        mUsernameView = (EditText) findViewById(R.id.username);
        mEmailView = (EditText) findViewById(R.id.email);
        mPasswordView = (EditText) findViewById(R.id.password);
        mConfirmPasswordView = (EditText) findViewById(R.id.password_confirm);

        Button mRegisterButton = (Button) findViewById(R.id.action_register);
        mRegisterButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptRegister();
            }
        });

        mRegisterFormView = findViewById(R.id.register_form);
        mProgressView = findViewById(R.id.register_progress);

    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptRegister() {
        if (mRegisterTask != null) {
            return;
        }

        // Reset errors.
        resetErrors();

        // Store values at the time of the login attempt.
        String username = mUsernameView.getText().toString();
        String email = mEmailView.getText().toString();
        String password = mPasswordView.getText().toString();
        String confirmPassword = mConfirmPasswordView.getText().toString();

        boolean hasError = false;
        View errorView = null;

        /** Move Client to global scope and google check if user exists, to work out something with checking email and username! **/

        if (!TextUtils.isEmpty(username) && !isUsernameValid(password)) {
            mUsernameView.setError(getString(R.string.error_invalid_username));
            errorView = mUsernameView;
            hasError = true;
        }

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            errorView = mPasswordView;
            hasError = true;
        }

        if (!confirmPassword.equals(password)) {
            mConfirmPasswordView.setError(getString(R.string.error_passwords_not_equal));
            errorView = mConfirmPasswordView;
            hasError = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(email)) {
            mEmailView.setError(getString(R.string.error_email_required));
            errorView = mEmailView;
            hasError = true;
        } else if (!isEmailValid(email)) {
            mEmailView.setError(getString(R.string.error_invalid_email));
            errorView = mEmailView;
            hasError = true;
        }

        if (hasError) {
            errorView.requestFocus();
        } else {
            showProgress(true);
            mRegisterTask = new UserRegisterTask(username, email, password, mApplication);
            mRegisterTask.execute((Void) null);
        }
    }

    private void resetErrors()
    {
        mUsernameView.setError(null);
        mEmailView.setError(null);
        mPasswordView.setError(null);
        mConfirmPasswordView.setError(null);
    }

    private boolean isUsernameValid(String username) {
        return (username.length() > 3 && username.length() < 10);
    }

    private boolean isEmailValid(String email) {
        //TODO: Replace this with your own logic
        return email.contains("@");
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    public void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mRegisterFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mRegisterFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mRegisterFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mRegisterFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return null;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {

    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserRegisterTask extends AsyncTask<Void, Void, Boolean> {

        private final String mUsername;
        private final String mEmail;
        private final String mPassword;

        private boolean mUsernameExists = false;
        private boolean mEmailExists = false;

        private ChatApplication mApplication;


        UserRegisterTask(String username, String email, String password, ChatApplication application) {
            mUsername = username;
            mEmail = email;
            mPassword = password;
            mApplication = application;
        }

        @Override
        protected Boolean doInBackground(Void... params) {

            mClient = new Client();
            mClient.connect();

            try {
                mClient.login();
                mUsernameExists = mClient.propertyExists("Username", mUsername);
                mEmailExists = mClient.propertyExists("Email", mEmail);

                if (mUsernameExists || mEmailExists)
                    return false;

            } catch( LoginException e) {
                Log.d("RegisterActivity", e.getMessage());
            }

            try {
                // we create a new connection because we already logged in for the search queries, we can't create an account if we are logged in
                // @TODO check if we can move this temporary variable to the try scope instead
                mClient = new Client();
                mClient.connect();

                HashMap<String, String> attributes = new HashMap<>();
                attributes.put("email", mEmail);
                mClient.createAccount(mUsername, mPassword, attributes);
            } catch (Exception e) {
                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mRegisterTask = null;
            showProgress(false);

            if (success) {
                Log.d("RegisterActivity.java", "Account created");
                Toast toast = Toast.makeText(mApplication, "Account has been created!", Toast.LENGTH_SHORT);
                toast.show();

                Intent intent = new Intent(mApplication, MainActivity.class);
                startActivity(intent);
            } else {
                if (mUsernameExists) {
                    mUsernameView.setError(getString(R.string.error_username_exists));
                    mUsernameView.requestFocus();
                }
                if (mEmailExists) {
                    mEmailView.setError(getString(R.string.error_email_exists));
                    mEmailView.requestFocus();
                }
            }
        }

        @Override
        protected void onCancelled() {
            mRegisterTask = null;
            showProgress(false);
        }
    }
}

