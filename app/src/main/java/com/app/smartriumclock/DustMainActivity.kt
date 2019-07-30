package com.app.smartriumclock

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
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
import kotlinx.android.synthetic.main.activity_dust_main.*
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*


class DustMainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    override fun onNavigationItemSelected(p0: MenuItem): Boolean {
        dl_main_drawer_root.closeDrawer(GravityCompat.START)
        return false
    }

    lateinit var drawerToggle: ActionBarDrawerToggle

    val timer by lazy { Timer() }
    //Data
    var nav_item = ArrayList<String>()
    //val compositeDisposable = CompositeDisposable()

    // Data Sets
    private val pm10DataSet by lazy { generateLineDataSet("미세", "#B0C17E") }
    private val pm2_5DataSet by lazy { generateLineDataSet("초미세", "#86A83E") }
    private val pm1DataSet by lazy { generateLineDataSet("극초미세", "#122716") }

    private val onNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.navigation_all -> {
                setAllChart()
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


        mypage_btn.setOnClickListener {
            val intent = Intent(this, PlantInfoMainActivity::class.java)
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

        val listAdapter = ListAdapter(this, nav_item)
        var layoutManager = LinearLayoutManager(this)
        recycler_drawer_list.apply {
            setLayoutManager(layoutManager)
            adapter = listAdapter
        }


        val left = dust_chart.axisLeft


        val xAxis = dust_chart.xAxis // x 축 설정
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            valueFormatter = object : ValueFormatter() {

                private val mFormat = SimpleDateFormat("HH:mm", Locale.KOREA)
                //MM월 W주
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

        dust_chart.apply {
            axisLeft.isEnabled = true
        }
        //LineChartDummyData()


    }

    private fun setTopPM10(data: BleReceive) {
        dust_num.text = data.data.toInt().toString()

        if (data.data.toInt() > 50) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                com.app.smartriumclock.notification.NotificationManager.sendNotification(
                    this,
                    1,
                    com.app.smartriumclock.notification.NotificationManager.Channel.MESSAGE,
                    getString(R.string.notification_channel_dust_title),
                    getString(R.string.notification_channel_dust_description)
                )
                nowTime()?.let { it1 -> nav_item.add("$it1- 미세먼지 나쁨") }
            } else {
                sendNotificationUnder(
                    getString(R.string.notification_channel_dust_title),
                    getString(R.string.notification_channel_dust_description)
                )
            }
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                com.app.smartriumclock.notification.NotificationManager.sendNotification(
                    this,
                    1,
                    com.app.smartriumclock.notification.NotificationManager.Channel.MESSAGE,
                    getString(R.string.notification_channel_dust_title_good),
                    getString(R.string.notification_channel_dust_description_good)
                )
                nowTime()?.let { it1 -> nav_item.add("$it1- 미세먼지 좋음") }
            } else {
                sendNotificationUnder(
                    getString(R.string.notification_channel_dust_title_good),
                    getString(R.string.notification_channel_dust_description_good)
                )
            }
        }
        recycler_drawer_list.adapter?.notifyDataSetChanged()
    }

    private fun setTopPM2_5(data: BleReceive) {
        ultra_dust_num.text = data.data.toInt().toString()
        if (data.data.toInt() > 70) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                com.app.smartriumclock.notification.NotificationManager.sendNotification(
                    this,
                    1,
                    com.app.smartriumclock.notification.NotificationManager.Channel.COMMENT,
                    getString(R.string.notification_channel_ultra_dust_title),
                    getString(R.string.notification_channel_ultra_dust_description)
                )
                nowTime()?.let { it1 -> nav_item.add("$it1- 미세먼지 나쁨") }
            } else {
                sendNotificationUnder(
                    getString(R.string.notification_channel_ultra_dust_title),
                    getString(R.string.notification_channel_ultra_dust_description)
                )
            }
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                com.app.smartriumclock.notification.NotificationManager.sendNotification(
                    this,
                    1,
                    com.app.smartriumclock.notification.NotificationManager.Channel.COMMENT,
                    getString(R.string.notification_channel_ultra_dust_title_good),
                    getString(R.string.notification_channel_ultra_dust_description_good)
                )
                nowTime()?.let { it1 -> nav_item.add("$it1- 미세먼지 좋음") }
            } else {
                sendNotificationUnder(
                    getString(R.string.notification_channel_ultra_dust_title_good),
                    getString(R.string.notification_channel_ultra_dust_description_good)
                )
            }
        }
        //recycler_drawer_list.adapter?.notifyDataSetChanged()
    }

    private fun setTopPM1(data: BleReceive) {
        super_ultra_dust_num.text = data.data.toInt().toString()
        if (data.data.toInt() > 50) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                com.app.smartriumclock.notification.NotificationManager.sendNotification(
                    this,
                    1,
                    com.app.smartriumclock.notification.NotificationManager.Channel.MESSAGE,
                    getString(R.string.notification_channel_dust_title),
                    getString(R.string.notification_channel_dust_description)
                )
                nowTime()?.let { it1 -> nav_item.add("$it1- 미세먼지 나쁨") }
            } else {
                sendNotificationUnder(
                    getString(R.string.notification_channel_dust_title),
                    getString(R.string.notification_channel_dust_description)
                )
            }
        }
        //recycler_drawer_list.adapter?.notifyDataSetChanged()
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

    fun setAllChart() {
        dust_chart.data = LineData(listOf(pm10DataSet, pm2_5DataSet, pm1DataSet))

        dust_chart.notifyDataSetChanged()
        dust_chart.invalidate()
    }

    fun setDustChart() {
        dust_chart.data = LineData(listOf(pm10DataSet))

        dust_chart.notifyDataSetChanged()
        dust_chart.invalidate()
    }

    fun setUltraDustChart() {
        dust_chart.data = LineData(listOf(pm2_5DataSet))

        dust_chart.notifyDataSetChanged()
        dust_chart.invalidate()
    }

    fun setSuperUltraDustChart() {
        dust_chart.data = LineData(listOf(pm1DataSet))


        dust_chart.notifyDataSetChanged()
        dust_chart.invalidate()
    }

    @SuppressLint("CheckResult")
    fun setAll() {

        // 데이터셋 최초에 설정
        dust_chart.data = LineData(listOf(pm10DataSet, pm2_5DataSet, pm1DataSet))

        MainApplication.database
            .bleReceiveDao()
            .getAllPM10()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    pm10DataSet.values =
                        it.mapIndexed { index, bleReceive ->
                            Entry(
                                bleReceive.date.toFloat(),
                                bleReceive.data.toFloat()
                            )
                        }
                    dust_chart.data.notifyDataChanged()
                    dust_chart.notifyDataSetChanged()
                    dust_chart.invalidate()

                    // Set Top PM10
                    it.firstOrNull { receive -> receive.data.toLong() > 0 }?.run { setTopPM10(this) }
                },
                {
                    it.printStackTrace()
                }
            )

        MainApplication.database
            .bleReceiveDao()
            .getAllPM2_5()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    pm2_5DataSet.values =
                        it.mapIndexed { index, bleReceive ->
                            Entry(
                                bleReceive.date.toFloat(),
                                bleReceive.data.toFloat()
                            )
                        }
                    dust_chart.data.notifyDataChanged()
                    dust_chart.notifyDataSetChanged()
                    dust_chart.invalidate()

                    // Set Top PM2.5
                    it.firstOrNull { receive -> receive.data.toLong() > 0 }?.run { setTopPM2_5(this) }
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
                    pm1DataSet.values =
                        it.mapIndexed { index, bleReceive ->
                            Entry(
                                bleReceive.date.toFloat(),
                                bleReceive.data.toFloat()
                            )
                        }
                    dust_chart.data.notifyDataChanged()
                    dust_chart.notifyDataSetChanged()
                    dust_chart.invalidate()

                    // Set Top PM1
                    it.firstOrNull { receive -> receive.data.toLong() > 0 }?.run { setTopPM1(this) }
                },
                {
                    it.printStackTrace()
                }
            )
    }

    fun nowTime(): String? {

        // 현재시간을 msec 으로 구한다.
        var now = System.currentTimeMillis()
        // 현재시간을 date 변수에 저장한다.
        var date = Date(now)
        // 시간을 나타냇 포맷을 정한다 ( yyyy/MM/dd 같은 형태로 변형 가능 )
        var simpleDataFormat = SimpleDateFormat("yyyyMMdd")
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

        timer.schedule(addTask, 0, 10 * 1000) //// 0초후 첫실행, Interval분마다 계속실행
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
