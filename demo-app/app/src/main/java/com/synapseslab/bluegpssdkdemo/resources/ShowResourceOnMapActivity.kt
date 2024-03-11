/*
 * Copyright (c) 2023 Synapses s.r.l.s. All rights reserved.
 *
 * Licensed under the Apache License.
 * You may obtain a copy of the License at
 *
 * https://github.com/synapseslab/android-bluegps-sdk-demoapp/blob/main/LICENSE.md
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.synapseslab.bluegpssdkdemo.resources

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.synapseslab.bluegps_sdk.component.map.BlueGPSMapListener
import com.synapseslab.bluegps_sdk.data.model.map.ConfigurationMap
import com.synapseslab.bluegps_sdk.data.model.map.GenericInfo
import com.synapseslab.bluegps_sdk.data.model.map.GenericResource
import com.synapseslab.bluegps_sdk.data.model.map.IconStyle
import com.synapseslab.bluegps_sdk.data.model.map.JavascriptCallback
import com.synapseslab.bluegps_sdk.data.model.map.MapStyle
import com.synapseslab.bluegps_sdk.data.model.map.NavigationStyle
import com.synapseslab.bluegps_sdk.data.model.map.ShowMap
import com.synapseslab.bluegps_sdk.data.model.map.TypeMapCallback
import com.synapseslab.bluegpssdkdemo.databinding.ActivityMapBinding
import com.synapseslab.bluegpssdkdemo.utils.Environment


private val TAG = "MapActivity"


class ShowResourceOnMapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMapBinding
    private var genericResource: GenericResource? = null

    private var configurationMap = ConfigurationMap(
        style = MapStyle(
            navigation = NavigationStyle(
                iconSource = "/api/public/resource/icons/commons/start.svg",
                iconDestination = "/api/public/resource/icons/commons/end.svg",
            ),
            icons = IconStyle(
                name = "chorus",
                align = "center",
                vAlign = "center",
                followZoom = true
            ),
        ),
        show = ShowMap(all = false, room = true, me = false),
    )


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        genericResource = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra("resource", GenericResource::class.java)
        } else {
            intent.getParcelableExtra("resource")
        }

        supportActionBar?.title = genericResource?.name
        binding.webView.initMap(
            sdkEnvironment = Environment.sdkEnvironment,
            configurationMap = configurationMap
        )
        binding.btnNavigationMode.visibility = View.GONE
        setListenerOnMapView()
        if (supportActionBar != null) {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
            supportActionBar!!.setDisplayShowHomeEnabled(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
        }
        return super.onOptionsItemSelected(item)
    }

    /**
     * Setup the listener for BlueGPSMapView in order to implement the code
     * to run when an event click on map occurs.
     */
    private fun setListenerOnMapView() {
        binding.webView.setBlueGPSMapListener(object : BlueGPSMapListener {
            override fun resolvePromise(
                data: JavascriptCallback,
                typeMapCallback: TypeMapCallback
            ) {
                /**
                 * Callback that intercept the click on the map
                 *
                 * @param data the clicked point with all info.
                 * @param typeMapCallback the type of the clicked point.
                 *
                 */
                when (typeMapCallback) {
                    TypeMapCallback.INIT_SDK_COMPLETED -> {
                        runOnUiThread {
                            binding.webView.selectPoi(genericResource!!)
                        }
                    }
                    TypeMapCallback.SUCCESS -> {
                        val cType = object : TypeToken<GenericInfo>() {}.type
                        val payloadResponse = Gson().fromJson<GenericInfo>(data.payload, cType)
                        Log.d(TAG, " ${payloadResponse} ")
                    }
                    TypeMapCallback.ERROR -> {
                        val cType = object : TypeToken<GenericInfo>() {}.type
                        val payloadResponse = Gson().fromJson<GenericInfo>(data.payload, cType)
                        Snackbar
                            .make(
                                findViewById(android.R.id.content),
                                "${payloadResponse.message}",
                                Snackbar.LENGTH_LONG
                            )
                            .show()
                    }
                    else -> {}
                }
            }
        })
    }


}