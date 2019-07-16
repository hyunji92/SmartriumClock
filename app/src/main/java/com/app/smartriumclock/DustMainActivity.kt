package com.app.smartriumclock

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.app.smartriumclock.model.BleReceive
import com.app.smartriumclock.setting.MyPageActivity
import com.app.smartriumclock.setting.SettingActivity
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.activity_dust_main.*
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.formatter.ValueFormatter
import java.util.concurrent.TimeUnit


class DustMainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    override fun onNavigationItemSelected(p0: MenuItem): Boolean {
        dl_main_drawer_root.closeDrawer(GravityCompat.START)
        return false
    }

    lateinit var drawerToggle: ActionBarDrawerToggle
    lateinit var drawerToggle2: ActionBarDrawerToggle

    val timer by lazy { Timer() }

    //val compositeDisposable = CompositeDisposable()

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
        if (dl_main_drawer_root.isDrawerOpen(GravityCompat.START)) {
            dl_main_drawer_root.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }

    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        drawerToggle.syncState()
        drawerToggle2.syncState()

    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        drawerToggle.onConfigurationChanged(newConfig)
        drawerToggle2.onConfigurationChanged(newConfig)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true
        } else if (drawerToggle2.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("CheckResult")
    fun test() {
        MainApplication.database
            .bleReceiveDao()
            .getAllPM2_5()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    dust_chart.data.clearValues()
                    dust_chart.data.addDataSet(LineDataSet(
                        it.mapIndexed { index, bleReceive -> Entry(index.toFloat(), bleReceive.data.toFloat()) },
                        "초미세"
                    ).apply {
                        color = ColorTemplate.rgb("#B0C17E")
                        setDrawCircles(false)
                        setDrawValues(false)
                        fillAlpha = 65
                        fillColor = ColorTemplate.getHoloBlue()
                        highLightColor = Color.rgb(176, 193, 126)
                        setDrawCircleHole(false)
                    })

                    dust_chart.notifyDataSetChanged()
                    dust_chart.invalidate()
                },
                {
                    it.printStackTrace()
                }
            )
        //       .apply { compositeDisposable.add(this) }
    }


    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navView: BottomNavigationView = findViewById(R.id.nav_view)
        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)

        drawerToggle = ActionBarDrawerToggle(
            this,
            dl_main_drawer_root,
            toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        )
        dl_main_drawer_root.addDrawerListener(drawerToggle)
        nv_main_navigation_root.setNavigationItemSelectedListener(this)

        drawerToggle2 = ActionBarDrawerToggle(
            this,
            dl_main_drawer_root,
            toolbar,
            R.string.drawer_open,
            R.string.drawer_close
        )
        dl_main_drawer_root.addDrawerListener(drawerToggle2)
        navigation_view_second.setNavigationItemSelectedListener(this)

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

        setAll()
        startPeriod()

        var test = MainApplication.database
            .bleReceiveDao()
            .getAllPM10()

        dust_num.text = test.takeLast(1).toString()


        var test1 =  MainApplication.database
            .bleReceiveDao()
            .getAllPM2_5()

        ultra_dust_num.text = test1.takeLast(1).toString()

        var test2 =  MainApplication.database
            .bleReceiveDao()
            .getAllPM1()

        super_ultra_dust_num.text = test2.takeLast(1).toString()

        MainApplication.database
            .bleReceiveDao()
            .getAllPM10()
            .takeLast(2)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    dust_num.text = it.last().data.toFloat().toString()
                },
                {
                    it.printStackTrace()
                }
            )
        MainApplication.database
            .bleReceiveDao()
            .getAllPM2_5()
            .takeLast(1)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    ultra_dust_num.text = it.last().data.toFloat().toString()
                },
                {
                    it.printStackTrace()
                }
            )
        MainApplication.database
            .bleReceiveDao()
            .getAllPM1()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    super_ultra_dust_num.text = it.last().data.toFloat().toString()
                },
                {
                    it.printStackTrace()
                }
            )

        val left = dust_chart.axisLeft
        val right = dust_chart.axisRight

        val xAxis = dust_chart.xAxis // x 축 설정
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            valueFormatter = object : ValueFormatter() {

                private val mFormat = SimpleDateFormat("HH:mm", Locale.KOREA)

                override fun getFormattedValue(value: Float): String {

                    val millis = TimeUnit.HOURS.toMillis(value.toLong())
                    return mFormat.format(Date(millis))
                }
            }
        }

        left.apply {
            setLabelCount(6,true)
            setDrawLabels(true) // no axis labels
            setDrawAxisLine(false) // no axis line
            setDrawGridLines(false) // no grid lines
            setDrawZeroLine(true) // draw a zero line
            granularity = 1f
        }

        right.apply {
            setDrawLabels(true) // no axis labels
            setDrawAxisLine(false) // no axis line
            setDrawGridLines(false) // no grid lines
            setDrawZeroLine(true) // draw a zero line
        }

        dust_chart.apply {
            axisRight.isEnabled = true // no right axis
            axisLeft.isEnabled = true
        }
        LineChartDummyData()

    }

    fun LineChartDummyData() {
        var dataSet = LineDataSet(
            listOf(
//                Entry(10f, 11f),
//                Entry(20f, 87f),
//                Entry(30f, 80f),
//                Entry(40f, 89f),
//                Entry(50f, 54f),
//                Entry(60f, 25f)
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
//                Entry(10f, 34f),
//                Entry(20f, 43f),
//                Entry(30f, 56f),
//                Entry(40f, 97f),
//                Entry(50f, 73f),
//                Entry(60f, 97f)
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
//                Entry(10f, 68f),
//                Entry(20f, 149f),
//                Entry(30f, 98f),
//                Entry(40f, 63f),
//                Entry(50f, 72f),
//                Entry(60f, 80f)
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
        dust_chart.apply {
            description.isEnabled = false // disable description text
            data = lineData
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
                    dust_chart.data.clearValues()
                    dust_chart.data.addDataSet(LineDataSet(
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
                    dust_chart.notifyDataSetChanged()
                    dust_chart.invalidate()
                },
                {
                    it.printStackTrace()
                }
            )

        // .apply { compositeDisposable.add(this) }
    }

    fun setUltraDustChart() {
        MainApplication.database
            .bleReceiveDao()
            .getAllPM2_5()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    dust_chart.data.clearValues()
                    dust_chart.data.addDataSet(LineDataSet(
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

                    dust_chart.notifyDataSetChanged()
                    dust_chart.invalidate()
                },
                {
                    it.printStackTrace()
                }
            )
        // .apply { compositeDisposable.add(this) }
    }

    fun setSuperUltraDustChart() {
        MainApplication.database
            .bleReceiveDao()
            .getAllPM1()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    dust_chart.data.clearValues()
                    dust_chart.data.addDataSet(LineDataSet(
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

                    dust_chart.notifyDataSetChanged()
                    dust_chart.invalidate()
                },
                {
                    it.printStackTrace()
                }
            )
        //.apply { compositeDisposable.add(this) }
    }

    @SuppressLint("CheckResult")
    fun setAll() {
        MainApplication.database
            .bleReceiveDao()
            .getAllPM10()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    dust_chart.data.clearValues()
                    dust_chart.data.addDataSet(LineDataSet(
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

                    dust_chart.notifyDataSetChanged()
                    dust_chart.invalidate()
                },
                {
                    it.printStackTrace()
                }
            )
        //.apply { compositeDisposable.add(this) }

        MainApplication.database
            .bleReceiveDao()
            .getAllPM2_5()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    dust_chart.data.clearValues()
                    dust_chart.data.addDataSet(LineDataSet(
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

                    dust_chart.notifyDataSetChanged()
                    dust_chart.invalidate()
                },
                {
                    it.printStackTrace()
                }
            )
        // .apply { compositeDisposable.add(this) }

        MainApplication.database
            .bleReceiveDao()
            .getAllPM1()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    dust_chart.data.clearValues()
                    dust_chart.data.addDataSet(LineDataSet(
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

                    dust_chart.notifyDataSetChanged()
                    dust_chart.invalidate()
                },
                {
                    it.printStackTrace()
                }
            )
        // .apply { compositeDisposable.add(this) }

    }

    fun nowTime(): String? {

        // 현재시간을 msec 으로 구한다.
        var now = System.currentTimeMillis()
        // 현재시간을 date 변수에 저장한다.
        var date = Date(now)
        // 시간을 나타냇 포맷을 정한다 ( yyyy/MM/dd 같은 형태로 변형 가능 )
        var simpleDataFormat = SimpleDateFormat("HHmm")
        // nowDate 변수에 값을 저장한다.
        var formatDate = simpleDataFormat.format(date)
        Log.d("TEST", "Result format : $formatDate")

        return formatDate
    }

    fun startPeriod() {

        var addTask = object : TimerTask() {
            override fun run() {
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
                //주기적으로 실행할 작업 추가
                BleManager.instance.writeQueue(BleManager.Command.Battery, Date())
                BleManager.instance.writeQueue(BleManager.Command.Humidity, Date())
                BleManager.instance.writeQueue(BleManager.Command.PM1, Date())
                BleManager.instance.writeQueue(BleManager.Command.PM10, Date())
                BleManager.instance.writeQueue(BleManager.Command.PM2_5, Date())
                BleManager.instance.writeQueue(BleManager.Command.Illuminance, Date())
                BleManager.instance.writeQueue(BleManager.Command.Temperature, Date())
            }
        }

        timer.schedule(addTask, 0, 5 * 1000) //// 0초후 첫실행, Interval분마다 계속실행
    }

    fun stopPeriod() {
        //Timer 작업 종료
        if (timer != null) timer.cancel()
    }


    override fun onDestroy() {
        super.onDestroy()
        stopPeriod()
        // compositeDisposable.dispose()
    }
}
