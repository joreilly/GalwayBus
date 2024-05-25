### Galway Bus Kotlin Multiplatform project using Jetpack Compose and SwiftUI

![kotlin-version](https://img.shields.io/badge/kotlin-2.0.0-blue?logo=kotlin)

**Note**: if you are trying this project out and aren't in Galway then hit the home icon in title bar to center in Galway so you can see meaningful data!


### Kotlin Multiplatform

This project also acted as initial platform I used when starting to explore **Kotlin Multiplatform**
capabilities. There's a Kotlin Multiplatform `ShareCode` module for example along with sample Android, iOS
and macOS apps. Also wrote a number of posts about some of my experiences using **KMP** in the project.  

* [SwiftUI meets Kotlin Multiplatform!](https://johnoreilly.dev/2019/06/08/swiftui-meetings-kotlin-multiplatform/)
* [Introduction to Multiplatform Persistence with SQLDelight](https://johnoreilly.dev/posts/sqldelight-multiplatform/)
* [Using Google Maps in a Jetpack Compose app](https://johnoreilly.dev/posts/jetpack-compose-google-maps/)
* [Using Google Maps in a Jetpack Compose app - Part 2!](https://johnoreilly.dev/posts/jetpack-compose-google-maps-part2/)


### Google Maps SDK

The project depends on `GOOGLE_API_KEY` environment variable to be defined for maps functionality to work.  Alternatively
you can update where this is read in `build.gradle`

### Screenshots 


**Android (Jetpack Compose)**

<img width="1081" alt="Screenshot 2022-10-08 at 12 04 27" src="https://user-images.githubusercontent.com/6302/194704565-eedc89c1-751a-455b-a0b2-8855b5be7bbb.png">



**iOS (SwiftUI)**

<img width="673" alt="Screenshot 2022-10-08 at 10 44 05" src="https://user-images.githubusercontent.com/6302/194701185-a797d31c-e6d3-48f2-bc04-e05b1e891a75.png">


### Languages, libraries and tools used

* [Kotlin](https://kotlinlang.org/)
* [Kotlin Corooutines](https://kotlinlang.org/docs/reference/coroutines-overview.html)
* [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)
* [Ktor client library](https://github.com/ktorio/ktor)
* [Android Architecture Components](https://developer.android.com/topic/libraries/architecture/index.html)
* [Koin](https://github.com/InsertKoinIO/koin)
* [Jetpack Compose](https://developer.android.com/jetpack/compose)
* [Maps Compose](https://github.com/googlemaps/android-maps-compose)
* [SwiftUI](https://developer.apple.com/documentation/swiftui)
* [KMP-NativeCoroutines](https://github.com/rickclephas/KMP-NativeCoroutines)
* [Multiplatform Settings](https://github.com/russhwolf/multiplatform-settings)


## Full set of Kotlin Multiplatform/Compose/SwiftUI samples

*  PeopleInSpace (https://github.com/joreilly/PeopleInSpace)
*  GalwayBus (https://github.com/joreilly/GalwayBus)
*  Confetti (https://github.com/joreilly/Confetti)
*  BikeShare (https://github.com/joreilly/BikeShare)
*  FantasyPremierLeague (https://github.com/joreilly/FantasyPremierLeague)
*  ClimateTrace (https://github.com/joreilly/ClimateTraceKMP)
*  GeminiKMP (https://github.com/joreilly/GeminiKMP)
*  MortyComposeKMM (https://github.com/joreilly/MortyComposeKMM)
*  StarWars (https://github.com/joreilly/StarWars)
*  WordMasterKMP (https://github.com/joreilly/WordMasterKMP)
*  Chip-8 (https://github.com/joreilly/chip-8)
