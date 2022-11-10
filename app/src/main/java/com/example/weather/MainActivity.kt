package com.example.weather

import android.app.Instrumentation.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import com.example.weather.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var view:View
    val requestPermissionLauncher=registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ){
        isGranted:Boolean->
        if(isGranted){
            Snackbar.make(
                view,
                R.string.location_permission_granted,
                Snackbar.LENGTH_SHORT
            ).show()
            updateLocationAndWeatherRepeatdly()
        }
        else{
            Snackbar.make(
                view,
                R.string.location_permission_denied,
                Snackbar.LENGTH_LONG
            ).show()
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        view = binding.root
        binding.windCv
        setContentView(R.layout.activity_main)
    }
    private fun updateLocationAndWeatherRepeatdly(){}
}