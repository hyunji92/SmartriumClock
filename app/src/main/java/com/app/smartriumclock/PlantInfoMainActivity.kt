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
import android.graphics.drawable.ColorDrawable
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
import kotlinx.android.synthetic.main.activity_plant_main.*
import java.text.SimpleDateFormat
import java.util.*


class PlantInfoMainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    override fun onNavigationItemSelected(p0: MenuItem): Boolean {
        dl_main_drawer_root.closeDrawer(GravityCompat.START)
        return false
    }

    val timer by lazy { Timer() }

    val compositeDisposable = CompositeDisposable()
    //Data
    var nav_item = ArrayList<String>()

    lateinit var drawerToggle: ActionBarDrawerToggle

    // Data Sets
    private val temperatureDataSet by lazy { generateLineDataSet("온도", "#B0C17E") }
    private val humidityDataSet by lazy { generateLineDataSet("습도", "#86A83E") }
    private val illuminanceDataSet by lazy { generateLineDataSet("조도", "#122716") }


    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_all -> {
                setAllChart()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_temperature -> {
                setTemperatureChart()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_huminity -> {
                setHumidityChart()
                return@OnNavigationItemSelectedListener true
            }
            R.id.navigation_illu -> {
                setIlluminanceChart()
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


    private fun generateLineDataSet(label: String, hex: String): LineDataSet {
        return LineDataSet(listOf(), label).apply {
            color = ColorTemplate.rgb(hex)
            setDrawCircles(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            fillAlpha = 65
            fillColor = ColorTemplate.getHoloBlue()
            highLightColor = Color.rgb(176, 193, 126)
            setDrawCircleHole(false)

            // Drawable
            setDrawFilled(true)
            fillDrawable = ColorDrawable(Color.parseColor(hex))
        }
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


        setGraph("HH:mm")

        day.setOnClickListener {
            setGraph("HH:mm")
        }
        week.setOnClickListener {
            setGraph("MM월 W주")
        }

        month.setOnClickListener {
            setGraph("MM월")
        }

        year.setOnClickListener {
            setGraph("yyyy년")
        }

    }

    private fun setGraph(pattern: String) {

        val left = plant_chart.axisLeft
        val xAxis = plant_chart.xAxis // x 축 설정
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            valueFormatter = object : ValueFormatter() {

                private val mFormat = SimpleDateFormat(pattern, Locale.KOREA)
                override fun getFormattedValue(value: Float): String {
                    //  val millis = TimeUnit.HOURS.toMillis(value.toLong())
                    //  eturn mFormat.format(Date(millis))
                    return mFormat.format(Date(value.toLong()))
                }
            }
        }

        left.apply {
            setLabelCount(8, true)
            setDrawLabels(true) // no axis labels
            setDrawAxisLine(false) // no axis line
            setDrawGridLines(false) // no grid lines
            setDrawZeroLine(true) // draw a zero line
            granularity = 1f
        }

        plant_chart.apply {
            axisLeft.isEnabled = true
        }
    }

    private fun setTemperature(data: BleReceive) {
        temper_num.text = data.data.toInt().toString()
        if (data.data.toInt() < 7) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                com.app.smartriumclock.notification.NotificationManager.sendNotification(
                    this,
                    1,
                    com.app.smartriumclock.notification.NotificationManager.Channel.MESSAGE,
                    getString(R.string.notification_channel_temper_title),
                    getString(R.string.notification_channel_temper_description)
                )
                nowTime()?.let { it1 -> nav_item.add("$it1 - 온도 낮음!!") }
            } else {
                sendNotificationUnder(
                    getString(R.string.notification_channel_temper_title),
                    getString(R.string.notification_channel_temper_description)
                )
            }
        }
        recycler_drawer_list_plant.adapter?.notifyDataSetChanged()
    }

    private fun setHumidity(data: BleReceive) {
        humidity_num.text = data.data.toInt().toString()

    }

    private fun setIlluminance(data: BleReceive) {
        illu_num.text = data.data.toInt().toString()
    }

    @SuppressLint("CheckResult")
    fun setAll() {
        // 데이터셋 최초에 설정
        plant_chart.data = LineData(listOf(temperatureDataSet, humidityDataSet, illuminanceDataSet))

        MainApplication.database
            .bleReceiveDao()
            .getAllTemperature()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    temperatureDataSet.values =
                        it.mapIndexed { index, bleReceive ->
                            Entry(
                                bleReceive.date.toFloat(),
                                bleReceive.data.toFloat()
                            )
                        }
                    plant_chart.data.notifyDataChanged()
                    plant_chart.notifyDataSetChanged()
                    plant_chart.invalidate()

                    // Set Top PM10
                    it.firstOrNull { receive -> receive.data.toLong() > 0 }?.run { setTemperature(this) }
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
                    humidityDataSet.values =
                        it.mapIndexed { index, bleReceive ->
                            Entry(
                                bleReceive.date.toFloat(),
                                bleReceive.data.toFloat()
                            )
                        }
                    plant_chart.data.notifyDataChanged()
                    plant_chart.notifyDataSetChanged()
                    plant_chart.invalidate()

                    // Set Top PM2.5
                    it.firstOrNull { receive -> receive.data.toLong() > 0 }?.run { setHumidity(this) }
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
                    illuminanceDataSet.values =
                        it.mapIndexed { index, bleReceive ->
                            Entry(
                                bleReceive.date.toFloat(),
                                bleReceive.data.toFloat()
                            )
                        }
                    plant_chart.data.notifyDataChanged()
                    plant_chart.notifyDataSetChanged()
                    plant_chart.invalidate()

                    // Set Top PM1
                    it.firstOrNull { receive -> receive.data.toLong() > 0 }?.run { setIlluminance(this) }
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

    fun setTemperatureChart() {
        plant_chart.data = LineData(listOf(temperatureDataSet))
        plant_chart.notifyDataSetChanged()
        plant_chart.invalidate()
    }

    fun setHumidityChart() {
        plant_chart.data = LineData(listOf(humidityDataSet))
        plant_chart.notifyDataSetChanged()
        plant_chart.invalidate()
    }

    fun setIlluminanceChart() {
        plant_chart.data = LineData(listOf(illuminanceDataSet))
        plant_chart.notifyDataSetChanged()
        plant_chart.invalidate()
    }

    fun setAllChart() {
        plant_chart.data = LineData(listOf(temperatureDataSet, humidityDataSet, illuminanceDataSet))

        plant_chart.notifyDataSetChanged()
        plant_chart.invalidate()
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

        timer.schedule(addTask, 0, 10 * 1000) //// 0초후 첫실행, Interval분마다 계속실행
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

    private class ListAdapter(context: Context, data: List<String>) : RecyclerView.Adapter<ListAdapter.myViewHolder>() {

        internal var context: Context
        internal var mData: List<String>

        init {
            this.context = context
            this.mData = data
        }

        override fun getItemCount(): Int = mData.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListAdapter.myViewHolder {
            val view = LayoutInflater.from(context).inflate(R.layout.notification_list_item, parent, false)
            return myViewHolder(view)
        }

        override fun onBindViewHolder(holder: ListAdapter.myViewHolder, position: Int) {
            holder.nav.text = mData[position]
        }

        inner class myViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            internal var nav: TextView = itemView.findViewById(R.id.nav) as TextView
        }
    }
}
