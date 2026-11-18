# BlueGPS Android Demo app

A minimal DemoApp for using BlueGPS SDK

## Sample App

To run the sample app, start by cloning this repo:

 ```shell
git clone git@github.com:synapseslab/android-bluegps-demoapp-public.git
```

and play with it.

### What's new with SDK 6.4

- **Background session** (SDK 6.4.0): the app keeps working in the background behind **one**
  ongoing notification whose content is entirely the app's. Try it in the **Session** tab
  ([SessionScreen](#sessionscreen)); the code is in `session/DemoSession.kt`.
- **One switch in this demo: the session.** Logging in starts nothing. **Start session** starts the
  session, beacon scanning and advertising together; **Stop session**, the Stop of the
  notification and the logout stop all of them; after a reboot they restart together. The app
  always shows one notification, with one icon.
- **New advertising API**: `BlueGPSLib.instance.startAdvertising()` / `stopAdvertising()` /
  `advertising`. The demo calls it from `DemoSession`.
- **`BlueGPSAdvertisingService` is deprecated** and removed in SDK 7.0.0. It still works. The old
  way has its own screen, apart from the new one: **Settings → Legacy advertising (deprecated)**
  ([LegacyAdvertisingScreen](#legacyadvertisingscreen)), code in `advertising/AdvManager.kt`.
- **The SDK logout stops everything**: session, advertising, beacon scanning and the location
  modules. The app no longer stops advertising itself before logging out.
- SDK 6.4.1: `guestLogin()` right after `initSDK` works, and a session resumed after a restart keeps
  its login mode (guest or credentials).

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

#### Login redirect

After the browser login, Keycloak sends the user back to the app through the `redirect_uri`. The SDK
supports two ways to receive it.

**Custom URI scheme (used by this demo, still supported).** You can keep the previous method: a
`redirect_uri` with your own scheme and host (for example `myapp://my.host`), with no
`/mobileapp/callback` App Link. Set the same scheme and host in your `AndroidManifest.xml`:

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

Even with the custom scheme, from SDK 6.1.7 the two manifest placeholders below are **required**,
or the manifest merge fails: the SDK manifest declares an HTTPS App Link on `AuthenticationActivity`
built from them. Set them to your BlueGPS host (see `app/build.gradle.kts`):

```kotlin
android {
    defaultConfig {
        manifestPlaceholders["authRedirectHost"] = "{{your-bluegps-host}}"
        manifestPlaceholders["authRedirectPath"] = "/mobileapp/callback"
    }
}
```

**HTTPS App Link (recommended).** Set `redirect_uri` to
`https://<authRedirectHost><authRedirectPath>`, register it in Keycloak, publish your package and
signing certificate fingerprints in `https://<authRedirectHost>/.well-known/assetlinks.json`, and do
not declare your own intent filter on `AuthenticationActivity`: the SDK declares it. Android then
delivers the login result only to your app, which a custom scheme cannot guarantee. Full steps in
the [SDK documentation](https://synapseslab.com/android-bluegps-sdk-public/doc/doc.html), *Login
redirect*.

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
├── advertising/        # Legacy advertising example (AdvManager, deprecated)
├── constants/          # Environment configuration object
├── receiver/           # BroadcastReceivers (beacon events, boot)
├── routes/             # Compose navigation graph (AppRoutes)
├── session/            # Background session: notification content and Stop action
├── screens/
│   ├── init/           # Splash / loading screen
│   ├── legacy/         # Legacy advertising (deprecated), opened from Settings
│   ├── login/          # Authentication screen
│   ├── main/           # Bottom-nav host screen
│   ├── map/            # Interactive indoor map
│   ├── booking/        # Resource search & booking
│   ├── navigation/     # Wayfinding / route-to-destination
│   ├── sse/            # Real-time Server-Sent Events log
│   ├── session/        # Background session and advertising
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
| SDK | `com.github.synapseslab:android-bluegps-sdk-public:6.4.1` |
| Min SDK | 24 |
| Target / Compile SDK | 36 |
| Kotlin | 2.1.0 |
| AGP | 8.13.0 |
| Gradle | 9.3.1 (runs on the JDK bundled with current Android Studio) |

**SDK requirements** for your own app: `compileSdk` 36 or higher, Kotlin 2.1 or higher, and OkHttp
5.1 (`okhttp` and `logging-interceptor`) if your app declares OkHttp itself: the SDK calls an API
that exists only from OkHttp 5, and forcing 4.x crashes at `initSDK`.

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
| Session | `session` | `SessionScreen` |
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

After a successful login of either type the ViewModel sets `isLoggedIn = true`, triggering
navigation to `MainScreen`. Nothing else starts at login: beacon scanning and advertising start
with the background session (Session tab).

Neither login writes its result to the log: it holds the access token (and the refresh token for
the user login).

**ViewModel state**

```kotlin
val isLoggedIn: StateFlow<Boolean?> // null = checking, true = logged in, false = logged out
```

**Key ViewModel methods**

| Method | Description |
|---|---|
| `checkLoginStatus()` | Observes `BlueGPSLib.instance.isAValidSession()` and updates `isLoggedIn`. |
| `guestLogin()` | Calls `BlueGPSAuthManager.instance.guestLogin()` and stores the returned token in the SDK. |
| `logout()` | Calls `BlueGPSAuthManager.instance.logout()` and sets `isLoggedIn = false`. The SDK stops session, advertising and tracking itself. |

---

### MainScreen

**File:** `screens/main/MainScreen.kt`

The root screen after login. It:
- Renders a `BottomNavigationBar` with five tabs.
- Does **not** start beacon scanning: since SDK 6.4 the demo starts it with the background session
  (see [SessionScreen](#sessionscreen)). Map and SSE show a hint (`SessionHint`) while the session
  is not running.

---

### MapScreen

**File:** `screens/map/MapScreen.kt` · `MapViewModel.kt` · `MapViewManager.kt`

Displays a full-screen interactive indoor map powered by `BlueGPSMapView`.

#### Loader while the map loads

Since SDK 6.3.8 the map stays invisible from `initMap()` until `TypeMapCallback.INIT_SDK_COMPLETED`
and then fades in, so there is no white flash. The page's own loading animation is not visible
either: what to show meanwhile is the app's choice. The demo shows a `CircularProgressIndicator`
over the map until:

- `INIT_SDK_COMPLETED` arrives;
- or an `ERROR` arrives, so the loader never spins forever;
- or 15 seconds pass, the same limit after which the SDK shows the map anyway.

`AUTH_ERROR` does not hide it: it arrives during the init, which goes on after `initAuth()`. The
state is kept with `rememberSaveable`: the map view is a singleton and `INIT_SDK_COMPLETED` arrives
only once, so coming back to the tab does not show the loader again.

To show the page's own animation instead, set `hideUntilReady = false` on the map view before
`initMap()`: the white flash comes back with it.

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

### SessionScreen

**File:** `screens/session/SessionScreen.kt` · `SessionViewModel.kt` · `session/DemoSession.kt` · `session/SessionStopReceiver.kt`

Shows the **background session** of SDK 6.4.0: one ongoing notification, drawn by the app, under
which advertising, beacon scanning and the location modules keep running when the app leaves the
screen. Same API as the BlueGPS iOS SDK.

| Control | What it does |
|---|---|
| **Start session** | `DemoSession.start()`: the session (`backgroundSession.start(channel, initial, updates, render)`), then beacon scanning (`BeaconTracking.start()` → `startTrackingUserPosition()`) and advertising (`startAdvertising()`). The notification appears at once, on the app's channel `live_session`, and the scan and advertising run under it. |
| **Stop session** | `backgroundSession.stop()`: stops the session and everything under it (advertising, beacon scanning) and removes the notification. |
| **Update notification** | Changes the content: the notification is redrawn (at most once per second). |
| **Stop** in the notification | The app's own action (`SessionStopReceiver`): same as **Stop session**. The SDK puts no text, icon or action of its own in the notification. |

The screen shows what the SDK reports: `state` (`Idle` / `Running`), `indicator` (the
notification: `Shown`, `Ended`, `Unavailable`), `limitations` (for example
`INDICATOR_NOT_ALLOWED` without `POST_NOTIFICATIONS`), the beacon scan
(`BlueGPSLib.instance.beaconScanRunningFlow`), `advertising` (`Off` / `On` / `Failed`) and the
last `events`. The notification text shows the beacon scan and advertising states too.

How the demo builds the notification:

```kotlin
BlueGPSLib.instance.backgroundSession.start(
    channel = IndicatorChannel(id = "live_session", name = getString(R.string.session_channel_name)),
    initial = content.value,
    updates = content,                 // a StateFlow kept in DemoSession, not in a ViewModel
) { c ->                               // render: runs at every new value
    setSmallIcon(R.drawable.ic_session)
    setContentTitle(getString(R.string.session_title))
    setContentText(c.summary)          // collapsed: "Beacon scan on · Advertising on, tag …"
    setStyle(NotificationCompat.BigTextStyle().bigText(c.details))   // expanded: one line per job
    setSubText(getString(R.string.session_updates, c.updates))
    setContentIntent(openApp)          // tap: back to the app
    addAction(0, getString(R.string.session_stop), stop)   // stop: PendingIntent → SessionStopReceiver
}
```

Things to know:

- **Start the session with the app in the foreground.** From the background Android refuses the
  foreground service and the indicator is `Unavailable(REQUEST_FAILED)`.
- **Keep the content in an object that lives as long as the app** (`DemoSession`): a flow tied to a
  screen stops with it and freezes the notification on its last value.
- **Without `POST_NOTIFICATIONS`**, or after the user swipes the notification away (Android 14+),
  the session goes on but its work pauses when the app goes to the background and resumes when it
  comes back.
- **Advertising starts after the notification is up.** The SDK starts advertising with the app in
  the foreground or with a visible notification: after a reboot the app is not in the foreground,
  so `DemoSession` waits for the indicator before calling `startAdvertising()`.
- What did not start (beacon scan, advertising) is shown on the screen with the reason.
- **The "left on" flag** (`DemoSession.wasLeftOn()`, SharedPreferences) is set by Start session and
  cleared by Stop session and by every `Stopped` event (logout, the Stop of the notification). When
  the UI starts with the flag set but no session running (the process died), it is cleared.
- When the activity is closed for good, `MainActivity` stops advertising **unless a session is
  running** (it can only be on without a session if started from the legacy screen).
- The notification icon (`ic_session`, the BlueGPS whale) is a copy in the demo's own resources:
  the content of the notification, icon included, belongs to the app.

Full guide: [SDK documentation, §4](https://synapseslab.com/android-bluegps-sdk-public/doc/doc.html).

---

### SettingsScreen

**File:** `screens/settings/SettingsScreen.kt` · `SettingsViewModel.kt`

A simple informational screen with a logout action, and the entry to the legacy advertising
screen.

| Field | Source |
|---|---|
| **Logged user** | JWT `preferred_username` or `email` claim decoded from `BlueGPSLib.instance.accessToken()` via `JWTDecode.getClaimRaw()`. |
| **Tag ID** | `BlueGPSLib.instance.userTagId()` — the BLE tag identifier assigned to this device. |
| **App version** | `PackageManager.getPackageInfo().versionName` |

Tapping **Logout** calls `LoginViewModel.logout()`, which logs out via Keycloak and navigates back to `LoginScreen`. The SDK logout stops the session, advertising and tracking (SDK 6.4.0+).

---

### LegacyAdvertisingScreen

**File:** `screens/legacy/LegacyAdvertisingScreen.kt` · `LegacyAdvertisingViewModel.kt` · `advertising/AdvManager.kt`

Opened from **Settings → Legacy advertising (deprecated)**. The old way to advertise, kept for apps
that still use `BlueGPSAdvertisingService` (deprecated since SDK 6.4.0, removed in 7.0.0). It is a
separate screen on purpose: the old and the new way must not be mixed in an app, because they drive
the same advertising.

| Control | What it does |
|---|---|
| **Bind** / **Unbind** | `AdvManager.bindBlueGPSAdvertisingService()` / `unBindBlueGPSAdvertisingService()`, on the application context. |
| **startAdv** | `getOrCreateConfiguration()`, then `AdvManager.startAdvertising(config)`. Without a session the service shows its own notification. |
| **stopAdv** | `AdvManager.stopAdvertising()`. |

The last `AdvertisingStatus` broadcast (`STARTED`, `STOPPED`, `ERROR`) is shown while the screen is
open, received with `RECEIVER_NOT_EXPORTED`. While the background session runs, Bind, startAdv and
stopAdv are disabled: the two ways drive the same advertising. The screen binds to the
application context and unbinds only with **Unbind**; in an app, bind and unbind with the
activity (see [BLE Advertising](#ble-advertising)).

---

## Background Components

### BLE Advertising

The demo uses the **new advertising API** (SDK 6.4.0+), with no binding and no broadcast, from
`DemoSession.start()`:

```kotlin
try {
    BlueGPSLib.instance.startAdvertising()     // configuration from the server
} catch (e: AdvertisingException) {
    // e.code: BLUETOOTH_OFF, BLUETOOTH_NOT_AUTHORIZED, INVALID_TAG_ID, CONFIGURATION_UNAVAILABLE,
    //         STARTED_FROM_BACKGROUND, ADVERTISING_NOT_SUPPORTED, ADVERTISING_FAILED
}
BlueGPSLib.instance.advertising                // StateFlow: Off · On(configuration) · Failed(code)
BlueGPSLib.instance.stopAdvertising()
```

#### Legacy: `AdvManager` (deprecated)

**File:** `advertising/AdvManager.kt`

> [!WARNING]
> `BlueGPSAdvertisingService` is **deprecated since SDK 6.4.0 and removed in 7.0.0**. It still
> works, on the same engine as `startAdvertising()`, so existing integrations do not break. The
> app flow no longer uses `AdvManager`: it is kept, marked `@Deprecated`, for apps that still use
> the service, and driven only by the [LegacyAdvertisingScreen](#legacyadvertisingscreen). Move to
> the new API when you can.

`AdvManager` is a singleton that wraps `BlueGPSAdvertisingService`:

| Method | Description |
|---|---|
| `bindBlueGPSAdvertisingService(context)` | Binds to the advertising service. Call it in `Activity.onCreate()`. |
| `unBindBlueGPSAdvertisingService(context)` | Unbinds the service and drops the reference to it. Call it in `Activity.onDestroy()`, symmetric with the bind in `onCreate()`. |
| `startAdvertising(config)` | Starts BLE advertisement with the provided `AndroidAdvConfiguration` (from `BlueGPSLib.instance.getOrCreateConfiguration()`). |
| `stopAdvertising()` | Stops BLE advertisement. |

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

Restarts the background session, and beacon scanning under it, after a device reboot.

- Listens for `ACTION_BOOT_COMPLETED` (delivered after the first unlock).
- Re-initializes `BlueGPSLib` with `Environment.sdkEnvironment`.
- Calls `DemoSession.start()`. Android lets an app start a foreground service right after
  `BOOT_COMPLETED`; at any other moment in the background the session cannot start.
- Only if the user left the session on (Start session, never stopped) and is still logged in.
- Android does not send `BOOT_COMPLETED` only at boot: also when the app is opened again after a
  force stop or after its data was cleared. The same rule applies: after a force stop with the
  session left on, it comes back; after the data was cleared, nothing starts.
- Beacon scanning and advertising restart with the session. Advertising waits for the notification:
  the app is not in the foreground at boot.
- A session started at boot has no location access until the app comes to the foreground
  (Android rule for foreground services started from the background).

---

## Permissions

The app declares and requests the following permissions at runtime:

| Permission | Purpose |
|---|---|
| `BLUETOOTH_CONNECT` | Connect to BLE devices (Android 12+). |
| `BLUETOOTH_ADVERTISE` | BLE advertising of the device tag (Android 12+). Declared by the SDK manifest, requested by the app. Without it `startAdvertising()` throws `AdvertisingException(BLUETOOTH_NOT_AUTHORIZED)`. |
| `BLUETOOTH_SCAN` | Scan for nearby BLE beacons (Android 12+). |
| `ACCESS_FINE_LOCATION` | Required for BLE scanning on Android < 12. |
| `ACCESS_COARSE_LOCATION` | Fallback location permission. |
| `ACCESS_BACKGROUND_LOCATION` | Keep tracking active when the app is in the background (Android 10+). |
| `POST_NOTIFICATIONS` | Show the background session notification and the beacon event notifications (Android 13+). Without it the session reports `INDICATOR_NOT_ALLOWED`, and advertising and beacon scanning run only while the app is in the foreground. |
| `FOREGROUND_SERVICE` | Run the background session (and the legacy advertising service) in the foreground. |

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
| `BlueGPSAuthManager.instance.guestLogin()` | Authenticates as a guest using client credentials. Never throws: a missing setting returns `Resource.Error` with code `400` (SDK 6.3.5+). | `Resource<KeyCloakAuthResponse>` |
| `BlueGPSAuthManager.instance.logout(callback)` | Logs out and revokes the token via Keycloak. | `Unit` (async callback) |

### Device & Configuration

| Method | Description | Returns |
|---|---|---|
| `BlueGPSLib.instance.startAdvertising()` | Starts BLE advertising, the configuration from the server (SDK 6.4.0+). Suspend; throws `AdvertisingException`. | `Unit` |
| `BlueGPSLib.instance.stopAdvertising()` | Stops BLE advertising. | `Unit` |
| `BlueGPSLib.instance.advertising` | Advertising state: `Off`, `On(configuration)`, `Failed(code)`. | `StateFlow<AdvertisingState>` |
| `BlueGPSLib.instance.backgroundSession` | The background session: `start`, `stop`, `state`, `indicator`, `limitations`, `events`. | `BackgroundSession` |
| `BlueGPSLib.instance.getOrCreateConfiguration()` | Retrieves the device's BLE advertising configuration from the server, creating one if needed (used by the legacy `AdvManager`). | `Resource<AndroidAdvConfiguration>` |
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
