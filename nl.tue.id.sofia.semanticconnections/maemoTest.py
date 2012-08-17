'''
Created on 15 June 2010

@author: Gerrit Niezen (g.niezen@tue.nl)

Notes:
audio module also provides set_volume(), current_volume(), set_position(us), current_position(), stop()

'''

import gtk
import hildon

# This is a callback function. The data arguments are ignored in this example.
def hello(widget, data):
   print "Hello World!"

def main():
   program = hildon.Program.get_instance()
   window = hildon.Window()
   program.add_window(window)
   window.connect("delete_event", gtk.main_quit, None)
   
   vbox = gtk.VBox()
   
   playButton = hildon.GtkButton(gtk.HILDON_SIZE_AUTO)
   stopButton = hildon.GtkButton(gtk.HILDON_SIZE_AUTO)
   cueButton =  hildon.GtkButton(gtk.HILDON_SIZE_AUTO)
   

         
   playButton.set_label("Play")
   playButton.connect("clicked", hello, None)

   # This packs the button into the window (a GTK+ container).
   vbox.pack_start(playButton) 
   vbox.pack_start(stopButton)
   vbox.pack_start(cueButton)
   window.add(vbox)


   # The final step is to display this newly created widget and all widgets it
   # contains.
   window.show_all()

   # All GTK+ applications must have a gtk_main(). Control ends here and waits
   # for an event to occur (like a key press or mouse event).
   gtk.main()

if __name__ == "__main__":
   main()
