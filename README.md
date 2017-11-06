# Android来去电监听，电话挂断

1、监听来电去电有什么用？

2、怎么监听，来电去电监听方式一样吗？

3、实战，有什么需要特别注意地方？

## 一. 监听来电去电能干什么

1、能够对监听到的电话做个标识，告诉用户这个电话是诈骗、推销、广告什么的

2、能够针对那些特殊的电话进行自动挂断，避免打扰到用户


## 二. 来电去电的监听方式（不一样的方式）

### 2.1 来去电监听方式一（PhoneStateListener）

　　来电监听是使用PhoneStateListener类，使用方式是，将PhoneStateListener对象（一般是自己继承PhoneStateListener类完成一些封装）注册到系统电话管理服务中去（TelephonyManager）

　　然后通过PhoneStateListener的回调方法onCallStateChanged(int state, String incomingNumber) 实现来电的监听 （详细实现可以参考后面给出的拓展阅读部分）

#### 注册监听

	private void registerPhoneStateListener() {
	    CustomPhoneStateListener customPhoneStateListener = new CustomPhoneStateListener(this);
	    TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
	    if (telephonyManager != null) {
	        telephonyManager.listen(customPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
	    }
	}
 

#### PhoneStateListener的onCallStateChanged方法监听来电状态

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
	        Log.d(PhoneListenService.TAG, "CustomPhoneStateListener state: " 
	              + state + " incomingNumber: " + incomingNumber);
	        switch (state) {
	            case TelephonyManager.CALL_STATE_IDLE:      // 电话挂断
	                break;
	            case TelephonyManager.CALL_STATE_RINGING:   // 电话响铃
	                Log.d(PhoneListenService.TAG, "CustomPhoneStateListener onCallStateChanged endCall");
	                HangUpTelephonyUtil.endCall(mContext);
	                break;
	            case TelephonyManager.CALL_STATE_OFFHOOK:   // 来电接通 或者 去电  但是没法区分
	                break;
	        }
	    }
	}
 

#### 三种状态源码解释

	/** Device call state: No activity. */
	public static final int CALL_STATE_IDLE = 0;    // 电话挂断
	/** Device call state: Ringing. A new call arrived and is
	 *  ringing or waiting. In the latter case, another call is
	 *  already active. */
	public static final int CALL_STATE_RINGING = 1;    // 来电响铃
	/** Device call state: Off-hook. At least one call exists
	  * that is dialing, active, or on hold, and no calls are ringing
	  * or waiting. */
	public static final int CALL_STATE_OFFHOOK = 2;    // 来电接通 或者 去电拨号 但是没法区分出来
	 

### 2.2 来去电监听方式二（广播监听，但是时机比上面的PhoneStateListener方式要晚一点）

	<receiver android:name=".PhoneStateReceiver"
	    android:enabled="true"
	    android:process=":PhoneListenService">
	    <intent-filter>
	        <action android:name="android.intent.action.NEW_OUTGOING_CALL" />
	        <action android:name="android.intent.action.PHONE_STATE" />
	    </intent-filter>
	</receiver>

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
 

## 三. 实战，有什么需要特别注意地方

### 3.1 双卡双待的手机怎么获取

　　对于双卡手机，每张卡都对应一个Service和一个PhoneStateListener，需要给每个服务注册自己的PhoneStateListener，服务的名称还会有点变化，厂商可能会修改

	public ArrayList<String> getMultSimCardInfo() {
	    // 获取双卡的信息，这个也是经验尝试出来的，不知道其他厂商有什么坑
	    ArrayList<String> phoneServerList = new ArrayList<String>();
	    for(int i = 1; i < 3; i++) {
	        try {
	            String phoneServiceName;
	            if (MiuiUtils.isMiuiV6()) {
	                phoneServiceName = "phone." + String.valueOf(i-1);
	            } else {
	                phoneServiceName = "phone" + String.valueOf(i);
	            }
	
	            // 尝试获取服务看是否能获取到
	            IBinder iBinder = ServiceManager.getService(phoneServiceName);
	            if(iBinder == null) continue;
	
	            ITelephony iTelephony = ITelephony.Stub.asInterface(iBinder);
	            if(iTelephony == null) continue;
	
	            phoneServerList.add(phoneServiceName);
	        } catch(Exception e) {
	            e.printStackTrace();
	        }
	    }
	    // 这个是默认的
	    phoneServerList.add(Context.TELEPHONY_SERVICE);
	    return phoneServerList;
	}
### 3.2 挂断电话

　　挂断电话使用系统服务提供的接口去挂断，但是挂断电话是个并不能保证成功的方法，所以会有多种方式挂断同时使用，下面提供

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
	        TelephonyManager telephonyManager = (TelephonyManager) 
	　　　　　　　　　　　　　　context.getSystemService(Context.TELEPHONY_SERVICE);
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
	            TelephonyManager telephonyManager = (TelephonyManager) 
	　　　　　　　　　　　　context.getSystemService(Context.TELEPHONY_SERVICE);
	
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
　　ITelephony接口在layoutlib.jar包中，需要导入 android sdk目录\platforms\android-8\data\layoutlib.jar

#### 挂断电话需要权限

	<uses-permission android:name="android.permission.CALL_PHONE" />
### 3.3 监听来去电状态放到后台服务（独立进程）

	<service android:name=".PhoneListenService"
	            android:label="Android来电监听"
	            android:process=":PhoneListenService"/>
 

#### 来去电监听Service

	package com.phone.listen;
	
	import android.app.Service;
	import android.content.Context;
	import android.content.Intent;
	import android.os.IBinder;
	import android.telephony.PhoneStateListener;
	import android.telephony.TelephonyManager;
	import android.util.Log;
	
	/**
	 * 来去电监听服务
	 */
	public class PhoneListenService extends Service {
	
	    public static final String TAG = PhoneListenService.class.getSimpleName();
	
	    public static final String ACTION_REGISTER_LISTENER = "action_register_listener";
	
	    @Override
	    public void onCreate() {
	        super.onCreate();
	        Log.d(TAG, "onCreate");
	    }
	
	    @Override
	    public int onStartCommand(Intent intent, int flags, int startId) {
	        Log.d(TAG, "onStartCommand action: " + intent.getAction() + 
	　　　　　　　　　　" flags: " + flags + " startId: " + startId);
	        String action = intent.getAction();
	        if (action.equals(ACTION_REGISTER_LISTENER)) {
	            registerPhoneStateListener();
	        }
	        return super.onStartCommand(intent, flags, startId);
	    }
	
	    private void registerPhoneStateListener() {
	        CustomPhoneStateListener customPhoneStateListener = new CustomPhoneStateListener(this);
	        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
	        if (telephonyManager != null) {
	            telephonyManager.listen(customPhoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
	        }
	    }
	}
 

### 3.4 整体配置文件

	<?xml version="1.0" encoding="utf-8"?>
	<manifest xmlns:android="http://schemas.android.com/apk/res/android"
	    package="com.phone.listen">
	    <uses-permission android:name="android.permission.READ_PHONE_STATE" />
	    <uses-permission android:name="android.permission.CALL_PHONE" />
	    <uses-permission android:name="android.permission.PROCESS_OUTGOING_CALLS" />
	
	    <application
	        android:allowBackup="true"
	        android:icon="@mipmap/ic_launcher"
	        android:label="@string/app_name"
	        android:supportsRtl="true"
	        android:theme="@style/AppTheme">
	        <activity android:name=".MainActivity">
	            <intent-filter>
	                <action android:name="android.intent.action.MAIN" />
	                <category android:name="android.intent.category.LAUNCHER" />
	            </intent-filter>
	        </activity>
	
	        <service android:name=".PhoneListenService"
	            android:label="Android来电监听"
	            android:process=":PhoneListenService"/>
	
	        <receiver android:name=".PhoneStateReceiver"
	            android:enabled="true"
	            android:process=":PhoneListenService">
	            <intent-filter>
	                <action android:name="android.intent.action.NEW_OUTGOING_CALL" />
	                <action android:name="android.intent.action.PHONE_STATE" />
	            </intent-filter>
	        </receiver>
	    </application>
	
	</manifest>
 
## 3.5 博客园文章地址

[http://www.cnblogs.com/popfisher/p/5650969.html](http://www.cnblogs.com/popfisher/p/5650969.html)

 

## 拓展阅读：

这篇文章重点从整体框架机制方面来介绍电话监听

[http://www.cnblogs.com/bastard/archive/2012/11/23/2784559.html](http://www.cnblogs.com/bastard/archive/2012/11/23/2784559.html)

这篇文章重点介绍一些api方法已经变量的含义

[http://blog.csdn.net/skiffloveblue/article/details/7491618](http://blog.csdn.net/skiffloveblue/article/details/7491618)