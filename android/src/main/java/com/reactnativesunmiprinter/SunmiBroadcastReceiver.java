package com.reactnativesunmiprinter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SunmiBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (SunmiScanModule.ACTION_DATA_CODE_RECEIVED.equals(action)) {
            String code = intent.getStringExtra(SunmiScanModule.DATA);
            byte[] arr = intent.getByteArrayExtra(SunmiScanModule.SOURCE);
            if (code != null && !code.isEmpty()) {
                // Assuming sendEvent is a static method or you have another way to handle this event
                SunmiScanModule.sendEvent(code);
            }
        }
    }
}
