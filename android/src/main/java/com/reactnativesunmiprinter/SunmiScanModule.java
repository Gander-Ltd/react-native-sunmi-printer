package com.reactnativesunmiprinter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

public class SunmiScanModule extends ReactContextBaseJavaModule {
  private static ReactApplicationContext reactContext;
  private static final int START_SCAN = 0x0000;
  private static final String E_ACTIVITY_DOES_NOT_EXIST = "E_ACTIVITY_DOES_NOT_EXIST";
  private static final String E_FAILED_TO_SHOW_SCAN = "E_FAILED_TO_SHOW_SCAN";
  private static final String ACTION_DATA_CODE_RECEIVED = "com.sunmi.scanner.ACTION_DATA_CODE_RECEIVED";
  private static final String DATA = "data";
  private static final String SOURCE = "source_byte";
  private Promise mPickerPromise;

  public enum ScannerSupporter {
        alps("alps"),
        CT58("CT58"),
        HAND_HELD("GM-B9920P"), HT380K("HT380K"),
        idata("idata"),
        KP18("KP18"),
        MT90("NLS-MT90"), MT66("NLS-MT66"), MT6210("NLS-MT6210"), MT9210("NLS-MT9210"),
        OTHER("OTHER"),
        PDA("PDA"), PDT90F("PDT-90F"),
        SG6900("SG6900"), SUNMI("SUNMI"), SEUIC("SEUIC"),
        T1("T1"),
        UBX("UBX");

        private String name;

        ScannerSupporter(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private ScannerSupporter getSupporter() {
        Log.e("TAG", "型号" + Build.MODEL + "，厂商" + Build.MANUFACTURER);
        // 先根据型号适配
        for (ScannerSupporter supporter : ScannerSupporter.values()) {
            if (supporter.getName().equals(Build.MODEL)) {
                return supporter;
            }
        }
        // 型号没有，再尝试根据厂商适配
        for (ScannerSupporter supporter : ScannerSupporter.values()) {
            if (supporter.getName().equals(Build.MANUFACTURER)) {
                return supporter;
            }
        }
        return ScannerSupporter.OTHER;
    }


  private BroadcastReceiver receiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      if (ACTION_DATA_CODE_RECEIVED.equals(action)) {
        String code = intent.getStringExtra(DATA);
        byte[] arr = intent.getByteArrayExtra(SOURCE);
        if (code != null && !code.isEmpty()) {
          sendEvent(code);
        }
      }
    }
  };


  private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
      if (intent != null) {
        Bundle bundle = intent.getExtras();
        ArrayList<HashMap<String, String>> result = (ArrayList<HashMap<String, String>>) bundle.getSerializable("data");
        if (null != result) {
          Iterator<HashMap<String, String>> it = result.iterator();
          while (it.hasNext()) {
            HashMap hashMap = it.next();
            sendEvent(hashMap.get("VALUE").toString());
          }
        }
      }
    }
  };

  public SunmiScanModule(ReactApplicationContext context) {
    super(context);
    reactContext = context;
    reactContext.addActivityEventListener(mActivityEventListener);
    registerReceiver();
  }

  @Override
  public String getName() {
    return "SunmiScanModule";
  }

  @ReactMethod
  public void scan(final Promise promise) {
    Activity currentActivity = getCurrentActivity();
    if (currentActivity == null) {
      promise.reject(E_ACTIVITY_DOES_NOT_EXIST, "Activity doesn't exist");
      return;
    }
    mPickerPromise = promise;
    try {
      Intent intent = new Intent("com.sunmi.scan");
      intent.setPackage("com.sunmi.sunmiqrcodescanner");
      intent.putExtra("PLAY_SOUND", true);
      currentActivity.startActivityForResult(intent, START_SCAN);
    } catch (Exception e) {
      mPickerPromise.reject("E_FAILED_TO_SHOW_SCAN", e);
      mPickerPromise = null;
    }
  }

  private void registerReceiver() {
    IntentFilter filter = new IntentFilter();
    filter.addAction(ACTION_DATA_CODE_RECEIVED);

    compatRegisterReceiver(reactContext, receiver, filter, true);
  }

  private void compatRegisterReceiver(
      Context context, BroadcastReceiver receiver, IntentFilter filter, boolean exported) {

        if (getSupporter() == ScannerSupporter.OTHER) {

            return;
        }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE
        && context.getApplicationInfo().targetSdkVersion >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
      context.registerReceiver(
          receiver, filter, exported ? Context.RECEIVER_EXPORTED : Context.RECEIVER_NOT_EXPORTED);
    } else {
      context.registerReceiver(receiver, filter);
    }
  }

  private static void sendEvent(String msg) {
    reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit("onScanSuccess", msg);
  }
}