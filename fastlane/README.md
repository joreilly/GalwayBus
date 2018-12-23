fastlane documentation
================
# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```
xcode-select --install
```

Install _fastlane_ using
```
[sudo] gem install fastlane -NV
```
or alternatively using `brew cask install fastlane`

# Available Actions
## Android
### android test
```
fastlane android test
```
Runs all the tests
### android beta
```
fastlane android beta
```
Submit a new Beta Build to Crashlytics Beta
### android deploy
```
fastlane android deploy
```
Deploy a new version to the Google Play
### android deployAlpha
```
fastlane android deployAlpha
```
Deploy app to play store alpha channel
### android deployInternalTest
```
fastlane android deployInternalTest
```

### android buildApp
```
fastlane android buildApp
```

### android buildInstantApp
```
fastlane android buildInstantApp
```

### android promoteAppToProd
```
fastlane android promoteAppToProd
```
Promote app from alpha to production in Play Store
### android screenshots
```
fastlane android screenshots
```
Generate app screenshots that will be uploaded to play store

----

This README.md is auto-generated and will be re-generated every time [fastlane](https://fastlane.tools) is run.
More information about fastlane can be found on [fastlane.tools](https://fastlane.tools).
The documentation of fastlane can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
