package com.kerblore.project_xskeltonalpha;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

public class IncomingCallInterceptor extends BroadcastReceiver
{

    private static String _prevState = "";

    @Override
    public void onReceive(Context context, Intent intent)
    {
        String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

        if(_prevState.equals(state))
        {
            return;
        }
        _prevState = state;

        boolean isFeatureEnabled = context.getSharedPreferences(context.getString(R.string.feature_state),Context.MODE_PRIVATE).getBoolean(context.getString(R.string.res_enabled),false);

        if(!isFeatureEnabled)
        {
            return;
        }

        if (TelephonyManager.EXTRA_STATE_RINGING.equals(state))
        {

            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

            /*
            * getSharedPreferences of contacts...
            * if(size != 0)
            * {
            *   if(!contains)
            *   {
            *       return;
            *   }
            * }
            **/

            Intent mailerService = new Intent(context,MailerService.class);
            mailerService.putExtra(context.getString(R.string.res_incoming_num), incomingNumber);
            context.startService(mailerService);

            Log.i("bcr","received successfully");
        }
    }

}