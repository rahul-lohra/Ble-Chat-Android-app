package com.rahullohra.blechatapp

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
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

    var mGattConnectedMap = HashMap<BluetoothGatt, Boolean>()
    val mDeviceGattMap = HashMap<BluetoothDevice, BluetoothGatt>()
    val mGattDeviceMap = HashMap<BluetoothGatt, BluetoothDevice>()
    var mScanning = false
    val mScannedDeviceIds = HashSet<String>()
    val mConnectedDevices: ArrayList<BluetoothDevice> = ArrayList()
    val mGattClientCallbackMap = HashMap<BluetoothDevice, GattClientCallback>()
    var mClientScanCallback: ScanCallback? = null
    var mBluetoothLeScanner: BluetoothLeScanner? = null
    val mScannedDevices = HashSet<BluetoothDevice>()

    lateinit var context: Context

    @SuppressLint("MissingPermission")
    fun startScanning() {
        if (mScanning) {
            return
        }
        val filters = ArrayList<ScanFilter>()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()
        filters.add(
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid(SERVICE_UUID))
                .build()
        )

        mClientScanCallback = BluetoothScanCallback() // Will setup this in 3rd step
        mBluetoothLeScanner = mBluetoothAdapter!!.bluetoothLeScanner
        if (mBluetoothLeScanner == null) {

        } else {
            mBluetoothLeScanner?.startScan(filters, settings, mClientScanCallback)
        }
        mScanning = true
    }

    private inner class BluetoothScanCallback : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            addScannedDevice(result)
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            for (result in results) {
                addScannedDevice(result)
            }
        }

        override fun onScanFailed(errorCode: Int) {

        }


        private fun addScannedDevice(result: ScanResult) {
            val device = result.device
            if (device != null && device.address != null) {
                mScannedDeviceIds.add(device.address)
            }

            if (!mScannedDevices.contains(device)) {
                mScannedDevices.add(device)
                Handler().postDelayed({ connectWithScannedDevice(device) }, 1000)
            }
        }
    }

    fun connectWithScannedDevice(device: BluetoothDevice) {
        var mGattClientCallback = mGattClientCallbackMap[device]
        if (mGattClientCallback == null) {
            mGattClientCallback = GattClientCallback()
            mGattClientCallbackMap[device] = mGattClientCallback
        }
        val mGatt = device.connectGatt(context, false, mGattClientCallbackMap[device])
        mDeviceGattMap[device] = mGatt
        mGattDeviceMap[mGatt] = device
    }

    inner class GattClientCallback : BluetoothGattCallback() {

        //SENDER
        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
//            handleOnCharacteristicWriteOfSender(gatt, characteristic, status)
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            handleDescriptorWriteOfSender(gatt, descriptor, status)
        }


        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            handleOnConnectionStateChange(gatt, status, newState)
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            handleOnServicesDiscovered(gatt, status)
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            super.onCharacteristicChanged(gatt, characteristic)
//            handleOnCharacteristicChanged(gatt, characteristic)
        }
    }

    fun handleOnConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
        if (status == BluetoothGatt.GATT_FAILURE) {
            disconnectFromGattServer(gatt)
            return
        } else if (status != BluetoothGatt.GATT_SUCCESS) {
            disconnectFromGattServer(gatt, true)
            return
        }
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            handleConnectedStateOfClient(gatt)
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            disconnectFromGattServer(gatt, true)
        }
    }

    fun handleDescriptorWriteOfSender (gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
            sendYourVeryFirstMessage(gatt)
        } else {
            if (gatt != null && mGattDeviceMap[gatt] != null) {
                handleFailureOfDescriptionWrite(mGattDeviceMap[gatt]!!.address)
            }
        }
    }

    fun handleFailureOfDescriptionWrite(address:String){
        //Do nothing
    }

    fun handleFailureOfSendingMessage(){}

    fun sendYourVeryFirstMessage(gatt: BluetoothGatt?, characteristicUuid: UUID = USER_META_DATA_UUID, bytes: ByteArray? = null){
        try {
            if (gatt != null) {
                val mGattConnected = mGattConnectedMap[gatt]
                if ((mGattConnected == null || !mGattConnected) && !mConnectedDevices.contains(gatt.device)) {
                    handleFailureOfSendingMessage()
                    return
                }
                val service = gatt.getService(SERVICE_UUID)
                if (service != null) {
                    val characteristic = service.getCharacteristic(characteristicUuid)
                    characteristic?.value = bytes
                    characteristic?.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
                    val success = gatt.writeCharacteristic(characteristic)
                    //check for success writing
                } else {
                    handleFailureOfSendingMessage()
                }
            } else {
                handleFailureOfSendingMessage()
            }
        } catch (E: NullPointerException) {
            handleFailureOfSendingMessage()
        }
    }

    fun handleConnectedStateOfClient(bluetoothGatt: BluetoothGatt) {
        mGattConnectedMap[bluetoothGatt] = true
        mDeviceGattMap[bluetoothGatt.device] = bluetoothGatt
        mGattDeviceMap[bluetoothGatt] = bluetoothGatt.device
        bluetoothGatt.discoverServices()
    }

    @RequiresApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    fun disconnectFromGattServer(gatt: BluetoothGatt, retry: Boolean = false) {
        mGattConnectedMap[gatt] = false
        if (retry) {
            gatt.connect()
        } else {
            gatt.disconnect()
            gatt.close()
        }
    }

    fun handleOnServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        if (status != BluetoothGatt.GATT_SUCCESS) {
            return
        }
        if (gatt != null)
            writeFirstDescriptor(gatt)
    }

    fun writeFirstDescriptor(gatt: BluetoothGatt) {
        val service = gatt.getService(SERVICE_UUID)
        var metaDataCharacteristic = service.getCharacteristic(USER_META_DATA_UUID)
        if (metaDataCharacteristic != null) {
            metaDataCharacteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
            var descriptor = metaDataCharacteristic.getDescriptor(USER_META_DATA_DESCRIPTOR_UUID)
            descriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            if (descriptor != null) {
                val descriptorWriteStartedSuccess = gatt.writeDescriptor(descriptor)
                //check whether write is success or not
                //You will get the message on GattServerCallback.onCharacteristicWriteRequest(..)
            } else {

            }
        }
    }

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
        service.addCharacteristic(Util.prepareCharateristic(USER_META_DATA_UUID, USER_META_DATA_DESCRIPTOR_UUID))

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
            handleOnConnectionStateChangeServer(device, status, newState)
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

    fun handleOnConnectionStateChangeServer(bleDevice: BluetoothDevice?, status: Int, newState: Int){
        if (bleDevice != null) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectedDevices.add(bleDevice)
                connectNewDevice(bleDevice)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectedDevices.remove(bleDevice)
            }
        }
    }

    fun connectNewDevice(device:BluetoothDevice){
        var mGattClientCallback = mGattClientCallbackMap[device]
        if (mGattClientCallback == null) {
            // Means a new device, so add it
            mGattClientCallback = GattClientCallback()
            mGattClientCallbackMap[device] = mGattClientCallback
            val mGatt = device.connectGatt(context, false,
                mGattClientCallbackMap[device])
            mDeviceGattMap[device] = mGatt
            mGattDeviceMap[mGatt] = device
        }
    }

}