package com.example.weather

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.Nullable
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.weather.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.snackbar.Snackbar
import edu.tcu.bmei.weather.model.WeatherResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.min


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var view:View
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var service: WeatherService
    private lateinit var weatherResponse: WeatherResponse
    private lateinit var dialog:Dialog
    private lateinit var current:LocalDateTime

    val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Snackbar.make(view, R.string.location_permission_granted, Snackbar.LENGTH_SHORT).show()
                updateLocationAndWeatherRepeatedly()
            } else {
                Snackbar.make(view, R.string.location_permission_denied, Snackbar.LENGTH_LONG).show()
            }
        }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//Step 4: requestLocationPermission()
//
        binding = ActivityMainBinding.inflate(layoutInflater)
        view = binding.root
        setContentView(view)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        val retrofit = Retrofit.Builder().baseUrl(getString(R.string.base_url))
            .addConverterFactory(GsonConverterFactory.create()).build()

        service = retrofit.create(WeatherService::class.java)
        requestLocationPermission()

    }

    fun requestLocationPermission() {

        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED -> {
                updateLocationAndWeatherRepeatedly()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_COARSE_LOCATION) -> {
                val snackbar = Snackbar.make(
                    view,
                    R.string.location_permission_required,
                    Snackbar.LENGTH_INDEFINITE
                )

                snackbar.setAction("OK"){
                    requestPermissionLauncher.launch(
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                }.show()
            }

            else -> {
                requestPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
            }

        }
    }


    private fun updateLocationAndWeatherRepeatedly() {
        lifecycleScope.launch(Dispatchers.IO) {
            while(true) {
                withContext(Dispatchers.Main) { updateLocationAndWeather() }
                delay(15000)
            }
        }

    }


    private suspend fun updateLocationAndWeather(){
        when(PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) -> {
                showInProgress()
                val cancellationTokenSource = CancellationTokenSource()
                var taskSuccessful = false
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                    null).addOnSuccessListener {
                    taskSuccessful = true
                    if(it != null){
                        updateWeather(it)
                    } else {
                        displayUpdateFailed()

                    }

                }
                withContext(Dispatchers.IO){
                    delay(10000)
                    if(!taskSuccessful){
                        cancellationTokenSource.cancel()
                        withContext(Dispatchers.Main) {
                            displayUpdateFailed()
                        }
                    }
                }
            }
        }

    }

    private fun updateWeather(location: Location) {
        //Timestamping
        current = LocalDateTime.now()
        System.out.println(current)
        val call = service.getWeather(
            location.latitude,
            location.longitude,
            getString(R.string.appid),
            "imperial"
        )
        call.enqueue(
            object: Callback<WeatherResponse> {
                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    val weatherResponseNullable = response.body()
                    if(weatherResponseNullable != null){
                        weatherResponse = weatherResponseNullable
                        displayWeather()
                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                    displayUpdateFailed()
                }
            }
        )



    }


    private fun displayUpdateFailed() {
        System.out.println("SYSTEM UPDATE FAILED")
        var curr = LocalDateTime.now()
        val minutes: Int = ChronoUnit.MINUTES.between(current, curr).toInt()
        val hours: Int = ChronoUnit.HOURS.between(current, curr).toInt()
        if(minutes==0){
            binding.connectionTv.text = getString(R.string.connecting)
        }
        if(minutes==1){
            binding.connectionTv.text = getString(R.string.updated, minutes.toString()+ " minute ago")
        }
        if(minutes>1){
            binding.connectionTv.text = getString(R.string.updated, minutes.toString()+ " minutes ago")
        }
        System.out.println(minutes)

        dialog.dismiss()
    }

    private fun displayWeather(){
        binding.connectionTv.text = getString(R.string.updated,"Just Now")
        binding.cityTv.text = weatherResponse.name
        //setting Sun Data
        var sunset = getDateTime(weatherResponse.sys.sunset)
        var sunrise = getDateTime(weatherResponse.sys.sunrise)
        binding.sunDataTv.text =getString(R.string.sun_data,sunrise,sunset)


        binding.windDataTv.text=getString(R.string.wind_data,weatherResponse.wind.speed,weatherResponse.wind.deg,weatherResponse.wind.gust)
        binding.precipitationDataTv.text = getString(R.string.precipitation_data,weatherResponse.main.humidity,weatherResponse.clouds.all)
        binding.otherDataTv.text = getString(R.string.other_data,weatherResponse.main.feels_like,weatherResponse.visibility*0.000621371,weatherResponse.main.pressure*0.02952998330101)

        var temp = capitalize(weatherResponse.weather[0].description)
        binding.descriptionTv.text = getString(R.string.description,temp,weatherResponse.main.temp_max,weatherResponse.main.temp_min)
        binding.temperatureTv.text = getString(R.string.temperature,weatherResponse.main.temp)
        when(weatherResponse.weather[0].icon){
            "01d" -> binding.conditionIv.setBackgroundResource(R.drawable.ic_01d)
            "01n" -> binding.conditionIv.setBackgroundResource(R.drawable.ic_01n)
            "02d" -> binding.conditionIv.setBackgroundResource(R.drawable.ic_02d)
            "02n" -> binding.conditionIv.setBackgroundResource(R.drawable.ic_02n)
            "03d","03n" -> binding.conditionIv.setBackgroundResource(R.drawable.ic_03)
            "04d","04n" -> binding.conditionIv.setBackgroundResource(R.drawable.ic_04)
            "09d","09n" -> binding.conditionIv.setBackgroundResource(R.drawable.ic_09)
            "10d" -> binding.conditionIv.setBackgroundResource(R.drawable.ic_10d)
            "10n" -> binding.conditionIv.setBackgroundResource(R.drawable.ic_10n)
            "11d","11n" -> binding.conditionIv.setBackgroundResource(R.drawable.ic_11)
            "13d","13n" -> binding.conditionIv.setBackgroundResource(R.drawable.ic_13)
            "50d","50n" -> binding.conditionIv.setBackgroundResource(R.drawable.ic_50)


        }


        dialog.dismiss()
    }

    fun capitalize(str: String): String {
        return str.trim().split("\\s+".toRegex())
            .map { it.capitalize() }.joinToString(" ")
    }


    private fun showInProgress() {
        dialog = Dialog(this)
        dialog.setContentView(R.layout.in_progress)
        dialog.setCancelable(false)
        dialog.show()

    }

    private fun getDateTime(s: Int): String? {
        val dt = Instant.ofEpochSecond(s.toLong())
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
        var temp = dt.toString();
        val dateToFormat = Date(s.toLong()*1000)
        System.out.println(dateToFormat)
        val dateFormatExpression = SimpleDateFormat("hh:mm a")
        val formattedDate = dateFormatExpression.format(dateToFormat)
        return formattedDate
    }
}

