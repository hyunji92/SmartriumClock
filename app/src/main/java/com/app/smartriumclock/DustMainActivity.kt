package com.app.smartriumclock

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.app.smartriumclock.model.BleReceive
import com.app.smartriumclock.setting.MyPageActivity
import com.app.smartriumclock.setting.SettingActivity
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.bottomnavigation.BottomNavigationView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_dust_main.*
import java.text.SimpleDateFormat
import java.util.*

class DustMainActivity : AppCompatActivity() {

    var readValue: String? = null

    val timer by lazy { Timer() }

    val compositeDisposable = CompositeDisposable()

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_all -> {
                setAll()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_dust -> {
                setDustChart()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_ultra_dust -> {
                setUltraDustChart()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_super_ultra_dust -> {
                setSuperUltraDustChart()
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
        BleManager.instance.onReceiveData = { command, date, data ->

            // Insert Receive Data
            MainApplication.database.bleReceiveDao().insert(
                BleReceive(
                    command.value,
                    Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, date.substring(0, 2).toInt())
                        set(Calendar.MINUTE, date.substring(2, 4).toInt())
                    }.timeInMillis,
                    data
                )
            )


            Log.d("DustMainActivity", "Command : ${command}, Date : ${date}, Data : ${data}")
        }

        // 요청
        BleManager.instance.writeQueue(BleManager.Command.Battery, Date())
        BleManager.instance.writeQueue(BleManager.Command.Humidity, Date())
        BleManager.instance.writeQueue(BleManager.Command.PM1, Date())
        BleManager.instance.writeQueue(BleManager.Command.PM10, Date())
        BleManager.instance.writeQueue(BleManager.Command.PM2_5, Date())
        BleManager.instance.writeQueue(BleManager.Command.Illuminance, Date())
        BleManager.instance.writeQueue(BleManager.Command.Temperature, Date())

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


    }


    // Test Code
    fun setDustChart() {
        MainApplication.database
            .bleReceiveDao()
            .getAllPM10()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    chart1.data.clearValues()
                    chart1.data.addDataSet(LineDataSet(
                        it.mapIndexed { index, bleReceive -> Entry(index.toFloat(), bleReceive.data.toFloat()) },
                        "미세"
                    ).apply {
                        color = ColorTemplate.rgb("#B0C17E")
                        setDrawCircles(false)
                        setDrawValues(false)
                        fillAlpha = 65
                        fillColor = ColorTemplate.getHoloBlue()
                        highLightColor = Color.rgb(176, 193, 126)
                        setDrawCircleHole(false)
                    })

                    chart1.notifyDataSetChanged()
                    chart1.invalidate()
                },
                {
                    it.printStackTrace()
                }
            )
            .apply { compositeDisposable.add(this) }
    }

    fun setUltraDustChart() {
        MainApplication.database
            .bleReceiveDao()
            .getAllPM2_5()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    chart1.data.clearValues()
                    chart1.data.addDataSet(LineDataSet(
                        it.mapIndexed { index, bleReceive -> Entry(index.toFloat(), bleReceive.data.toFloat()) },
                        "초미세"
                    ).apply {
                        color = ColorTemplate.rgb("#86A83E")
                        setDrawCircles(false)
                        setDrawValues(false)
                        fillAlpha = 65
                        fillColor = ColorTemplate.getHoloBlue()
                        highLightColor = Color.rgb(176, 193, 126)
                        setDrawCircleHole(false)
                    })

                    chart1.notifyDataSetChanged()
                    chart1.invalidate()
                },
                {
                    it.printStackTrace()
                }
            )
            .apply { compositeDisposable.add(this) }
    }

    fun setSuperUltraDustChart() {
        MainApplication.database
            .bleReceiveDao()
            .getAllPM1()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    chart1.data.clearValues()
                    chart1.data.addDataSet(LineDataSet(
                        it.mapIndexed { index, bleReceive -> Entry(index.toFloat(), bleReceive.data.toFloat()) },
                        "극초미세"
                    ).apply {
                        color = ColorTemplate.rgb("#122716")
                        setDrawCircles(false)
                        setDrawValues(false)
                        fillAlpha = 65
                        fillColor = ColorTemplate.getHoloBlue()
                        highLightColor = Color.rgb(176, 193, 126)
                        setDrawCircleHole(false)
                    })

                    chart1.notifyDataSetChanged()
                    chart1.invalidate()
                },
                {
                    it.printStackTrace()
                }
            )
            .apply { compositeDisposable.add(this) }
    }

    fun setAll() {
        MainApplication.database
            .bleReceiveDao()
            .getAllPM10()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    chart1.data.clearValues()
                    chart1.data.addDataSet(LineDataSet(
                        it.mapIndexed { index, bleReceive -> Entry(index.toFloat(), bleReceive.data.toFloat()) },
                        "미세"
                    ).apply {
                        color = ColorTemplate.rgb("#B0C17E")
                        setDrawCircles(false)
                        setDrawValues(false)
                        fillAlpha = 65
                        fillColor = ColorTemplate.getHoloBlue()
                        highLightColor = Color.rgb(176, 193, 126)
                        setDrawCircleHole(false)
                    })

                    chart1.notifyDataSetChanged()
                    chart1.invalidate()
                },
                {
                    it.printStackTrace()
                }
            )
            .apply { compositeDisposable.add(this) }

        MainApplication.database
            .bleReceiveDao()
            .getAllPM2_5()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    chart1.data.clearValues()
                    chart1.data.addDataSet(LineDataSet(
                        it.mapIndexed { index, bleReceive -> Entry(index.toFloat(), bleReceive.data.toFloat()) },
                        "초미세"
                    ).apply {
                        color = ColorTemplate.rgb("#86A83E")
                        setDrawCircles(false)
                        setDrawValues(false)
                        fillAlpha = 65
                        fillColor = ColorTemplate.getHoloBlue()
                        highLightColor = Color.rgb(176, 193, 126)
                        setDrawCircleHole(false)
                    })

                    chart1.notifyDataSetChanged()
                    chart1.invalidate()
                },
                {
                    it.printStackTrace()
                }
            )
            .apply { compositeDisposable.add(this) }

        MainApplication.database
            .bleReceiveDao()
            .getAllPM1()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    chart1.data.clearValues()
                    chart1.data.addDataSet(LineDataSet(
                        it.mapIndexed { index, bleReceive -> Entry(index.toFloat(), bleReceive.data.toFloat()) },
                        "극초미세"
                    ).apply {
                        color = ColorTemplate.rgb("#122716")
                        setDrawCircles(false)
                        setDrawValues(false)
                        fillAlpha = 65
                        fillColor = ColorTemplate.getHoloBlue()
                        highLightColor = Color.rgb(176, 193, 126)
                        setDrawCircleHole(false)
                    })

                    chart1.notifyDataSetChanged()
                    chart1.invalidate()
                },
                {
                    it.printStackTrace()
                }
            )
            .apply { compositeDisposable.add(this) }
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

        var addTask = object : TimerTask() {
            override fun run() {
                //주기적으로 실행할 작업 추가
                BleManager.instance.writeQueue(BleManager.Command.PM1, Date())
            }
        }

        timer.schedule(addTask, 0, 5 * 1000) //// 0초후 첫실행, Interval분마다 계속실행
    }

    fun stopPeriod() {
        //Timer 작업 종료
        if (timer != null) timer.cancel()
    }


//        dataSet2.apply {
//            color = ColorTemplate.rgb("#122716")
//            setDrawCircles(false)
//            setDrawValues(false)
//            fillAlpha = 65
//            fillColor = ColorTemplate.getHoloBlue()
//            highLightColor = Color.rgb(134, 193, 126)
//            setDrawCircleHole(false)
//        }
//
//        val lineData = LineData(dataSet, dataSet1, dataSet2)
//        chart1.apply {
//            description.isEnabled = false // disable description text
//            data = lineData
//        }

    override fun onDestroy() {
        super.onDestroy()
        stopPeriod()
        compositeDisposable.dispose()
    }
}
