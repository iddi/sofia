import threading
import select
import time
import socket

pyb_present = False

try:
    import pybonjour
    pyb_present = True
except ImportError:
    pyb_present = False

TIMEOUT = 5

discovered_lock = threading.Semaphore()
discovered = []
discovered_event = threading.Event()
discovery_running = False

def discover(type = "Manual", name = None):
    if type == "Manual":
        return discover_Manual_TCP()
    elif type == "mDNS":
        if pyb_present:
            return discover_mDNS(name)
        else:
            print "mDNS discovery not possible"
            return []

def discover_Manual_TCP():
    print "Manual Discovery. Enter details:"
    ssname = raw_input("SmartSpace name       >")
    ip =     raw_input("SmartSpace IP Address >" )
    port =   raw_input("SmartSpace Port       >" )
    print ssname, ip, port
    rtuple = ( ssname, ("TCP", (ip,int(port))  ))
    
    return rtuple


def discover_mDNS(name = None, reg_type = "_kspace._tcp"):
    global discovery_running
    if not discovery_running:
        # print "Starting mDNS discovery"
        d = mDNS_Discovery(reg_type)
        d.start()
        discovery_running = True

    if not name:
        discovered_lock.acquire()
        global discovered
        tmp = []
        print discovered
        for i in discovered:
            tmp.append(i)
        discovered_lock.release()
        print tmp
        return tmp
    else:
        discovered_lock.acquire()
        # print discovered
        tmp = filter(lambda x: x[0] == name, discovered)
        discovered_lock.release()
        print tmp
        return tmp


class mDNS_Discovery(threading.Thread):

    def __init__(self, reg_type):
        global discovery_running
        discovery_running = True
        self.resolved = []
        self.discovered = {}
        self.reg_type = reg_type
        threading.Thread.__init__(self)

    def address_cb(self, sdRef, flags, interfaceIndex, errorCode,
                   fullname, rrtype, rrclass, rdata, ttl):
        if errorCode == pybonjour.kDNSServiceErr_NoError:
            #print "RDATA type for A  is ", type(rdata)
            #print "Converted: ", socket.inet_ntoa(rdata)

            # Extract Smart Space name, discard _serv._tcp crap
            ss_name = self.service_name.split('.')[0]
            discovered_lock.acquire()
            # Use TCP for communication, as zeroconf is IP based tech
            discovered.append((ss_name, ("TCP", (socket.inet_ntoa(rdata), self.port))))
            discovered_lock.release()
            discovered_event.set()

    def resolve_cb(self, sdRef, flags, interfaceIndex, errorCode, fullname,
                   hosttarget, port, txtRecord):
        if errorCode == pybonjour.kDNSServiceErr_NoError:
            #print 'Resolved service:'
            #print '  fullname   =', fullname
            #print '  hosttarget =', hosttarget
            #print '  port       =', port
            self.service_name = fullname
            self.hostname = hosttarget
            self.port = port
            address_sdRef = pybonjour.DNSServiceQueryRecord(fullname = hosttarget,
                                                            rrtype = pybonjour.kDNSServiceType_A,
                                                            callBack = self.address_cb)

            try:
                ready = select.select([address_sdRef], [], [], TIMEOUT)
                if address_sdRef in ready[0]:
                    pybonjour.DNSServiceProcessResult(address_sdRef)
                else:
                    print 'Resolve timed out'
                        
            finally:
                address_sdRef.close()
                
            self.resolved.append(True)
            
    def browse_cb(self, sdRef, flags, interfaceIndex,
                  errorCode, serviceName, regtype, replyDomain):
        if errorCode != pybonjour.kDNSServiceErr_NoError:
            return

        if not (flags & pybonjour.kDNSServiceFlagsAdd):
            # print 'Service removed: ', serviceName, " ", regtype
            discovered_lock.acquire()
            del self.discovered[hash(serviceName+regtype)]
            for item in discovered:
                if item[0] == serviceName:
                    discovered.remove(item)
            discovered_lock.release()
            return

        if hash(serviceName+regtype) not in self.discovered:
            self.discovered[hash(serviceName+regtype)] = True
            
            # print 'Service added; resolving'
            
            resolve_sdRef = pybonjour.DNSServiceResolve(0, interfaceIndex,
                                                        serviceName, regtype,
                                                        replyDomain, self.resolve_cb)
            try:
                while not self.resolved:
                    ready = select.select([resolve_sdRef], [], [], TIMEOUT)
                    if resolve_sdRef not in ready[0]:
                        print 'Resolve timed out'
                        break
                    pybonjour.DNSServiceProcessResult(resolve_sdRef)
                else:
                    self.resolved.pop()
            finally:
                resolve_sdRef.close()
                discovered_event.clear()

    def run(self):
        browse_sdRef = pybonjour.DNSServiceBrowse(regtype = self.reg_type, callBack = self.browse_cb)
        
        try:
            try:
                while True:
                    discovered_event.clear()
                    ready = select.select([browse_sdRef], [], [])
                    if browse_sdRef in ready[0]:
                        pybonjour.DNSServiceProcessResult(browse_sdRef)
                    # time.sleep(0.1)
            except KeyboardInterrupt:
                pass
        finally:
            browse_sdRef.close()


