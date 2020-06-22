package com.fivestars.networkswitchpoc

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities.*
import android.net.NetworkRequest
import android.os.Bundle
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import fr.bmartel.speedtest.SpeedTestReport
import fr.bmartel.speedtest.SpeedTestSocket
import fr.bmartel.speedtest.inter.ISpeedTestListener
import fr.bmartel.speedtest.model.SpeedTestError
import io.sentry.core.Sentry
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.text.DecimalFormat
import java.time.Duration
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

        GlobalScope.launch(Dispatchers.IO) {
            while (true) {
                switchToWifi(connectivityManager)
                withContext(Dispatchers.Main) {
                    loadUrl("http://www.fivestars.com")
                }
                delay(TimeUnit.MINUTES.toMillis(1))
                switchToEthernet(connectivityManager)
                withContext(Dispatchers.Main) {
                    loadUrl("http://www.fivestars.com")
                }
            }
        }

        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(
                view: WebView?,
                errorCode: Int,
                description: String?,
                failingUrl: String?
            ) {
                super.onReceivedError(view, errorCode, description, failingUrl)
                Sentry.captureException(Throwable("Network: :" +currentNetwork?.networkHandle + " " +description))
            }
        }
    }

    private suspend fun switchToEthernet(connectivityManager: ConnectivityManager): Long {
        return switchNetwork(ethernetRequest, connectivityManager)
    }

    private suspend fun switchToWifi(connectivityManager: ConnectivityManager): Long {
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
                        continuation.resume(network.networkHandle)
                    }
                })
        }


}


