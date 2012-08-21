---

# Install Eclipse for RCP and RAP Developers (Helios) 

http://www.eclipse.org/downloads/packages/eclipse-rcp-and-rap-developers/heliossr2

**It is important to get this version of Eclipse, because we need its support on OSGi and certain plugins for OSGi.**

# Install Git

In eclipse,

* Help->Install New Software
* Work with: -> (Helios.. from dropdown menu)
* type filter text -> git
* Select Eclipse EGit

# Install Android SDK
If Android devices are going to be used, install Android SDK. Follow the instructions at http://developer.android.com/sdk/index.html, download the SDK, install it, including the Platforms and Packages, and the Eclipse Plugin. 

# Install SOFIA ADK and TUeSIB
You may [try](Install-From-Sofia-Git-and-Gerrit-Git) to grab SOFIA ADK from the SOFIA ADK git and TUeSIB from Gerrit Niezen's personal git. It is easier to grab everything from the iddi/sofia git, since the packages maintained on this git are tested to be compatible with each other.

In eclipse,
* File -> Import
* Select Git -> Projects from Git, Next
* Clone ... -> URI: https://github.com/iddi/sofia.git, Next, select "master", Next, Finish
* After it is done, Import Projects from Git again, select "sofia", Next
* Select "Import Existing Projects", Next
* "Select All" the projects, Finish


# Run SIB

These steps might not be necessary if everything mentioned below is checked.
* Eclipse -> Window -> Preferences -> Plug-in Development -> Target Platform, check "Running Platform", Edit,
* Content -> Select ... (make sure everything below is checked)
 * javax.servlet
 * javax.xml
 * org.eclipse.osgi
 * org.eclipse.osgi.services
 * org.eclipse.osgi.util
 * org.eclipse.core.runtime
* Run -> Run configuration -> Check unchecked bundles

eu.sofia.sib.osgi -> Run.. -> OSGi Framework

# Start the OSGI service

just to get some help on the commands: 

    sspace

create sib

    sspace create -sib -name=test

create the gateway

    sspace create -gw -name=testgw -type=TCP/IP -idSib=1

start the services

    sspace start -sib -id=1
    sspace start -gw -id=1

# Install Python (e.g. version 2.7)

Download installer from: http://python.org/

It would be convenient if the python command can be accessed from the command line. Add python path to Windows %PATH%.

For running certain KPs you will need the serial and iso8601 python modules. To install modules into your python installation, it's best to use a package manager, e.g. setuptools from http://pypi.python.org/pypi/setuptools

To install a module for python, just execute the easy_install.exe followed by the module name you would like to install:
 
    easy_install iso8601
    easy_install pyserial
    easy_install datetime

# Install Smart-M3 Python KPI
---------------------------
You may [try] (Download Smart-M3 Python KPI) to download the Smart-M3 Python KPI from SourceForge. However it is recommended to use the KPI already included in the iddi/sofia git.

From the command line:
    
    cd <your local git>\sofia\smart-m3_pythonKP-0.9.6
    python setup.py install

# Run SSLS

SSLS is a utility for viewing, monitoring and manipulating the contents of Smart-M3 (http://sourceforge.net/projects/smart-m3/) store as well as executing smodels rules over the contents of the store.Enables modification of store by external programs.

Use the SSLS from the iddi/sofia git. or you may [try](Get-SSLS-from-SourceForge) to get it from SourceForge.

To edit directly .ssls and .sslslogin in Eclipse, In the package explorer, in the upper right corner of the view, there is a little down arrow. Tool tip will say view menu. Click that, Select Filters. uncheck .* resources. So Package Explorer -> View Menu -> Filters -> uncheck .* resources. Or you may go to the directory <your local git>\ssls-Sept-05-11, edit these two files using any editor you like.

Execute ssls.py in the same directory...

    cd <your local git>\sofia\ssls-Sep-05-11
    python ssls.py

* use "help" to get helphttps://github.com/iddi/sofia/wiki/Installation
* use "ls" to list triples, e.g. use

    `ls *,*,sofia:SmartObject`

to see all the smart objects


-----------------------------------------------------------
# Scenario 'semantic connections'
-----------------------------------------------------------

download the semantic connections KP stuff from: https://github.com/iddi/nl.tue.id.sofia/tree/master/nl.tue.id.sofia.semanticconnections

1) In the file TripleStore.py change line 12
"smartSpace = ('<SIB NAME>', (TCPConnector, ('<IP ADDRESS OF COMPUTER WITH RUNNING SIB>', <PORT>)))"
with your specifics.

2) In the file "lightKP.py" change the line 
"    lightPort = serial.Serial("COM3:",115200)"

with the serial port of the light Bluetooth device (ArduinoBT). On Windows this is probably "COMn:" (DON'T FORGET THE : !!), and on Mac this is something like "/dev/tty/<???>".

3) In the file "connectorKP.py" change the line
"ser = serial.Serial('COM5:',115200)"

with the serial port of the FireFly device. On Windows this is probably "COMn:" (DON'T FORGET THE : !!), and on Mac this is something like "/dev/tty.FireFly-451A-SPP".

4) Logitech Media Server
To install the Logitech Media Server go to: http://wiki.slimdevices.com/index.php/Windows_Installation_Guide