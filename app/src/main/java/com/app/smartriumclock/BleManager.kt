package com.app.smartriumclock

import android.app.Application
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.Handler
import android.util.Log
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

class BleManager {

    private val TAG = "BleManager"

    private lateinit var application: Application

    private val context: Context
        get() = application.applicationContext

    // Bluetooth Manager
    private val bleManager: BluetoothManager by lazy { context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    private val bleAdapter by lazy { bleManager.adapter }
    private val bleScanner by lazy { bleAdapter.bluetoothLeScanner }

    // scan results
    var scanResults = HashMap<String, BluetoothDevice>()

    // scan callback
    private val scanCallback: ScanCallback by lazy { BLEScanCallback(scanResults) { onUpdateScanResult?.invoke() } }
    var onUpdateScanResult: (() -> Unit)? = null

    // scan handler
    private val scanHandler: Handler by lazy { Handler() }

    // BLE Gatt
    private var bleGatt: BluetoothGatt? = null

    // IsScanning
    private var isScanning = false

    // Is Connected
    var onChangeConnect: ((Boolean) -> Unit)? = null
    private var isConnected = false
        set(value) {
            field = value
            scanHandler.post { onChangeConnect?.invoke(field) }
        }

    // 스캔
    private val SCAN_PERIOD = 5000 * 10

    // 블루투스 데이터 가죠오는 콜백
    var onCharacteristicChanged: ((BluetoothGatt, BluetoothGattCharacteristic) -> Unit)? = null

    companion object {
        val instance = BleManager()
    }

    fun initialize(application: Application) {
        this.application = application
    }

    fun startScan() {
        Log.d(TAG, "Scanning...")

        // Gatt Service 해제
        disconnectGattServer()

        // check ble adapter and ble enabled
        if (!bleAdapter.isEnabled) {
            requestEnableBLE()

            // 블루 투스 연결기계 찾을 수 가 없
            Log.d(TAG, "Scanning Failed: ble not enabled")
            return
        }

        // 블루투스 스캔 필터
        val scanFilters = arrayListOf(
                ScanFilter.Builder()
                        .setDeviceName("CHIPSEN")
                        .build()
        )

        // scan settings
        // set low power scan mode ( BLE 만 스캔 )
        val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                .build()

        // 스캔 시작
        bleScanner.startScan(scanFilters, settings, scanCallback)

        // 스캔 상태 변경
        isScanning = true

        // 스캔 종료 핸들러 추가
        scanHandler.postDelayed(this::stopScan, SCAN_PERIOD.toLong())
    }

    /*
   * Handle scan results after scan stopped
   */
    fun scanComplete() {
        stopScan()
//        // check if nothing found
//        if (scan_results_.isEmpty()) {
//            //tv_status_.setText("scan results is empty")
//            Log.d(TAG, "scan results is empty")
//            return
//        }
//        // loop over the scan results and connect to them
//        for (device_addr in scan_results_.keys) {
//            Log.d(TAG, "Found device: " + device_addr)
//            // get device instance using its MAC address
//            val device = scan_results_.get(device_addr)
//            if (MAC_ADDR.equals(device_addr)) {
//                Log.d(TAG, "connecting device: " + device_addr)
//                // connect to the device
//                device?.let { connectDevice(it) }
//            }
//        }
    }

    /**
     * Connect to the ble device
     */
    fun connectDevice(device: BluetoothDevice) {
        // update the status
        Log.d(TAG, "Connecting to ${device.address}")

        // 블루투스 기기 연결
        bleGatt = device.connectGatt(context, false, GattClientCallback())
    }

    /*
     Request BLE enable
     */
    private fun requestEnableBLE() {
        // startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT)
    }

    /*
     Request Fine Location permission
     */
    private fun requestLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_FINE_LOCATION)
        }
    }

    /*
    Stop scanning
    */
    private fun stopScan() {
        // check pre-conditions
        if (isScanning && bleAdapter.isEnabled) {
            bleScanner.stopScan(scanCallback)
        }

        // update the status
        Log.d(TAG, "scanning stopped")
    }

    /*
     Disconnect Gatt Server
     */
    fun disconnectGattServer() {
        Log.d(TAG, "Closing Gatt connection")

        // reset the connection flag
        isConnected = false

        // disconnect and close the gatt
        if (bleGatt != null) {
            bleGatt?.apply {
                disconnect()
                close()
            }
        }
    }

    fun write(value: String) {
        val service = bleGatt?.getService(UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb"))
        val writableCharacteristic = service?.getCharacteristic(UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb"))
        val result = writableCharacteristic?.setValue(value)
        val writeResult = bleGatt!!.writeCharacteristic(writableCharacteristic)
        Log.d(TAG, "Result $result, WriteResult $writeResult")
    }

    /**
     * Gatt Client Callback class
     */
    inner class GattClientCallback : BluetoothGattCallback() {

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            Log.d(TAG, "Discover Service status : ${status}")

            // Temporary
            val service = gatt.getService(UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb"))

            // Notifiable
            val readCharacteristic = service?.getCharacteristic(UUID.fromString("0000fff1-0000-1000-8000-00805f9b34fb"))
            val setResultNotification = bleGatt!!.setCharacteristicNotification(readCharacteristic, true)

            val descriptor = readCharacteristic?.getDescriptor(
                    UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"));
            descriptor?.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bleGatt?.writeDescriptor(descriptor)

            Log.d(TAG, "setResultNotification : ${setResultNotification}")

            // Write
            val writableCharacteristic = service?.getCharacteristic(UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb"))

            // 현재시간을 msec 으로 구한다.
            var now = System.currentTimeMillis();
            // 현재시간을 date 변수에 저장한다.
            var date = Date(now);
            // 시간을 나타냇 포맷을 정한다 ( yyyy/MM/dd 같은 형태로 변형 가능 )
            var simpleDataFormat =  SimpleDateFormat("HHmm");
            // nowDate 변수에 값을 저장한다.
            var formatDate = simpleDataFormat.format(date)
            Log.d(TAG, "Result format : $formatDate")


            //val result = writableCharacteristic?.setValue("W00"+ formatDate +"0000")
            val result_dust = writableCharacteristic?.setValue("W02"+ formatDate +"0001")
            Log.d(TAG, "Result dust:  " + "W02"+ formatDate +"0001")

            val writeResult = bleGatt!!.writeCharacteristic(writableCharacteristic)

            Log.d(TAG, "Result : ${result_dust}, writeResult : ${writeResult}")
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            super.onCharacteristicRead(gatt, characteristic, status)

            Log.d(TAG, "onCharacteristicRead")
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)

            // 넘어온 데이터 콜백
            onCharacteristicChanged?.invoke(gatt, characteristic)

            // 로깅
            Log.d(TAG, "onCharacteristicChanged : ${characteristic?.value?.toString(Charset.forName("UTF-8"))}")

        }

        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newStatus: Int) {
            super.onConnectionStateChange(gatt, status, newStatus)

            // 취소 혹은 연결 실패 하면 연결 끊기
            if (status == BluetoothGatt.GATT_FAILURE) {
                disconnectGattServer()
                Log.d(TAG, "BluetoothGatt.GATT_FAILURE")
                return
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "!BluetoothGatt.GATT_SUCCESS")
                disconnectGattServer()
                return
            }

            // 기기 연결 상태가 변경되는거에 따라 처리
            if (newStatus == BluetoothProfile.STATE_CONNECTED) {

                // set the connection flag
                isConnected = true

                Log.d(TAG, "Connected to the GATT server")

                bleGatt?.discoverServices()
            } else if (newStatus == BluetoothProfile.STATE_DISCONNECTED) {
                disconnectGattServer()
                Log.d(TAG, "Disconnected to the GATT server")
            }
        }
    }

    /**
     * BLE Scan Callback class
     */
    class BLEScanCallback constructor(
            private val scanResults: HashMap<String, BluetoothDevice>,
            private val onUpdate: () -> Unit
    ) :
            ScanCallback() {

        private val TAG = "BLEScanCallback"

        override fun onScanResult(callbackType: Int, result: ScanResult) {
            Log.d(TAG, "onScanResult")
            addScanResult(result)
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            for (result in results) {
                addScanResult(result)
            }
        }

        override fun onScanFailed(error: Int) {
            Log.e(TAG, "BLE scan failed with code $error")
        }

        /**
         * Add scan result
         */
        private fun addScanResult(scanResult: ScanResult) {
            // get scanned device
            val device = scanResult.device

            // add the device to the result list
            scanResults[device.address] = device

            // Update Callback
            onUpdate.invoke()

            Log.d(TAG, "Scan Result ${device.name}:(${device.address})")
            Log.d(TAG, "add scanned device: ${device.address}")
        }
    }
}

