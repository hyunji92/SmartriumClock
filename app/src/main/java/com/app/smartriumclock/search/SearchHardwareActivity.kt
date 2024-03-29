package com.app.smartriumclock.search

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import android.view.View
import com.app.smartriumclock.BleManager
import com.app.smartriumclock.DustMainActivity
import com.app.smartriumclock.R
import kotlinx.android.synthetic.main.activity_search_hardware.*

class SearchHardwareActivity : AppCompatActivity() {

    // Tag name for Log message
    private val TAG = "Central"

    // Hardware RecyclerView Adapter
    private val scanHardwareAdapter by lazy { SearchHardwareAdapter(this, BleManager.instance.scanResults) }

    // flag for connection
    private var isConnected = false
        set(value) {
            field = value
            yellow_next_btn.visibility = if (field) View.VISIBLE else View.INVISIBLE
            yellow_next_btn.isEnabled = field
        }

    var userFirst: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_hardware)

        // Disable Button
        isConnected = false

        val prefs = getSharedPreferences("my_pref", Context.MODE_PRIVATE)
        val editor = prefs.edit()

        userFirst = prefs.getBoolean("first_user_select", false)
        // Click Listener
        if (!userFirst) {
            yellow_next_btn.setOnClickListener {
                val intent = Intent(this, SellectPlantActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK and Intent.FLAG_ACTIVITY_CLEAR_TASK and Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                this.finish()
            }
            editor.putBoolean("first_user_select", true)
            editor.apply()
        } else {
            yellow_next_btn.setOnClickListener {
                val intent = Intent(this, DustMainActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK and Intent.FLAG_ACTIVITY_CLEAR_TASK and Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                this.finish()
            }
        }

        // 리사이클러뷰 설정
        hardware_list.layoutManager = LinearLayoutManager(this)
        hardware_list.adapter = scanHardwareAdapter

        //  블루트스 스캔 결과 업데이트
        BleManager.instance.onUpdateScanResult = { scanHardwareAdapter.notifyDataSetChanged() }
        BleManager.instance.onChangeConnect = { this.isConnected = it }

        // 스캔 시작
        BleManager.instance.startScan()

        go_website.setOnClickListener {
            intent = Intent(Intent.ACTION_VIEW)
            var uri = Uri.parse("http://www.it-sago.com")
            intent.data = uri
            startActivity(intent)

        }

    }

    override fun onResume() {
        super.onResume()
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            finish()
        }
    }

    /**
     * 블루투스 기기 연결 요청
     */
    fun onRequestConnectDevice(device: BluetoothDevice) {

        // Finish Scan
        BleManager.instance.scanComplete()

        // Connect Device
        BleManager.instance.connectDevice(device)
    }
}
