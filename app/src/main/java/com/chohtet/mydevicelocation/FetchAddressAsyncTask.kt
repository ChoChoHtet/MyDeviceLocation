package com.chohtet.mydevicelocation

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.AsyncTask
import android.text.TextUtils
import android.util.Log
import java.io.IOException
import java.lang.IllegalArgumentException
import java.util.*

/**
 * Param -> to pass parameter into doInBackground()
 * Progress ->mark progress into onProgressUpdate()
 * Result -> to publish result in onPostExecute()
 * AsyncTask<Param,Progress,Result>
 */
class FetchAddressAsyncTask(
    private val context: Context,
    private val listener:TaskCompleted) :AsyncTask<Location,Void,String>(){
    override fun doInBackground(vararg params: Location?): String {
        val geocoder=Geocoder(context, Locale.getDefault())
        val location=params[0]
        var addresses= emptyList<Address>()
        var resultMessage=""
        try {
            addresses = geocoder.getFromLocation(location!!.latitude,location.longitude,1)
            //Catch Geocoder is not able to find address for the given coordinates
            if (addresses.isEmpty()){
                if (resultMessage.isEmpty()){
                    resultMessage="No address Found."
                }
            }else{
                val address= addresses[0]
                val addressPart= arrayListOf<String>()
                for (i in 0..address.maxAddressLineIndex){
                    addressPart.add(address.getAddressLine(i))
                }
               // resultMessage=TextUtils.join("\n",addressPart)
                resultMessage=address.countryCode
                    .plus(" ").plus(address.countryName)
                    .plus(" ").plus(address.locale)
            }
        }catch (e:IOException){
            // Catch network or other I/O problems
           resultMessage= "Service not available."
            Log.e("MyLocation",resultMessage,e)
        }catch (e2:IllegalArgumentException){
            // Catch invalid latitude or longitude values
            resultMessage="Invalid lat lon used."
            Log.e("MyLocation",resultMessage,e2)
        }
        return resultMessage
    }

    override fun onPostExecute(result: String?) {
        listener.onTaskCompleted(result)
        super.onPostExecute(result)
    }
    interface TaskCompleted{
        fun onTaskCompleted(result: String?)
    }

}