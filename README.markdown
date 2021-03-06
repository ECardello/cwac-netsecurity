# CWAC-NetSecurity: Simplifying Secure Internet Access

This library contains a backport of
[the Android 7.0 network security configuration](https://developer.android.com/preview/features/security-config.html)
subsystem. In Android 7.0, this subsystem makes it easier for developers
to tie their app to particular certificate authorities or certificates,
support self-signed certificates, and handle other advanced SSL
certificate scenarios. This backport allows the same XML configuration
to be used, going back to API Level 17 (Android 4.2).

This library also offers a `TrustManagerBuilder` and related classes
to make it easier for developers to integrate the network security
configuration backport, particularly for
[OkHttp3](https://github.com/square/okhttp)
and `HttpURLConnection`.

**NOTE**: If Google releases their own backport of network security
configuration, please consider using it. Official backports are usually
stronger candidates than are unofficial ones like the one contained
in this library.

## Installation

The artifact for this library is distributed via the CWAC repository,
so you will need to configure that in your module's `build.gradle` file,
along with your `compile` statement:

```groovy
repositories {
    maven {
        url "https://s3.amazonaws.com/repo.commonsware.com"
    }
}

dependencies {
    compile 'com.commonsware.cwac:netsecurity:0.0.1'
    compile 'com.squareup.okhttp3:okhttp:3.4.0'
}
```

If you are using this library with OkHttp3, you also need to have
a `compile` statement for a compatible OkHttp3 artifact, as shown
above.

If you are using `HttpURLConnection`, or tying this code into some
other HTTP client stack, you can skip the OkHttp3 dependency.

## Basic Usage

Start by following
[Google's documentation for the Android 7.0 network security configuration](https://developer.android.com/preview/features/security-config.html).
Ideally, confirm that your configuration works using an Android 7.0+
device.

Next, add in this `<meta-data>` element to your manifest, as a child
of the `<application>` element:

```xml
<meta-data
  android:name="android.security.net.config"
  android:resource="@xml/net_security_config" />
```

The value for `android:resource` should be the same XML resource that
you used in the `android:networkSecurityConfig` attribute in the
`<application>` element.

Then, in your code where you want to set up your network communications,
create a `TrustManagerBuilder` and teach it to load the configuration
from the manifest:

```java
TrustManagerBuilder tmb=
  new TrustManagerBuilder().withManifestConfig(ctxt);
```

(where `ctxt` is some `Context`)

If you are using OkHttp3, create your basic `OkHttpClient.Builder`,
then call:

```java
OkHttp3Integrator.applyTo(tmb, okb);
```

(where `tmb` is the `TrustManagerBuilder` from before, and `okb`
is your `OkHttpClient.Builder`)

At this point, you can create your `OkHttpClient` from the `Builder`
and start using it.

If you are using `HttpURLConnection`, you can call `applyTo()` on
the `TrustManagerBuilder` itself, passing in the `HttpURLConnection`.
Afterwards, you can start using the `HttpURLConnection` to make your
HTTP request.

In either case, on Android 7.0+ devices, `withManifestConfig()` will
*not* use the backport. Instead, the platform-native implementation
of the network security configuration subsystem will be used. On
Android 4.2-6.0 devices, the backport will be used.

[JavaDocs for the library are available](http://javadocs.commonsware.com/cwac/netsecurity/index.html).

## Basic Limitations

If you use `HttpURLConnection`, you cannot use `<domain-config>`
elements in the network security configuration XML. Similarly,
you cannot use `cleartextTrafficPermitted` with `HttpURLConnection`.
If you have them in the XML, they will be ignored.

OkHttp3 should support the full range of network security configuration
XML features.

## Advanced Usage

If you want to do more sophisticated things with the network security
configuration backport and/or `TrustManagerBuilder`, there is a
[separate page of documentation](https://github.com/commonsguy/cwac-netsecurity/blob/master/docs/ADVANCED_USAGE.markdown)
for that.

## Compiling from Source and Running the Test Suites

The instrumentation tests in `androidTest/` are subdivided into two
groups: `pub` and `priv`.

The `pub` tests hit publicly-available Web servers (mostly those
hosted by CommonsWare). As such, you should be able to run those
tests without issue.

The `priv` tests need additional configuration on your part. That
configuration is designed to be held in a `gradle.properties`
file that you need to add to your root directory of your copy
of the project code. Specifically, three values should reside there:

- `TEST_PRIVATE_HTTP_URL`: a URL to some Web server that you control
- `TEST_PRIVATE_HTTPS_URL`: a URL to some Web server that you control, where the communications are secured via SSL using a self-signed certificate
- `TEST_PRIVATE_HTTP_REDIR_URL`: a URL to some Web server that you control that, when requested, issues a server-side redirect to an SSL-secured page (such as the one from `TEST_PRIVATE_HTTPS_URL`)

The first two URLs should each return:

```json
{"Hello": "world"}
```

You will need to define those values in your `gradle.properties` file
even if you are just planning on modifying the code, as otherwise
the `build.gradle` files for the library modules will fail, as they expect
those values.

In addition, if you wish to run the `priv` tests, you will need to
replace the `androidTest/res/raw/selfsigned.crt` file in each library
module with the CRT file that matches your self-signed certificate that
`TEST_PRIVATE_HTTPS_URL` uses.

## Dependencies

`netsecurity` has a `provided` dependency on OkHttp3. This library
should fairly closely track the latest OkHttp3 release, presently
**3.4.0**. If you find
that the library has fallen behind, please
[file an issue](https://github.com/commonsguy/cwac-netsecurity/issues)
if one has not already been filed.

`netsecurity` depends upon the `support-annotations` from the Android SDK.

Otherwise, there are no external dependencies.

## Version

The current version is **0.0.1**.

## Demo

The `demo/` module is an Android app that uses OkHttp3, Retrofit,
and Picasso to show the latest Android questions on Stack Overflow
in a `ListView`. Retrofit and Picasso use a common OkHttp3-defined
`OkHttpClient` object, and that client uses `netsecurity` to
ensure that connections to key hosts, such as the Stack Exchange
Web service API, use SSL certificates from the expected certificate
authorities.

## License

All of the code in this repository is licensed under the
Apache Software License 2.0. Look to the headers of the Java source
files to determine the actual copyright holder, as it is a mix of
the Android Open Source Project and CommonsWare, LLC.

## Questions

If you have questions regarding the use of this code, please post a question
on [StackOverflow](http://stackoverflow.com/questions/ask) tagged with
`commonsware-cwac` and `android` after [searching to see if there already is an answer](https://stackoverflow.com/search?q=[commonsware-cwac]+camera). Be sure to indicate
what CWAC module you are having issues with, and be sure to include source code 
and stack traces if you are encountering crashes.

If you have encountered what is clearly a bug, or if you have a feature request,
please read [the contribution guidelines](.github/CONTRIBUTING.md), then
post an [issue](https://github.com/commonsguy/cwac-netsecurity/issues).
**Be certain to include complete steps for reproducing the issue.**

Do not ask for help via social media.

## AOSP Version Tracking and Release Notes

|Library Version|AOSP Code Base                                                                                          |Release Notes|
|:-------------:|:------------------------------------------------------------------------------------------------------:|-------------|
|v0.0.1         |[`android-n-preview-4`](https://android.googlesource.com/platform/frameworks/base/+/android-n-preview-4)|initial release|
