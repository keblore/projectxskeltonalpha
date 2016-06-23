package com.kerblore.project_xskeltonalpha;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BootCompleteHandler  extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Log.i("Boot Complete Handler", "Received successfully");
    }

}
