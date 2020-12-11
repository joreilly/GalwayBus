### Galway Bus Kotlin Multiplatform project using Jetpack Compose and SwiftUI


### Jetpack Compose

The Jetpack Compose version of app is in separate `android-app` module and is still work in progress.  The existing
`app` module (based on fragments/layouts etc) is what's currently used for version in Play Store.


### Kotlin Multiplatform

Currently this project is being mostly used as platform to explore some of the relatively new Kotlin Multiplatform
capabilities.  There is now **Kotlin Multiplatform** `ShareCode` module for example and some sample iOS apps in `ios` folder. Have also
written a couple of posts about some of my experiences doing this so far with this project.  Also, 
see [PeopleInSpace](https://github.com/joreilly/PeopleInSpace) and [BikeShare](https://github.com/joreilly/BikeShare) for 
other examples of use of Kotlin Multiplatform code.

* [SwiftUI meets Kotlin Multiplatform!](https://johnoreilly.dev/2019/06/08/swiftui-meetings-kotlin-multiplatform.html)
* [Introduction to Multiplatform Persistence with SQLDelight](https://johnoreilly.dev/posts/sqldelight-multiplatform/)
* [Using Google Maps in a Jetpack Compose app](https://johnoreilly.dev/posts/jetpack-compose-google-maps/)

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
