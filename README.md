# BlueGPS Android Demo app

A minimal DemoApp for using BlueGPS SDK

## Sample App

To run the sample app, start by cloning this repo:

 ```shell
git clone git@github.com:synapseslab/android-bluegps-demoapp-public.git
```

and play with it.

## Examples

#### LoginScreen
This screen provides two authentication methods: a "Guest Login" and a standard "Login".
The standard login process is handled by launching the `AuthLoginActivity` from the BlueGPS SDK.

#### MapScreen
This screen displays an interactive map using the BlueGPS SDK. It integrates an [AndroidView] to host the `MapViewManager` and provides UI controls for map interactions such as changing floors, showing a specific tag, and toggling "follow me" mode.

#### BookingScreen
This screen allows users to search for and book resources, such as desks or meeting rooms.
It displays a list of available resources, provides a search bar to filter them, and offers options to schedule a booking or navigate to the resource's location.

#### NavigationScreen
This screen embeds the BlueGPS map view using [AndroidView] and leverages the [MapViewManager] to control the map's behavior. Upon composition, it automatically triggers the navigation from the user's current location to the provided [position].

#### Use BlueGPS Advertising Service
Centralized helper to work with [BlueGPSAdvertisingService].

#### SSEScreen
This screen displays real-time updates for region enter/exit events and beacon scans.
It provides a user interface to visualize logs from the SSE service and allows clearing these logs.


## SDK Changelog

[Changelog link](https://synapseslab.com/android-bluegps-sdk-public/changelog/changelog.html)

## Documentation
For comprehensive details about BlueGPS SDK, please refer to the complete documentation available here.

**For version 2+** refer this
[Documentation link](https://synapseslab.com/android-bluegps-sdk-public/v2/v2.html)


## License

[LICENSE](https://github.com/synapseslab/android-bluegps-demoapp-public/blob/main/LICENSE.md)
