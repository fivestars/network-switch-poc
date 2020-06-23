package com.fivestars.networkswitchpoc

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities.TRANSPORT_ETHERNET
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import android.net.NetworkRequest
import android.os.Bundle
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import io.sentry.core.Sentry
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.lang.Thread.sleep
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine


class MainActivity : AppCompatActivity() {

    private val wifiRequest: NetworkRequest =
        NetworkRequest.Builder().addTransportType(TRANSPORT_WIFI).build()
    private val ethernetRequest: NetworkRequest = NetworkRequest.Builder().addTransportType(
        TRANSPORT_ETHERNET
    ).build()

    private var currentNetwork: Network? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView.webViewClient = WebViewClient()


        val connectivityManager =
            getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        GlobalScope.launch {
            while (true) {

                val wifiId = switchToWifi(connectivityManager)
                Log.e("Darran", wifiId.toString() + " finished")

                withContext(Dispatchers.Main) {
                    loadUrl("http://www.fivestars.com")
                }

                val ethernetId = switchToEthernet(connectivityManager)
                Log.e("Darran", ethernetId.toString() + " finished")

                withContext(Dispatchers.Main) {
                    loadUrl("http://www.fivestars.com")
                }

            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                Log.e("Darran", url + " finished")
            }

            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                Log.e("Darran", "Network: :" + currentNetwork?.networkHandle + " " + description)
                Sentry.captureException(Throwable("Network: :" + currentNetwork?.networkHandle + " " + description))
            }
        }
    }

    private suspend fun switchToEthernet(connectivityManager: ConnectivityManager): Long {
        delay(5000)
        Log.e("Darran", "Switching to ethernet")
        return switchNetwork(ethernetRequest, connectivityManager)
    }

    private suspend fun switchToWifi(connectivityManager: ConnectivityManager): Long {
        delay(5000)
        Log.e("Darran", "Switching to wifi")
        return switchNetwork(wifiRequest, connectivityManager)
    }

    private fun loadUrl(url: String) {
        val webSettings: WebSettings = webView.settings
        webSettings.javaScriptEnabled = true
        webSettings.domStorageEnabled = true
        webView.loadUrl(url)
    }

    companion object {
        const val TAG = "NetworkSwitchPOC"
    }

    suspend fun switchNetwork(
        networkRequest: NetworkRequest,
        connectivityManager: ConnectivityManager
    ): Long =
        suspendCoroutine { continuation ->
            connectivityManager.requestNetwork(
                networkRequest,
                object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        super.onAvailable(network)
                        connectivityManager.unregisterNetworkCallback(this)
                        currentNetwork = network
                        connectivityManager.bindProcessToNetwork(network)
                        continuation.resume(network.networkHandle)
                    }
                })
        }


}


