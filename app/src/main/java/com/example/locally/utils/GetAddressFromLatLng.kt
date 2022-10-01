package com.example.locally.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class GetAddressFromLatLng(private val context: Context,
                           private val lat:Double,
                           private val lng:Double) {

    private val geocoder: Geocoder = Geocoder(context, Locale.getDefault())  //to decode the lat and lng value to text address
    private lateinit var mAddressListener:AddressListener


    suspend fun launchBackgroundProcessForRequest() {
        val address=getAddress()

        withContext(Dispatchers.Main){
            //switch to Main thread, cuz we're going to update the UI related values from here on
            // if we get a valid address
            if (address.isEmpty()) {
                mAddressListener.onError()
            } else {
                mAddressListener.onAddressFound(address)  //updaing UI
            }
        }
    }


    private suspend fun getAddress():String{
        try {
            //there may be multiple locations/places associated with the lat and lng, we take the top/most relevant address
            val addressList:List<Address>?=geocoder.getFromLocation(lat,lng,1)

            if(!addressList.isNullOrEmpty()){
                val address: Address =addressList[0]
                val sb=StringBuilder()
                for(i in 0..address.maxAddressLineIndex){  //Returns the largest index currently in use to specify an address line.
                    sb.append(address.getAddressLine(i)+" ")
                }
                sb.deleteCharAt(sb.length-1)   //to remove the last " "

                return sb.toString()
            }
        }
        catch (e:Exception){
            e.printStackTrace()
        }
        return ""
    }

    fun setCustomAddressListener(addressListener: AddressListener){  //to attach the listener to the class property
        this.mAddressListener=addressListener
    }

    //can be defined anywhere
    interface AddressListener{
        fun onAddressFound(address:String)
        fun onError()
    }

}