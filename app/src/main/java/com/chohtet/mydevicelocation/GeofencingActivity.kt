package com.chohtet.mydevicelocation

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_geofencing.*

class GeofencingActivity : AppCompatActivity(),
    OnMapReadyCallback, GoogleMap.OnMapClickListener, GoogleMap.OnMarkerClickListener,
    GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
    LocationListener {
    private var myGoogleMap: GoogleMap? = null
    private var googleApiClient: GoogleApiClient? = null
    private lateinit var lastLocation: Location
    private lateinit var mLocationCallBack: LocationCallback
    private var locMarker: Marker? = null
    private var geoMarker: Marker? = null
    private val GEO_DURATION = 60 * 60 * 1000
    private val GEO_REQUEST = "GEO_REQUEST"
    private val GE0_RADIUS = 500f // meters
    private val GEO_REQ_CODE = 444
    private var pendingIntent: PendingIntent? = null
    private var geoLimit: Circle? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    companion object {
        private const val EXTRA_LAT_LNG = "EXTRA_LAT_LNG"
        fun newIntent(context: Context, message:String): Intent {
            val intent = Intent(context, MainActivity::class.java)
            intent.putExtra(EXTRA_LAT_LNG, message)
            return intent
        }
    }

    private val TAG = "MYGEO"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_geofencing)
        fusedLocationProviderClient= LocationServices.getFusedLocationProviderClient(this)
        val mapFragment = supportFragmentManager.findFragmentById(R.id.myMap) as SupportMapFragment
        mapFragment.getMapAsync(this@GeofencingActivity)
        mapFragment.getMapAsync(this)
        createGoogleApi()
        locationCallBack()
        //getLastKnowLocation()
    }

    override fun onStart() {
        super.onStart()
        googleApiClient!!.connect()
    }

    override fun onStop() {
        super.onStop()
        googleApiClient!!.disconnect()
    }


    private fun createGoogleApi() {
        Log.d(TAG, "create Google API")
        if (googleApiClient == null) {
            googleApiClient = GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()
        }
    }

    private fun getLastKnowLocation() {
        Log.d(TAG, "Get Lat known location")
        if (checkPermission()) {
//            LocationServices.getFusedLocationProviderClient(this).lastLocation.addOnSuccessListener {
//                lastLocation = it
//                Log.i(TAG,"Last Loc: lat-> ${lastLocation.latitude} lon:-> ${lastLocation.longitude}")
//            }
           fusedLocationProviderClient.requestLocationUpdates(getLocationRequest(), mLocationCallBack, null)
           // writeLastLocation()
        }else{
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),99)
        }
    }

    @RequiresApi(29)
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
       if (requestCode == 99 && grantResults.isNotEmpty()){
           if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                 fusedLocationProviderClient.requestLocationUpdates(getLocationRequest(), mLocationCallBack, null)
           }
       }
    }

    private fun getLocationRequest(): LocationRequest {
        val locationRequest = LocationRequest()
        locationRequest.interval = 10000
        locationRequest.fastestInterval = 5000
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        return locationRequest
    }

    private fun writeLastLocation() {
        writeActualLocation(lastLocation)
    }

    private fun startLocationUpdate() {
    }

    private fun locationCallBack() {
        mLocationCallBack = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                lastLocation = locationResult!!.lastLocation
//                val mapFragment = supportFragmentManager.findFragmentById(R.id.myMap) as SupportMapFragment
//                mapFragment.getMapAsync(this@GeofencingActivity)

                writeLastLocation()
            }
        }

    }

    private fun checkPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_BACKGROUND_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
            this, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createGeoFence(latLng: LatLng, radius: Float): Geofence {
        Log.d(TAG, "create geo fence")
        return Geofence.Builder()
            .setRequestId(GEO_REQUEST)
            .setCircularRegion(latLng.latitude, latLng.longitude, radius)
            .setExpirationDuration(GEO_DURATION.toLong())
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER.or(Geofence.GEOFENCE_TRANSITION_EXIT))
            .build()
    }

    private fun createGeoFenceRequest(geofence: Geofence): GeofencingRequest {
        Log.d(TAG, "create geo fence request")
        return GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()
    }

    private fun createGeoFenceIntent(): PendingIntent {
        Log.d(TAG, "createGeofencePendingIntent")
        if (pendingIntent != null)
            return pendingIntent!!
        val intent = Intent(this, GeofenceService::class.java)
        return PendingIntent.getBroadcast(
            this,
            GEO_REQ_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    private fun addGeoFence(request: GeofencingRequest) {
        Log.d(TAG, "add Geofence Request")
        if (checkPermission()) {
            LocationServices.getGeofencingClient(this)
                .addGeofences(request, createGeoFenceIntent())
                .addOnSuccessListener {
                    Log.d(TAG, "add Geofence Succeed")
                    drawGeoFence()
                }
                .addOnFailureListener {
                    Log.d(TAG, "add Geofence fail: ${it.localizedMessage}")
                    it.printStackTrace()
                }

        }
    }

    private fun drawGeoFence() {
        if (geoLimit != null)
            geoLimit!!.remove()
        val circleOptions = CircleOptions()
            .center(geoMarker!!.position)
            .strokeColor(Color.argb(50, 70, 70, 70))
            .fillColor(Color.argb(100, 150, 150, 150))
            .radius(GE0_RADIUS.toDouble())
        geoLimit = myGoogleMap!!.addCircle(circleOptions)
    }

    private fun startGeoFence() {
        Log.d(TAG, "Start geofence")
        if (geoMarker != null) {
            val geofence = createGeoFence(geoMarker!!.position, GE0_RADIUS)
            val geoRequest = createGeoFenceRequest(geofence)
            addGeoFence(geoRequest)
        } else Log.d(TAG, "Geo marker is null")

    }


    private fun geofenceMarker(latLng: LatLng?) {
        val markerOptions = MarkerOptions().title("Geo Marker").position(latLng!!)
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE))
        if (myGoogleMap != null) {
            if (geoMarker != null)
                geoMarker!!.remove()
            geoMarker = myGoogleMap!!.addMarker(markerOptions)
        }

    }

    private fun markerLocation(latLng: LatLng) {
        val markerOptions = MarkerOptions()
            .title("${latLng.latitude} ${latLng.longitude}")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            .position(latLng)
        if (myGoogleMap != null) {
            //remove interior  marker
            if (locMarker != null)
                locMarker!!.remove()
            locMarker = myGoogleMap!!.addMarker(markerOptions)
            val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, 14f)
            myGoogleMap!!.animateCamera(cameraUpdate)


        }

    }

    private fun writeActualLocation(loc: Location) {
        markerLocation(LatLng(loc.latitude, loc.longitude))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
       menuInflater.inflate(R.menu.main_menu,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.startGeo) {
            startGeoFence()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onMapReady(map: GoogleMap?) {
        Log.d(TAG, "onMapReady")
        myGoogleMap = map
        //Add marker
//        val myLoc = LatLng(16.866070, 96.195129)
//        myGoogleMap?.addMarker(MarkerOptions().position(myLoc).title("My Location"))
//        //myGoogleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(myLoc))
//        myGoogleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(myLoc,14f))
        myGoogleMap?.setOnMapClickListener(this)
        myGoogleMap?.setOnMarkerClickListener(this)
    }

    override fun onMapClick(latLng: LatLng?) {
        Log.d(TAG, "onMapClicked: $latLng")
        geofenceMarker(latLng)
    }

    override fun onMarkerClick(marker: Marker?): Boolean {
        Log.d(TAG, "onMarkerClicked: ${marker?.position}")
        return false
    }

    override fun onConnected(bundle: Bundle?) {
        Log.i(TAG, "Google API client connection callback connected")
        getLastKnowLocation()
    }

    override fun onConnectionSuspended(p0: Int) {
        Log.w(TAG, "Google API client connection callback suspected")
    }

    override fun onConnectionFailed(p0: ConnectionResult) {
        Log.w(TAG, "Google API client connection fail listener failed")
    }

    override fun onLocationChanged(location: Location?) {
        Log.w(TAG, "On Location Changed")
        lastLocation = location!!
        writeActualLocation(lastLocation)
    }
}