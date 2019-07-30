package com.app.smartriumclock

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.content.res.Configuration
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.view.GravityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.smartriumclock.model.BleReceive
import com.app.smartriumclock.setting.SettingActivity
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_plant_info_main.*
import kotlinx.android.synthetic.main.activity_plant_info_main.mypage_btn
import kotlinx.android.synthetic.main.activity_plant_info_main.toolbar

import kotlinx.android.synthetic.main.activity_plant_main.*
import kotlinx.android.synthetic.main.activity_plant_main.dl_main_drawer_root
import kotlinx.android.synthetic.main.activity_plant_main.nv_main_navigation_root
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit


class PlantInfoMainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    override fun onNavigationItemSelected(p0: MenuItem): Boolean {
        dl_main_drawer_root.closeDrawer(GravityCompat.START)
        return false
    }

    val timer by lazy { Timer() }

    val compositeDisposable = CompositeDisposable()
    //Data
    var nav_item =  ArrayList<String>()

    lateinit var drawerToggle: ActionBarDrawerToggle

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_all -> {
                setAll()
                topNum()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_temperature -> {
                setTemperatureChart()
                topNum()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_huminity -> {
                setHumidityChart()
                topNum()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_illu -> {
                setIlluminanceChart()
                topNum()
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

    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        drawerToggle.onConfigurationChanged(newConfig)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (drawerToggle.onOptionsItemSelected(item)) {
            return true
        }
        return super.onOptionsItemSelected(item)
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_plant_main)
//        setSupportActionBar(toolbar)

        val navView: BottomNavigationView = findViewById(R.id.nav_plant_view)
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


        mypage_btn.setOnClickListener {
            val intent = Intent(this, DustMainActivity::class.java)
            intent.addFlags(FLAG_ACTIVITY_SINGLE_TOP and Intent.FLAG_ACTIVITY_CLEAR_TOP)
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
            Log.d("PlantInfoMainActivity", "Command : ${command}, Date : ${date}, Data : ${data}")
            if (data.toInt() > 40) {
                nav_item.add("$date : 미세먼지 나쁨 ")
            } else {
                nav_item.add("$date : 미세먼지 좋음 ")
            }
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

        val listAdapter = ListAdapter(this, nav_item)
        var layoutManager = LinearLayoutManager(this)
        recycler_drawer_list_plant.apply {
            setLayoutManager(layoutManager)
            adapter = listAdapter
        }


        topNum()

        val left = plant_chart.axisLeft
        val right = plant_chart.axisRight


        val xAxis = plant_chart.xAxis // x 축 설정
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
            setDrawLabels(true) // no axis labels
            setDrawAxisLine(false) // no axis line
            setDrawGridLines(false) // no grid lines
            setDrawZeroLine(true) // draw a zero line
        }

        right.apply {
            setDrawLabels(true) // no axis labels
            setDrawAxisLine(false) // no axis line
            setDrawGridLines(false) // no grid lines
            setDrawZeroLine(true) // draw a zero line
        }

        plant_chart.apply {
            axisRight.isEnabled = true // no right axis
            axisLeft.isEnabled = true // no right axis
        }

        LineChartDummyData()
    }

    fun topNum(){
        MainApplication.database
            .bleReceiveDao()
            .getAllTemperature()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    temper_num.text = it[it.count()-1].data.toInt().toString()
                    if (it[it.count()-1].data.toInt() > 50) {
//
                    }
                },
                {
                    it.printStackTrace()
                }
            )
        MainApplication.database
            .bleReceiveDao()
            .getAllHumidity()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    humidity_num.text = it[it.count()-1].data.toInt().toString()
                    if (it[it.count()-1].data.toInt() > 70) {

                    }
                },
                {
                    it.printStackTrace()
                }
            )
        MainApplication.database
            .bleReceiveDao()
            .getAllIlluminance()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    illu_num.text = it[it.count()-1].data.toInt().toString()
                    if (it[it.count()-1].data.toInt() > 50) {

                    }

                },
                {
                    it.printStackTrace()
                }
            )
    }

    @SuppressLint("WrongConstant")
    fun sendNotificationUnder(status: String, message: String) {
        val res = resources
        val notificationIntent = Intent(this, DustMainActivity::class.java)
        notificationIntent.putExtra("notificationId", 9999) //전달할 값
        val contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT)
        val builder = NotificationCompat.Builder(this)
        builder.setContentTitle(status)
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setLargeIcon(BitmapFactory.decodeResource(res, R.mipmap.ic_launcher))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setWhen(System.currentTimeMillis())
            .setDefaults(Notification.DEFAULT_ALL)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_MESSAGE)
                .setPriority(Notification.PRIORITY_HIGH)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
        }
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(1234, builder.build())
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
        plant_chart.apply {
            description.isEnabled = false // disable description text
            data = lineData
        }
    }

    fun setTemperatureChart() {
        MainApplication.database
            .bleReceiveDao()
            .getAllTemperature()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    plant_chart.data.clearValues()
                    plant_chart.data.addDataSet(LineDataSet(
                        it.mapIndexed { index, bleReceive -> Entry(index.toFloat(), bleReceive.data.toFloat()) },
                        "온도"
                    ).apply {
                        color = ColorTemplate.rgb("#B0C17E")
                        setDrawCircles(false)
                        setDrawValues(false)
                        fillAlpha = 65
                        fillColor = ColorTemplate.getHoloBlue()
                        highLightColor = Color.rgb(176, 193, 126)
                        setDrawCircleHole(false)
                    })

                    plant_chart.notifyDataSetChanged()
                    plant_chart.invalidate()
                },
                {
                    it.printStackTrace()
                }
            )
            .apply { compositeDisposable.add(this) }
    }

    fun setHumidityChart() {
        MainApplication.database
            .bleReceiveDao()
            .getAllHumidity()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    plant_chart.data.clearValues()
                    plant_chart.data.addDataSet(LineDataSet(
                        it.mapIndexed { index, bleReceive -> Entry(index.toFloat(), bleReceive.data.toFloat()) },
                        "습도"
                    ).apply {
                        color = ColorTemplate.rgb("#86A83E")
                        setDrawCircles(false)
                        setDrawValues(false)
                        fillAlpha = 65
                        fillColor = ColorTemplate.getHoloBlue()
                        highLightColor = Color.rgb(176, 193, 126)
                        setDrawCircleHole(false)
                    })
                    plant_chart.notifyDataSetChanged()
                    plant_chart.invalidate()
                },
                {
                    it.printStackTrace()
                }
            )
            .apply { compositeDisposable.add(this) }
    }

    fun setIlluminanceChart() {
        MainApplication.database
            .bleReceiveDao()
            .getAllIlluminance()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    plant_chart.data.clearValues()
                    plant_chart.data.addDataSet(LineDataSet(
                        it.mapIndexed { index, bleReceive -> Entry(index.toFloat(), bleReceive.data.toFloat()) },
                        "조도"
                    ).apply {
                        color = ColorTemplate.rgb("#122716")
                        setDrawCircles(false)
                        setDrawValues(false)
                        fillAlpha = 65
                        fillColor = ColorTemplate.getHoloBlue()
                        highLightColor = Color.rgb(176, 193, 126)
                        setDrawCircleHole(false)
                    })

                    plant_chart.notifyDataSetChanged()
                    plant_chart.invalidate()
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
            .getAllTemperature()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    plant_chart.data.clearValues()
                    plant_chart.data.addDataSet(LineDataSet(
                        it.mapIndexed { index, bleReceive -> Entry(index.toFloat(), bleReceive.data.toFloat()) },
                        "온도"
                    ).apply {
                        color = ColorTemplate.rgb("#B0C17E")
                        setDrawCircles(false)
                        setDrawValues(false)
                        fillAlpha = 65
                        fillColor = ColorTemplate.getHoloBlue()
                        highLightColor = Color.rgb(176, 193, 126)
                        setDrawCircleHole(false)
                    })

                    plant_chart.notifyDataSetChanged()
                    plant_chart.invalidate()
                },
                {
                    it.printStackTrace()
                }
            )
            .apply { compositeDisposable.add(this) }

        MainApplication.database
            .bleReceiveDao()
            .getAllHumidity()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    plant_chart.data.clearValues()
                    plant_chart.data.addDataSet(LineDataSet(
                        it.mapIndexed { index, bleReceive -> Entry(index.toFloat(), bleReceive.data.toFloat()) },
                        "습도"
                    ).apply {
                        color = ColorTemplate.rgb("#86A83E")
                        setDrawCircles(false)
                        setDrawValues(false)
                        fillAlpha = 65
                        fillColor = ColorTemplate.getHoloBlue()
                        highLightColor = Color.rgb(176, 193, 126)
                        setDrawCircleHole(false)
                    })

                    plant_chart.notifyDataSetChanged()
                    plant_chart.invalidate()
                },
                {
                    it.printStackTrace()
                }
            )
            .apply { compositeDisposable.add(this) }

        MainApplication.database
            .bleReceiveDao()
            .getAllIlluminance()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    plant_chart.data.clearValues()
                    plant_chart.data.addDataSet(LineDataSet(
                        it.mapIndexed { index, bleReceive -> Entry(index.toFloat(), bleReceive.data.toFloat()) },
                        "조도"
                    ).apply {
                        color = ColorTemplate.rgb("#122716")
                        setDrawCircles(false)
                        setDrawValues(false)
                        fillAlpha = 65
                        fillColor = ColorTemplate.getHoloBlue()
                        highLightColor = Color.rgb(176, 193, 126)
                        setDrawCircleHole(false)
                    })

                    plant_chart.notifyDataSetChanged()
                    plant_chart.invalidate()
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


    override fun onDestroy() {
        super.onDestroy()
        stopPeriod()
        compositeDisposable.dispose()
    }

    private class ListAdapter(context:Context, data:List<String>): RecyclerView.Adapter<ListAdapter.myViewHolder>() {

        internal var context:Context
        internal var mData:List<String>

        init{
            this.context = context
            this.mData = data
        }

        override fun getItemCount(): Int = mData.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType:Int):ListAdapter.myViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.notification_list_item, parent, false)
            return myViewHolder(view)
        }
        override fun onBindViewHolder(holder:ListAdapter.myViewHolder, position:Int) {
            holder.nav.text = mData[position]
        }
        inner class myViewHolder(itemView: View): RecyclerView.ViewHolder(itemView) {
            internal var nav: TextView = itemView.findViewById(R.id.nav) as TextView
        }
    }
}
