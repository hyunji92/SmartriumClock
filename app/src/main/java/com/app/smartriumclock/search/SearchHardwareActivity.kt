package com.app.smartriumclock.search

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.ParcelUuid
import android.support.annotation.RequiresApi
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.app.smartriumclock.DustMainActivity
import com.app.smartriumclock.R
import kotlinx.android.synthetic.main.activity_search_hardware.*
import java.util.*
import kotlin.collections.HashMap

class SearchHardwareActivity : AppCompatActivity() {

    // Tag name for Log message
    private val TAG = "Central"
    // used to identify adding bluetooth names
    private val REQUEST_ENABLE_BT = 1
    // used to request fine location permission
    private val REQUEST_FINE_LOCATION = 2
    // scan period in milliseconds
    private val SCAN_PERIOD = 5000
    // ble adapter
    private lateinit var ble_adapter_: BluetoothAdapter
    // flag for scanning
    private var is_scanning_ = false
    // flag for connection
    private var connected_ = false
    // scan results
    private var scan_results_ = HashMap<String, BluetoothDevice>()
    // scan callback
    private var scan_cb_: ScanCallback? = null
    // ble scanner
    private lateinit var ble_scanner_: BluetoothLeScanner
    // scan handler
    private var scan_handler_: Handler? = null

    // BLE Gatt
    private var ble_gatt_: BluetoothGatt? = null

    var SERVICE_STRING = "0000aab0-f845-40fa-995d-658a43feea4c"
    var UUID_TDCS_SERVICE = UUID.fromString(SERVICE_STRING)
    var CHARACTERISTIC_COMMAND_STRING = "0000AAB1-F845-40FA-995D-658A43FEEA4C"
    var UUID_CTRL_COMMAND = UUID.fromString(CHARACTERISTIC_COMMAND_STRING)
    var CHARACTERISTIC_RESPONSE_STRING = "0000AAB2-F845-40FA-995D-658A43FEEA4C"
    var UUID_CTRL_RESPONSE = UUID.fromString(CHARACTERISTIC_RESPONSE_STRING)
    val MAC_ADDR = "78:A5:04:58:A7:92"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_hardware)

        val bleManager: BluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        ble_adapter_ = bleManager.adapter

        yellow_next_btn.setOnClickListener {
            val intent = Intent(this, DustMainActivity::class.java)
            startActivity(intent)
        }

        startScan()
    }

    override fun onResume() {
        super.onResume()
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            finish()
        }
    }

    /*
 Start BLE scan
 */
    private fun startScan() {
//        tv_status_.setText("Scanning...")

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !== PackageManager.PERMISSION_GRANTED)
        {
            requestLocationPermission()
            //tv_status_.setText("Scanning Failed: no fine location permission")
            return
        }
        disconnectGattServer()

        // check ble adapter and ble enabled
        if (ble_adapter_ == null || !ble_adapter_.isEnabled) {
            requestEnableBLE()
            // 블루 투스 연결기계 찾을 수 가 없
//            tv_status_.setText("Scanning Failed: ble not enabled")
            return
        }
        // check if location permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) !== PackageManager.PERMISSION_GRANTED) {
                requestLocationPermission()
//            tv_status_.setText("Scanning Failed: no fine location permission")
                return
            }
        } else {
            // 블루투스를 연결할 수 없습니다.
            TODO("VERSION.SDK_INT < M")
        }

        val filters = ArrayList<ScanFilter>()
        val scan_filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(UUID_TDCS_SERVICE))
            .build()
        filters.add(scan_filter)

        // scan settings
        // set low power scan mode
        var settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        scan_cb_ = BLEScanCallback(scan_results_)

        ble_scanner_.startScan(filters, settings, scan_cb_)
        is_scanning_ = true

        scan_handler_ = Handler()
        scan_handler_?.postDelayed(this::stopScan, SCAN_PERIOD.toLong())

        if (is_scanning_ && ble_adapter_ != null && ble_adapter_.isEnabled() && ble_scanner_ != null) {
            // stop scanning
            ble_scanner_.stopScan(scan_cb_)
            scanComplete()
        }

    }

    /*
 Handle scan results after scan stopped
 */
    private fun scanComplete() {
        // check if nothing found
        if (scan_results_.isEmpty()) {
            //tv_status_.setText("scan results is empty")
            Log.d(TAG, "scan results is empty")
            return
        }
        // loop over the scan results and connect to them
        for (device_addr in scan_results_.keys) {
            Log.d(TAG, "Found device: " + device_addr)
            // get device instance using its MAC address
            val device = scan_results_.get(device_addr)
            if (MAC_ADDR.equals(device_addr)) {
                Log.d(TAG, "connecting device: " + device_addr)
                // connect to the device
                device?.let { connectDevice(it) }
            }
        }
    }

    /*
    Connect to the ble device
    */
    private fun connectDevice(_device: BluetoothDevice) {
        // update the status
        //tv_status_.setText("Connecting to " + _device.getAddress())
        val gatt_client_cb = GattClientCallback()
        ble_gatt_ = _device.connectGatt(this, false, gatt_client_cb)
    }

    /*
     Request BLE enable
     */
    private fun requestEnableBLE() {
        val ble_enable_intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        startActivityForResult(ble_enable_intent, REQUEST_ENABLE_BT)
    }

    /*
     Request Fine Location permission
     */
    private fun requestLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_FINE_LOCATION)
        }
    }

    /*
    Stop scanning
    */
    private fun stopScan() {
        // check pre-conditions
        if (is_scanning_ && ble_adapter_ != null && ble_adapter_.isEnabled() && ble_scanner_ != null) {
            // stop scanning
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                ble_scanner_.stopScan(scan_cb_)
            }
        }
        // reset flags
        scan_cb_ = null
        is_scanning_ = false
        scan_handler_ = null
        // update the status
        //tv_status_.setText("scanning stopped")
    }

    /*
     Disconnect Gatt Server
     */
    fun disconnectGattServer() {
        Log.d(TAG, "Closing Gatt connection")
        // reset the connection flag
        connected_ = false
        // disconnect and close the gatt
        if (ble_gatt_ != null)
        {
            ble_gatt_!!.apply {
                disconnect()
                close()
            }
        }
    }

    /*
    Gatt Client Callback class
    */
    inner class GattClientCallback: BluetoothGattCallback() {
        override fun onConnectionStateChange(_gatt:BluetoothGatt, _status:Int, _new_state:Int) {
            super.onConnectionStateChange(_gatt, _status, _new_state)
            if (_status == BluetoothGatt.GATT_FAILURE)
            {
                disconnectGattServer()
                return
            }
            else if (_status != BluetoothGatt.GATT_SUCCESS)
            {
                disconnectGattServer()
                return
            }
            if (_new_state == BluetoothProfile.STATE_CONNECTED)
            {
                // update the connection status message
                //tv_status_.setText("Connected")
                // set the connection flag
                connected_ = true
                Log.d(TAG, "Connected to the GATT server")
            }
            else if (_new_state == BluetoothProfile.STATE_DISCONNECTED)
            {
                disconnectGattServer()
            }
        }
    }
    /*
    BLE Scan Callback class
    */
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    inner class BLEScanCallback constructor(_scan_results: HashMap<String, BluetoothDevice>) : ScanCallback() {
        private var cb_scan_results_: HashMap<String, BluetoothDevice> = _scan_results
        override fun onScanResult(_callback_type: Int, _result: ScanResult) {
            Log.d(TAG, "onScanResult")
            addScanResult(_result)
        }

        override fun onBatchScanResults(_results: List<ScanResult>) {
            for (result in _results) {
                addScanResult(result)
            }
        }

        override fun onScanFailed(_error: Int) {
            Log.e(TAG, "BLE scan failed with code " + _error)
        }

        /*
       Add scan result
       */
        private fun addScanResult(_result: ScanResult) {
            // get scanned device
            val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                _result.getDevice()
            } else {
                TODO("VERSION.SDK_INT < LOLLIPOP")
            }
            // get scanned device MAC address
            val device_address = device.address
            // add the device to the result list
            cb_scan_results_.put(device_address, device)
            // log
            Log.d(TAG, "scan results device: $device")
            //tv_status_.setText("add scanned device: " + device_address)
        }
    }
}
