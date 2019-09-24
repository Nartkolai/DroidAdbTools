# Droid adb tools

This application connects via the adb protocol to another android device, receives a screenshot and transfers the touch to the managed device from the picture.
Inspiration: [ADB Control](https://marian.schedenig.name/2014/07/03/remote-control-your-android-phone-through-adb/)</p>
Sources used:</p>
* package com.jjnford.android.util [source](https://github.com/jjNford/android-shell)
* package name.schedenig.adbcontrol [source](https://marian.schedenig.name/2014/07/03/remote-control-your-android-phone-through-adb/)

Source of answers: [Stack Overflow](https://stackoverflow.com/)

This version of the application only works with the built-in "adb" binary file. In android version 6 and above, the built-in "adb" system is missing, so the program will not work with the remote device.

In order for the program to work in the version of android 6 and more, it is necessary to create the “adb” folder in the root “sdcard” into which the binaries “adb” and “libcrypto.so” should be copied.
Video: [Youtube](https://youtu.be/H-ziD5EttN8)

*"application.properties":* 
>USER_HOME=/home/ </p>
>VERSION_CODE=0
