package com.rahullohra.blechatapp

import android.annotation.TargetApi
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.os.Build
import java.util.*

class Util{
    companion object {
        @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        fun prepareCharateristic(characteristicUuid: UUID, descriptorUuid: UUID): BluetoothGattCharacteristic {
            val userMetaDataCharacteristic = BluetoothGattCharacteristic(
                characteristicUuid,
                BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE)

            userMetaDataCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE

            val metaDataDescriptor = BluetoothGattDescriptor(descriptorUuid, BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE)
            userMetaDataCharacteristic.addDescriptor(metaDataDescriptor)
            return userMetaDataCharacteristic
        }
    }

}