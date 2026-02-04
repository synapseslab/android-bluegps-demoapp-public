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
        clientId = "{{provided-client-secret}}", // for user authentication
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

## Inside the Demo app

#### LoginScreen
This screen provides two authentication methods: a "Guest Login" and a standard "Login".
The standard login process is handled by launching the `AuthLoginActivity` from the BlueGPS SDK.

#### MapScreen
This screen displays an interactive map using the BlueGPS SDK. It integrates an `AndroidView` to host the `MapViewManager` and provides UI controls for map interactions such as changing floors, showing a specific tag, and toggling "follow me" mode.

#### BookingScreen
This screen allows users to search for and book resources, such as desks or meeting rooms.
It displays a list of available resources, provides a search bar to filter them, and offers options to schedule a booking or navigate to the resource's location.

#### NavigationScreen
This screen embeds the BlueGPS map view using `AndroidView` and leverages the `MapViewManager` to control the map's behavior. Upon composition, it automatically triggers the navigation from the user's current location to the provided `position`.

#### Use BlueGPS Advertising Service
Centralized helper to work with `BlueGPSAdvertisingService`.

#### SSEScreen
This screen displays real-time updates for region enter/exit events and beacon scans.
It provides a user interface to visualize logs from the SSE service and allows clearing these logs.


## SDK Changelog

[Changelog link](https://synapseslab.com/android-bluegps-sdk-public/changelog/changelog.html)

## Documentation
For comprehensive details about BlueGPS SDK, please refer to the complete documentation available here.

[Documentation link](https://synapseslab.com/android-bluegps-sdk-public/doc/doc.html)


## License

[LICENSE](https://github.com/synapseslab/android-bluegps-demoapp-public/blob/main/LICENSE.md)
