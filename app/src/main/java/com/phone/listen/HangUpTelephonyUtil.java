package com.phone.listen;

import android.content.Context;
import android.os.RemoteException;
import android.telephony.TelephonyManager;

import com.android.internal.telephony.ITelephony;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * 封装挂断电话接口
 */

public class HangUpTelephonyUtil {
    public static boolean endCall(Context context) {
        boolean callSuccess = false;
        ITelephony telephonyService = getTelephonyService(context);
        try {
            if (telephonyService != null) {
                callSuccess = telephonyService.endCall();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (Exception e){
            e.printStackTrace();
        }
        if (callSuccess == false) {
            Executor eS = Executors.newSingleThreadExecutor();
            eS.execute(new Runnable() {
                @Override
                public void run() {
                    disconnectCall();
                }
            });
            callSuccess = true;
        }
        return callSuccess;
    }

    private static ITelephony getTelephonyService(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        Class clazz;
        try {
            clazz = Class.forName(telephonyManager.getClass().getName());
            Method method = clazz.getDeclaredMethod("getITelephony");
            method.setAccessible(true);
            return (ITelephony) method.invoke(telephonyManager);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static boolean disconnectCall() {
        Runtime runtime = Runtime.getRuntime();
        try {
            runtime.exec("service call phone 5 \n");
        } catch (Exception exc) {
            exc.printStackTrace();
            return false;
        }
        return true;
    }

    // 使用endCall挂断不了，再使用killCall反射调用再挂一次
    public static boolean killCall(Context context) {
        try {
            // Get the boring old TelephonyManager
            TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            // Get the getITelephony() method
            Class classTelephony = Class.forName(telephonyManager.getClass().getName());
            Method methodGetITelephony = classTelephony.getDeclaredMethod("getITelephony");

            // Ignore that the method is supposed to be private
            methodGetITelephony.setAccessible(true);

            // Invoke getITelephony() to get the ITelephony interface
            Object telephonyInterface = methodGetITelephony.invoke(telephonyManager);

            // Get the endCall method from ITelephony
            Class telephonyInterfaceClass = Class.forName(telephonyInterface.getClass().getName());
            Method methodEndCall = telephonyInterfaceClass.getDeclaredMethod("endCall");

            // Invoke endCall()
            methodEndCall.invoke(telephonyInterface);
        } catch (Exception ex) { // Many things can go wrong with reflection calls
            return false;
        }
        return true;
    }

}
