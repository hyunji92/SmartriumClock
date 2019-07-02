package com.app.smartriumclock

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.app.smartriumclock.setting.MyPageActivity
import com.app.smartriumclock.setting.SettingActivity
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.android.synthetic.main.activity_dust_main.*
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.*

class DustMainActivity : AppCompatActivity() {

    var readValue: String? = null

    val timer by lazy { Timer() }

    lateinit var addTask :TimerTask

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_all -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_temperature -> {

                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_huminity -> {

                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_illu -> {

                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_setting -> {

                val intent = Intent(this, SettingActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK and Intent.FLAG_ACTIVITY_CLEAR_TASK and Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)

        mypage_btn.setOnClickListener {
            val intent = Intent(this, MyPageActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK and Intent.FLAG_ACTIVITY_CLEAR_TASK and Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }
        // 데이터 리스너
        BleManager.instance.onCharacteristicChanged = { gatt, characteristic ->
            readValue = characteristic.value.toString(Charset.forName("UTF-8"))
            Log.d("BleManager", readValue)
        }

        if (readValue == null || readValue!!.contains(":")) {
            // 데이터 요청
            BleManager.instance.write("W00" + nowTime() + "0000")
        }
        startPeriod()

        val left = chart1.axisLeft
        val right = chart1.axisRight
        left.apply {
            setDrawLabels(false) // no axis labels
            setDrawAxisLine(false) // no axis line
            setDrawGridLines(false) // no grid lines
            setDrawZeroLine(true) // draw a zero line
        }

        right.apply {
            setDrawLabels(false) // no axis labels
            setDrawAxisLine(false) // no axis line
            setDrawGridLines(false) // no grid lines
            setDrawZeroLine(true) // draw a zero line
        }

        chart1.apply {
            axisRight.isEnabled = false // no right axis
            axisLeft.isEnabled = false // no right axis
        }

        if (readValue != null) {
            //데이터 정제
            //데이터 계산, 그래프 나타내내기 위해 필요
            LineChartDummyData()
        } else {
            LineChartDummyData()
        }

    }

    fun nowTime(): String? {

        // 현재시간을 msec 으로 구한다.
        var now = System.currentTimeMillis();
        // 현재시간을 date 변수에 저장한다.
        var date = Date(now);
        // 시간을 나타냇 포맷을 정한다 ( yyyy/MM/dd 같은 형태로 변형 가능 )
        var simpleDataFormat = SimpleDateFormat("HHmm");
        // nowDate 변수에 값을 저장한다.
        var formatDate = simpleDataFormat.format(date)
        Log.d("TEST", "Result format : $formatDate")

        return formatDate
    }

    fun startPeriod() {
        timer.schedule(addTask, 0, 5 * (60 * 1000)) //// 0초후 첫실행, Interval분마다 계속실행

        addTask = object : TimerTask() {
            override fun run() {
                //주기적으로 실행할 작업 추가
                BleManager.instance.write("W00" + nowTime() + "0000")
            }
        }
    }
    fun stopPeriod() {
        //Timer 작업 종료
        if (timer != null) timer.cancel()
    }

    fun LineChartDummyData() {
        var dataSet = LineDataSet(
            listOf(
                Entry(10f, 11f),
                Entry(20f, 87f),
                Entry(30f, 80f),
                Entry(40f, 89f),
                Entry(50f, 54f),
                Entry(60f, 25f)
            ), "미세"
        )


        dataSet.apply {
            color = ColorTemplate.rgb("#B0C17E")
            setDrawCircles(false)
            setDrawValues(false)
            fillAlpha = 65
            fillColor = ColorTemplate.getHoloBlue()
            highLightColor = Color.rgb(176, 193, 126)
            setDrawCircleHole(false)
        }

        val dataSet1 = LineDataSet(
            listOf(
                Entry(10f, 34f),
                Entry(20f, 43f),
                Entry(30f, 56f),
                Entry(40f, 97f),
                Entry(50f, 73f),
                Entry(60f, 97f)
            ), "초미세"
        )

        dataSet1.apply {
            color = ColorTemplate.rgb("#86A83E")
            setDrawCircles(false)
            setDrawValues(false)
            fillAlpha = 65
            fillColor = ColorTemplate.getHoloBlue()
            highLightColor = Color.rgb(134, 193, 126)
            setDrawCircleHole(false)
        }

        val dataSet2 = LineDataSet(
            listOf(
                Entry(10f, 68f),
                Entry(20f, 149f),
                Entry(30f, 98f),
                Entry(40f, 63f),
                Entry(50f, 72f),
                Entry(60f, 80f)
            ), "극초미세"
        )
        dataSet2.apply {
            color = ColorTemplate.rgb("#122716")
            setDrawCircles(false)
            setDrawValues(false)
            fillAlpha = 65
            fillColor = ColorTemplate.getHoloBlue()
            highLightColor = Color.rgb(134, 193, 126)
            setDrawCircleHole(false)
        }

        val lineData = LineData(dataSet, dataSet1, dataSet2)
        chart1.apply {
            description.isEnabled = false // disable description text
            data = lineData
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPeriod()
    }
}
