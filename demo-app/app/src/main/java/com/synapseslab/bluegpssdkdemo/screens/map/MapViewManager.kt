package com.synapseslab.bluegpssdkdemo.screens.map

import android.content.Context
import android.view.ViewGroup
import com.synapseslab.bluegps_sdk.component.map.BlueGPSMapView
import com.synapseslab.bluegps_sdk.core.BlueGPSLib
import com.synapseslab.bluegps_sdk.data.model.map.ConfigurationMap
import com.synapseslab.bluegps_sdk.data.model.map.IconStyle
import com.synapseslab.bluegps_sdk.data.model.map.MapStyle
import com.synapseslab.bluegps_sdk.data.model.map.NavigationStyle
import com.synapseslab.bluegps_sdk.data.model.map.ShowMap
import com.synapseslab.bluegps_sdk.data.model.map.ToolboxMap
import com.synapseslab.bluegps_sdk.data.model.map.ToolboxMapParameter
import com.synapseslab.bluegpssdkdemo.constants.Environment

/**
 * Singleton object responsible for managing a single instance of [BlueGPSMapView].
 *
 * This manager ensures that only one `BlueGPSMapView` is created and reused throughout
 * the application, preventing unnecessary re-initialization and resource consumption.
 * It provides a centralized point for configuring and accessing the map view.
 */
object MapViewManager {

    private var customMapView: BlueGPSMapView? = null

    /**
     * Returns the singleton instance of [BlueGPSMapView].
     *
     * If the map view has not been created yet, this function initializes it with a
     * default configuration. The map view is configured to match its parent's size,
     * sets the user's tag ID if available, and initializes the map with the specified
     * SDK environment and map configuration.
     *
     * @param context The [Context] used to create the `BlueGPSMapView`.
     * @return The singleton [BlueGPSMapView] instance.
     */
    fun getInstance(context: Context): BlueGPSMapView {
        if (customMapView == null) {
            customMapView = BlueGPSMapView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )

                BlueGPSLib.instance.userTagId()?.let {
                    configurationMap.tagid = it
                }

                initMap(Environment.sdkEnvironment, configurationMap)
            }
        }
        return customMapView!!
    }

    /**
     * The default map configuration used for initializing the [BlueGPSMapView].
     *
     * This configuration defines the visual style of the map, including navigation icons,
     * icon styles, and which elements (like the user's location, all tags, and rooms)
     * are visible by default. It also disables some of the default map toolbox controls.
     */
    private val configurationMap = ConfigurationMap(
        style = MapStyle(
            navigation = NavigationStyle(
                iconSource = "/api/public/resource/icons/commons/start.svg",
                iconDestination = "/api/public/resource/icons/commons/end.svg",
            ),
            icons = IconStyle(
                radiusMeter = 1.0,
                name = "bluegps",
                align = "center",
                vAlign = "bottom",
                followZoom = true
            ),
        ),
        toolbox = ToolboxMap(
            mapControl = ToolboxMapParameter(enabled = false),
            layer = ToolboxMapParameter(enabled = false)
        ),
        show = ShowMap(me = true, all = false, room = true),
    )
}