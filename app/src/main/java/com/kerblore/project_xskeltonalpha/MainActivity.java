package com.kerblore.project_xskeltonalpha;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.ExponentialBackOff;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Message;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import pub.devrel.easypermissions.AfterPermissionGranted;
import pub.devrel.easypermissions.EasyPermissions;

public class MainActivity extends AppCompatActivity implements EasyPermissions.PermissionCallbacks
{

    private GoogleAccountCredential _mailAccountCredential;
    private Gmail _mailService;
    static final int REQUEST_ACCOUNT_PICKER = 1000;
    static final int REQUEST_AUTHORIZATION = 1001;
    static final int REQUEST_GOOGLE_PLAY_SERVICES = 1002;
    static final int REQUEST_PERMISSION_GET_ACCOUNTS = 1003;

    private static final String[] SCOPES = {GmailScopes.GMAIL_COMPOSE};

    private Button btnStart,btnStop;
    private EditText eTxtEmail;

    private SharedPreferences _featureState;
    private boolean _isFeatureEnabled = false;
    private String _trackingMailId = "";

//    private boolean _insideSendMailFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeMailFunctionalities();
        initializeViews();
    }

    private void initializeViews()
    {
        btnStart=(Button)findViewById(R.id.btnStart);
        btnStop =(Button)findViewById(R.id.btnStop);
        eTxtEmail=(EditText) findViewById(R.id.eTxtEmail);

        _featureState = getSharedPreferences(getString(R.string.feature_state),Context.MODE_PRIVATE);
        _isFeatureEnabled = _featureState.getBoolean(getString(R.string.res_enabled),false);
        _trackingMailId = _featureState.getString(getString(R.string.res_account_name),"Email Id to which calls should be tracked");

        btnStart.setEnabled(!_isFeatureEnabled);
        btnStop.setEnabled(_isFeatureEnabled);
        eTxtEmail.setEnabled(!_isFeatureEnabled);
        eTxtEmail.setText(_trackingMailId);

        btnStart.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {

                boolean isValidEmail = Patterns.EMAIL_ADDRESS.matcher(eTxtEmail.getText()).matches();
                if(isValidEmail)
                {
                    Toast.makeText(MainActivity.this, eTxtEmail.getText().toString()+"\nTracking Started", Toast.LENGTH_SHORT).show();
                    eTxtEmail.setEnabled(false);
                    btnStart.setEnabled(false);
                    btnStop.setEnabled(true);
                    _isFeatureEnabled = true;
                    _trackingMailId = eTxtEmail.getText().toString();

                    SharedPreferences.Editor editor = _featureState.edit();
                    editor.putString(getString(R.string.res_account_name),_trackingMailId);
                    editor.putBoolean(getString(R.string.res_enabled),true);
                    editor.apply();

//                    sendMail();
                    new Thread(new Runnable()
                    {
                        public void run()
                        {
                            try
                            {
                                String trackerMailId = getSharedPreferences(getString(R.string.feature_state),Context.MODE_PRIVATE).getString(getString(R.string.res_account_name),"");
                                MimeMessage mimemsg = createEmail(trackerMailId, _mailAccountCredential.getSelectedAccount().name, "Missed Call Alert App Activated", "Activation Success");

                                try
                                {
                                    sendMessage(_mailService, mimemsg);

                                }
                                catch (SecurityException se)
                                {
                                    Log.e("send mail se",se.getMessage());
                                }
                                catch (MessagingException me)
                                {
                                    Log.e("send mail error",me.getCause().getMessage());
                                }
                                catch (UserRecoverableAuthIOException urauthioe)
                                {
                                    startActivityForResult(urauthioe.getIntent(), REQUEST_AUTHORIZATION);
                                }
                                catch (IOException ioe)
                                {
                                    Log.e("send mail IO error", ioe.getCause().getMessage());
                                }
                            }
                            catch(MessagingException me2)
                            {
                                Log.e("create mail error",me2.getCause().getMessage());
                            }
                        }
                    }).start();

                }
                else
                {
                    Toast.makeText(MainActivity.this,"Invalid Email Address",Toast.LENGTH_SHORT).show();
                    eTxtEmail.setText(_trackingMailId);
                }
            }
        });

        btnStop.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Toast.makeText(MainActivity.this, "Stopped", Toast.LENGTH_SHORT).show();
                eTxtEmail.setEnabled(true);
                btnStart.setEnabled(true);
                btnStop.setEnabled(false);
                _isFeatureEnabled = false;

                SharedPreferences.Editor editor = _featureState.edit();
                editor.putBoolean(getString(R.string.res_enabled),false);
                editor.apply();
            }
        });

    }

/*
    private void sendMail()
    {
        _insideSendMailFlag = true;
        new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    String trackerMailId = getSharedPreferences(getString(R.string.feature_state),Context.MODE_PRIVATE).getString(getString(R.string.res_account_name),"");
                    MimeMessage mimemsg = createEmail(trackerMailId, _mailAccountCredential.getSelectedAccount().name, "Misscall Alert App Activated", "Activation Success");

                    try
                    {
                        sendMessage(_mailService, mimemsg);

                    }
                    catch (SecurityException se)
                    {
                        Log.e("send mail se",se.getMessage());
                    }
                    catch (MessagingException me)
                    {
                        Log.e("send mail error",me.getCause().getMessage());
                    }
                    catch (UserRecoverableAuthIOException urauthioe)
                    {
                        startActivityForResult(urauthioe.getIntent(), REQUEST_AUTHORIZATION);
                    }
                    catch (IOException ioe)
                    {
                        Log.e("send mail IO error", ioe.getCause().getMessage());
                    }
                }
                catch(MessagingException me2)
                {
                    Log.e("create mail error",me2.getCause().getMessage());
                }
            }
        }).start();
    }

*/
    private MimeMessage createEmail(String to, String from, String subject, String bodyText) throws MessagingException
    {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(from));
        email.addRecipient(javax.mail.Message.RecipientType.TO,new InternetAddress(to));
        email.setSubject(subject);
        MimeBodyPart bodyPart = new MimeBodyPart();
        bodyPart.setText(bodyText);
        MimeMultipart multipart = new MimeMultipart();
        multipart.addBodyPart(bodyPart);
        email.setContent(multipart);
        return email;
    }

    private void sendMessage(Gmail service, MimeMessage email) throws MessagingException, IOException
    {
        String userId = "me";
        Message message = createMessageWithEmail(email);
        message = service.users().messages().send(userId, message).execute();
    }

    private Message createMessageWithEmail(MimeMessage email) throws MessagingException, IOException
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        email.writeTo(bytes);
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes.toByteArray());
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    private void initializeMailFunctionalities()
    {
        _mailAccountCredential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), Arrays.asList(SCOPES)).setBackOff(new ExponentialBackOff());
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        getResultsFromApi();
    }

    private void getResultsFromApi()
    {
        if (!isGooglePlayServicesAvailable())
        {
            acquireGooglePlayServices();
        }
        else if (_mailAccountCredential.getSelectedAccountName() == null)
        {
            chooseAccount();
        }
        else if (!isDeviceOnline())
        {
           Log.e("no net","No network connection available.");
        }
        else
        {
            HttpTransport transport = AndroidHttp.newCompatibleTransport();
            JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
            _mailService = new Gmail.Builder(transport, jsonFactory, _mailAccountCredential)
                    .setApplicationName(getString(R.string.app_name))
                    .build();

        }
    }

    private boolean isGooglePlayServicesAvailable()
    {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
        return connectionStatusCode == ConnectionResult.SUCCESS;
    }

    private boolean isDeviceOnline()
    {
        ConnectivityManager connMgr = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }

    private void acquireGooglePlayServices()
    {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        final int connectionStatusCode = apiAvailability.isGooglePlayServicesAvailable(this);
        if (apiAvailability.isUserResolvableError(connectionStatusCode))
        {
            showGooglePlayServicesAvailabilityErrorDialog(connectionStatusCode);
        }
    }

    @AfterPermissionGranted(REQUEST_PERMISSION_GET_ACCOUNTS)
    private void chooseAccount()
    {
        if (EasyPermissions.hasPermissions(this, Manifest.permission.GET_ACCOUNTS))
        {
            String accountName = getSharedPreferences(getString(R.string.account_details),Context.MODE_PRIVATE).getString(getString(R.string.res_account_name), null);
            if (accountName != null)
            {
                _mailAccountCredential.setSelectedAccountName(accountName);
                getResultsFromApi();
            } else
            {
                // Start a dialog from which the user can choose an account
                startActivityForResult(_mailAccountCredential.newChooseAccountIntent(),REQUEST_ACCOUNT_PICKER);
            }
        } else
        {
            // Request the GET_ACCOUNTS permission via a user dialog
            EasyPermissions.requestPermissions(this,"This app needs to access your Google account (via Contacts).",REQUEST_PERMISSION_GET_ACCOUNTS,Manifest.permission.GET_ACCOUNTS);
        }
    }

    void showGooglePlayServicesAvailabilityErrorDialog(final int connectionStatusCode)
    {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        Dialog dialog = apiAvailability.getErrorDialog(MainActivity.this,connectionStatusCode,REQUEST_GOOGLE_PLAY_SERVICES);
        dialog.show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode)
        {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != RESULT_OK)
                {
                    System.out.println("This app requires Google Play Services. Please install Google Play Services on your device and relaunch this app.");
                } else
                {
                    getResultsFromApi();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
                if (resultCode == RESULT_OK && data != null && data.getExtras() != null)
                {
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null)
                    {
                        SharedPreferences settings = getSharedPreferences(getString(R.string.account_details),Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString(getString(R.string.res_account_name), accountName);
                        editor.apply();
                        _mailAccountCredential.setSelectedAccountName(accountName);
                        getResultsFromApi();
                    }
                }
                break;
            case REQUEST_AUTHORIZATION:
                if (resultCode == RESULT_OK)
                {
                    getResultsFromApi();
                    /*
                    if(_insideSendMailFlag)
                    {
                        sendMail();
                    }
                    else
                    {
                        getResultsFromApi();
                    }*/
                }
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this);
    }

    @Override
    public void onPermissionsGranted(int requestCode, List<String> list)
    {
        // Do nothing.
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms)
    {

    }
}
