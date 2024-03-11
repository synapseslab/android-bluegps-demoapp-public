# BlueGPS Android Demo app

A minimal DemoApp for using BlueGPS SDK

## Sample App

To run the sample app, start by cloning this repo:

 ```shell
git clone git@github.com:synapseslab/android-bluegps-demoapp-public.git
```

and play with it.

## Examples

#### Authentication

- [Keycloak Authentication](https://github.com/synapseslab/android-bluegps-demoapp-public/blob/main/demo-app/app/src/main/java/com/synapseslab/bluegpssdkdemo/keycloak/KeycloakActivity.kt) - [(documentation)](https://synapseslab.com/android-bluegps-sdk-public/v2/v2.html#32-app-authentication)

#### BlueGPS MapView
- [Map navigation and interaction](https://github.com/synapseslab/android-bluegps-demoapp-public/blob/main/demo-app/app/src/main/java/com/synapseslab/bluegpssdkdemo/map/MapActivity.kt) - [(documentation)](https://synapseslab.com/android-bluegps-sdk-public/v2/v2.html#5-bluegpsmapview)

#### Search objects
BlueGPSSDK provides some built-in capabilities to search for resources and objects within the backend.
- [Search objects](https://github.com/synapseslab/android-bluegps-demoapp-public/blob/main/demo-app/app/src/main/java/com/synapseslab/bluegpssdkdemo/search_object/SearchObjectsActivity.kt) - [(documentation)](https://synapseslab.com/android-bluegps-sdk-public/v2/v2.html#8-search-object-api)

#### Controllable items API
BlueGPSSDK provides a logic to interact with controllable items exposed by the backend. Controllable items could be anything that can be remote controlled by the application.

- [IOT Controllable elements](https://github.com/synapseslab/android-bluegps-demoapp-public/blob/main/demo-app/app/src/main/java/com/synapseslab/bluegpssdkdemo/controllable_elements/ControllableElementsActivity.kt) - [(documentation)](https://github.com/synapseslab/android-bluegps-demoapp-public/blob/main/documentation/bluegps_android_sdk.md#9-controllable-items-api)

#### Area API
BlueGPSSDK provides some built-in capabilities for rooms and areas.


- [Area API](https://github.com/synapseslab/android-bluegps-demoapp-public/blob/main/demo-app/app/src/main/java/com/synapseslab/bluegpssdkdemo/area/AreaActivity.kt) - [(documentation)](https://synapseslab.com/android-bluegps-sdk-public/v2/v2.html#10-area-api)

#### Use BlueGPS Advertising Service
- [BlueGPS Advertising Service](https://github.com/synapseslab/android-bluegps-demoapp-public/blob/main/demo-app/app/src/main/java/com/synapseslab/bluegpssdkdemo/login/MainActivity.kt#L62) - [(documentation)](https://synapseslab.com/android-bluegps-sdk-public/v2/v2.html#4-use-bluegps-advertising-service)

#### Server Sent Events
The purpose of diagnostic API is to give an indication to the integrator of the status of the BlueGPS system.


- [SSE Notify region changes](https://github.com/synapseslab/android-bluegps-demoapp-public/blob/main/demo-app/app/src/main/java/com/synapseslab/bluegpssdkdemo/sse/NotifyRegionActivity.kt) - [(documentation)](https://synapseslab.com/android-bluegps-sdk-public/v2/v2.html#62-notify-region-changes)
- [SSE Notify position changes](https://github.com/synapseslab/android-bluegps-demoapp-public/blob/main/demo-app/app/src/main/java/com/synapseslab/bluegpssdkdemo/sse/NotifyPositionActivity.kt) - [(documentation)](https://synapseslab.com/android-bluegps-sdk-public/v2/v2.html#63-notify-position-changes)
- [SSE Diagnostic Tag](https://github.com/synapseslab/android-bluegps-demoapp-public/blob/main/demo-app/app/src/main/java/com/synapseslab/bluegpssdkdemo/sse/DiagnosticTagActivity.kt) - [(documentation)](https://synapseslab.com/android-bluegps-sdk-public/v2/v2.html#61-diagnostic-sse)
- [SSE Generic Events](https://github.com/synapseslab/android-bluegps-demoapp-public/blob/main/demo-app/app/src/main/java/com/synapseslab/bluegpssdkdemo/sse/GenericEventsActivity.kt) - [(documentation)](https://synapseslab.com/android-bluegps-sdk-public/v2/v2.html#64-notify-generic-events)

## SDK Changelog

[Changelog link](https://synapseslab.com/android-bluegps-sdk-public/changelog/changelog.md)

## Documentation
For comprehensive details about BlueGPS SDK, please refer to the complete documentation available here.

**For version 2+** refer this
[Documentation link](https://synapseslab.com/android-bluegps-sdk-public/v2/v2.html)


## License

[LICENSE](https://github.com/synapseslab/android-bluegps-demoapp-public/blob/main/LICENSE.md)
