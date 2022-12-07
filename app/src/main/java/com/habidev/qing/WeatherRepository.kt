package com.habidev.qing

import android.content.Context
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import java.util.concurrent.Executor
import kotlin.math.max
import kotlin.math.roundToInt

class Weather (
    context: Context,
    private val executor: Executor
) {
    private var location: String = context.resources.getString(R.string.weatherLocation)
    private var apiKey: String = context.resources.getString(R.string.openWeatherApiKey)
    private var weatherUrlCurrent: String = "http://api.openweathermap.org/data/2.5/weather?q=$location,KR&appid=$apiKey"
    private var weatherUrl5days: String = "http://api.openweathermap.org/data/2.5/forecast?q=$location,KR&appid=$apiKey"

    fun getWeatherCurrent (
        callback: (String, String, Int, Int) -> Unit
    ) {
        executor.execute {
            try {
                val con = connect(weatherUrlCurrent)
                con?.requestMethod = "GET"
                val responseCode = con!!.responseCode
                val result = if (responseCode == HttpURLConnection.HTTP_OK) {
                    readBody(con.inputStream)
                } else {
                    readBody(con.errorStream)
                }

                val jsonObject = JSONObject(result)

                val weatherInfo = jsonObject.getJSONArray("weather").getJSONObject(0).getString("main")
                val temp = jsonObject.getJSONObject("main").getString("temp")

                getMinMax { minTemp, maxTemp ->
                    callback(weatherInfo, temp, minTemp, maxTemp)
                }

            } catch (e: java.lang.Exception) {
                Log.e("FAIL", e.toString())
                callback("", "", -99, 99)
            }
        }
    }

    private fun getMinMax(
        callback: (Int, Int) -> Unit
    ) {
        executor.execute {
            try {
                val con = connect(weatherUrl5days)
                con?.requestMethod = "GET"
                val responseCode = con!!.responseCode
                val result = if (responseCode == HttpURLConnection.HTTP_OK) {
                    readBody(con.inputStream)
                } else {
                    readBody(con.errorStream)
                }

                val jsonArray = JSONObject(result).getJSONArray("list")

                val (minTemp, maxTemp) = getMinMaxTemp(jsonArray)

                callback(minTemp.toInt(), maxTemp.toInt())
            } catch (e: java.lang.Exception) {
                Log.e("FAIL", e.toString())
                callback(-99, 99)
            }
        }
    }

    private fun getMinMaxTemp(jsonArray: JSONArray): Pair<Double, Double> {
        var min = 500.0
        var max = 0.0

        for (i in 0 until 8) {
            val weatherJSONObject = jsonArray.getJSONObject(i).getJSONObject("main")

            val minTemp = weatherJSONObject.getString("temp_min").toDouble()
            val maxTemp = weatherJSONObject.getString("temp_max").toDouble()

            min = if (minTemp < min) {
                minTemp
            } else {
                min
            }

            max = if (maxTemp > max) {
                maxTemp
            } else {
                max
            }
        }

        min = kelvinToCelsius(min)
        max = kelvinToCelsius(max)

        return Pair(min, max)
    }

    private fun kelvinToCelsius(temp: Double): Double {
        return temp - 273.15
    }

    private fun connect(apiUrl: String): HttpURLConnection? {
        return try {
            val url = URL(apiUrl)
            url.openConnection() as HttpURLConnection
        } catch (e: MalformedURLException) {
            throw RuntimeException("API URL이 잘못되었습니다. : $apiUrl", e)
        } catch (e: IOException) {
            throw RuntimeException("연결이 실패했습니다. : $apiUrl", e)
        }
    }

    private fun readBody(body: InputStream): String {
        val streamReader = InputStreamReader(body)
        try {
            BufferedReader(streamReader).use { lineReader ->
                val responseBody = StringBuilder()
                var line: String?
                while (lineReader.readLine().also { line = it } != null) {
                    responseBody.append(line)
                }
                return responseBody.toString()
            }
        } catch (e: IOException) {
            throw RuntimeException("API 응답을 읽는데 실패했습니다.", e)
        }
    }
}