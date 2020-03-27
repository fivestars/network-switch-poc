package com.fivestars.networkswitchpoc

import android.content.Context
import android.net.*
import android.net.NetworkCapabilities.*
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fivestars.rootutil.RootUtil
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.lang.Exception


class MainActivity : AppCompatActivity() {

    private val cellularRequest: NetworkRequest = NetworkRequest.Builder().addTransportType(TRANSPORT_CELLULAR).build()
    private val wifiRequest: NetworkRequest = NetworkRequest.Builder().addTransportType(TRANSPORT_WIFI).build()
    private val ethernetRequest: NetworkRequest = NetworkRequest.Builder().addTransportType(
        TRANSPORT_ETHERNET).build()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        cellular_button.setOnClickListener {
            connectivityManager.requestNetwork(cellularRequest, object : ConnectivityManager.NetworkCallback() {
                override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
                    super.onBlockedStatusChanged(network, blocked)
                    Log.e("darran", "blocked status changed")

                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                    Log.e("darran", "capabilities changed")

                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    Log.e("darran", "lost")

                }

                override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                    super.onLinkPropertiesChanged(network, linkProperties)
                    Log.e("darran", "properties changed")

                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    Log.e("darran", "unavailable")

                }

                override fun onLosing(network: Network, maxMsToLive: Int) {
                    super.onLosing(network, maxMsToLive)
                    Log.e("darran", "losing")

                }

                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    Toast.makeText(this@MainActivity, "CELLULAR READY", Toast.LENGTH_SHORT).show()
                    Log.e("darran", "network count is: " +connectivityManager.allNetworks.size)
                    Log.e("darran", "network is metered: " +connectivityManager.isActiveNetworkMetered)
                    Log.e("darran", "bind is tru: " +connectivityManager.bindProcessToNetwork(network))
                    Log.e("darran", "bound network is: " +connectivityManager.boundNetworkForProcess)
                    GlobalScope.launch {
                        fetchIp(network)
                    }
                    connectivityManager.unregisterNetworkCallback(this)
                }
            })
        }

        wifi_button.setOnClickListener {
            connectivityManager.requestNetwork(wifiRequest, object : ConnectivityManager.NetworkCallback() {
                override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
                    super.onBlockedStatusChanged(network, blocked)
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                }

                override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                    super.onLinkPropertiesChanged(network, linkProperties)
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                }

                override fun onLosing(network: Network, maxMsToLive: Int) {
                    super.onLosing(network, maxMsToLive)
                }

                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    Toast.makeText(this@MainActivity, "WIFI READY", Toast.LENGTH_SHORT).show()
                    Log.e("darran", "network info is: " +connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI))
                    Log.e("darran", "network is metered: " +connectivityManager.isActiveNetworkMetered)
                    Log.e("darran", "bind is tru: " +connectivityManager.bindProcessToNetwork(network))
                    Log.e("darran", "bound network is: " +connectivityManager.boundNetworkForProcess)
                    GlobalScope.launch {
                        val ipAddress = fetchIp(network)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "IP is: " +ipAddress, Toast.LENGTH_SHORT).show()
                        }
                    }
                    connectivityManager.unregisterNetworkCallback(this)
                }
            })
        }

        ethernet_button.setOnClickListener {

            val success = RootUtil.executeAsRoot("ifconfig eth0 down").first

            if (success) {
                val success = RootUtil.executeAsRoot("ifconfig eth0 up").first
                if (!success) {
                    return@setOnClickListener
                }
            }

            connectivityManager.requestNetwork(ethernetRequest, object : ConnectivityManager.NetworkCallback() {
                override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
                    super.onBlockedStatusChanged(network, blocked)
                }

                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    super.onCapabilitiesChanged(network, networkCapabilities)
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                }

                override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                    super.onLinkPropertiesChanged(network, linkProperties)
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                }

                override fun onLosing(network: Network, maxMsToLive: Int) {
                    super.onLosing(network, maxMsToLive)
                }

                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    Toast.makeText(this@MainActivity, "ConnectivityManager.TYPE_ETHERNET READY", Toast.LENGTH_SHORT).show()
                    Log.e("darran", "network info is: " +connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET))
                    Log.e("darran", "network is metered: " +connectivityManager.isActiveNetworkMetered)
                    Log.e("darran", "bind is tru: " +connectivityManager.bindProcessToNetwork(network))
                    Log.e("darran", "bound network is: " +connectivityManager.boundNetworkForProcess)
                    GlobalScope.launch {
                        val ipAddress = fetchIp(network)
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@MainActivity, "IP is: " +ipAddress, Toast.LENGTH_SHORT).show()
                        }
                    }
                    connectivityManager.unregisterNetworkCallback(this)
                }
            })
        }
    }

    suspend fun fetchIp(network: Network): String? = withContext(Dispatchers.IO) {

        try {
            Log.v(TAG, "IP for www.icanhazip.com is: " +network.getByName("www.icanhazip.com"))
            val client = OkHttpClient();

            val request = Request.Builder()
                .url("http://icanhazip.com")
                .build();

            val response = client.newCall(request).execute()
            val result = response.body?.string()
            Log.e("darran", "The ip is: $result")
            return@withContext result
        } catch (e: Exception) {
            Log.e("darran", "exception: $e")
        }

        return@withContext null
    }

    companion object {
        const val TAG = "NetworkSwitchPOC"
    }
}
