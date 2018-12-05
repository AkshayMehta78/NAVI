# NAVI

**Navigation Assistant for Visually Impaired**

This prototype consists of an Android application and a wearable, which communicate through BLE. It demonstrates the basic functionality of NAVI. 

## Android application

The Android application counts your steps and sends a random signal (vibrate once or vibrate two times) for every ten steps. After 40 steps a finish signal is sent, which triggers the wearable to vibrate for five seconds.

## Wearable

The wearable consists of a ESP32, which controls a vibration emitter. 

## Other

This project was build as part of the lecture Mobile Innovations for Global Challenges by Dr. Christelle Scharff. Check the [wiki](https://github.com/AkshayMehta78/NAVI/wiki) for more information.
