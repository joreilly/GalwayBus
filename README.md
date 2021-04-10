### Galway Bus Kotlin Multiplatform project using Jetpack Compose and SwiftUI


**Note**: if you are trying this project out and aren't in Galway then hit the home icon in title bar to center in Galway so you can see meaningful data!

### Jetpack Compose

The main Jetpack Compose based app module is contained in `android-app`...this is also the version that's now 
published to Play Store.  The previous "legacy" version of app (based on fragments/layouts etc) is still contained in `app` module but will be removed in near future.


### Kotlin Multiplatform

Currently this project is also being used as platform to explore some of the relatively new **Kotlin Multiplatform**
capabilities.  There is a Kotlin Multiplatform `ShareCode` module for example along with sample iOS
and macOS apps. Have also written a couple of posts about some of my experiences doing this so far with this project.  Also, 
see [PeopleInSpace](https://github.com/joreilly/PeopleInSpace) and [BikeShare](https://github.com/joreilly/BikeShare) for 
other examples of use of Kotlin Multiplatform code.

* [SwiftUI meets Kotlin Multiplatform!](https://johnoreilly.dev/2019/06/08/swiftui-meetings-kotlin-multiplatform/)
* [Introduction to Multiplatform Persistence with SQLDelight](https://johnoreilly.dev/posts/sqldelight-multiplatform/)
* [Using Google Maps in a Jetpack Compose app](https://johnoreilly.dev/posts/jetpack-compose-google-maps/)


### Google Maps SDK

The project depends on `GOOGLE_API_KEY` environment variable to be defined for maps functionality to work.  Alternatively
you can update where this is read in `build.gradle`

### Languages, libraries and tools used

* [Kotlin](https://kotlinlang.org/)
* [Kotlin Corooutines](https://kotlinlang.org/docs/reference/coroutines-overview.html)
* [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)
* [Ktor client library](https://github.com/ktorio/ktor)
* [Android Architecture Components](https://developer.android.com/topic/libraries/architecture/index.html)
* [Koin](https://github.com/InsertKoinIO/koin)
* [Logger](https://github.com/orhanobut/logger)
* [Jetpack Compose](https://developer.android.com/jetpack/compose)
* [SwiftUI](https://developer.apple.com/documentation/swiftui)
