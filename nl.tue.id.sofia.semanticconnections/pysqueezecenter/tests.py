import server

class SBMsgHandler:
    def handle(self, msg):
        for i in msg:
            print i

#s = server.Server("10.0.2.10")
s = server.Server("127.0.0.1")
s.connect()
s.subscribe(SBMsgHandler(),"00:04:20:2A:B0:A5")

'''
print s.get_players()

p = s.get_player("Lounge")
p.set_volume(10)

r = s.request("songinfo 0 100 track_id:94")
print r

r = s.request("trackstat getrating 1019")
print r
'''


