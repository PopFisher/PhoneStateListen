package com.phone.listen;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyManager;
import android.util.Log;

/**
 * 来去电监听
 */

public class CustomPhoneStateListener extends PhoneStateListener {

    private Context mContext;

    public CustomPhoneStateListener(Context context) {
        mContext = context;
    }

    @Override
    public void onServiceStateChanged(ServiceState serviceState) {
        super.onServiceStateChanged(serviceState);
        Log.d(PhoneListenService.TAG, "CustomPhoneStateListener onServiceStateChanged: " + serviceState);
    }

    @Override
    public void onCallStateChanged(int state, String incomingNumber) {
        Log.d(PhoneListenService.TAG, "CustomPhoneStateListener state: " + state + " incomingNumber: " + incomingNumber);
        switch (state) {
            case TelephonyManager.CALL_STATE_IDLE:      // 电话挂断
                break;
            case TelephonyManager.CALL_STATE_RINGING:   // 电话响铃
                Log.d(PhoneListenService.TAG, "CustomPhoneStateListener onCallStateChanged endCall");
                HangUpTelephonyUtil.endCall(mContext);
                break;
            case TelephonyManager.CALL_STATE_OFFHOOK:    // 来电接通 或者 去电，去电接通  但是没法区分
                break;
        }
    }
}
