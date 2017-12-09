### Android Galway Bus app using Kotlin/Architecture Components/Clean Architecture


[![Google Play logo](http://www.android.com/images/brand/android_app_on_play_logo_large.png)](https://play.google.com/store/apps/details?id=com.surrus.galwaybus)


This was created as part of effort to get more familiar with developing Android apps using 
Kotlin and the new [Android Architecture Components](https://developer.android.com/topic/libraries/architecture/index.html)
and also to explore use of [Clean Architecture](https://8thlight.com/blog/uncle-bob/2012/08/13/the-clean-architecture.html) approach. It's heavily based on https://github.com/bufferapp/clean-architecture-components-boilerplate. This is stil work in progress and also, at least for now,
 have omitted `Mapper` classes (using same data model across the different layers....though this will likely change).

Note also that this is using [REST endpoint](https://github.com/appsandwich/galwaybus) provided by @appsandwich to retrieve
 Galway Bus route/timetable info  (Thanks Vinny!)

## Languages, libraries and tools used

* [Kotlin](https://kotlinlang.org/)
* [Room](https://developer.android.com/topic/libraries/architecture/room.html)
* [Android Architecture Components](https://developer.android.com/topic/libraries/architecture/index.html)
* Android Support Libraries
* [RxJava2](https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0)
* [Dagger 2](https://github.com/google/dagger)
* [Retrofit](http://square.github.io/retrofit/)
* [OkHttp](http://square.github.io/okhttp/)
* [Gson](https://github.com/google/gson)
* [Mockito](http://site.mockito.org/)


