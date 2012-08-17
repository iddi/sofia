#!/usr/bin/env python

from pysqueezecenter.server import Server
from pysqueezecenter.player import Player

#sc = Server(hostname="192.168.1.1", port=9090, username="user", password="password")
sc = Server(hostname="127.0.0.1", port=9090, username="user", password="password")
sc.connect()

print "Logged in: %s" % sc.logged_in
print "Version: %s" % sc.get_version()

sq = sc.get_player("00:04:20:2A:B0:A5")

print "Name: %s | Mode: %s | Time: %s | Connected: %s | WiFi: %s" % (sq.get_name(), sq.get_mode(), sq.get_time_elapsed(), sq.is_connected, sq.get_wifi_signal_strength())

print sq.get_track_title()
print sq.get_time_remaining()
#print sq.play()

#sq.playlist_play("/home/gerrit/Music/Bittereinder/10 Penworstel.mp3")
#sq.playlist_load_tracks_title("die%20waarskuwing")
#sq.next()

#Delete all alarms
alarms  = str(sq.get_alarms(50,"all"))
for b in alarms.split(" "):
    item = b.split(":")
    if  item[0] == "id":
        print item[1]
        sq.delete_alarm(item[1])




