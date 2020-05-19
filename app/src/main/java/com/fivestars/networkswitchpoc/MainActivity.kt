package com.fivestars.networkswitchpoc

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities.*
import android.net.NetworkRequest
import android.os.Bundle
import android.util.Log
import android.webkit.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.fivestars.rootutil.RootUtil
import fr.bmartel.speedtest.SpeedTestReport
import fr.bmartel.speedtest.SpeedTestSocket
import fr.bmartel.speedtest.inter.ISpeedTestListener
import fr.bmartel.speedtest.model.SpeedTestError
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.text.DecimalFormat


class MainActivity : AppCompatActivity() {

    private val cellularRequest: NetworkRequest = NetworkRequest.Builder().addTransportType(TRANSPORT_CELLULAR).build()
    private val wifiRequest: NetworkRequest = NetworkRequest.Builder().addTransportType(TRANSPORT_WIFI).build()
    private val ethernetRequest: NetworkRequest = NetworkRequest.Builder().addTransportType(
        TRANSPORT_ETHERNET).build()

    var pattern = "#.##"
    var decimalFormat = DecimalFormat(pattern)
    var networkReference = "wlan"

    private var networkInstance: Network? = null
    set(value) {
        field = value
        reload_webview_button.isEnabled = true
        speed_test.isEnabled = true
    }
    
    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        reload_webview_button.isEnabled = false
        reload_webview_button.setOnClickListener {
            loadUrl("http://icanhazip.com")
        }

        speed_test.isEnabled = false
        speed_test.setOnClickListener {
            download_status.text = "Running Speed Test"
            runDownloadTest()
        }

        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        cellular_button.setOnClickListener {
            connectivityManager.requestNetwork(cellularRequest, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    onConnectedToNetwork(connectivityManager, network, "LTE", this)
                }
            })
        }

        wifi_button.setOnClickListener {
            val dropEthSuccess = RootUtil.executeAsRoot("ifconfig eth0 down").first

            val success = RootUtil.executeAsRoot("ifconfig wlan0 down").first

            if (success) {
                val success = RootUtil.executeAsRoot("ifconfig wlan0 up").first
                if (!success) {
                    return@setOnClickListener
                }
            }

            connectivityManager.requestNetwork(wifiRequest, object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    onConnectedToNetwork(connectivityManager, network, "wlan", this)
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
                    onConnectedToNetwork(connectivityManager, network, "eth", this)
                }
            })
        }
    }

    private fun runDownloadTest() {
        speed_test.isEnabled = false
        val speedTestSocket = SpeedTestSocket()

        speedTestSocket.addSpeedTestListener(object : ISpeedTestListener {
            override fun onCompletion(report: SpeedTestReport) {
                // called when download/upload is complete
                println("[COMPLETED] rate in octet/s : " + report.transferRateOctet)
                println("[COMPLETED] rate in bit/s   : " + report.transferRateBit)

                GlobalScope.launch(Dispatchers.Main) {

                    PacketLoss.after = withContext(Dispatchers.IO) {
                        Parse.parseNetworkInfo(executeCommand("ip -s -o link", "\n")!!, networkReference)
                    }

                    download_status.text =
                        "DL Mbps : " + decimalFormat.format((report.transferRateBit.toDouble() / 1000000)) + " Mbps - Packet Loss: ${PacketLoss.calculatePacketLoss(PacketLoss.before!!, PacketLoss.after!!)}"

                    runUploadTest()
                }

            }

            override fun onError(
                speedTestError: SpeedTestError,
                errorMessage: String
            ) {
                // called when a download/upload error occur
            }

            override fun onProgress(percent: Float, report: SpeedTestReport) {
                // called to notify download/upload progress
                println("[PROGRESS] progress : $percent%")
                GlobalScope.launch(Dispatchers.Main) {
                    download_status.text =
                        "DL Mbps : " + decimalFormat.format((report.transferRateBit.toDouble() / 1000000)) + " Mbps - Progress: ${decimalFormat.format(percent)} %"
                }
            }
        })

        GlobalScope.launch(Dispatchers.IO) {
            PacketLoss.before = Parse.parseNetworkInfo(
                executeCommand("ip -s -o link", "\n")!!, networkReference
            )
            speedTestSocket.startDownload("http://ashburn.va.speedtest.frontier.com:8080/speedtest/random4000x4000.jpg");
        }
    }

    fun executeCommand(command: String, lineBreak: String?): String? {
        return try {
            val p = Runtime.getRuntime().exec("ip -s -o link")
            val outputStream =
                DataOutputStream(p.outputStream)
            outputStream.writeBytes("$command \n")
            outputStream.writeBytes("exit\n")
            outputStream.flush()
            p.waitFor()
            val output = StringBuilder()
            val reader =
                BufferedReader(InputStreamReader(p.inputStream))
            var line = reader.readLine()
            while (line != null) {
                output.append(line).append(lineBreak)
                line = reader.readLine()
            }
            output.toString()
        } catch (ie: InterruptedException) {
            "IEEXception $ie"
        } catch (e: IOException) {
            "IOEXception $e"
        }
    }

    private fun runUploadTest() {
        val speedTestSocket = SpeedTestSocket()

        speedTestSocket.addSpeedTestListener(object : ISpeedTestListener {
            override fun onCompletion(report: SpeedTestReport) {
                // called when download/upload is complete
                println("[COMPLETED] rate in octet/s : " + report.transferRateOctet)
                println("[COMPLETED] rate in bit/s   : " + report.transferRateBit)

                GlobalScope.launch(Dispatchers.Main) {

                    PacketLoss.after = withContext(Dispatchers.IO) {
                        Parse.parseNetworkInfo(executeCommand("ip -s -o link", "\n")!!, networkReference)
                    }

                    upload_status.text =
                        "UL Mbps : " + decimalFormat.format((report.transferRateBit.toDouble() / 1000000)) + " Mbps - Packet Loss: ${PacketLoss.calculatePacketLoss(PacketLoss.before!!, PacketLoss.after!!)}"
                    speed_test.isEnabled = true
                }

            }

            override fun onError(
                speedTestError: SpeedTestError,
                errorMessage: String
            ) {
                // called when a download/upload error occur
            }

            override fun onProgress(percent: Float, report: SpeedTestReport) {
                // called to notify download/upload progress
                println("[PROGRESS] progress : $percent%")
                GlobalScope.launch(Dispatchers.Main) {
                    upload_status.text =
                        "UL Mbps : " + decimalFormat.format((report.transferRateBit.toDouble() / 1000000)) + " Mbps - Progress: ${decimalFormat.format(percent)} %"
                }
            }
        })

        GlobalScope.launch(Dispatchers.IO) {
            PacketLoss.before = Parse.parseNetworkInfo(
                executeCommand("ip -s -o link", "\n")!!,
                networkReference
            )
            speedTestSocket.startUpload("http://ashburn.va.speedtest.frontier.com:8080/speedtest/upload.php", 31000000);
        }
    }

    private fun loadUrl(url: String) {
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                if (networkInstance != null) {
                    try {
                        val client = OkHttpClient.Builder().socketFactory(networkInstance!!.socketFactory).build()

                        val call: Call = client.newCall(
                            Request.Builder()
                                .url(request.url.toString())
                                .build()
                        )

                        val response: Response = call.execute()

                        return WebResourceResponse(
                            response.header(
                                "content-type",
                                "text/plain"
                            ),  // You can set something other as default content-type
                            response.header(
                                "content-encoding",
                                "utf-8"
                            ),  // Again, you can set another encoding as default
                            response.body?.byteStream()
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
        webView.loadUrl(url)
    }

    private fun onConnectedToNetwork(connectivityManager: ConnectivityManager, network: Network, connectionType: String, networkCallback: ConnectivityManager.NetworkCallback) {
        Toast.makeText(this@MainActivity, "$connectionType is ready", Toast.LENGTH_SHORT).show()
        networkReference = connectionType
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

    companion object {
        const val TAG = "NetworkSwitchPOC"
    }
}
