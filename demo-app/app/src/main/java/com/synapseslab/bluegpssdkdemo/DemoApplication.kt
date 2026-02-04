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

package com.synapseslab.bluegpssdkdemo

import android.app.Application
import com.synapseslab.bluegps_sdk.core.BlueGPSLib
import com.synapseslab.bluegpssdkdemo.constants.Environment

class DemoApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        /**
         * Initialize BlueGPS SDK
         *
         * @param context the context of the app.
         * @param sdkEnvironment for Keycloak authentication.
         *
         */
        BlueGPSLib.instance.initSDK(
            sdkEnvironment = Environment.sdkEnvironment,
            context = this,
            enabledNetworkLogs = true
        )
    }
}