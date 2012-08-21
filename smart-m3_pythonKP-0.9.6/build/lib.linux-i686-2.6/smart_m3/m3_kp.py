from StringIO import StringIO
from xml.sax import saxutils
from xml.sax import make_parser
from xml.sax.handler import ContentHandler
from collections import deque
import threading
import socket
import uuid
import discovery

# SSAP Constants

M3_SUCCESS = 'm3:Success'
M3_SIB_NOTIFICATION_RESET = 'm3:SIB.Notification.Reset'
M3_SIB_NOTIFICATION_CLOSING = 'm3:SIB.Notification.Closing'
M3_SIB_ERROR = 'm3:SIB.Error'
M3_SIB_ERROR_ACCESS_DENIED = 'm3:SIB.Error.AccessDenied'
M3_SIB_FAILURE_OUT_OF_RESOURCES = 'm3:SIB.Failure.OutOfResources'
M3_SIB_FAILURE_NOT_IMPLEMENTED = 'm3:SIB.Failure.NotImplemented'

M3_KP_ERROR = 'm3:KP.Error'
M3_KP_ERROR_REQUEST = 'm3:KP.Error.Request'
M3_KP_ERROR_MESSAGE_INCOMPLETE = 'm3:KP.Error.Message.Incomplete'
M3_KP_ERROR_MESSAGE_SYNTAX = 'm3:KP.Error.Message.Syntax'

SIB_ANY_URI = 'http://www.nokia.com/NRC/M3/sib#any'

CDATA_START = '<![CDATA['
CDATA_END = ']]>'

END_TAG = '</SSAP_message>'
ETL = 15 # Length of end tag string
TCP = socket.getprotobyname('TCP')
CURRENT_TR_ID = 1
KP_ID_POSTFIX = str(uuid.uuid4())

XML_CODING = '<?xml version="1.0" encoding="UTF-8"?>'

SSAP_MESSAGE_TEMPLATE = '''
<SSAP_message>
<node_id>%s</node_id>
<space_id>%s</space_id>
<transaction_type>%s</transaction_type>
<message_type>REQUEST</message_type>
<transaction_id>%s</transaction_id>
%s
</SSAP_message>'''

SSAP_JOIN_PARAM_TEMPLATE = '<parameter name = "credentials">%s</parameter>'

SSAP_INSERT_PARAM_TEMPLATE = '''
<parameter name = "insert_graph" encoding = "%s">%s</parameter>
<parameter name = "confirm">%s</parameter>'''

SSAP_REMOVE_PARAM_TEMPLATE = '''
<parameter name = "remove_graph" encoding = "%s">%s</parameter>
<parameter name = "confirm">%s</parameter>'''

SSAP_UPDATE_PARAM_TEMPLATE = '''
<parameter name = "insert_graph" encoding = "%s">%s</parameter>
<parameter name = "remove_graph" encoding = "%s">%s</parameter>
<parameter name = "confirm">%s</parameter>'''

SSAP_QUERY_TEMPLATE = '''
<parameter name = "type">%s</parameter>
<parameter name = "query">%s</parameter>'''

SSAP_UNSUBSCRIBE_TEMPLATE = '<parameter name = "subscription_id">%s</parameter>'

SSAP_WQL_VALUES_TEMPLATE = '''
<wql_query>
<node name = "start" type = "%s">%s</node>
<path_expression>%s</path_expression>
</wql_query>'''

SSAP_WQL_RELATED_TEMPLATE = '''
<wql_query>
<node name = "start" type = "%s">%s</node>
<node name = "end" type = "%s">%s</node>
<path_expression>%s</path_expression>
</wql_query>'''

SSAP_WQL_ISTYPE_TEMPLATE = '''
<wql_query>
<node>%s</node>
<node name = "type">%s</node>
</wql_query>'''

SSAP_WQL_ISSUBTYPE_TEMPLATE = '''
<wql_query>
<node name = "subtype">%s</node>
<node name = "supertype">%s</node>
</wql_query>'''

SSAP_WQL_NODETYPES_TEMPLATE = '''
<wql_query>
<node>%s</node>
</wql_query>'''

M3_TRIPLE_TEMPLATE = '''
<triple>
<subject type = "%s">%s</subject>
<predicate>%s</predicate>
<object type = "%s">%s</object>
</triple>'''

# Utilities

def get_tr_id():
    global CURRENT_TR_ID
    rv = CURRENT_TR_ID
    CURRENT_TR_ID = CURRENT_TR_ID + 1
    return rv
    
def parse_M3RDF(results):
    results_list = []
    parser = make_parser()
    mh = NodeM3RDFHandler(results_list, {})
    parser.setContentHandler(mh)
    parser.parse(StringIO(results.encode('utf-8')))
    return results_list

def parse_URI_list(results):
    results_list = []
    parser = make_parser()
    mh = UriListHandler(results_list)
    parser.setContentHandler(mh)
    parser.parse(StringIO(results.encode('utf-8')))
    return results_list

# Exceptions

class M3Exception(Exception):
    pass

class KPError(M3Exception):
    def __init__(self, args):
        self.args = args

class SIBError(M3Exception):
    def __init__(self, args):
        self.args = args

class M3Notification(M3Exception):
    def __init__(self, args):
        self.args = args

# Classes for URI, Literal, bNode and Triple

class Node:
    pass

class URI(Node):
    def __init__(self, value):
        self.value = value
        self.nodetype = "URI"

    def __str__(self):
        return str(self.value)

    def __repr__(self):
        return "<%s>"%self.value

    def __eq__(self, other):
        if not isinstance(other, URI):
            return False
        else:
            return self.value == other.value

    def __ne__(self, other):
        if not isinstance(other, URI):
            return True
        else:
            return self.value != other.value

    def __hash__(self):
        return hash(self.value)

class bNode(Node):
    def __init__(self, value):
        self.value = value
        self.nodetype = "bnode"

    def __str__(self):
        return str(self.value)

    def __repr__(self):
        return "_:%s"%self.value

    def __eq__(self, other):
        if not isinstance(other, bNode):
            return False
        else:
            return self.value == other.value

    def __ne__(self, other):
        if not isinstance(other, bNode):
            return True
        else:
            return self.value != other.value

    def __hash__(self):
        return hash(self.value)

class Literal(Node):
    def __init__(self, value, lang = None, dt = None):
        if lang and dt:
            raise KPError("lang and dt in Literal are mutually exclusive")
        self.value = value
        self.lang = lang
        self.dt = dt
        self.nodetype = "literal"

    def __str__(self):
        return str(self.value)

    def __repr__(self):
        return '"%s"'%self.value

    def __eq__(self, other):
        if not isinstance(other, Literal):
            return False
        else:
            return self.value == other.value and self.lang == other.lang and self.dt == other.dt

    def __ne__(self, other):
        if not isinstance(other, Literal):
            return True
        else:
            return self.value != other.value or self.lang != other.lang or self.dt != other.dt

    def __hash__(self):
        if lang and not dt:
            return hash(self.value+str(self.lang))
        elif dt and not lang:
            return hash(self.value+str(self.dt))
        else:
            return hash(self.value)


class Triple(tuple):
    def __new__(cls, *args):
        if len(args) == 3:
            s, p, o = tuple(args)
        elif len(args) == 1:
            s, p, o = tuple(args[0])
        else:
            raise KPError("Not a triple")
        for i in (s, p, o):
            if not (isinstance(i, Node) or i == None):
                raise KPError("s, p, o should be instances of Node")
        return tuple.__new__(cls, (s, p, o))

    def __eq__(self, other):
        return self[0]==other[0] and self[1]==other[1] and self[2]==other[2]

    def __ne__(self, other):
        return self[0]!=other[0] or self[1]!=other[1] or self[2]!=other[2]

    def encode_to_m3(self):
        s, p, o = self
        for i in self:
            if i == None:
                i = SIB_ANY_URI
        return M3_TRIPLE_TEMPLATE%(s.nodetype, str(s),
                                   str(p),
                                   o.nodetype, str(o))


# KP definitions            

class KP:
    def __init__(self, node_id):
        global KP_ID_POSTFIX
        self.member_of = []
        self.connections = []
        self.node_id = node_id + "-" + KP_ID_POSTFIX
        self.user_node_id = node_id

    def discover(self, method = "Manual", browse = True, name = None):
        '''Discover available smart spaces. Currently available
           discovery methods are:
             - Manual: give IP address and port
             - Bonjour discovery (if pyBonjour package has been installed)
           Returns a handle for smart space if method is Manual or
           browse is True, otherwise returns a list of handles
           Handle is a tuple
           (SSName, (connector class, (connector constructor args)))
           '''
        disc = discovery.discover(method, name)
        def _insert_connector(item):
            n, h = item
            c, a = h
            c = TCPConnector
            rv = (n, (c, a))
            return rv
            
        if browse and not method == "Manual":
            while True:
                i = 1
                print "Please select Smart Space from list or (R)efresh:"
                for item in disc:
                    print i, " : ", item[0]
                    i += 1
                    
                inp = raw_input("Pick number or (R)efresh: ")
                if inp == "R" or inp == "r":
                    disc = discovery.discover(method, name)
                    continue
                else:
                    try:
                        disc = disc[int(inp)-1]
                    except KeyboardInterrupt:
                        break
                    except:
                        print "Incorrect command!" 
                        print "Choose a number in a list or (R)efresh"
                        continue
                    else:
                        #print "CONNECTION METHOD", disc[1][0]
                        if disc[1][0] == "TCP":
                            return _insert_connector(disc)
                        else:
                            #print "Unknown connection type"
                            return None
        elif method == "Manual":
            if disc[1][0] == "TCP":
                return _insert_connector(disc)
            else:
                print "Unknown connection type:", item[1][0]
                return None
        else:
            ss_list = []
            for item in disc:
                if item[1][0] == "TCP":
                    ss_list.append(_insert_connector(item))
                else:
                    print "Unknown connection type:", item[1][0]
            return ss_list
        
    def join(self, dest):
        '''Join a smart space. Argument dest is a smart space handle of form
        (SSName, (connector class, (connector constructor args))).
        join() accepts handles returned by discover() method.
        Returns True if succesful, False otherwise.
        '''
        targetSS, handle = dest
        connector, args = handle
        conn = connector(args)
        tr_id = get_tr_id()
        tmp = [SSAP_MESSAGE_TEMPLATE%(str(self.node_id), str(targetSS),
                                      "JOIN", str(tr_id),
                                      SSAP_JOIN_PARAM_TEMPLATE%("XYZZY"))]
        join_msg = "".join(tmp)

        conn.connect()
        conn.send(join_msg)
        cnf = conn.receive()
        conn.close()
        if "status" in cnf and cnf["status"] == M3_SUCCESS:
            self.member_of.append(targetSS)
            return True
        elif "status" in cnf:
            #print "Could not join SS", targetSS
            raise SIBError(cnf["status"])
        else:
            raise SIBError(M3_SIB_ERROR)
    
    def leave(self, dest):
        '''Leave a smart space. Argument dest is a smart space handle of form
        (SSName, (connector class, (connector constructor args))).
        leave() accepts handles returned by discover() method.
        Returns True if succesful, False otherwise.
        '''
        targetSS, handle = dest
        connector, args = handle
        conn = connector(args)
        tr_id = get_tr_id()

        leave_msg = SSAP_MESSAGE_TEMPLATE%(str(self.node_id), str(targetSS),
                                           "LEAVE", str(tr_id), "")
        conn.connect()
        conn.send(leave_msg)
        #print "Sent leave msg"
        cnf = conn.receive()
        conn.close()
        if "status" in cnf and cnf["status"] == M3_SUCCESS:
            tmp = filter(lambda x: x != targetSS, self.member_of)
            self.member_of = tmp
            return True
        elif "status" in cnf:
            tmp = filter(lambda x: x != targetSS, self.member_of)
            self.member_of = tmp
            raise SIBError(cnf["status"])
        else:
            tmp = filter(lambda x: x != targetSS, self.member_of)
            self.member_of = tmp
            raise SIBError(M3_SIB_ERROR)
            


    def CreateInsertTransaction(self, dest):
        '''Creates an insert transaction for inserting information
        to a smart space.
        Returns an instance of Insert(Transaction) class
        '''
        c = Insert(dest, self.node_id)
        self.connections.append(("PROACTIVE", c))
        return c

    def CloseInsertTransaction(self, trans):
        '''Closes an insert transaction
        '''
        trans.close()
        self.connections = filter(lambda x: x[1] != trans, self.connections)

    def CreateRemoveTransaction(self, dest):
        '''Creates a remove transaction for removing information
        from a smart space.
        Returns an instance of Remove(Transaction) class
        '''
        c = Remove(dest, self.node_id)
        self.connections.append(("RETRACTION", c))
        return c

    def CloseRemoveTransaction(self, trans):
        '''Closes a remove transaction
        '''
        trans.close()
        self.connections = filter(lambda x: x[1] != trans, self.connections)

    def CreateUpdateTransaction(self, dest):
        c = Update(dest, self.node_id)
        self.connections.append(("UPDATE", c))
        return c

    def CloseUpdateTransaction(self, trans):
        '''Closes an update transaction
        '''
        trans.close()
        self.connections = filter(lambda x: x[1] != trans, self.connections)

    def CreateSubscribeTransaction(self, dest, once = False):
        '''Creates a subscribe transaction for subscribing to
        information in a smart space.
        Returns an instance of Subscribe(Transaction) class
        '''
        c = Subscribe(dest, self.node_id, once)
        self.connections.append(("REACTIVE", c))
        return c

    def CloseSubscribeTransaction(self, trans):
        '''Closes a subscribe transaction
        '''
        trans.close()
        self.connections = filter(lambda x: x[1] != trans, self.connections)

    def CreateQueryTransaction(self, dest):
        '''Creates a query transaction for querying information
        in a smart space.
        Returns an instance of Query(Transaction) class
        '''
        c = Query(dest, self.node_id)
        self.connections.append(("QUERY", c))
        return c

    def CloseQueryTransaction(self, trans):
        '''Closes a query transaction
        '''
        trans.close()
        self.connections = filter(lambda x: x[1] != trans, self.connections)

class Transaction:
    '''Abstract base class for M3 operations.
    '''
    def __init__(self, dest, node_id):
        self.node_id = node_id
        self.tr_id = ""
        self.targetSS, handle = dest
        connector, args = handle
        self.conn = connector(args)

    def _check_error(self, msg):
        if "status" in msg and msg["status"] == M3_SUCCESS:
            return True
        elif "status" in msg:
            raise SIBError((msg["status"],))
        else:
            raise SIBError((M3_SIB_ERROR,))
 

    def _encode(self, triples, wildcard = True):
        tmp = ['<triple_list>']
        for t in triples:
            s, p, o = t
            if wildcard:
                if s == None:
                    s = URI(SIB_ANY_URI)
                if p == None:
                    p = URI(SIB_ANY_URI)
                if o == None:
                    o = URI(SIB_ANY_URI)
            
            if o.nodetype == "literal":
                tmp.append(M3_TRIPLE_TEMPLATE%(s.nodetype, str(s), str(p), 
                                               o.nodetype, 
                                               ''.join([CDATA_START,
                                                        str(o),
                                                        CDATA_END])))
            else:
                tmp.append(M3_TRIPLE_TEMPLATE%(s.nodetype, str(s), str(p), 
                                               o.nodetype, str(o)))
        tmp.append('</triple_list>')
        return "".join(tmp)

class Insert(Transaction):
    '''Class handling Insert transactions.
    '''
    def __init__(self, dest, node_id):
        Transaction.__init__(self, dest, node_id)
        self.tr_type = "INSERT"
                
    def _create_msg(self, tr_id, payload, confirm, encoding):
        '''Internal. Create a SSAP XML message for INSERT REQUEST
        '''
        params = SSAP_INSERT_PARAM_TEMPLATE%(encoding.upper(),
                                             payload,
                                             str(confirm).upper())
        tmp = SSAP_MESSAGE_TEMPLATE%(str(self.node_id), str(self.targetSS),
                                     self.tr_type, str(tr_id),
                                     params)
        return tmp

    def send(self, payload, pl_type = "python",
             encoding = "rdf-m3", confirm = True):
        '''Accepted format for payload is determined by the parameter pl_type.
           - pl_type == 'python': payload is a list of Triple instances

           - pl_type != 'python': payload is given in encoded form,
             either rdf-xml or rdf-m3. The encoding parameter has to be
             set accordingly. 

             If confirmation is requested, the return value will be a 
             dictionary associating named blank nodes in the insert graph 
             to the actual URIs stored in the SIB (works for rdf-m3 
             encoding only)
        '''
        self.tr_id = get_tr_id()
        if pl_type == "python":
            if not isinstance(payload, list):
                payload = [payload]
            payload_enc = self._encode(payload, wildcard = False)
            xml_msg = self._create_msg(self.tr_id,
                                       payload_enc,
                                       confirm,
                                       encoding)
        else:
            xml_msg = self._create_msg(self.tr_id,
                                       payload,
                                       confirm,
                                       encoding)

        self.conn.connect()
        self.conn.send(xml_msg)
        if confirm:
            rcvd = self.conn.receive()
            self._check_error(rcvd)
            if encoding.lower() == 'rdf-m3':
                bnodes = {}
                icf = BNodeUriListHandler(bnodes)
                parser = make_parser()
                parser.setContentHandler(icf)
                parser.parse(StringIO(rcvd['bnodes']))
                return (rcvd["status"], bnodes)
            else:
                return (rcvd["status"], {})

    def close(self):
        self.conn.close()


class Remove(Transaction):
    '''Class for handling Remove transactions.
    '''
    def __init__(self, dest, node_id):
        Transaction.__init__(self, dest, node_id)
        self.tr_type = "REMOVE"

    def _create_msg(self, tr_id, triples, type, confirm):
        '''Internal. Create a SSAP XML message for REMOVE REQUEST
        '''
        params = SSAP_REMOVE_PARAM_TEMPLATE%(str(type).upper(),
                                             str(triples),
                                             str(confirm).upper())
        tmp = SSAP_MESSAGE_TEMPLATE%(str(self.node_id), str(self.targetSS),
                                     self.tr_type, str(tr_id), params)
        return tmp

    def remove(self, triples, type = 'rdf-m3', confirm = True):
        '''Remove triples listed in parameter triples. Triples may
           contain wildcards (None as subject, predicate, or object).
           The removed triples will then be the result of such pattern
           query.
        '''
        tr_id = get_tr_id()

        if type.lower() == 'rdf-m3':
            if not isinstance(triples, list):
                triples = [triples]
            triples_enc = self._encode(triples)
        else:
            print "Only rdf-m3 encoding supported at moment!"
            raise SIBError(M3_SIB_FAILURE_NOT_IMPLEMENTED)

        xml_msg = self._create_msg(tr_id, triples_enc, type, confirm)
        self.conn.connect()
        self.conn.send(xml_msg)
        if confirm:
            rcvd = self.conn.receive()
            self._check_error(rcvd)
            return rcvd["status"]
        else:
            return M3_SUCCESS

    def close(self):
        self.conn.close()

class Update(Transaction):
    def __init__(self, dest, node_id):
        Transaction.__init__(self, dest, node_id)
        self.tr_type = "UPDATE"

    def _create_msg(self, tr_id, i_triples, i_type, r_triples, r_type, confirm):
        '''Internal. Create a SSAP message for UPDATE REQUEST
        '''
        params = SSAP_UPDATE_PARAM_TEMPLATE%(str(i_type).upper(),
                                             str(i_triples),
                                             str(r_type).upper(),
                                             str(r_triples),
                                             str(confirm).upper())
        tmp = SSAP_MESSAGE_TEMPLATE%(str(self.node_id), str(self.targetSS),
                                     self.tr_type, str(tr_id), params)
        return tmp

    def update(self, i_triples, i_enc, 
               r_triples, r_enc, confirm = True):
        '''Perform an atomic update operation. First, removes the triples 
           given in parameter r_triples and then inserts the triples given
           in parameter i_triples. Parameters i_enc and r_enc have to be 
           currently 'RDF-M3'. The triples in i_triples should be formatted
           as in insert and the triples in r_triples as in remove.
        '''
        tr_id = get_tr_id()
        i_triples = self._encode(i_triples, wildcard = False)
        r_triples = self._encode(r_triples)
        xml_msg = self._create_msg(tr_id, i_triples, i_enc, 
                                   r_triples, r_enc, confirm)
        self.conn.connect()
        self.conn.send(xml_msg)
        if confirm:
            # print "UPDATE: waiting for receive"
            rcvd = self.conn.receive()
            # print "UPDATE: confirmation received:"
            # print rcvd
            self._check_error(rcvd)
            if i_enc.lower() == 'rdf-m3':
                bnodes = {}
                icf = BNodeUriListHandler(bnodes)
                parser = make_parser()
                parser.setContentHandler(icf)
                parser.parse(StringIO(rcvd['bnodes']))
                return (rcvd["status"], bnodes)
            else:
                return (rcvd["status"], {})

    def close(self):
        self.conn.close()


class Query(Transaction):
    def __init__(self, dest, node_id):
        Transaction.__init__(self, dest, node_id)
        self.tr_type = "QUERY"

    def _create_wql_values_msg(self, tr_id, s_node, path):
        query = SSAP_WQL_VALUES_TEMPLATE%(s_node.nodetype, str(s_node), 
                                          str(path))
        params = SSAP_QUERY_TEMPLATE%("WQL-VALUES", query)
        msg = SSAP_MESSAGE_TEMPLATE%(str(self.node_id), str(self.targetSS),
                                     self.tr_type, str(tr_id), params)
        return msg

    def _create_wql_related_msg(self, tr_id, s_node, e_node, path):
        query = SSAP_WQL_RELATED_TEMPLATE%(s_node.nodetype, str(s_node), 
                                           e_node.nodetype, str(e_node),
                                           str(path))
        params = SSAP_QUERY_TEMPLATE%("WQL-RELATED", query)
        msg = SSAP_MESSAGE_TEMPLATE%(str(self.node_id), str(self.targetSS),
                                     self.tr_type, str(tr_id), params)
        return msg

    def _create_wql_nodetypes_msg(self, tr_id, node):
        query = SSAP_WQL_NODETYPES_TEMPLATE%(str(node))
        params = SSAP_QUERY_TEMPLATE%("WQL-NODETYPES", query)
        msg = SSAP_MESSAGE_TEMPLATE%(str(self.node_id), str(self.targetSS),
                                     self.tr_type, str(tr_id), params)
        return msg

    def _create_wql_istype_msg(self, tr_id, node, type):
        query = SSAP_WQL_ISTYPE_TEMPLATE%(str(node), str(type))
        params = SSAP_QUERY_TEMPLATE%("WQL-ISTYPE", query)
        msg = SSAP_MESSAGE_TEMPLATE%(str(self.node_id), str(self.targetSS),
                                     self.tr_type, str(tr_id), params)
        return msg

    def _create_wql_issubtype_msg(self, tr_id, subtype, supertype):
        query = SSAP_WQL_ISSUBTYPE_TEMPLATE%(str(subtype), str(supertype))
        params = SSAP_QUERY_TEMPLATE%("WQL-ISSUBTYPE", query)
        msg = SSAP_MESSAGE_TEMPLATE%(str(self.node_id), str(self.targetSS),
                                     self.tr_type, str(tr_id), params)
        return msg

    def _create_rdf_msg(self, tr_id, triples):
        params = SSAP_QUERY_TEMPLATE%("RDF-M3", self._encode(triples))
        msg = SSAP_MESSAGE_TEMPLATE%(str(self.node_id), str(self.targetSS),
                                     self.tr_type, str(tr_id), params)
        return msg

    def wql_values_query(self, start_node, path):
        '''Query information stored in a smart
           space with a wql values query. Returns the list of 
           RDF graph nodes reachable from the start_node via path.

           Parameters: 
               start_node: URI or Literal; the starting node of 
                           wql values query
               path: string; the wql path expression
           Returns: list of URI or Literal instances
        '''
        self.tr_id = get_tr_id()
        xml_msg = self._create_wql_values_msg(self.tr_id, start_node, path)
        self.conn.connect()
        self.conn.send(xml_msg)
        response = self.conn.receive()
        self._check_error(response)
        if "results" in response:
            node_list = parse_URI_list(response["results"])
            return node_list
        else:
            raise SIBError(M3_SIB_ERROR)

    def wql_related_query(self, start_node, end_node, path):
        '''Query information stored in a smart space using a wql
           related query. Returns True if the end_node can be 
           reached from the start_node via path

           Parameters: 
               start_node: URI or Literal; the starting node of 
                           wql related query
               end_node: URI or Literal; the end node of 
                         wql related query
               path: string; the wql path expression
           Returns: boolean
        '''
        self.tr_id = get_tr_id()
        xml_msg = self._create_wql_related_msg(self.tr_id, 
                                               start_node, end_node, path)
        self.conn.connect()
        self.conn.send(xml_msg)
        response = self.conn.receive()
        self._check_error(response)
        if "results" in response:
            if response["results"] == "TRUE":
                return True
            else:
                return False
        else:
            raise SIBError(M3_SIB_ERROR)

    def wql_nodetypes_query(self, node):
        '''Query information stored in a smart space using a wql
           nodetypes query. Returns a list of URIs that are the RDF types
           of node. Note that node must be a URI instance.

           Parameters: 
               node: URI; the node whose RDF types will be queried
           Returns: list of URI instances

        '''
        self.tr_id = get_tr_id()
        if isinstance(node, Literal):
            raise KPError(M3_KP_ERROR_REQUEST)
        xml_msg = self._create_wql_nodetypes_msg(self.tr_id, node)
        self.conn.connect()
        self.conn.send(xml_msg)
        response = self.conn.receive()
        self._check_error(response)
        if "results" in response:
            node_list = parse_URI_results(response["results"])
            return node_list
        else:
            raise SIBError(M3_SIB_ERROR)

    def wql_istype_query(self, node, nodetype):
        '''Query information stored in a smart space using a wql
           istype query. The method returns True if the instance 
           node is of the type specified by the type node.

           Parameters: 
               node: URI; the instance node whose RDF types will be queried
               nodetype: URI; the type node
           Returns: boolean
        '''
        self.tr_id = get_tr_id()
        if isinstance(node, Literal) or isinstance(type, Literal):
            return None # No literals allowed here
        xml_msg = self._create_wql_istype_msg(self.tr_id, 
                                              node, type)
        self.conn.connect()
        self.conn.send(xml_msg)
        response = self.conn.receive()
        self._check_error(response)
        if "results" in response:
            if response["results"] == "TRUE":
                return True
            else:
                return False
        else:
            raise SIBError(M3_SIB_ERROR)

    def wql_issubtype_query(self, subtype, supertype):
        '''Query information stored in a smart space using a wql
           issubtype query. The method returns True if the subtype 
           node is a subtype of the supertype node.

           Parameters: 
               subtype: URI; the subtype
               supertype: URI; the supertype node
           Returns: boolean
        '''

        self.tr_id = get_tr_id()
        if isinstance(subtype, Literal) or isinstance(supertype, Literal):
            return None # No literals allowed here
        xml_msg = self._create_wql_issubtype_msg(self.tr_id, 
                                                 subtype, supertype)
        self.conn.connect()
        self.conn.send(xml_msg)
        response = self.conn.receive()
        self._check_error(response)
        if "results" in response:
            if response["results"] == "TRUE":
                return True
            else:
                return False
        else:
            raise SIBError(M3_SIB_ERROR)

    def rdf_query(self, triples):
        '''Query information stored in a smart space using a RDF pattern 
           query. The parameter triple should be a list 
           Parameters: 
               triples: list of Triples; where any of subject, predicate, 
                        or object may be None, signifying a 
                        wildcard. If there are multiple triple patterns in
                        a query, the results are a union of all results
                        
           Returns: list of Triples
        '''
        if type(triples) != list:
            triples = [triples]
        self.tr_id = get_tr_id()
        xml_msg = self._create_rdf_msg(self.tr_id, triples)
        self.conn.connect()
        self.conn.send(xml_msg)
        response = self.conn.receive()
        self._check_error(response)
        if "results" in response:
            triple_list = parse_M3RDF(response["results"])
            return triple_list
        else:
            raise SIBError(M3_SIB_ERROR)

    def close(self):
        self.conn.close()

class Subscribe(Transaction):
    def __init__(self, dest, node_id, once = False):
        self.node_id = node_id
        self.tr_id = 0
        self.targetSS, handle = dest
        connector, args = handle
        self.conn = connector(args)
        self.unsub_conn = connector(args)
        self.once = once
        self.tr_type = "SUBSCRIBE"

    def _create_wql_values_msg(self, tr_id, s_node, path):
        query = SSAP_WQL_VALUES_TEMPLATE%(s_node.nodetype, str(s_node), 
                                          str(path))
        params = SSAP_QUERY_TEMPLATE%("WQL-VALUES", query)
        msg = SSAP_MESSAGE_TEMPLATE%(str(self.node_id), str(self.targetSS),
                                     self.tr_type, str(tr_id), params)
        return msg

    def _create_wql_related_msg(self, tr_id, s_node, e_node, path):
        query = SSAP_WQL_RELATED_TEMPLATE%(s_node.nodetype, str(s_node), 
                                           e_node.nodetype, str(e_node),
                                           str(path))
        params = SSAP_QUERY_TEMPLATE%("WQL-RELATED", query)
        msg = SSAP_MESSAGE_TEMPLATE%(str(self.node_id), str(self.targetSS),
                                     self.tr_type, str(tr_id), params)
        return msg

    def _create_wql_nodetypes_msg(self, tr_id, node):
        query = SSAP_WQL_NODETYPES_TEMPLATE%(str(node))
        params = SSAP_QUERY_TEMPLATE%("WQL-NODETYPES", query)
        msg = SSAP_MESSAGE_TEMPLATE%(str(self.node_id), str(self.targetSS),
                                     self.tr_type, str(tr_id), params)
        return msg

    def _create_wql_istype_msg(self, tr_id, node, type):
        query = SSAP_WQL_ISTYPE_TEMPLATE%(str(node), str(type))
        params = SSAP_QUERY_TEMPLATE%("WQL-ISTYPE", query)
        msg = SSAP_MESSAGE_TEMPLATE%(str(self.node_id), str(self.targetSS),
                                     self.tr_type, str(tr_id), params)
        return msg

    def _create_wql_issubtype_msg(self, tr_id, subtype, supertype):
        query = SSAP_WQL_ISSUBTYPE_TEMPLATE%(str(subtype), str(supertype))
        params = SSAP_QUERY_TEMPLATE%("WQL-ISSUBTYPE", query)
        msg = SSAP_MESSAGE_TEMPLATE%(str(self.node_id), str(self.targetSS),
                                     self.tr_type, str(tr_id), params)
        return msg

    def _create_rdf_msg(self, tr_id, triples):
        params = SSAP_QUERY_TEMPLATE%("RDF-M3", self._encode(triples))
        msg = SSAP_MESSAGE_TEMPLATE%(str(self.node_id), str(self.targetSS),
                                     self.tr_type, str(tr_id), params)
        return msg

    def subscribe_rdf(self, triple, msg_handler):
        '''Subscribe to information stored in a smart space using 
           a RDF pattern query. The parameter triple should be a list 
           Parameters: 
               triples: list of Triples; where any of subject, predicate, 
                        or object may be None, signifying a 
                        wildcard. If there are multiple triple patterns in
                        a query, the results are a union of all results
               msg_handler: callback for changed results. Should be a class
                            with method handle(added, removed). The method
                            will be called when query results change with 
                            added and removed being a list of Triples added
                            and removed from previous query results.
           Returns: list of Triples; this is the query result when 
                    the subscription was set up.
           
           When the query results change in the smart space the subscription
           calls msg_handler.handle(added, removed) where added is a list
           of triples added to the query results since last callback and
           removed is a list of triples removed from the query results after
           last callback
        '''
        if type(triple) != list:
            triple = [triple]
        self.msg_handler = msg_handler
        self.tr_id = get_tr_id()
        xml_msg = self._create_rdf_msg(self.tr_id, triple)
        #print "SUBSCRIBE sending:", xml_msg
        self.conn.connect()
        self.conn.send(xml_msg)
        cnf = self.conn.receive()
        self._check_error(cnf)

        self.sub_id = cnf["subscription_id"]
        initial_result = parse_M3RDF(cnf["results"])
        sub_h = RDFSubscribeHandler(self.node_id,
                                    self.tr_id,
                                    self.conn,
                                    msg_handler)
        sub_h.start()
        return initial_result


    def subscribe_wql_values(self, start_node, path, 
                             msg_handler):
        '''Subscribe to information stored in a smart
           space with a wql values query. Returns the list of 
           RDF graph nodes reachable from the start_node via path.

           Parameters: 
               start_node: URI or Literal; the starting node of 
                           wql values query
               path: string; the wql path expression
               msg_handler: callback for changed results. Should be a class
                            with method handle(added, removed). The method
                            will be called when query results change with 
                            added and removed being a list of URIs or
                            Literals added and removed from previous 
                            query results.
           Returns: list of URI or Literal instances; this is the query 
                    result when the subscription was set up.
        '''
        self.msg_handler = msg_handler
        self.tr_id = get_tr_id()
        xml_msg = self._create_wql_values_msg(self.tr_id, start_node, path)
        self.conn.connect()
        self.conn.send(xml_msg)
        cnf = self.conn.receive()
        self._check_error(cnf)

        self.sub_id = cnf["subscription_id"]
        initial_result = parse_URI_list(cnf["results"])
        sub_h = WQLNodeSubscribeHandler(self.node_id, self.tr_id,
                                        self.conn, msg_handler)
        sub_h.start()
        return initial_result

    def subscribe_wql_related(self, start_node, end_node, path, 
                             msg_handler):
        '''Subscribe to information stored in a smart space using a wql
           related query. 

           Parameters: 
               start_node: URI or Literal; the starting node of 
                           wql related query
               end_node: URI or Literal; the end node of 
                         wql related query
               path: string; the wql path expression
               msg_handler: callback for changed results. Should be a class
                            with method handle(added, removed). The method
                            will be called when query results change with 
                            added and removed being a boolean value.
                            Please note that as the wql related query 
                            returns a boolean value, the added and 
                            removed parameters will always contain opposite 
                            values, i.e. if added is "True" then removed
                            will always be "False" and vice versa. Also,
                            the current result will be in added parameter 
                            while the previous result is in the removed 
                            parameter.
           Returns: boolean; this is the query result when 
                    the subscription was set up.
        '''
        self.msg_handler = msg_handler
        self.tr_id = get_tr_id()
        xml_msg = self._create_wql_related_msg(self.tr_id, start_node,
                                               end_node, path)
        self.conn.connect()
        self.conn.send(xml_msg)
        cnf = self.conn.receive()
        self._check_error(cnf)

        self.sub_id = cnf["subscription_id"]
        #self.msg_handler.handle(initial_result)            
        sub_h = WQLBooleanSubscribeHandler(self.node_id, self.tr_id,
                                           self.conn, msg_handler)
        sub_h.start()
        if cnf["results"] == "TRUE":
            return True
        else:
            return False

    def subscribe_wql_nodetypes(self, node, msg_handler):
        '''Subscribe to information stored in a smart space using a wql
           nodetypes query. Note that node must be a URI instance.

           Parameters: 
               node: URI; the node whose RDF types will be queried
               msg_handler: callback for changed results. Should be a class
                            with method handle(added, removed). The method
                            will be called when query results change with 
                            added and removed being a list of URIs or
                            Literals added and removed from previous 
                            query results.
           Returns: list of URI instances; this is the query result when 
                    the subscription was set up.
        '''
        if isinstance(node, Literal):
            raise KPError(M3_KP_ERROR)
        self.msg_handler = msg_handler
        self.tr_id = get_tr_id()
        xml_msg = self._create_wql_nodetypes_msg(self.tr_id, node)
        self.conn.connect()
        self.conn.send(xml_msg)
        cnf = self.conn.receive()
        self._check_error(cnf)
        
        self.sub_id = cnf["subscription_id"]
        initial_result = parse_URI_list(cnf["results"])
        sub_h = WQLNodeSubscribeHandler(self.node_id, self.tr_id,
                                        self.conn, msg_handler)
        sub_h.start()
        return initial_result

    def subscribe_wql_istype(self, node, type, 
                             msg_handler):
        '''Subscribe to information stored in a smart space using a wql
           istype query. 

           Parameters: 
               node: URI; the instance node whose RDF types will be queried
               nodetype: URI; the type node
               msg_handler: callback for changed results. Should be a class
                            with method handle(added, removed). The method
                            will be called when query results change with 
                            added and removed being a boolean value.
                            Please note that as the wql related query 
                            returns a boolean value, the added and 
                            removed parameters will always contain opposite 
                            values, i.e. if added is "True" then removed
                            will always be "False" and vice versa. Also,
                            the current result will be in added parameter 
                            while the previous result is in the removed 
                            parameter.
           Returns: boolean; this is the query result when 
                    the subscription was set up.
        '''
        if isinstance(node, Literal) or isinstance(type, Literal):
            return None # No literals allowed here
        self.msg_handler = msg_handler
        self.tr_id = get_tr_id()
        xml_msg = self._create_wql_istype_msg(self.tr_id, node,
                                               type)
        self.conn.connect()
        self.conn.send(xml_msg)
        cnf = self.conn.receive()
        self._check_error(cnf)
        
        self.sub_id = cnf["subscription_id"]
        #self.msg_handler.handle(initial_result)            
        sub_h = WQLBooleanSubscribeHandler(self.node_id, self.tr_id,
                                           self.conn, msg_handler)
        sub_h.start()
        if cnf["results"] == "TRUE":
            return True
        else:
            return False

    def subscribe_wql_issubtype(self, subtype, supertype, 
                             msg_handler):
        '''Subscribe to information stored in a smart space using a wql
           issubtype query. 

           Parameters: 
               subtype: URI; the subtype
               supertype: URI; the supertype node
               msg_handler: callback for changed results. Should be a class
                            with method handle(added, removed). The method
                            will be called when query results change with 
                            added and removed being a boolean value.
                            Please note that as the wql related query 
                            returns a boolean value, the added and 
                            removed parameters will always contain opposite 
                            values, i.e. if added is "True" then removed
                            will always be "False" and vice versa. Also,
                            the current result will be in added parameter 
                            while the previous result is in the removed 
                            parameter.
           Returns: boolean; this is the query result when 
                    the subscription was set up.
        '''
        if isinstance(subtype, Literal) or isinstance(supertype, Literal):
            return None # No literals allowed here
        self.msg_handler = msg_handler
        self.tr_id = get_tr_id()
        xml_msg = self._create_wql_issubtype_msg(self.tr_id, 
                                                 subtype, supertype)
        self.conn.connect()
        self.conn.send(xml_msg)
        cnf = self.conn.receive()
        self._check_error(cnf)

        self.sub_id = cnf["subscription_id"]
        #self.msg_handler.handle(initial_result)            
        sub_h = WQLBooleanSubscribeHandler(self.node_id, self.tr_id,
                                           self.conn, msg_handler)
        sub_h.start()
        if cnf["results"] == "TRUE":
            return True
        else:
            return False

    def close(self):
        tmp = SSAP_UNSUBSCRIBE_TEMPLATE%self.sub_id
        msg = SSAP_MESSAGE_TEMPLATE%(self.node_id, self.targetSS,
                                     "UNSUBSCRIBE",
                                     str(get_tr_id()), tmp)
        self.unsub_conn.connect()
        self.unsub_conn.send(msg)
        self.unsub_conn.close()

class RDFSubscribeHandler(threading.Thread):
    def __init__(self, node_id, tr_id, connector,
                 msg_handler):
        threading.Thread.__init__(self)
        self.node_id = node_id
        self.tr_id = tr_id
        self.conn = connector
        self.msg_handler = msg_handler

    def run(self):
        while True:
            msg = self.conn.receive()
            # print "SUBSCRIBE RECEIVED:", msg
            if msg["transaction_type"] == "SUBSCRIBE":
                if msg["message_type"] == "INDICATION":
                    added_list = parse_M3RDF(msg["new_results"])
                    removed_list = parse_M3RDF(msg["obsolete_results"])
                    self.msg_handler.handle(added_list, removed_list)

                elif msg["message_type"] == "CONFIRM":
                    # Handle status information
                    pass
            elif msg["transaction_type"] == "UNSUBSCRIBE":
                if msg["message_type"] == "CONFIRM":
                    self.conn.close()
                    return
                elif msg["message_type"] == "INDICATION":
                    self.conn.close()
                    return
            else:
                raise SIBError(M3_SIB_ERROR)

class WQLNodeSubscribeHandler(threading.Thread):
    def __init__(self, node_id, tr_id, connector,
                 msg_handler):
        threading.Thread.__init__(self)
        self.node_id = node_id
        self.tr_id = tr_id
        self.conn = connector
        self.msg_handler = msg_handler
        
    def run(self):
        while True:
            msg = self.conn.receive()
            #print "SUBSCRIBE RECEIVED: ", msg
            if msg["transaction_type"] == "SUBSCRIBE":
                if msg["message_type"] == "INDICATION":
                    if "new_results" in msg and "obsolete_results" in msg:
                        add_node_list = parse_URI_list(msg["new_results"])
                        rem_node_list = parse_URI_list(msg["obsolete_results"])
                        self.msg_handler.handle(add_node_list, rem_node_list)
                elif msg["message_type"] == "CONFIRM":
                    # Handle status information
                    pass
            elif msg["transaction_type"] == "UNSUBSCRIBE":
                if msg["message_type"] == "CONFIRM":
                    self.conn.close()
                    return
                elif msg["message_type"] == "INDICATION":
                    self.conn.close()
                    return
            else:
                raise SIBError(M3_SIB_ERROR)

class WQLBooleanSubscribeHandler(threading.Thread):
    def __init__(self, node_id, tr_id, connector,
                 msg_handler):
        threading.Thread.__init__(self)
        self.node_id = node_id
        self.tr_id = tr_id
        self.conn = connector
        self.msg_handler = msg_handler
        
    def run(self):
        report_type = self.report_type
        while True:
            msg = self.conn.receive()
            #print "SUBSCRIBE RECEIVED: ", msg
            if msg["transaction_type"] == "SUBSCRIBE":
                if msg["message_type"] == "INDICATION":
                    if "new_results" in msg and "obsolete_results" in msg:
                        # As the subscription query returns a boolean value
                        # the "added" and "removed" fields will just
                        # have changed. I.e. if added is "True" then removed
                        # will always be "False" and vice versa
                        if msg["new_results"] == "TRUE":
                            self.msg_handler.handle(True, False)
                        else:
                            self.msg_handler.handle(False, True)
                elif msg["message_type"] == "CONFIRM":
                    # Handle status information
                    pass
            elif msg["transaction_type"] == "UNSUBSCRIBE":
                if msg["message_type"] == "CONFIRM":
                    self.conn.close()
                    return
                elif msg["message_type"] == "INDICATION":
                    self.conn.close()
                    return
            else:
                raise SIBError(M3_SIB_ERROR)

class Connector:
    def connect(self):
        '''Open a connection to SIB'''
        pass

    def send(self, msg):
        '''Sends a message to SIB. Parameter msg is the
        XML serialization of the message'''
        pass

    def receive(self):
        '''Receives a message from SIB. Returns a parsed message'''
        pass

    def close(self):
        '''Close the connection to SIB'''
        pass

    def _parse_msg(self, msg):
        parsed_msg = {}
        parser = make_parser()
        ssap_mh = SSAPMsgHandler(parsed_msg)
        parser.setContentHandler(ssap_mh)        
        parser.parse(StringIO(msg))
        return parsed_msg

class TCPConnector(Connector):
    def __init__(self, arg_tuple):
        self.sib_address, self.port = arg_tuple
        self.msg_buffer = ""

    def connect(self):
        self.s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        #self.s.setsockopt(TCP, socket.TCP_NODELAY, 1)
        self.s.connect((self.sib_address, self.port))
        
    def send(self, msg):
        length = len(msg)
        sent = self.s.send(msg)
        while sent < length:
            # If complete msg could not be sent, try to send the remaining part
            sent += self.s.send(msg[sent:])
        # Node sends only a single msg from each socket
        # Receiving is still possible after SHUT_WR
        # This is an implementation decision

        # Removed shutdown to circumvent a bug in OpenC for symbian
        # This change allows the node libary to run in S60 python
        self.s.shutdown(socket.SHUT_WR)
        
    def receive(self):
        msg_received = False
        msg_end_index = self.msg_buffer.find(END_TAG)
        # Check if msg_buffer contains a complete message
        # If so, return it without receiving from network
        if msg_end_index != -1: 
            msg = self.msg_buffer[:msg_end_index + ETL]
            self.msg_buffer = self.msg_buffer[msg_end_index + ETL:]
            return self._parse_msg(msg)
        msg = self.msg_buffer
        self.msg_buffer = ""
        # Different conditions cause breaking out of the loop
        # Not a perpetual while loop
        while True:
            chunk = self.s.recv(4096)
            if len(chunk) == 0: # Socket closed by SIB
                msg_end_index = msg.find(END_TAG)
                # Two cases:
                # 1: Socket was closed, no complete message
                #    -> return empty dict, discard incomplete message if any
                # 2: Socket was closed, complete message received
                #    -> return parsed message
                if msg_end_index == -1:
                    print "Socket closed with incomplete msg"
                    return {}
                else:
                    return self._parse_msg(msg)
            msg = msg + chunk
            msg_end_index = msg.find(END_TAG)
            if msg_end_index == -1: # Received partial msg
                continue
            elif msg_end_index + ETL < len(msg):
                # Received also start of next msg
                self.msg_buffer += msg[msg_end_index + ETL:]
                return self._parse_msg(msg[:msg_end_index + ETL])
            elif msg_end_index + ETL == len(msg): # Complete msg received
                return self._parse_msg(msg)
           
    def close(self):
        if hasattr(self, 's'):
            self.s.close()

class NodeM3RDFHandler(ContentHandler):
    "Handler for SAX events from M3 RDF encoded triples"
    def __init__(self, triple_list, bnodes):
        # After parsing, triple_list will contain instances of Triple type
        self.triple_list = triple_list
        self.inSubject = False
        self.inPredicate = False
        self.inObject = False
        self.inTriple = False
        self.literal = False
        self.bNodeMap = bnodes

    def startElement(self, name, attrs):
        if name == "subject":
            self.inSubject = True
            self.subject = ""
            return
        elif name == "predicate":
            self.inPredicate = True
            self.predicate = ""
            return
        elif name == "object":
            self.inObject = True
            self.object = ""
            if attrs.get('type', None).lower() == "literal":
                self.literal = True
            return
        elif name == "triple":
            self.inTriple = True
            return
        
    def characters(self, ch):
        if self.inSubject:
            self.subject += ch
        elif self.inPredicate:
            self.predicate += ch
        elif self.inObject:
            self.object += ch
            
    def endElement(self, name):
        if name == "subject":
            self.inSubject = False
            return
        elif name == "predicate":
            self.inPredicate = False
            return
        elif name == "object":
            self.inObject = False
            return
        elif name == "triple":
            self.inTriple = False
            s = URI(self.subject)
            p = URI(self.predicate)
            if self.literal:
                o = Literal(self.object)
            else:
                o = URI(self.object)
            self.triple_list.append(Triple(s, p, o))
            self.literal = False
            return

class SSAPMsgHandler(ContentHandler):
    # Parser for received SSAP messages
    # Specification of different messages
    # Can be found from Smart Space wiki
    # http://swiki.nokia.com/SmartSpaces/AccessProtocol

    def __init__(self, array):
        self.array = array
        self.stack = deque()
        self.parameter_name = ''
        self.in_parameter = False
        self.object_literal = False
        self.content = []

    def startElement(self, name, attrs):
        if name == "parameter":
            self.stack.append(attrs.get("name", None))
            self.in_parameter = True
        elif self.in_parameter:
            self.content.append('<%s'%name)
            for i in attrs.items():
                self.content.append(' %s = "%s"'%(str(i[0]), 
                                                  str(i[1])))
            self.content.append(">")
            if name == 'literal' or (name == 'object' and attrs.get('type', None) == 'literal'):
                self.object_literal = True
                self.content.append(CDATA_START)
        else:
            self.stack.append(name)

    def characters(self, ch):
        self.content.append(ch)
            
    def endElement(self, name):
        if self.in_parameter and name != 'parameter':
            if self.object_literal and (name == 'literal' or name == 'object'):
                self.content.append(CDATA_END)
                self.object_literal = False
            self.content.extend(["</", name, ">"])
            return
        elif name == "parameter":
            self.in_parameter = False
        self.array[self.stack.pop()] = ''.join(self.content).strip()
        self.content = []
            
class UriListHandler(ContentHandler):
    def __init__(self, node_list):
        self.node_list = node_list
        self.in_uri = False
        self.in_lit = False
            
    def startElement(self, name, attrs):
        if name == "uri":
            self.uri = ""
            self.in_uri = True
            return
        elif name == "literal":
            self.literal = ""
            self.in_lit = True
        else:
            return
            
    def characters(self, ch):
        if self.in_uri:
            self.uri += ch
            return
        elif self.in_lit:
            self.literal += ch
            return
        else:
            return

    def endElement(self, name):
        if name == "uri":
            self.in_uri = False
            self.node_list.append(URI(self.uri))
            return
        elif name == "literal":
            self.in_lit = False
            self.node_list.append(Literal(self.literal))
            return
        else:
            return

class BNodeUriListHandler(ContentHandler):
    def __init__(self, bnode_map):
        self.bnode_map = bnode_map
        self.bnode = None
        self.inURI = False
            
    def startElement(self, name, attrs):
        if name == "uri":
            self.URI = ""
            self.inURI = True
            self.bnode = bNode(attrs.get("tag", ""))
            return
        else:
            return
            
    def characters(self, ch):
        if self.inURI:
            self.URI += ch
            return
        else:
            return

    def endElement(self, name):
        if name == "uri":
            self.inURI = False
            if not self.bnode in self.bnode_map:
                self.bnode_map[self.bnode] = URI(self.URI)
            return
