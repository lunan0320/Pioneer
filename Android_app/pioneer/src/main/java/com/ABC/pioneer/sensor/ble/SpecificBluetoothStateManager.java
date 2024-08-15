
package com.ABC.pioneer.sensor.ble;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.ABC.pioneer.sensor.datatype.BluetoothState;

/**
 * 监测蓝牙状态是否变化
 */
public class SpecificBluetoothStateManager implements BluetoothStateManager {
    private BluetoothState state = null;
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                try {
                    final int nativeState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                    switch (nativeState) {
                        case BluetoothAdapter.STATE_ON:
                            state = BluetoothState.poweredOn;
                            for (BluetoothStateManagerDelegate delegate : delegates) {
                                delegate.bluetoothStateManager(BluetoothState.poweredOn);
                            }
                            break;
                        case BluetoothAdapter.STATE_OFF:
                            state = BluetoothState.poweredOff;
                            for (BluetoothStateManagerDelegate delegate : delegates) {
                                delegate.bluetoothStateManager(BluetoothState.poweredOff);
                            }
                            break;
                    }
                } catch (Throwable e) {
                }
            }
        }
    };

    /**
     * 监测蓝牙状态是否变化
     */
    public SpecificBluetoothStateManager(Context context) {
        state = state();

        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        context.registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    public BluetoothState state() {
        if (state == null) {
            final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                state = BluetoothState.unsupported;
                return state;
            }
            switch (BluetoothAdapter.getDefaultAdapter().getState()) {
                case BluetoothAdapter.STATE_ON:
                    state = BluetoothState.poweredOn;
                    break;
                case BluetoothAdapter.STATE_OFF:
                case BluetoothAdapter.STATE_TURNING_OFF:
                case BluetoothAdapter.STATE_TURNING_ON:
                default:
                    state = BluetoothState.poweredOff;
                    break;
            }
        }
        return state;
    }
}
