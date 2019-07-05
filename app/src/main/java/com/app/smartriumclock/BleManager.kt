package com.app.smartriumclock

import android.app.Application
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
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

    // 데이터 콜백
    var onReceiveData: ((Command, String, String) -> Unit)? = null

    // BLE 앱 서비스
    private val bleAppService
        get() =
            bleGatt?.getService(UUID.fromString("0000fff0-0000-1000-8000-00805f9b34fb"))

    // BLE Writable Characteristic
    private val bleWritableCharacteristic
        get() = bleAppService?.getCharacteristic(UUID.fromString("0000fff2-0000-1000-8000-00805f9b34fb"))

    // Write Value Queue
    private val writeQueue = ArrayDeque<String>()

    companion object {
        val instance = BleManager()
    }

    // Write Commands
    enum class Command(val value: String) {
        Battery("00"),
        PM10("01"),
        PM2_5("02"),
        PM1("03"),
        Temperature("04"),
        Humidity("05"),
        Illuminance("06")
    }

    // Command 생성에 사용되는 데이트 포맷
    private val commandDateFormat by lazy { SimpleDateFormat("HHmm", Locale.getDefault()) }

    // 최근 실행 요청한 Command 및 요청 시간
    private var latestRequestCommand: String? = null
    private var latestRequestAt: Date? = null
    private var isRunningRequest = false

    // Request Timeout 설정 후 해당 시간 내에 요청응답이 오지 않으면 무시
    private val requestTimeout = 5 * 1000

    // Read Value Regex
    private val readRegex = """R(\d{2})(\d{4})(\d{4})""".toRegex()

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

    /**
     * Handle scan results after scan stopped
     */
    fun scanComplete() {
        stopScan()
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

    /**
     * Request BLE enable
     */
    private fun requestEnableBLE() {
        // startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT)
    }

    /**
     * Stop scanning
     */
    private fun stopScan() {
        // check pre-conditions
        if (isScanning && bleAdapter.isEnabled) {
            bleScanner.stopScan(scanCallback)
        }

        // update the status
        Log.d(TAG, "scanning stopped")
    }

    /**
     * Disconnect Gatt Server
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

    /**
     * BLE 쓰기 요청
     */
    fun writeQueue(command: Command, at: Date) {

        // Create Command
        val writeValue = "W${command.value}${commandDateFormat.format(at)}0000"

        // Log
        Log.d(TAG, "Add To Queue : Command - $writeValue")

        // Write Queue에 추가
        writeQueue.push(writeValue)

        // Write Queue 체크
        checkWriteQueue()
    }

    /**
     * Write Queue 체크 해서 큐에 BLE에 Write 해야할 Value가 있다면
     * 요청 하고 아닌 경우 넘어감
     */
    private fun checkWriteQueue() {

        // Queue가 비어있음
        if (writeQueue.isEmpty()) return

        // 아직 요청이 실행중일때
        if (isRunningRequest) return

        // 입력해야할 Value
        val writeValue = writeQueue.poll()

        // 요청 상태 변경 및 최근 실행 정보 저장
        isRunningRequest = true
        latestRequestAt = Date()
        latestRequestCommand = writeValue

        // Writable Characteristic에 데이터 입력
        val writeResult = bleWritableCharacteristic?.run {
            setValue(writeValue)
            bleGatt?.writeCharacteristic(this)
        } ?: false

        // Log
        Log.d(TAG, "Write To Characteristic : ${writeValue} Result : $writeResult")

        // Handler로 Timeout 초 후에 아직 데이터가 오지 않았다면 다음 데이터 요청
        val tempRequestAt = latestRequestAt?.time
        scanHandler.postDelayed({
            if (isRunningRequest && tempRequestAt == latestRequestAt?.time) {
                isRunningRequest = false
                latestRequestAt = null
                latestRequestCommand = null
                checkWriteQueue()

                Log.d(TAG, "Restart Check Write Queue")
            }
        }, requestTimeout.toLong())
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
                UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
            )
            descriptor?.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            bleGatt?.writeDescriptor(descriptor)

            Log.d(TAG, "setResultNotification : ${setResultNotification}")
        }

        /**
         * 데이터가 응답이 왔을때
         */
        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)

            // 콜백
            onCharacteristicChanged?.invoke(gatt, characteristic)

            // 가져와진 데이터
            val value = characteristic.value.toString(Charset.forName("UTF-8"))

            // Data Receive
            Log.d(TAG, "Receive Data : $latestRequestCommand -> $value")

            // 넘어온 데이터 콜백에서 데이터 파싱
            val matchResult = readRegex.find(value)

            // 결과 데이터를 못가져 왔을때 Queue에 추가
            if (matchResult != null) {
                val commandCode = matchResult.groups[1]?.value ?: throw Exception("Command Code")
                val date = matchResult.groups[2]?.value ?: throw Exception("Date")
                val data = matchResult.groups[3]?.value ?: throw Exception("Data")

                // 미래형
                // val command: Command? = Command.values().firstOrNull { it.value == commandCode }

                // 현재형
                val command: Command? =
                    Command.values().firstOrNull { latestRequestCommand?.contains("W${it.value}") ?: false }

                // Command Callback
                if (command != null) {
                    onReceiveData?.invoke(command, date, data)
                } else {
                    Log.e(TAG, "Received Data But Unsupported Command Code")
                }

                Log.d(TAG, "Receive Data : $latestRequestCommand - ($commandCode),($date),($data)")
            } else {
                // 처리를 어떻게 해야할지???
            }

            // Request 업데이트
            isRunningRequest = false
            latestRequestAt = null
            latestRequestCommand = null

            // Check Queue
            checkWriteQueue()
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

