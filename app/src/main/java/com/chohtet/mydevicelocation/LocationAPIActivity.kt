package com.chohtet.mydevicelocation

import android.Manifest
import android.animation.Animator
import android.animation.AnimatorInflater
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import kotlinx.android.synthetic.main.activity_location_a_p_i.*

class LocationAPIActivity : AppCompatActivity(),FetchAddressAsyncTask.TaskCompleted {
    private val REQUEST_LOCATION=111
    private var trackingLocation=false
    private lateinit var mlocation: Location
    private lateinit var mRotateAnim: Animator
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var mLocationCallBack: LocationCallback
    override fun onResume() {
        if (trackingLocation){
            startTrackingLocation()
        }
        super.onResume()
    }

    override fun onPause() {
        if (trackingLocation){
            stopLocationTracking()
            trackingLocation=true
        }
        super.onPause()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_a_p_i)

        mFusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(this)
        mLocationCallBack= object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                if (trackingLocation){
                    val lastLocation=locationResult?.lastLocation
                    FetchAddressAsyncTask(this@LocationAPIActivity,this@LocationAPIActivity)
                        .execute(lastLocation)
                    lastLocation?.let {
                        val currentLoc=Location("LOC_FROM")
                        currentLoc.latitude= it.latitude
                        currentLoc.longitude=it.longitude
                        val desLoc=Location("LOC_TO")
                        desLoc.latitude=16.856832
                        desLoc.longitude=96.213009
                        tvDistance.text="Distance ".plus(currentLoc.distanceTo(desLoc)/1000)
                    }

                }
            }
        }
        mRotateAnim = AnimatorInflater.loadAnimator(this,R.animator.rotate)
        mRotateAnim.setTarget(imgIcon)
        btnStartLocation.setOnClickListener {
            if (trackingLocation){
                stopLocationTracking()
            }else {
                startTrackingLocation()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (REQUEST_LOCATION == requestCode){
            if (grantResults.isNotEmpty() && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                startTrackingLocation()
            }else{
                showToast("Location permission was denied")
            }
        }
    }
    private fun startTrackingLocation(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),REQUEST_LOCATION)
        }else{
            //showToast("Location Permission have granted")
//            mFusedLocationProviderClient.lastLocation.addOnSuccessListener {location ->
//                if (location != null){
////                    mlocation=location
////                    tvLat.text=getString(R.string.location_text,
////                    mlocation.latitude,mlocation.longitude,mlocation.time)
//                    FetchAddressAsyncTask(this,this).execute(location)
//                }else{
//                    tvLat.text="No Location"
//                }
//            }
            mFusedLocationProviderClient.requestLocationUpdates(
                getLocationRequest(),mLocationCallBack,null)
        }
        tvAddress.text="Loading...."
        mRotateAnim.start()
        trackingLocation=true
        btnStartLocation.text= "Stop Tracking Location"
    }
    private fun stopLocationTracking(){
        if (trackingLocation){
            trackingLocation=false
            btnStartLocation.text="Start Tracking Location"
            tvAddress.text="Default"
            mRotateAnim.end()
            mFusedLocationProviderClient.removeLocationUpdates(mLocationCallBack)
        }

    }
    private fun showToast(message:String){
        Toast.makeText(this,message, Toast.LENGTH_LONG).show()
    }
    private fun getLocationRequest(): LocationRequest {
        val locationRequest= LocationRequest()
        locationRequest.interval=10000
        locationRequest.fastestInterval=5000
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        return locationRequest
    }
    private fun locationDistance(){

    }

    override fun onTaskCompleted(result: String?) {
        tvAddress.text=getString(R.string.address_text,result,System.currentTimeMillis())
    }
}