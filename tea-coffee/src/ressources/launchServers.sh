#!/bin/bash

wPort=8000
hPort=8001
mPort=8002
size=100

java WelcomeServer $wPort $size &
java HashServer $hPort $size &
java MonitorServer localhost $wPort $mPort
