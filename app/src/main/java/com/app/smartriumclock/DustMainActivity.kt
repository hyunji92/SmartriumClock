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
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
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

    // Data Sets
    private val pm10DataSet by lazy { generateLineDataSet("미세", "#B0C17E") }
    private val pm2_5DataSet by lazy { generateLineDataSet("초미세", "#86A83E") }
    private val pm1DataSet by lazy { generateLineDataSet("극초미세", "#122716") }

    // Data Group
    // daily, weekly, monthly, yearly
    private var group = "daily"
        set(value) {
            field = value

            // Dispose Already Observed DataSource
            bleReceiveDisposable.dispose()

            // Initialize New Disposable
            bleReceiveDisposable = CompositeDisposable()

            // Observe New DataSource
            setAll()
        }

    // Composite Disposable
    private var bleReceiveDisposable = CompositeDisposable()

    // Labels
    private var xLabels = arrayListOf<Long>()

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
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            fillAlpha = 65
            fillColor = ColorTemplate.getHoloBlue()
            highLightColor = Color.rgb(176, 193, 126)

            // 한개 짜리 데이터가 보이지 않아서 활성화
            setDrawCircles(true)
            setDrawCircleHole(true)
            circleColors = listOf(Color.BLACK)

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
        }

        // 현재 베터리 요청
        BleManager.instance.writeQueue(BleManager.Command.Battery, Date())

        // 과거 데이터 부터 현재 시간의 데이터 까지 요청
        for (i in 0..Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            val date = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, i)
            }.time
            BleManager.instance.writeQueue(BleManager.Command.Humidity, date)
            BleManager.instance.writeQueue(BleManager.Command.PM1, date)
            BleManager.instance.writeQueue(BleManager.Command.PM10, date)
            BleManager.instance.writeQueue(BleManager.Command.PM2_5, date)
            BleManager.instance.writeQueue(BleManager.Command.Illuminance, date)
            BleManager.instance.writeQueue(BleManager.Command.Temperature, date)
        }

        setAll()
        startPeriod()

        val listAdapter = ListAdapter(this, nav_item)
        var layoutManager = LinearLayoutManager(this)
        recycler_drawer_list.apply {
            setLayoutManager(layoutManager)
            adapter = listAdapter
        }

        // 기본 데이터 설정
        setGraph("HH:00")


        // 일별
        day.setOnClickListener {
            setGraph("HH:00")

            group = "daily"
        }

        // 주별
        week.setOnClickListener {
            setGraph("MM월 W주")

            group = "weekly"
        }

        // 웗별
        month.setOnClickListener {
            setGraph("MM월")

            group = "monthly"
        }

        // 년별
        year.setOnClickListener {
            setGraph("yyyy년")

            group = "yearly"
        }
    }


    //https://devming.tistory.com/217
    private fun setGraph(pattern: String) {
        val left = dust_chart.axisLeft
        val xAxis = dust_chart.xAxis // x 축 설정
        xAxis.apply {
            position = XAxis.XAxisPosition.BOTTOM
            valueFormatter = object : ValueFormatter() {

                private val mFormat = SimpleDateFormat(pattern, Locale.KOREA)
                override fun getFormattedValue(value: Float): String {
                    try {
                        val time = xLabels[value.toInt()]
                        return mFormat.format(Date(time))
                    } catch (e: Exception) {
                        return ""
                    }
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
    }

    private fun setTopPM10(data: BleReceive) {
        dust_num.text = data.data.toInt().toString()


        if (data.data.toInt() < 30) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                com.app.smartriumclock.notification.NotificationManager.sendNotification(
                    this,
                    1,
                    com.app.smartriumclock.notification.NotificationManager.Channel.MESSAGE,
                    getString(R.string.notification_channel_dust_title_good),
                    getString(R.string.notification_channel_dust_description_good)
                )
                nowTime()?.let { it1 -> nav_item.add("$it1 - 미세먼지 좋음") }
            } else {
                sendNotificationUnder(
                    getString(R.string.notification_channel_dust_title_good),
                    getString(R.string.notification_channel_dust_description_good)
                )
            }
        } else if (data.data.toInt() > 50) {
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
        recycler_drawer_list.adapter?.notifyDataSetChanged()
    }

    private fun setTopPM2_5(data: BleReceive) {
        ultra_dust_num.text = data.data.toInt().toString()

        if (data.data.toInt() < 50) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                com.app.smartriumclock.notification.NotificationManager.sendNotification(
                    this,
                    1,
                    com.app.smartriumclock.notification.NotificationManager.Channel.COMMENT,
                    getString(R.string.notification_channel_ultra_dust_title_good),
                    getString(R.string.notification_channel_ultra_dust_description_good)
                )
                nowTime()?.let { it1 -> nav_item.add("$it1- 초미세먼지 좋음") }
            } else {
                sendNotificationUnder(
                    getString(R.string.notification_channel_ultra_dust_title_good),
                    getString(R.string.notification_channel_ultra_dust_description_good)
                )
            }
        } else if (data.data.toInt() > 70) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                com.app.smartriumclock.notification.NotificationManager.sendNotification(
                    this,
                    1,
                    com.app.smartriumclock.notification.NotificationManager.Channel.COMMENT,
                    getString(R.string.notification_channel_ultra_dust_title),
                    getString(R.string.notification_channel_ultra_dust_description)
                )
                nowTime()?.let { it1 -> nav_item.add("$it1- 초미세먼지 나쁨") }
            } else {
                sendNotificationUnder(
                    getString(R.string.notification_channel_ultra_dust_title),
                    getString(R.string.notification_channel_ultra_dust_description)
                )
            }
        }
        recycler_drawer_list.adapter?.notifyDataSetChanged()
    }

    private fun setTopPM1(data: BleReceive) {
        super_ultra_dust_num.text = data.data.toInt().toString()

        if (data.data.toInt() < 70) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                com.app.smartriumclock.notification.NotificationManager.sendNotification(
                    this,
                    1,
                    com.app.smartriumclock.notification.NotificationManager.Channel.MESSAGE,
                    getString(R.string.notification_channel_super_ultra_dust_title_good),
                    getString(R.string.notification_channel_super_ultra_dust_description_good)
                )
                nowTime()?.let { it1 -> nav_item.add("$it1- 극초미세먼지 좋음") }
            } else {
                sendNotificationUnder(
                    getString(R.string.notification_channel_super_ultra_dust_title_good),
                    getString(R.string.notification_channel_super_ultra_dust_description_good)
                )
            }
        } else if (data.data.toInt() > 80) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                com.app.smartriumclock.notification.NotificationManager.sendNotification(
                    this,
                    1,
                    com.app.smartriumclock.notification.NotificationManager.Channel.MESSAGE,
                    getString(R.string.notification_channel_super_ultra_dust_title),
                    getString(R.string.notification_channel_super_ultra_dust_description)
                )
                nowTime()?.let { it1 -> nav_item.add("$it1- 극초미세먼지 나쁨") }
            } else {
                sendNotificationUnder(
                    getString(R.string.notification_channel_super_ultra_dust_title),
                    getString(R.string.notification_channel_super_ultra_dust_description)
                )
            }
        }
        recycler_drawer_list.adapter?.notifyDataSetChanged()
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

    private fun getDataSource(command: String, group: String): Observable<List<BleReceive>> {
        val dao = MainApplication.database.bleReceiveDao()
        return when (group) {
            "daily" -> {
                val startedAt = Calendar.getInstance().apply {
                    set(Calendar.HOUR, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val endedAt = Calendar.getInstance().apply {
                    timeInMillis = startedAt.timeInMillis
                    add(Calendar.DATE, 1)// 하루 추가
                }
                dao.getToday(command, startedAt.timeInMillis, endedAt.timeInMillis)
            }
            "weekly" -> dao.getWeekly(command)
            "monthly" -> dao.getMonthly(command)
            "yearly" -> dao.getYearly(command)
            else -> dao.getAll(command)
        }
    }

    private fun setAll() {

        // 데이터셋 최초에 설정
        dust_chart.data = LineData(listOf(pm10DataSet, pm2_5DataSet, pm1DataSet))

        // Command Set
        val disposables =
            mapOf(Pair("01", pm10DataSet), Pair("02", pm2_5DataSet), Pair("03", pm1DataSet)).map { commandSet ->
                getDataSource(commandSet.key, this.group)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(
                        {
                            // 데이터
                            commandSet.value.values =
                                it.mapIndexed { index, bleReceive ->
                                    //                                    Log.d(
//                                        "DustMain",
//                                        "Index : ${index.toFloat()}, value : ${bleReceive.data.toFloat()}"
//                                    )
                                    Entry(index.toFloat(), bleReceive.data.toFloat())
                                }

                            // Labels
                            it.mapIndexed { index, bleReceive ->
                                //                                Log.d("DustMainXAxis", "Time : ${bleReceive.date.toLong()}")
                                xLabels.add(index, bleReceive.date.toLong())
                            }

                            // Status
//                            Log.d("DustMain", "Result : (${commandSet.key}, ${commandSet.value.values.toString()}})")

                            // Update XAxis Label Count
                            updateXAxisLabelCount()

                            dust_chart.data.notifyDataChanged()
                            dust_chart.notifyDataSetChanged()
                            dust_chart.invalidate()
                        },
                        {
                            it.printStackTrace()
                        }
                    )
            }

        disposables.forEach { bleReceiveDisposable.add(it) }

        // 최신 데이터요청
        MainApplication.database.bleReceiveDao().getRecent(BleManager.Command.PM10.value)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    it.firstOrNull { receive -> receive.data.toLong() > 0 }?.run { setTopPM10(this) }
                },
                {}
            ).apply { bleReceiveDisposable.add(this) }

        MainApplication.database.bleReceiveDao().getRecent(BleManager.Command.PM2_5.value)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    it.firstOrNull { receive -> receive.data.toLong() > 0 }?.run { setTopPM2_5(this) }
                },
                {}
            ).apply { bleReceiveDisposable.add(this) }

        MainApplication.database.bleReceiveDao().getRecent(BleManager.Command.PM1.value)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                {
                    it.firstOrNull { receive -> receive.data.toLong() > 0 }?.run { setTopPM1(this) }
                },
                {}
            ).apply { bleReceiveDisposable.add(this) }
    }

    private fun updateXAxisLabelCount() {
        val count = maxOf(
            pm10DataSet.entryCount,
            pm2_5DataSet.entryCount,
            pm1DataSet.entryCount
        )
        // Log.d("DustMainXAxis", "Count ( ${count}, true ) ")
        dust_chart.xAxis.setLabelCount(count, true)
        dust_chart.invalidate()
    }

    fun nowTime(): String? {

        // 현재시간을 msec 으로 구한다.
        var now = System.currentTimeMillis()
        // 현재시간을 date 변수에 저장한다.
        var date = Date(now)
        // 시간을 나타냇 포맷을 정한다 ( yyyy/MM/dd 같은 형태로 변형 가능 )
        var simpleDataFormat = SimpleDateFormat("MM.dd")
        // nowDate 변수에 값을 저장한다.
        var formatDate = simpleDataFormat.format(date)
        // Log.d("TEST", "Result format : $formatDate")

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
                    // Log.d("DustMainActivity", "Command : ${command}, Date : ${date}, Data : ${data}")
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

