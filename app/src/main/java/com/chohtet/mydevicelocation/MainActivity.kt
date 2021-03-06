package com.chohtet.mydevicelocation

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle

import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnLocation.setOnClickListener {
            startActivity(Intent(this,LocationAPIActivity::class.java))
        }
        btnGeofencing.setOnClickListener {
            startActivity(Intent(this,GeofencingActivity::class.java))
        }

    }


}
