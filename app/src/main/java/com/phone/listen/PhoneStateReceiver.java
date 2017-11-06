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
        String action = intent.getAction();
        Log.d(PhoneListenService.TAG, "PhoneStateReceiver action: " + action);

        String resultData = this.getResultData();
        Log.d(PhoneListenService.TAG, "PhoneStateReceiver getResultData: " + resultData);

        if (action.equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            Log.d(PhoneListenService.TAG, "PhoneStateReceiver onReceive action: ACTION_NEW_OUTGOING_CALL");
            // 去电，可以用定时挂断
            // 双卡的手机可能不走这个Action
            String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            Log.d(PhoneListenService.TAG, "PhoneStateReceiver intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER): " + phoneNumber);
        } else {
            // 来电去电都会走
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            Log.d(PhoneListenService.TAG, "PhoneStateReceiver onReceive state: " + state);

            String extraIncomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
            Log.d(PhoneListenService.TAG, "PhoneStateReceiver onReceive extraIncomingNumber: " + extraIncomingNumber);

            if (state.equalsIgnoreCase(TelephonyManager.EXTRA_STATE_RINGING)) {
                Log.d(PhoneListenService.TAG, "PhoneStateReceiver onReceive endCall");
                HangUpTelephonyUtil.endCall(context);
            }
        }
    }
}
