package com.app.smartriumclock

import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.AppCompatActivity
import com.app.smartriumclock.setting.MyPageActivity
import com.app.smartriumclock.setting.SettingActivity
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import kotlinx.android.synthetic.main.activity_dust_main.*
import kotlinx.android.synthetic.main.activity_main.*

class PlantInfoMainActivity : AppCompatActivity() {

//    private lateinit var dataSet: LineDataSet
//    private lateinit var dataSet1: LineDataSet
//    private lateinit var dataSet2: LineDataSet

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_all -> {
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dust -> {

                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_ultra_dust -> {

                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_super_ultra_dust -> {

                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_setting -> {

                val intent = Intent(this, SettingActivity::class.java)
                intent.addFlags(FLAG_ACTIVITY_SINGLE_TOP and Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                return@OnNavigationItemSelectedListener true
            }
        }
        false
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_main)
//        setSupportActionBar(toolbar)

        val navView: BottomNavigationView = findViewById(R.id.nav_plant_view)
        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)

        mypage_btn.setOnClickListener {
            val intent = Intent(this, MyPageActivity::class.java)
            intent.addFlags(FLAG_ACTIVITY_SINGLE_TOP and Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }


//        // 데이터 리스너
//        BleManager.instance.onCharacteristicChanged = { gatt, characteristic ->
//            val readValue = characteristic.value.toString(Charset.forName("UTF-8"))
//            Log.d("BleManager", readValue)
//        }
//
//        // 데이터 요청
//        BleManager.instance.write("W0015300000")


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


        var dataSet = LineDataSet(listOf(
                Entry(10f, 11f),
                Entry(20f, 87f),
                Entry(30f, 80f),
                Entry(40f, 89f),
                Entry(50f, 54f),
                Entry(60f, 25f)
        ), "온도")


        dataSet.apply {
            color = ColorTemplate.rgb("#B0C17E")
            setDrawCircles(false)
            setDrawValues(false)
            fillAlpha = 65
            fillColor = ColorTemplate.getHoloBlue()
            highLightColor = Color.rgb(176, 193, 126)
            setDrawCircleHole(false)
        }

        val dataSet1 = LineDataSet(listOf(
                Entry(10f, 34f),
                Entry(20f, 43f),
                Entry(30f, 56f),
                Entry(40f, 97f),
                Entry(50f, 73f),
                Entry(60f, 97f)
        ), "습도")

        dataSet1.apply {
            color = ColorTemplate.rgb("#86A83E")
            setDrawCircles(false)
            setDrawValues(false)
            fillAlpha = 65
            fillColor = ColorTemplate.getHoloBlue()
            highLightColor = Color.rgb(134, 193, 126)
            setDrawCircleHole(false)
        }

        val dataSet2 = LineDataSet(listOf(
                Entry(10f, 68f),
                Entry(20f, 149f),
                Entry(30f, 98f),
                Entry(40f, 63f),
                Entry(50f, 72f),
                Entry(60f, 80f)
        ), "조도")
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
        // disable description text
        chart1.apply {
            description.isEnabled = false
            data = lineData
        }

    }
}
