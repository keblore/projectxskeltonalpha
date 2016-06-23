package com.kerblore.project_xskeltonalpha;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

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
import java.util.Properties;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class MailerService extends IntentService
{

    private GoogleAccountCredential _mailAccountCredential;
    private Gmail _mailService;

    private static final String[] SCOPES = {GmailScopes.GMAIL_COMPOSE};

    private String _incomingNumber = "xxxxxxx";

    private boolean _mailSent = false;

    public MailerService()
    {
        super("mailer_service");
    }


    @Override
    protected void onHandleIntent(Intent intent)
    {
        Bundle extras = intent.getExtras();
        _incomingNumber = extras.getString(getString(R.string.res_incoming_num));

        final SharedPreferences credentials = getSharedPreferences(getString(R.string.account_details), Context.MODE_PRIVATE);
        _mailAccountCredential = GoogleAccountCredential.usingOAuth2(getApplicationContext(), Arrays.asList(SCOPES)).setBackOff(new ExponentialBackOff());
        _mailAccountCredential.setSelectedAccountName(credentials.getString(getString(R.string.res_account_name),""));
        HttpTransport transport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        _mailService = new Gmail.Builder(transport, jsonFactory, _mailAccountCredential)
                .setApplicationName(getString(R.string.app_name))
                .build();

        new Thread(new Runnable()
        {
            public void run()
            {
                try
                {
                    String trackerMailId = getSharedPreferences(getString(R.string.feature_state),Context.MODE_PRIVATE).getString(getString(R.string.res_account_name),"");
                    MimeMessage mimemsg = createEmail(trackerMailId, _mailAccountCredential.getSelectedAccount().name, "Missed Call Alert - "+_incomingNumber, "prototype success");

                    try
                    {
                        sendMessage(_mailService, mimemsg);
                    }
                    catch (MessagingException me)
                    {
                        Log.e("send mail error",me.getMessage());
                    }
                    catch (UserRecoverableAuthIOException urauthioe)
                    {
                        Log.e("test urauth","test....");
                    }
                    catch (IOException ioe)
                    {
                        Log.e("send mail IO error", "problem");
                    }
                }
                catch(MessagingException e)
                {
                    Log.e("create mail error",e.getMessage());
                }
            }
        }).start();

        while(!_mailSent);
    }

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
        _mailSent = true;

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


    @Override
    public void onDestroy()
    {
        _mailAccountCredential = null;
        _mailService = null;
        super.onDestroy();
    }
}
