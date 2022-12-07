package com.habidev.qing

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.*
import android.provider.ContactsContract
import android.util.Log
import androidx.annotation.RequiresApi
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

class RecognitionService: Service(), CommandFunction {
    private val tag = "RecognitionService"

    private lateinit var clientId: String
    private lateinit var redirectUri: String
    private lateinit var spotifyMusicUri: String
    private var spotifyAppRemote: SpotifyAppRemote? = null

    private lateinit var weather: Weather

    private lateinit var notificationBuilder: Notification.Builder
    private lateinit var notificationManager: NotificationManager

    private val notificationID = 101
    private val notificationTitle: CharSequence = "Qing Recognition"
    private val channelId: String = "1"
    private val channelName: String = "Qing Recognition Channel"

    private val recognitionBinder = RecognitionBinder()

    private lateinit var speechRecognizer: ServiceSpeechRecognizer
    private lateinit var audioRecorder: AudioRecorder

    override fun onCreate() {
        speechRecognizer = ServiceSpeechRecognizer(this)

        speechRecognizer.startListening()

        initNotificationManager()
        initNotificationChannel()
        initNotification()

        initSpotifyValue()
        connectSpotify()

        initWeather()

        startForeground(notificationID, notificationBuilder.build())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "micOn" -> unMuteMic()
            "micOff" -> muteMic()
        }

        return super.onStartCommand(intent, flags, startId)
    }

    override fun onBind(intent: Intent?): IBinder {
        return recognitionBinder
    }

    override fun onDestroy() {
        speechRecognizer.stopListening()
        speechRecognizer.destroy()
    }

    inner class RecognitionBinder: Binder() {
        fun getService(): RecognitionService {
            return this@RecognitionService
        }
    }

    private fun initNotificationManager() {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private fun initNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)

            notificationManager.createNotificationChannel(notificationChannel)
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag")
    private fun initNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val contentIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, notificationIntent, 0)
        }

        notificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
        } else {
            Notification.Builder(this)
        }.setSmallIcon(R.drawable.notification_icon)
            .setContentTitle(notificationTitle)
            .setContentIntent(contentIntent)
            .setOngoing(true)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            addActionButtonToNotification()
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    @SuppressLint("UnspecifiedImmutableFlag")
    private fun addActionButtonToNotification() {
        val micOnIntent = Intent(this, RecognitionService::class.java).apply {
            action = "micOn"
            putExtra(channelId, 0)
        }

        val micOffIntent = Intent(this, RecognitionService::class.java).apply {
            action = "micOff"
            putExtra(channelId, 0)
        }

        val micOnPendingIntent = PendingIntent.getService(this, 0, micOnIntent, PendingIntent.FLAG_IMMUTABLE)
        val micOffPendingIntent = PendingIntent.getService(this, 0, micOffIntent, PendingIntent.FLAG_IMMUTABLE)

        val micOnIcon = Icon.createWithResource(applicationContext, R.drawable.mic_on)
        val micOffIcon = Icon.createWithResource(applicationContext, R.drawable.mic_off)

        val micOnAction = Notification.Action.Builder(micOnIcon, "micOn", micOnPendingIntent).build()
        val micOffAction = Notification.Action.Builder(micOffIcon, "micOff", micOffPendingIntent).build()

        notificationBuilder
            .addAction(micOnAction)
            .addAction(micOffAction)
    }

    private fun restartServiceSpeechRecognizer() {
        speechRecognizer.startListening()
    }

    override fun command(resultList: ArrayList<String>?) {
        val command: String = resultList.toString()

        if (command.contains("전화")) {
            Log.d(tag, "Call")

            val whoArray = arrayListOf<String>()

            for (result in resultList!!) {
                val index = result.indexOf("한테")
                val who: String = result.subSequence(0, index).toString()

                Log.d(tag, "Call$who")

                whoArray.add(who)
            }

            call(whoArray)
        } else if (command.contains("날씨")) {
            getCurrentWeather()
        } else if (command.contains("문자")) {
            Log.d(tag, "Message")
        } else if (command.contains("타이머")) {
            Log.d(tag, "Timer")
        } else if (command.contains("노래")) {
            Log.d(tag, "Play Music")
            playMusic()
        } else if (command.contains("에서") && command.contains("원")) {
            for (result in resultList!!) {
                val placeIndex = result.indexOf("에서")
                val moneyIndex = result.indexOf("원")

                if (placeIndex != -1 && moneyIndex != -1) {
                    val place = result.subSequence(0, placeIndex).toString()
                    val money = result.subSequence(placeIndex + 3, moneyIndex).toString()

                    Log.d(tag, place)

                    if (money.contains(",")) {
                        Log.d(tag, money)
                    } else {
                        try {
                            money.toInt()

                            Log.d(tag, money)
                        } catch (e: java.lang.NumberFormatException) {
                            val testMoney = korToNumber(money)
                            Log.e(tag, testMoney.toString(), e)
                        }
                    }
                }

//                      spendMoney(money)
            }
        }

        restartServiceSpeechRecognizer()
    }

    override fun startRecording() {
        audioRecorder = AudioRecorder(this)
        audioRecorder.startRecord()
    }

    override fun stopRecording() {
        audioRecorder.stopRecord()
    }

    override fun playTriggerSound() {
        val toneGenerator = ToneGenerator(AudioManager.STREAM_ALARM, 100)
        toneGenerator.startTone(ToneGenerator.TONE_CDMA_PIP, 150)
    }

    private fun initSpotifyValue() {
        clientId = resources.getString(R.string.clientId)
        spotifyMusicUri = resources.getString(R.string.spotifyMusicUri)
        redirectUri = resources.getString(R.string.redirectUri)
    }

    private fun connectSpotify() {
        val connectionParams = ConnectionParams.Builder(clientId)
            .setRedirectUri(redirectUri)
            .showAuthView(true)
            .build()

        SpotifyAppRemote.connect(this, connectionParams, object: Connector.ConnectionListener {
            override fun onConnected(appRemote: SpotifyAppRemote?) {
                Log.d(tag, "Spotify Connected")

                spotifyAppRemote = appRemote
            }

            override fun onFailure(error: Throwable?) {
                Log.e(tag, error?.message, error)
            }
        })
    }

    private fun disconnectSpotify() {
        spotifyAppRemote?.let {
            Log.d(tag, "Spotify Disconnected")
            SpotifyAppRemote.disconnect(it)
        }
    }

    private fun playMusic() {
        spotifyAppRemote?.playerApi?.play(spotifyMusicUri)
    }

    private fun pauseMusic() {
        spotifyAppRemote?.playerApi?.pause()
    }

    private fun call(whoArray: ArrayList<String>) {
        Log.d(tag, "Call")

        val number = findContact(whoArray)
        if (number != "") {
            val tel = Uri.parse("tel:$number")
            val callIntent = Intent(Intent.ACTION_DIAL, tel)
            callIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(callIntent)
        } else {
            Log.e(tag, "Can't Find Contact")
        }
    }

    private fun findContact(whoArray: ArrayList<String>): String {
        var number = ""
        val uri: Uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI

        for (who in whoArray) {
            Log.d(tag, who)

            val cursor: Cursor? = contentResolver.query(uri, null, "${ContactsContract.Contacts.DISPLAY_NAME} LIKE ?", arrayOf("%$who%"), null)

            if (cursor != null && cursor.count > 0) {
                cursor.moveToFirst()

                val numberColumn = cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)

                val match = "[^0-9]"

                val tempNumber = cursor.getString(numberColumn)

                number = tempNumber.replace(match.toRegex(), "")

                cursor.close()

                return number
            }

            cursor?.close()
        }

        return number
    }

    private fun message(who: String, message: String) {
        Log.d(tag, "message")
    }

    private fun timer() {
        Log.d(tag, "timer")
    }

    private fun spendMoney(money: String) {
        Log.d(tag, money)
    }

    private fun korToNumber(input: String): Long {
        var result: Long = 0
        var tmpResult: Long = 0
        var num: Long = 0
        val NUMBER = "영일이삼사오육칠팔구"
        val UNIT = "십백천만억조"
        val UNIT_NUM = longArrayOf(
            10,
            100,
            1000,
            10000,
            Math.pow(10.0, 8.0).toLong(),
            Math.pow(10.0, 12.0).toLong()
        )
        val st = StringTokenizer(input, UNIT, true)
        while (st.hasMoreTokens()) {
            val token: String = st.nextToken()
            //숫자인지, 단위(UNIT)인지 확인
            val check = NUMBER.indexOf(token)
            if (check == -1) { //단위인 경우
                if ("만억조".indexOf(token) == -1) {
                    tmpResult += (if (num != 0L) num else 1) * UNIT_NUM[UNIT.indexOf(token)]
                } else {
                    tmpResult += num
                    result += (if (tmpResult != 0L) tmpResult else 1) * UNIT_NUM.get(
                        UNIT.indexOf(
                            token
                        )
                    )
                    tmpResult = 0
                }
                num = 0
            } else { //숫자인 경우
                num = check.toLong()
            }
        }

        return result + tmpResult + num
    }

    private fun getCurrentWeather() {
        Log.d(tag, "weather")

        weather.getWeatherCurrent { weatherInfo, tempKelvin, minTemp, maxTemp ->
            val temp: String = (tempKelvin.toFloat() - 273.15).toInt().toString()

            weatherNotification(weatherInfo, temp, minTemp.toString(), maxTemp.toString())
        }
    }

    private fun weatherNotification(weatherInfo: String, temp: String, minTemp: String, maxTemp: String) {
        val weatherIntent = Intent(this, WeatherActivity::class.java)
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.getActivity(this, 0, weatherIntent, PendingIntent.FLAG_IMMUTABLE)
        } else {
            PendingIntent.getActivity(this, 0, weatherIntent, 0)
        }

        val weatherNotificationBuilder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, channelId)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("$temp'C")
                .setContentText("$weatherInfo/Min:$minTemp/Max:$maxTemp")
                .setContentIntent(pendingIntent)
        } else {
            Notification.Builder(this)
                .setSmallIcon(R.drawable.notification_icon)
                .setContentTitle("$temp'C")
                .setContentText("$weatherInfo/Min:$minTemp/Max:$maxTemp")
        }

        notificationManager.notify(202, weatherNotificationBuilder.build())
    }

    private fun initWeather() {
        val executor = ThreadPoolExecutor(5, 5, 50, TimeUnit.MILLISECONDS, LinkedBlockingQueue())
        weather = Weather(this, executor)
    }

    private fun muteMic() {
        Log.d(tag, "mute")

        speechRecognizer.stopListening()
        speechRecognizer.cancel()
    }

    private fun unMuteMic() {
        Log.d(tag, "unmute")

        speechRecognizer.startListening()
    }


}