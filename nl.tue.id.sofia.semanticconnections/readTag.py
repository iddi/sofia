#!/usr/bin/env python
#-*- coding:utf-8 -*-

'''
Created on 5 nov 2009

@author: gniezen
'''
import sys
import os
import socket

#Enable the following before running (disabled for sphinx.ext.autodoc)
sys.path.append('./lib/') 
import RFIDIOtconfig




# ========= Main =============


try:
        card= RFIDIOtconfig.card
except:
        os._exit(True)

card.info('rfidReader v0.1')
card.settagtype(card.ALL)




    
while 42:
        if card.select():
            print '    Tag ID: ' + card.uid,
            print
            sys.stdout.flush()
            
               
            card.acs_halt()





