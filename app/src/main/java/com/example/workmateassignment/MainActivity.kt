package com.example.workmateassignment

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import org.json.JSONObject
import java.io.IOException


class MainActivity : AppCompatActivity() {

    private var key = ""
    private lateinit var handler: Handler
    private var clockValue: String? = null
    private var clockInTime = ""
    private var clockOutTime = ""
    private var contactNumber = ""
    lateinit var partner: JSONObject

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        key = login("https://api.helpster.tech/v1/auth/login/")
        getStaffRequest("https://api.helpster.tech/v1/staff-requests/26074/")

        intent?.let {
            clockValue = it.getStringExtra("ClockValue")
            clockValue?.let {
                if (it.equals("Clocking In...")) {
                    callClockApi("https://api.helpster.tech/v1/staff-requests/26074/clock-in/")
                } else {
                    callClockApi("https://api.helpster.tech/v1/staff-requests/26074/clock-out/")
                }
            }
        }

        btnClock.setOnClickListener {
            val intent = Intent(this, ClockActivity::class.java)
            intent.putExtra("ClockValue", btnClock.text)
            startActivity(intent)
        }

    }

    fun callClockApi(url: String) {
        val client = OkHttpClient().newBuilder().authenticator(object : Authenticator {

            override fun authenticate(route: Route, response: Response): Request? {
                val credential = Credentials.basic("+6281313272005", "alexander")
                if (credential.equals(response.request().header("Authorization"))) {
                    return null
                }
                return response.request().newBuilder()
                    .header("Authorization", credential).build()
            }
        }).build()

        val formBody = FormBody.Builder()
            .add("latitude", "-6.2446691")
            .add("longitude", "106.8779625")
            .build()

        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                val resp = response.body()!!.string()

                if (response.code() != 400) {
                    val jsonObject = JSONObject(resp)

                    if (jsonObject.has("timesheet")) {
                        val jObj = jsonObject.get("timesheet")
                        if (jObj is JSONObject) {
                            clockInTime = jObj.getString("clock_in_time")
                            clockOutTime = jObj.getString("clock_out_time")
                            partner = jObj.getJSONObject("partner")
                        } else {
                            clockInTime = jsonObject.getString("clock_in_time")
                            partner = jsonObject.getJSONObject("partner")
                        }
                    }
                    contactNumber = partner.getString("mobile")
                }
                handler.post(Runnable {
                    if (clockInTime.isNotEmpty()) {
                        val clockInArray = clockInTime.split("T")
                        val res = clockInArray[1].split(".")
                        clockInTime = res[0]
                    }
                    if (clockOutTime.isNotEmpty()) {
                        val clockOutArray = clockOutTime.split("T")
                        val res2 = clockOutArray[1].split(".")
                        clockOutTime = res2[0]
                    }

                    textViewContactNumber.text = contactNumber
                    textViewClockIn.text = clockInTime
                    textViewClockOut.text = clockOutTime
                    if (!clockInTime.equals("") && !clockOutTime.equals("")) {
                        btnClock.visibility = View.GONE
                    } else {
                        btnClock.text = "Clock Out"
                        btnClock.visibility = View.VISIBLE
                    }
                })

            }
        })
    }

    fun login(url: String): String {
        val formBody = FormBody.Builder()
            .add("username", "+6281313272005")
            .add("password", "alexander")
            .build()

        val client = OkHttpClient()
        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                val resp = response.body()!!.string()
                val jsonObject = JSONObject(resp)
                key = jsonObject.getString("key")

            }
        })
        return key
    }

    fun getStaffRequest(url: String) {

        val httpBuilder = HttpUrl.parse(url)?.newBuilder()
        httpBuilder?.addQueryParameter("token ", key)

        val request = Request.Builder()
            .url(httpBuilder?.build())
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                val res = response.body()!!.string()
                val jsonObject = JSONObject(res)
                handler = Handler(Looper.getMainLooper())
                val client = jsonObject.getJSONObject("client")
                val position = jsonObject.getJSONObject("position")
                val wageAmount = jsonObject.getString("wage_amount")
                val wageType = jsonObject.getString("wage_type")
                val name = client.getString("name")
                val title = position.getString("name")
                val location = jsonObject.getJSONObject("location")
                val address = location.getJSONObject("address")
                val street = address.getString("street_1")
                handler.post(Runnable {
                    textView3.text = name
                    textViewWageAmount.text = wageAmount
                    textViewWageType.text = wageType
                    textView1.text = title
                    textViewStreet.text = street
                })
            }
        })
    }
}
