#!/usr/bin/env python

from pysqueezecenter.server import Server
from pysqueezecenter.player import Player

sc = Server(hostname="127.0.0.1", port=9090, username="user", password="password")
sc.connect()

print "Version: %s" % sc.get_version()

sq = sc.get_player("00:04:20:2A:B0:A5")

print "Name: %s | Mode: %s | Time: %s | Connected: %s | WiFi: %s" % (sq.get_name(), sq.get_mode(), sq.get_time_elapsed(), sq.is_connected, sq.get_wifi_signal_strength())

print sq.get_track_title()
print sq.get_time_remaining()

#Delete all alarms
alarms  = str(sq.get_alarms(50,"all"))
for b in alarms.split(" "):
    item = b.split(":")
    if  item[0] == "id":
        print item[1]
        sq.delete_alarm(item[1])




