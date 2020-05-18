package com.fivestars.networkswitchpoc

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities.*
import android.net.NetworkRequest
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.webkit.*
import android.widget.FrameLayout
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
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


class MainActivity : AppCompatActivity() {

    private val cellularRequest: NetworkRequest = NetworkRequest.Builder().addTransportType(TRANSPORT_CELLULAR).build()
    private val wifiRequest: NetworkRequest = NetworkRequest.Builder().addTransportType(TRANSPORT_WIFI).build()
    private val ethernetRequest: NetworkRequest = NetworkRequest.Builder().addTransportType(
        TRANSPORT_ETHERNET).build()
    
    private var networkInstance: Network? = null
    set(value) {
        field = value
        reload_webview_button.isEnabled = true
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        reload_webview_button.isEnabled = false
        reload_webview_button.setOnClickListener {
            loadSpeedTest()
        }

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        cellular_button.setOnClickListener {
            connectivityManager.requestNetwork(cellularRequest, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    postConnect(connectivityManager, network, "LTE", this)
                }
            })
        }

        wifi_button.setOnClickListener {
            connectivityManager.requestNetwork(wifiRequest, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    postConnect(connectivityManager, network, "WiFi", this)
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

                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    postConnect(connectivityManager, network, "Ethernet", this)
                }
            })
        }
    }

    private fun loadSpeedTest() {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                if (networkInstance != null) {
                    try {
                        val url = URL(request.url.toString())
                        val connection: HttpURLConnection =
                            networkInstance?.openConnection(url) as HttpURLConnection
                        connection.setDoOutput(true)
                        connection.setRequestMethod(request.method)
                        for ((key, value) in request.requestHeaders) {
                            connection.setRequestProperty(key, value)
                        }
                        val responseCode: Int = connection.getResponseCode()
                        val responseMessage: String = connection.getResponseMessage()
                        var contentType: String = connection.getContentType()
                        var encoding: String? = connection.getContentEncoding()

                        // Transform response headers from Map<String, List<String>> to Map<String, String>
                        val headerFields: HashMap<String?, String?> = HashMap()
                        connection.headerFields.map {
                            headerFields.put(it.key, it.value[0])
                        }
                        val contentTypeData =
                            contentType.split(";\\s?").toTypedArray()
                        contentType = contentTypeData[0]
                        if (contentTypeData.size > 1) {
                            encoding = contentTypeData[1]
                        }
                        if (contentType.contains("text") && encoding == null) {
                            encoding = "utf-8"
                        }
                        val inputStream: InputStream
                        inputStream = try {
                            connection.getInputStream()
                        } catch (e: java.lang.Exception) {
                            connection.getErrorStream()
                        }
                        return WebResourceResponse(
                            contentType,
                            encoding,
                            responseCode,
                            responseMessage,
                            headerFields,
                            inputStream
                        )
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }
        }
        val webSettings: WebSettings = webView.getSettings()
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webView.loadUrl("http://icanhazip.com")
    }

    private fun postConnect(connectivityManager: ConnectivityManager, network: Network, connectionType: String, networkCallback: ConnectivityManager.NetworkCallback) {
        Toast.makeText(this@MainActivity, connectionType, Toast.LENGTH_SHORT).show()
        Log.e(TAG, "network info is: $network")
        Log.e(TAG, "network is metered: " +connectivityManager.isActiveNetworkMetered)
        Log.e(TAG, "bind is tru: " +connectivityManager.bindProcessToNetwork(network))
        Log.e(TAG, "bound network is: " +connectivityManager.boundNetworkForProcess)
        GlobalScope.launch {
            withContext(Dispatchers.Main) {
                networkInstance = network
            }
        }
        connectivityManager.unregisterNetworkCallback(networkCallback)
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
            Log.e(TAG, "The ip is: $result")
            return@withContext result
        } catch (e: Exception) {
            Log.e(TAG, "exception: $e")
        }

        return@withContext null
    }

    companion object {
        const val TAG = "NetworkSwitchPOC"
    }
}
