# BlueGPS Android Demo app

A minimal DemoApp for using BlueGPS SDK

## Sample App

To run the sample app, start by cloning this repo:

 ```shell
git clone git@github.com:synapseslab/android-bluegps-demoapp-public.git
```

and play with it.

## Getting Started
Your first step is initializing the BlueGPSLib, which is the main entry point for all operations in the library. BlueGPSLib is a singleton: you'll create it once and re-use it across your application.

A best practice is to initialize BlueGPSLib in the Application class:

```kotlin
class App : Application() {
    override fun onCreate() {
        super.onCreate()

        BlueGPSLib.instance.initSDK(
            sdkEnvironment = Environment.sdkEnvironment,
            context = applicationContext,
        )
    }
}
```

> [!NOTE]
> The BlueGSP-SDK use an `Environment` where integrator have to put SDK data for register the SDK and for create a communication with the BlueGPS Server. The management of the environment is demanded to the app.

```kotlin
object Environment {

    private val SDK_ENDPOINT = "{{provided-bluegps-endpoint}}"

    val keyCloakParameters = KeyCloakParameters(
        authorization_endpoint = "https://[BASE-URL]/realms/[REALMS]/protocol/openid-connect/auth",
        token_endpoint = "https://[BASE-URL]/realms/[REALMS]/protocol/openid-connect/token",
        redirect_uri = "{{HOST}}://{{SCHEME}}",
        clientId = "{{provided-client-id}}", // for user authentication
        userinfo_endpoint = "https://[BASE-URL]/realms/[REALMS]/protocol/openid-connect/userinfo",
        end_session_endpoint = "https://[BASE-URL]/realms/[REALMS]/protocol/openid-connect/logout",
        guestClientSecret = "{{provided-guest-client-secret}}", // for guest authentication
        guestClientId = "{{provided-guest-client-id}}" // for guest authentication
    )

    val sdkEnvironment = SdkEnvironment(
        sdkEndpoint = SDK_ENDPOINT,
        keyCloakParameters = keyCloakParameters,
    )
}
```

### App Authentication
The BlueGPS_SDK offers a client for managing authentication and authorization within your application. It leverages [Keycloak](https://www.keycloak.org/) to handle user authentication.

BlueGPS provides 2 kinds of authentication:

- **User Authentication:**

If you want only the User authentication you must set the **`clientId`**.

This means that for each device this is the user on Keycloak that can manage grants for this particular user.

- **Guest Authentication:**

If you want only the Guest authentication, you must set the **`guestClientSecret`** and **`guestClientId`**.

This means that we don't have a user that has to login but we use client credentials and there is not an individual user for each app install. Instead BlueGPS treats the user account as a "guest"
In this case multiple devices can use the same client credentials to be authenticated and BlueGPS will register the user as a device, and not as a formal Keycloak user.

> [!NOTE]
> This paramaters are provided by **Synapses** after the purchase of the **BlueGPS license**.

Finally in your `AndroidManifest.xml` add this and change `host` and `scheme` with your configuration.

```xml
<activity
    android:name="com.synapseslab.bluegps_sdk.authentication.presentation.AuthenticationActivity"
    android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />

        <data
            android:host="{HOST}"
            android:scheme="{SCHEME}" />
    </intent-filter>
</activity>
```

Now your app is ready for use keycloak.

---

## App Architecture

The demo app follows **MVVM** with **Jetpack Compose** and **Material 3**. Each screen has its own `ViewModel` managing immutable UI state via `StateFlow`. All SDK operations go through the `BlueGPSLib` singleton; no networking happens directly in ViewModels.

```
DemoApplication
└── BlueGPSLib.instance.initSDK(...)   ← SDK initialized once

MainActivity
└── NavHost
    ├── InitScreen                      ← splash / auth check
    ├── LoginScreen + LoginViewModel
    └── MainScreen (BottomNavigation)
        ├── MapScreen    + MapViewModel
        ├── SSEScreen    + SSEViewModel
        ├── BookingScreen + BookingViewModel
        ├── SettingsScreen + SettingsViewModel
        └── NavigationScreen            ← launched from BookingScreen
```

### Package structure

```
com.synapseslab.bluegpssdkdemo/
├── advertising/        # BLE advertising service wrapper (AdvManager)
├── constants/          # Environment configuration object
├── receiver/           # BroadcastReceivers (beacon events, boot)
├── routes/             # Compose navigation graph (AppRoutes)
├── screens/
│   ├── init/           # Splash / loading screen
│   ├── login/          # Authentication screen
│   ├── main/           # Bottom-nav host screen
│   ├── map/            # Interactive indoor map
│   ├── booking/        # Resource search & booking
│   ├── navigation/     # Wayfinding / route-to-destination
│   ├── sse/            # Real-time Server-Sent Events log
│   └── settings/       # User info & logout
├── ui/theme/           # Material 3 colors, typography, theme
└── utils/              # JWT decode helper
```

### Tech stack

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material 3 |
| State | `StateFlow` / `SharedFlow` + Kotlin Coroutines |
| Navigation | Compose Navigation (`NavHost`) |
| DI | ViewModel scoped to Compose (no Hilt in this demo) |
| SDK | `com.github.synapseslab:android-bluegps-sdk-public:6.0.0` |
| Min SDK | 24 |
| Target / Compile SDK | 36 |
| Kotlin | 2.1.0 |
| AGP | 8.13.0 |

---

## Navigation

Navigation is handled by a single `NavHost` rooted in `AppRoutes`. On startup the app lands on `InitScreen` while `LoginViewModel` checks whether an active session exists. The result drives an automatic redirect:

```
isLoggedIn == null  →  InitScreen (loading indicator)
isLoggedIn == false →  LoginScreen
isLoggedIn == true  →  MainScreen (replaces back-stack)
```

`MainScreen` hosts a nested `NavHost` with a `BottomNavigationBar`:

| Tab | Route | Screen |
|---|---|---|
| Map | `map` | `MapScreen` |
| SSE | `sse` | `SSEScreen` |
| Booking | `booking` | `BookingScreen` |
| Settings | `settings` | `SettingsScreen` |

`BookingScreen` can push `NavigationScreen` onto the stack by navigating to `navigation/{positionJson}`, where `positionJson` is a Gson-serialized `Position` object.

---

## Inside the Demo app

### InitScreen

A minimal loading screen shown while the app verifies the current session. It renders a centered `CircularProgressIndicator` and is replaced automatically once the session check completes.

---

### LoginScreen

**File:** `screens/login/LoginScreen.kt` · `LoginViewModel.kt`

Provides two ways to authenticate:

| Button | Behaviour |
|---|---|
| **Guest Login** | Calls `BlueGPSAuthManager.instance.guestLogin()` directly — no browser required. Suitable for shared/kiosk deployments. |
| **Login** | Launches the SDK's `AuthLoginActivity`, which opens a Keycloak browser flow for full user authentication. |

After a successful login of either type the ViewModel:
1. Calls `getOrCreateConfiguration()` to retrieve or create the device's BLE advertising configuration from the server.
2. Starts BLE advertising via `AdvManager` so the device is detectable by BlueGPS beacons.
3. Sets `isLoggedIn = true`, triggering navigation to `MainScreen`.

**ViewModel state**

```kotlin
val isLoggedIn: StateFlow<Boolean?> // null = checking, true = logged in, false = logged out
```

**Key ViewModel methods**

| Method | Description |
|---|---|
| `checkLoginStatus()` | Observes `BlueGPSLib.instance.isAValidSession()` and updates `isLoggedIn`. |
| `guestLogin()` | Calls `BlueGPSAuthManager.instance.guestLogin()` and stores the returned token in the SDK. |
| `logout()` | Calls `BlueGPSAuthManager.instance.logout()`, stops advertising, and sets `isLoggedIn = false`. |
| `getOrCreateConfiguration()` | Fetches the device's `AndroidAdvConfiguration` and starts advertising with `AdvManager`. |

---

### MainScreen

**File:** `screens/main/MainScreen.kt`

The root screen after login. It:
- Renders a `BottomNavigationBar` with four tabs.
- Calls `BlueGPSLib.instance.startTrackingUserPosition()` on composition, passing a `PendingIntent` that targets `BeaconEventReceiver`.
- Calls `BlueGPSLib.instance.stopTrackingUserPosition()` on disposal.

This ensures continuous BLE-based position tracking while the user is inside the app.

---

### MapScreen

**File:** `screens/map/MapScreen.kt` · `MapViewModel.kt` · `MapViewManager.kt`

Displays a full-screen interactive indoor map powered by `BlueGPSMapView`.

#### Map configuration (`MapViewManager`)

`MapViewManager` is a singleton that owns the single `BlueGPSMapView` instance and configures it with:

```kotlin
ConfigurationMap(
    style = MapStyle(
        icons = IconStyle(
            name   = "bluegps",
            format = "svg"
        )
    ),
    show = ShowMap(
        me    = true,   // show the current user's position
        room  = true,   // show rooms
        all   = false   // hide all other tags
    ),
    toolbox = Toolbox(
        mapControl    = false,
        layerControl  = false
    )
)
```

The map is embedded in Compose via `AndroidView`.

#### UI controls

A dropdown menu in the top-right corner exposes three actions:

| Action | SDK call | Description |
|---|---|---|
| **Change floor** | `MapViewManager.gotoFloor(floor)` | Opens a `ModalBottomSheet` listing all available floors; tapping one moves the map view to that floor. |
| **Show tag** | `MapViewManager.showTag(tagId)` | Centers the map on the current user's tag. |
| **Follow me** | `MapViewManager.forceFollowMe(enable)` | Toggles continuous camera tracking of the user's position. |

#### ViewModel commands

`MapViewModel` exposes a `SharedFlow<MapCommand>` consumed by `MapScreen`:

```kotlin
sealed interface MapCommand {
    data class ForceFollowMe(val enable: Boolean) : MapCommand
    object GetFloorList : MapCommand
    data class GotoFloor(val floor: Floor) : MapCommand
    data class ShowTag(val tagId: String) : MapCommand
}
```

#### Map event callbacks

`MapViewManager` listens to map lifecycle events:

| Event | Handler |
|---|---|
| `INIT_SDK_COMPLETED` | Applies dark / light theme to the map based on the system setting. |
| `AUTH_ERROR` | Re-injects the current access token via `BlueGPSLib.instance.accessToken()`. |
| `SUCCESS` | Logged for debugging. |
| `ERROR` | Logged for debugging. |

#### ViewModel state

```kotlin
val isFollowMeEnabled: StateFlow<Boolean>
```

---

### BookingScreen

**File:** `screens/booking/BookingScreen.kt` · `BookingViewModel.kt`

Allows users to search for and book rooms or other resources.

#### UI flow

1. A `TextField` at the top filters results by name in real time.
2. The list below shows matching `BGPResource` items fetched from the server.
3. Each list item has two action buttons:
   - **Book** — schedules the resource for the next day, 16:00–17:00.
   - **Navigate** — navigates to `NavigationScreen` with the resource's `Position`.

#### API calls

| Action | SDK method | Parameters |
|---|---|---|
| Search resources | `BlueGPSLib.instance.search(filter)` | `Filter(search = text, section = SectionFilterType.BOOKING, resourceTypes = [ResourceType.ROOM], filterType = FilterType.SEARCH)` |
| Book resource | `BlueGPSLib.instance.schedule(request)` | `ScheduleRequest(elementId, elementType, dayStart, start, end, meetingName, videoConference)` |

#### UI state

```kotlin
data class BookingUiState(
    val resources: List<BGPResource>  = emptyList(),
    val searchText: String            = "",
    val isLoading: Boolean            = false,
    val errorMessage: String?         = null,
    val scheduleStatus: ScheduleStatus = ScheduleStatus.Idle
)

sealed class ScheduleStatus {
    object Idle : ScheduleStatus()
    data class Success(val request: ScheduleRequest) : ScheduleStatus()
    data class Error(val message: String) : ScheduleStatus()
}
```

A `Snackbar` is shown when `scheduleStatus` transitions to `Success` or `Error`.

---

### NavigationScreen

**File:** `screens/navigation/NavigationScreen.kt`

Embeds the BlueGPS map and automatically navigates from the user's current position to a destination.

The destination `Position` is passed as a JSON-encoded route argument (`navigation/{positionJson}`) from `BookingScreen`. On composition the screen calls:

```kotlin
MapViewManager.gotoFromMe(position)
```

which draws the optimal indoor route on the map and starts guiding the user.

---

### SSEScreen

**File:** `screens/sse/SSEScreen.kt` · `SSEViewModel.kt`

Displays real-time event streams from the BlueGPS backend via two independent log panels.

#### Beacon scan log

Collects events from `BlueGPSLib.instance.bgpBeaconLocationFlow` (a cold `Flow<BeaconLocation>`). Each event is displayed as:

```
<event_type>: <message>
```

`BeaconEvent` types include `ENTER` and `EXIT`.

#### Region log

Subscribes to region changes via `BlueGPSLib.instance.startNotifyRegionChanges(tags, regions, callback)`.

The ViewModel computes ENTER / EXIT transitions by diffing the previous and current sets of active regions:

- A tag appearing in a new region → **ENTER** event logged.
- A tag no longer in any region → **EXIT** event logged.
- A tag key disappearing entirely from the response → treated as **EXIT**.

Both logs have a **Clear** button that resets the displayed text.

#### UI state

```kotlin
data class SSEUiState(
    val beaconStatus: String = "",
    val regionStatus: String = ""
)
```

#### ViewModel lifecycle

| Event | Action |
|---|---|
| `init {}` | Launches beacon flow collection and starts region change notifications. |
| `onCleared()` | Calls `BlueGPSLib.instance.stopNotifyRegionChanges()` and cancels coroutines. |

---

### SettingsScreen

**File:** `screens/settings/SettingsScreen.kt` · `SettingsViewModel.kt`

A simple informational screen with a logout action.

| Field | Source |
|---|---|
| **Logged user** | JWT `preferred_username` or `email` claim decoded from `BlueGPSLib.instance.accessToken()` via `JWTDecode.getClaimRaw()`. |
| **Tag ID** | `BlueGPSLib.instance.userTagId()` — the BLE tag identifier assigned to this device. |
| **App version** | `PackageManager.getPackageInfo().versionName` |

Tapping **Logout** calls `LoginViewModel.logout()`, which stops advertising, logs out via Keycloak, and navigates back to `LoginScreen`.

---

## Background Components

### BLE Advertising (`AdvManager`)

**File:** `advertising/AdvManager.kt`

`AdvManager` is a singleton that wraps `BlueGPSAdvertisingService`. It is responsible for making the device discoverable by BlueGPS beacons.

| Method | Description |
|---|---|
| `bindBlueGPSAdvertisingService(context)` | Binds to the foreground advertising service. Called in `MainActivity.onCreate()`. |
| `unBindBlueGPSAdvertisingService(context)` | Unbinds the service. Called in `MainActivity.onStop()`. |
| `startAdvertising(config)` | Starts BLE advertisement with the provided `AndroidAdvConfiguration` (contains the device's tag ID). |
| `stopAdvertising()` | Stops BLE advertisement. Called in `MainActivity.onDestroy()`. |

The advertising configuration (`AndroidAdvConfiguration`) is retrieved from the server via `BlueGPSLib.instance.getOrCreateConfiguration()` after every successful login.

---

### BeaconEventReceiver

**File:** `receiver/BeaconEventReceiver.kt`

A `BroadcastReceiver` that handles beacon proximity events even when the app is in the background or closed.

- Listens for `BlueGPSLib.ACTION_BEACON_EVENT` intents.
- Reconstructs a `BeaconLocation` from the intent extras (`id`, `message`, `data`, `event_name`).
- Posts a status-bar notification grouped under `"bluegps_beacon_events"`. Each notification has a unique ID derived from the beacon ID and event name to avoid duplicates.

---

### BootCompletedReceiver

**File:** `receiver/BootCompletedReceiver.kt`

Restarts position tracking automatically after a device reboot.

- Listens for `ACTION_BOOT_COMPLETED`.
- Re-initializes `BlueGPSLib` with `Environment.sdkEnvironment`.
- Restarts `startTrackingUserPosition()` with a `BeaconEventReceiver` `PendingIntent`.
- Ignores the broadcast if the device uptime exceeds 10 minutes (filters spurious broadcasts sent during development).

---

## Permissions

The app declares and requests the following permissions at runtime:

| Permission | Purpose |
|---|---|
| `BLUETOOTH_CONNECT` | Connect to BLE devices (Android 12+). |
| `BLUETOOTH_SCAN` | Scan for nearby BLE beacons (Android 12+). |
| `ACCESS_FINE_LOCATION` | Required for BLE scanning on Android < 12. |
| `ACCESS_COARSE_LOCATION` | Fallback location permission. |
| `ACCESS_BACKGROUND_LOCATION` | Keep tracking active when the app is in the background (Android 10+). |
| `POST_NOTIFICATIONS` | Show beacon event notifications (Android 13+). |
| `FOREGROUND_SERVICE` | Run the advertising service in the foreground. |

`MainActivity` uses `ActivityResultContracts.RequestMultiplePermissions` to request foreground permissions first, then `RequestPermission` for background location separately. If any permission is denied, a rationale UI is shown with a **Grant Permissions** button.

---

## SDK API Reference

All network and SDK operations are accessed via `BlueGPSLib.instance`. Results are wrapped in a sealed `Resource` type:

```kotlin
sealed class Resource<T> {
    data class Success<T>(val data: T)              : Resource<T>()
    data class Error<T>(val code: Int, val message: String) : Resource<T>()
    data class Exception<T>(val e: Throwable)       : Resource<T>()
}
```

### Authentication

| Method | Description | Returns |
|---|---|---|
| `BlueGPSLib.instance.initSDK(sdkEnvironment, context)` | Initializes the SDK. Must be called before any other API. | `Unit` |
| `BlueGPSLib.instance.isAValidSession()` | Emits `true` if the current token is valid. | `Flow<Boolean>` |
| `BlueGPSLib.instance.accessToken()` | Returns the raw JWT access token string. | `String?` |
| `BlueGPSAuthManager.instance.guestLogin()` | Authenticates as a guest using client credentials. | `Resource<TokenResponse>` |
| `BlueGPSAuthManager.instance.logout(callback)` | Logs out and revokes the token via Keycloak. | `Unit` (async callback) |

### Device & Configuration

| Method | Description | Returns |
|---|---|---|
| `BlueGPSLib.instance.getOrCreateConfiguration()` | Retrieves the device's BLE advertising configuration from the server, creating one if needed. | `Resource<AndroidAdvConfiguration>` |
| `BlueGPSLib.instance.userTagId()` | Returns the BLE tag ID assigned to this device. | `String?` |

### Position Tracking

| Method | Description |
|---|---|
| `BlueGPSLib.instance.startTrackingUserPosition(context, pendingIntent)` | Starts continuous BLE-based position tracking. Beacon events are delivered via the provided `PendingIntent`. |
| `BlueGPSLib.instance.stopTrackingUserPosition(context)` | Stops position tracking and cancels beacon event delivery. |
| `BlueGPSLib.instance.bgpBeaconLocationFlow` | A `Flow<BeaconLocation>` that emits beacon proximity events in real time. |

### Map

Map operations are performed through `MapViewManager`, which wraps `BlueGPSMapView`:

| Method | Description |
|---|---|
| `MapViewManager.initMap(sdkEnvironment, configurationMap)` | Initializes the map with environment and display settings. |
| `MapViewManager.getFloor(callback)` | Asynchronously retrieves the list of available floors. |
| `MapViewManager.gotoFloor(floor)` | Moves the map view to the specified floor. |
| `MapViewManager.showTag(tagId)` | Centers the map on the given tag ID. |
| `MapViewManager.forceFollowMe(enable)` | Enables or disables continuous camera tracking of the user. |
| `MapViewManager.gotoFromMe(position)` | Computes and renders an indoor route from the user's current position to `position`. |

### Resource Booking

| Method | Description | Returns |
|---|---|---|
| `BlueGPSLib.instance.search(filter)` | Searches bookable resources. Accepts a `Filter` with `search`, `section`, `resourceTypes`, and `filterType`. | `Resource<List<BGPResource>>` |
| `BlueGPSLib.instance.schedule(request)` | Books a resource for the specified time window. | `Resource<Unit>` |

**`ScheduleRequest` fields:**

```kotlin
data class ScheduleRequest(
    val elementId: String,
    val elementType: String,
    val dayStart: String,      // "yyyy-MM-dd"
    val start: String,         // "HH:mm"
    val end: String,           // "HH:mm"
    val meetingName: String,
    val videoConference: Boolean
)
```

### Region / SSE Events

| Method | Description |
|---|---|
| `BlueGPSLib.instance.startNotifyRegionChanges(tags, regions, callbackHandler)` | Subscribes to region ENTER/EXIT events for the given tags and regions. |
| `BlueGPSLib.instance.stopNotifyRegionChanges()` | Unsubscribes from region events. |

---

## SDK Changelog

[Changelog link](https://synapseslab.com/android-bluegps-sdk-public/changelog/changelog.html)

## Documentation
For comprehensive details about BlueGPS SDK, please refer to the complete documentation available here.

[Documentation link](https://synapseslab.com/android-bluegps-sdk-public/doc/doc.html)


## License

[LICENSE](https://github.com/synapseslab/android-bluegps-demoapp-public/blob/main/LICENSE.md)
