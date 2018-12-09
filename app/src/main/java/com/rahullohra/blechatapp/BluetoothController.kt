package com.rahullohra.blechatapp

import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.ParcelUuid
import androidx.annotation.RequiresApi
import java.util.*

class BluetoothController {

    val SERVICE_ID = "364710c0–6359–4bdf-9946–9f54d07eb8d3"
    val USER_META_DATA_ID = "770bf5dc-53f8–4d55–8506–15a51baee22d"
    val USER_META_DATA_DESCRIPTOR_ID = "aeafd752-d418-4696-8271-923525a93b44"
    val SERVICE_UUID = UUID.fromString(SERVICE_ID)
    val USER_META_DATA_UUID = UUID.fromString(USER_META_DATA_ID)
    val USER_META_DATA_DESCRIPTOR_UUID = UUID.fromString(USER_META_DATA_DESCRIPTOR_ID)

    var mBluetoothManager: BluetoothManager? = null
    var mBluetoothAdapter: BluetoothAdapter? = null
    var mBluetoothLeAdvertiser: BluetoothLeAdvertiser? = null
    var mGattServer: BluetoothGattServer? = null

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun setupBluetoothManager(context: Context, handler: Handler) {
        mBluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        handler.postDelayed({
            setupBluetoothAdvertiser(context)
        }, 2000L)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun setupBluetoothAdvertiser(context: Context) {
        mBluetoothAdapter = mBluetoothManager!!.adapter
        if (mBluetoothAdapter!!.isMultipleAdvertisementSupported) {

            mBluetoothLeAdvertiser = mBluetoothAdapter!!.bluetoothLeAdvertiser;
            val gattServerCallback = GattServerCallback()
            mGattServer = mBluetoothManager!!.openGattServer(context, gattServerCallback)
            setupServer()
            startAdvertising()
        }
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun setupServer() {
        val service = BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY)
        service.addCharacteristic(Util.provideCharacteristic(USER_META_DATA_UUID, USER_META_DATA_DESCRIPTOR_UUID))

        mGattServer?.addService(service)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun startAdvertising() {
        if (mBluetoothLeAdvertiser == null) {
            return
        }
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
            .setConnectable(true)
            .setTimeout(0)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
            .build()

        val parcelUuid = ParcelUuid(SERVICE_UUID)
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)// because data is greater than 31 bytes
            .addServiceUuid(parcelUuid)
            .build()

        mBluetoothLeAdvertiser?.startAdvertising(settings, data, mAdvertiseCallback);
    }

    private val mAdvertiseCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
        }

        override fun onStartFailure(errorCode: Int) {

        }
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    inner class GattServerCallback : BluetoothGattServerCallback() {

        override fun onConnectionStateChange(device: BluetoothDevice?, status: Int, newState: Int) {
            super.onConnectionStateChange(device, status, newState);
        }

        //RECEIVER END
        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {

        }

        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        override fun onCharacteristicReadRequest(
            device: BluetoothDevice?,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
        }


        @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
        override fun onDescriptorWriteRequest(
            device: BluetoothDevice?,
            requestId: Int,
            descriptor: BluetoothGattDescriptor?,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value)
        }
    }

}