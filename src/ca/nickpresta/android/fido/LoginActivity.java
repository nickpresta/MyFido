package ca.nickpresta.android.fido;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.NameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class LoginActivity extends Activity {
	private static final String TAG = "LoginActivity";
	
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // Values for email and password at the time of the login attempt.
    private String mPhoneNumber;
    private String mPassword;

    // UI references.
    private EditText mPhoneNumberView;
    private EditText mPasswordView;
    private View mLoginFormView;
    private View mLoginStatusView;
    private TextView mLoginStatusMessageView;
    private TextView mForgotPasswordView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_login);
        
        mPhoneNumberView = (EditText) findViewById(R.id.phoneNumber);

        // Set up the login form.
        mPasswordView = (EditText) findViewById(R.id.password);
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });
        
        mForgotPasswordView = (TextView) findViewById(R.id.forgot_password);
        mForgotPasswordView.setMovementMethod(LinkMovementMethod.getInstance());

        mLoginFormView = findViewById(R.id.login_form);
        mLoginStatusView = findViewById(R.id.login_status);
        mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);

        findViewById(R.id.log_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });
    }

    /**
     * Attempts to log in the account specified by the login form.
     * If there are form errors (invalid phone number, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mPhoneNumberView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        mPhoneNumber = mPhoneNumberView.getText().toString();
        mPassword = mPasswordView.getText().toString();

        boolean error = false;
        String errorString = null;
        View focusView = null;

        // Check for a valid password.
        if (TextUtils.isEmpty(mPassword)) {
        	errorString = getString(R.string.error_field_required);
            focusView = mPasswordView;
            error = true;
        } else if (mPassword.length() < 1) {
            errorString = getString(R.string.error_invalid_password);
            focusView = mPasswordView;
            error = true;
        }

        // Check for a valid phone number.
        mPhoneNumber = mPhoneNumber.replaceAll("[^0-9]+", "");
        if (TextUtils.isEmpty(mPhoneNumber)) {
            errorString = getString(R.string.error_field_required);
            focusView = mPhoneNumberView;
            error = true;
        } else if (mPhoneNumber.length() != 10) {
            errorString = getString(R.string.error_invalid_phone_number);
            focusView = mPhoneNumberView;
            error = true;
        }

        if (error) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
            ((TextView) focusView).setError(errorString);
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            mLoginStatusMessageView.setText(R.string.login_progress_logging_in);
            showProgress(true);
            mAuthTask = new UserLoginTask();
            mAuthTask.execute((Void) null);
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    private void showProgress(final boolean show) {
		int shortAnimTime = getResources().getInteger(
				android.R.integer.config_shortAnimTime);

		mLoginStatusView.setVisibility(View.VISIBLE);
		mLoginStatusView.animate().setDuration(shortAnimTime)
				.alpha(show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationEnd(Animator animation) {
						mLoginStatusView.setVisibility(show ? View.VISIBLE
								: View.GONE);
					}
				});

		mLoginFormView.setVisibility(View.VISIBLE);
		mLoginFormView.animate().setDuration(shortAnimTime).alpha(show ? 0 : 1)
				.setListener(new AnimatorListenerAdapter() {
					@Override
					public void onAnimationEnd(Animator animation) {
						mLoginFormView.setVisibility(show ? View.GONE
								: View.VISIBLE);
					}
				});
    }

    /**
     * Represents an asynchronous login task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
        	DefaultHttpClient client = new DefaultHttpClient();
        	// Create a local instance of cookie store
            BasicCookieStore cookieStore = new BasicCookieStore();
            // Create local HTTP context
            HttpContext localContext = new BasicHttpContext();
            // Bind custom cookie store to the local context
            localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
            
            HttpPost httpPost = new HttpPost(getString(R.string.login_url));
            
            List<NameValuePair> postData = new ArrayList<NameValuePair>(2);
            postData.add(new BasicNameValuePair("FidoSignIn_1_1{actionForm.fidonumber}", mPhoneNumber));
            postData.add(new BasicNameValuePair("FidoSignIn_1_1{actionForm.password}", mPassword));
            postData.add(new BasicNameValuePair("FidoSignIn_1_1{actionForm.loginAsGAM}", "false"));
            
            try {
            	httpPost.setEntity(new UrlEncodedFormEntity(postData));
				HttpResponse response = client.execute(httpPost, localContext);
				
				if (response.getStatusLine().getStatusCode() != 200) {
					return false;
				}
				
				for (Cookie cookie : cookieStore.getCookies()) {
					if (cookie.getName().equals("lithiumSSO:fido")) {
						return true;
					}
				}				
			} catch (ClientProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            
            return false;
        }

        @Override
        protected void onPostExecute(final Boolean success) {
            mAuthTask = null;
            showProgress(false);

            if (success) {
            	InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            	imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
            	Toast.makeText(getApplicationContext(), "Logged in successfully!", Toast.LENGTH_LONG).show();
                //finish();
            } else {
            	mPasswordView.requestFocus();
                mPasswordView.setError(getString(R.string.error_incorrect_password));
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}
