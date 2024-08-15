
package com.ABC.pioneer.sensor.ble;

import com.ABC.pioneer.sensor.datatype.BluetoothState;

public interface BluetoothStateManagerDelegate {
    void bluetoothStateManager(BluetoothState didUpdateState);
}
