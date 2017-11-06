package com.phone.listen;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * Created by popfisher on 2017/11/6.
 */

public class PhoneStateReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            // 去电，可以用定时挂断
        } else {
            //来电
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            Log.d(PhoneListenService.TAG, "PhoneStateReceiver onReceive state: " + state);
            if (state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING)) {
                Log.d(PhoneListenService.TAG, "PhoneStateReceiver onReceive endCall");
                HangUpTelephonyUtil.endCall(context);
            }
        }
    }
}
