import StringIO
from xml.sax import saxutils
from xml.sax import make_parser
from xml.sax.handler import ContentHandler
import threading
import socket
import uuid
import discovery

# SSAP Constants

M3_SUCCESS = "m3:Success"
M3_SIB_NOTIFICATION_RESET = "m3:SIB.Notification.Reset"
M3_SIB_NOTIFICATION_CLOSING = "m3:SIB.Notification.Closing"
M3_SIB_ERROR = "m3:SIB.Error"
M3_SIB_ERROR_ACCESS_DENIED = "m3:SIB.Error.AccessDenied"
M3_SIB_FAILURE_OUT_OF_RESOURCES = "m3:SIB.Failure.OutOfResources"
M3_SIB_FAILURE_NOT_IMPLEMENTED = "m3:SIB.Failure.NotImplemented"

M3_KP_ERROR = "m3:KP.Error"
M3_KP_ERROR_REQUEST = "m3:KP.Error.Request"
M3_KP_ERROR_MESSAGE_INCOMPLETE = "m3:KP.Error.Message.Incomplete"
M3_KP_ERROR_MESSAGE_SYNTAX = "m3:KP.Error.Message.Syntax"


END_TAG = "</SSAP_message>"
ETL = 15 # Length of end tag string
TCP = socket.getprotobyname("TCP")
CURRENT_TR_ID = 1
KP_ID_POSTFIX = str(uuid.uuid4())

def get_tr_id():
    global CURRENT_TR_ID
    rv = CURRENT_TR_ID
    CURRENT_TR_ID = CURRENT_TR_ID + 1
    return rv

def convert_query_to_insert(qtriples):
    pass


class Node:
    """Base class for Smart Space Nodes
    """
    def __init__(self, node_id):
        global KP_ID_POSTFIX
        self.member_of = []
        self.connections = []
        self.node_id = node_id + "-" + KP_ID_POSTFIX
        self.user_node_id = node_id

    def CreateInsertTransaction(self, dest):
        """Creates an insert transaction for inserting information
        to a smart space.
        Returns an instance of Insert(Transaction) class
        """
        c = Insert(dest, self.node_id)
        self.connections.append(("PROACTIVE", c))
        return c

    def CloseInsertTransaction(self, trans):
        """Closes an insert transaction
        """
        trans.close()
        self.connections = filter(lambda x: x[1] != trans, self.connections)

    def CreateRemoveTransaction(self, dest):
        """Creates a remove transaction for removing information
        from a smart space.
        Returns an instance of Remove(Transaction) class
        """
        c = Remove(dest, self.node_id)
        self.connections.append(("RETRACTION", c))
        return c

    def CloseRemoveTransaction(self, trans):
        """Closes a remove transaction
        """
        trans.close()
        self.connections = filter(lambda x: x[1] != trans, self.connections)

    def CreateUpdateTransaction(self, dest):
        c = Update(dest, self.node_id)
        self.connections.append(("UPDATE", c))
        return c

    def CloseUpdateTransaction(self, trans):
        """Closes an update transaction
        """
        trans.close()
        self.connections = filter(lambda x: x[1] != trans, self.connections)

    def CreateSubscribeTransaction(self, dest, once = False):
        """Creates a subscribe transaction for subscribing to
        information in a smart space.
        Returns an instance of Subscribe(Transaction) class
        """
        c = Subscribe(dest, self.node_id, once)
        self.connections.append(("REACTIVE", c))
        return c

    def CloseSubscribeTransaction(self, trans):
        """Closes a subscribe transaction
        """
        trans.close()
        self.connections = filter(lambda x: x[1] != trans, self.connections)

    def CreateQueryTransaction(self, dest):
        """Creates a query transaction for querying information
        in a smart space.
        Returns an instance of Query(Transaction) class
        """
        c = Query(dest, self.node_id)
        self.connections.append(("QUERY", c))
        return c

    def CloseQueryTransaction(self, trans):
        """Closes a query transaction
        """
        trans.close()
        self.connections = filter(lambda x: x[1] != trans, self.connections)

class ParticipantNode(Node):
    
    def discover(self, method = "Manual", browse = True, name = None):
        """Discover available smart spaces. Currently available
           discovery methods are:
             - Manual: give IP address and port
             - Bonjour discovery (if pyBonjour package has been installed)
           Returns a handle for smart space if method is Manual or
           browse is True, otherwise returns a list of handles
           Handle is a tuple
           (SSName, (connector class, (connector constructor args)))
           """
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
        """Join a smart space. Argument dest is a smart space handle of form
        (SSName, (connector class, (connector constructor args))).
        join() accepts handles returned by discover() method.
        Returns True if succesful, False otherwise.
        """
        targetSS, handle = dest
        connector, args = handle
        conn = connector(args)
        tr_id = get_tr_id()

        tmp = ["<SSAP_message><transaction_type>JOIN</transaction_type>",
               "<message_type>REQUEST</message_type>"]
        tmp.extend(["<transaction_id>", str(tr_id),"</transaction_id>"])
        tmp.extend(["<node_id>", str(self.node_id), "</node_id>"])
        tmp.extend(["<space_id>", str(targetSS), "</space_id>"])
        tmp.extend(['<parameter name = "credentials">',
                    "XYZZY", "</parameter>", "</SSAP_message>"])
        join_msg = "".join(tmp)

        conn.connect()
        conn.send(join_msg)
        cnf = conn.receive()
        conn.close()
        if cnf["status"] == M3_SUCCESS:
            self.member_of.append(targetSS)
            return True
        else:
            #print "Could not join SS", targetSS
            return False
    
    def leave(self, dest):
        """Leave a smart space. Argument dest is a smart space handle of form
        (SSName, (connector class, (connector constructor args))).
        leave() accepts handles returned by discover() method.
        Returns True if succesful, False otherwise.
        """
        targetSS, handle = dest
        connector, args = handle
        conn = connector(args)
        tr_id = get_tr_id()

        tmp = ["<SSAP_message><transaction_type>LEAVE</transaction_type>",
               "<message_type>REQUEST</message_type>"]
        tmp.extend(["<transaction_id>", str(tr_id),"</transaction_id>"])
        tmp.extend(["<node_id>", str(self.node_id), "</node_id>"])
        tmp.extend(["<space_id>", str(targetSS), "</space_id>"])
        tmp.extend(["</SSAP_message>"])
        leave_msg = "".join(tmp)

        conn.connect()
        conn.send(leave_msg)
        #print "Sent leave msg"
        cnf = conn.receive()
        conn.close()
        if "status" in cnf and cnf["status"] == M3_SUCCESS:
            tmp = filter(lambda x: x != targetSS, self.member_of)
            self.member_of = tmp
            return True
        else:
            tmp = filter(lambda x: x != targetSS, self.member_of)
            self.member_of = tmp
            return False


class Transaction:
    """Abstract base class for M3 transactions.
    """
    def __init__(self, dest, node_id):
        self.node_id = node_id
        self.tr_id = ""
        self.targetSS, handle = dest
        connector, args = handle
        self.conn = connector(args)

    def _encode(self, triples):
        """Internal. Encode triples to RDF-M3. Triples should be a list of
        ((s, p, o), subject_type, object_type), where subject_type is
        either "bnode" or "URI" and object_type is either "URI" or 
        "literal".
        """
        tmp = ['<triple_list>']
        for t, stype, otype in triples:
            # print "_encode: instance type is", self.__class__
            s, p, o = t
            if isinstance(self, Remove):
                if s == None:
                    s = "http://www.nokia.com/NRC/M3/sib#any"
                    stype = 'URI'
                elif s=='sib:any' or s=="http://www.nokia.com/NRC/M3/sib#any":
                    stype = 'URI'
                if p == None:
                    p = "http://www.nokia.com/NRC/M3/sib#any"
                if o == None:
                    o = "http://www.nokia.com/NRC/M3/sib#any"
                    otype = 'URI'
                elif o=='sib:any' or o=="http://www.nokia.com/NRC/M3/sib#any":
                    otype = 'URI'
            tmp.extend(['<triple>', 
                        '<subject type = "%s">%s</subject>'%(stype.lower(), s)])
            tmp.extend(['<predicate>%s</predicate>'%p,
                        '<object type = "%s"><![CDATA[%s]]></object>'%(otype.lower(), o)])
            tmp.append('</triple>')
        tmp.append('</triple_list>')
        return "".join(tmp)

class Insert(Transaction):
    """Class handling Insert transactions.
    """
                
    def _create_msg(self, tr_id, payload, confirm, expire_time, encoding):
        """Internal. Create a SSAP XML message for INSERT REQUEST
        """
        tmp = ["<SSAP_message><transaction_type>INSERT</transaction_type>",
               "<message_type>REQUEST</message_type>"]
        tmp.extend(["<transaction_id>", str(tr_id),"</transaction_id>"])
        tmp.extend(["<node_id>", str(self.node_id), "</node_id>"])
        tmp.extend(["<space_id>", str(self.targetSS), "</space_id>"])
        tmp.extend(['<parameter name="insert_graph" encoding="%s">'%encoding.upper(),
                    str(payload), "</parameter>"])
        tmp.extend(['<parameter name = "confirm">',
                    str(confirm).upper(),
                    "</parameter>",
                    "</SSAP_message>"])
        return "".join(tmp)

    def send(self, payload, pl_type = "python",
             encoding = "rdf-m3", confirm = True,
             expire_time = None):
        """Accepted format for payload is determined by the parameter pl_type.
           - pl_type == 'python': payload is a list of tuples
             (('s', 'p', 'o'), subject_type, object_type)
             where subject_type specifies the type of subject: 'URI' for
             a resource (uri) subject and 'bnode' for blank node subject
             (bnode can be an empty string), and object_type specifies the
             type of object: 'URI' for a resource (uri) object and 'literal' 
             for a literal object.

           - pl_type != 'python': payload is given in encoded form,
             either rdf-xml or rdf-m3. The encoding parameter has to be
             set accordingly. 

             If confirmation is requested, the return value will be a 
             dictionary associating named blank nodes in the insert graph 
             to the actual URIs stored in the SIB (works for rdf-m3 
             encoding only)
        """
        self.tr_id = get_tr_id()
        if pl_type == "python":
            m3_payload = self._encode(payload)
            xml_msg = self._create_msg(self.tr_id,
                                       m3_payload,
                                       confirm,
                                       expire_time,
                                       encoding)
        else:
            xml_msg = self._create_msg(self.tr_id,
                                       payload,
                                       confirm,
                                       expire_time,
                                       encoding)

        self.conn.connect()
        self.conn.send(xml_msg)
        if confirm:
            rcvd = self.conn.receive()
            bnodes = {}
            icf = BNodeUriListHandler(bnodes)
            parser = make_parser()
            parser.setContentHandler(icf)
            parser.parse(StringIO.StringIO(rcvd['bnodes']))
            if encoding.lower() == 'rdf-m3':
                return (rcvd["status"], bnodes)
            else:
                return (rcvd["status"], {})

    def close(self):
        self.conn.close()


class Remove(Transaction):
    """Class for handling Remove transactions.
    """
    def _create_msg(self, tr_id, triples, type, confirm):
        """Internal. Create a SSAP XML message for REMOVE REQUEST
        """
        tmp = ["<SSAP_message><transaction_type>REMOVE</transaction_type>",
               "<message_type>REQUEST</message_type>"]
        tmp.extend(["<transaction_id>", str(tr_id),"</transaction_id>"])
        tmp.extend(["<node_id>", str(self.node_id), "</node_id>"])
        tmp.extend(["<space_id>", str(self.targetSS), "</space_id>"])
        tmp.extend(['<parameter name="remove_graph" encoding="%s">'%str(type).upper()])
        tmp.extend([str(triples), "</parameter>"])
        tmp.extend(['<parameter name = "confirm">',
                    str(confirm).upper(),
                    "</parameter>",
                    "</SSAP_message>"])
        return "".join(tmp)

    def remove(self, r_triples, r_type = 'rdf-m3', confirm = True):
        """Remove triples listed in parameter triples. Format as in Query.
           No blank nodes allowed.
        """
        tr_id = get_tr_id()
        if confirm:
            confirm_str = "TRUE"
        else:
            confirm_str = "FALSE"

        if r_type.lower() == 'rdf-m3':
            triples = self._encode(r_triples)
        else:
            print "Only rdf-m3 encoding supported at moment!"
            return M3_SIB_FAILURE_NOT_IMPLEMENTED
        xml_msg = self._create_msg(tr_id, triples, r_type, confirm_str)
        self.conn.connect()
        self.conn.send(xml_msg)
        if confirm:
            rcvd = self.conn.receive()
            return rcvd["status"]

    def close(self):
        self.conn.close()

class Update(Transaction):

    def _create_msg(self, tr_id, i_triples, i_type, r_triples, r_type, confirm):
        """Internal. Create a SSAP message for UPDATE REQUEST
        """
        tmp = ["<SSAP_message><transaction_type>UPDATE</transaction_type>",
               "<message_type>REQUEST</message_type>"]
        tmp.extend(["<transaction_id>", str(tr_id),"</transaction_id>"])
        tmp.extend(["<node_id>", str(self.node_id), "</node_id>"])
        tmp.extend(["<space_id>", str(self.targetSS), "</space_id>"])
        tmp.extend(['<parameter name = "insert_graph" encoding = "%s">'%i_type.upper()])
        tmp.extend([i_triples,'</parameter>'])
        tmp.extend(['<parameter name = "remove_graph" encoding = "%s">'%r_type.upper()])
        tmp.extend([r_triples,'</parameter>'])
        tmp.extend(['<parameter name = "confirm">',
                    str(confirm),
                    "</parameter>",
                    "</SSAP_message>"])
        return "".join(tmp)

    def update(self, i_triples, i_enc, 
               r_triples, r_enc, confirm = True):
        """Perform an atomic update operation. First, removes the triples 
           given in parameter r_triples and then inserts the triples given
           in parameter i_triples. Parameters i_enc and r_enc have to be 
           currently 'RDF-M3'
        """
        tr_id = get_tr_id()
        if confirm:
            confirm_str = "TRUE"
        else:
            confirm_str = "FALSE"
        i_triples = self._encode(i_triples)
        r_triples = self._encode(r_triples)
        xml_msg = self._create_msg(tr_id, i_triples, i_enc, 
                                   r_triples, r_enc, confirm_str)
        self.conn.connect()
        self.conn.send(xml_msg)
        if confirm:
            # print "UPDATE: waiting for receive"
            rcvd = self.conn.receive()
            # print "UPDATE: confirmation received:"
            # print rcvd
            if rcvd["status"] != M3_SUCCESS:
                return (rcvd["status"],{})
            bnodes = {}
            icf = BNodeUriListHandler(bnodes)
            parser = make_parser()
            parser.setContentHandler(icf)
            parser.parse(StringIO.StringIO(rcvd['bnodes']))
            if i_enc.lower() == 'rdf-m3':
                return (rcvd["status"], bnodes)
            else:
                return (rcvd["status"], {})

    def close(self):
        self.conn.close()


class Query(Transaction):
    
    def _encode(self, triples):
        tmp = ['<triple_list>']
        for t in triples:
	    # print "T=",t
            # triple, obj_type = t
            # Hack to get Vesa's ssls working
            if len(t) == 3:
                triple, subj_type, obj_type = t
            else: # len(t) == 2
                triple, obj_type = t
            s, p, o = triple
            if s == None:
                s = "http://www.nokia.com/NRC/M3/sib#any"
            if p == None:
                p = "http://www.nokia.com/NRC/M3/sib#any"
            if o == None:
                o = "http://www.nokia.com/NRC/M3/sib#any"
                obj_type = 'URI'
            elif o == 'sib:any' or o == "http://www.nokia.com/NRC/M3/sib#any":
                obj_type = 'URI'
            tmp.extend(['<triple><subject>%s</subject><predicate>%s</predicate><object type = "%s"><![CDATA[%s]]></object></triple>'%(s, p, obj_type.lower(), o)])
        tmp.append('</triple_list>')
        return "".join(tmp)

    def _create_msg_header(self, tr_id):
        tmp = ["<SSAP_message><transaction_type>QUERY</transaction_type>",
               "<message_type>REQUEST</message_type>"]
        tmp.extend(["<transaction_id>", str(tr_id),"</transaction_id>"])
        tmp.extend(["<node_id>", str(self.node_id), "</node_id>"])
        tmp.extend(["<space_id>", str(self.targetSS), "</space_id>"])
        return tmp

    def _create_wql_values_msg(self, tr_id, start_node, path):
        s_node, sn_type = start_node
        if sn_type:
            sn_type = "literal"
        else:
            sn_type = "URI"
        tmp = self._create_msg_header(tr_id)
        tmp.extend(['<parameter name = "type">WQL-VALUES</parameter>'])
        tmp.extend(['<parameter name = "query">', '<wql_query>',
                    '<node name="start" type = "%s">'%sn_type, 
                    str(s_node), '</node>'])
        tmp.extend(['<path_expression>', str(path),
                    '</path_expression>', '</wql_query>', '</parameter>',
                    '</SSAP_message>'])
        return "".join(tmp)

    def _create_wql_related_msg(self, tr_id, start_node, end_node, path):
        s_node, sn_type = start_node
        e_node, en_type = end_node
        if sn_type:
            sn_type = "literal"
        else:
            sn_type = "URI"
        if en_type:
            en_type = "literal"
        else:
            en_type = "URI"
        tmp = self._create_msg_header(tr_id)
        tmp.extend(['<parameter name = "type">WQL-RELATED</parameter>'])
        tmp.extend(['<parameter name = "query">', '<wql_query>',
                    '<node name="start" type = "%s">'%sn_type, 
                    str(s_node), '</node>'])
        tmp.extend(['<node name="end" type= "%s">'%en_type,
                    str(e_node), '</node>'])
        tmp.extend(['<path_expression>', str(path),
                    '</path_expression>', '</wql_query>', '</parameter>',
                    '</SSAP_message>'])
        return "".join(tmp)

    def _create_wql_nodetypes_msg(self, tr_id, node):
        tmp = self._create_msg_header(tr_id)
        tmp.extend(['<parameter name = "type">WQL-NODETYPES</parameter>'])
        tmp.extend(['<parameter name = "query"><wql_query>',
                    '<node>', str(node), '</node>'])
        tmp.extend(['</wql_query>', '</parameter>', '</SSAP_message>'])
        return "".join(tmp)

    def _create_wql_istype_msg(self, tr_id, node, type):
        tmp = self._create_msg_header(tr_id)
        tmp.extend(['<parameter name = "type">WQL-ISTYPE</parameter>'])
        tmp.extend(['<parameter name = "query">', '<wql_query>',
                    '<node>',
                    str(node), '</node>'])
        tmp.extend(['<node name="type">', str(type),
                    '</node>','</wql_query>', '</parameter>',
                    '</SSAP_message>'])
        return "".join(tmp)

    def _create_wql_issubtype_msg(self, tr_id, subtype, supertype):
        tmp = self._create_msg_header(tr_id)
        tmp.extend(['<parameter name = "type">WQL-ISSUBTYPE</parameter>'])
        tmp.extend(['<parameter name = "query"><wql_query>',
                    '<node name="subtype">',
                    str(subtype), '</node>'])
        tmp.extend(['<node name="supertype">',
                    str(supertype), '</node>', '</wql_query>', 
                    '</parameter>', '</SSAP_message>'])
        return "".join(tmp)

    def _create_rdf_msg(self, tr_id, triples):
        tmp = self._create_msg_header(tr_id)
        tmp.extend(['<parameter name = "type">RDF-M3</parameter>'])
        tmp.extend(['<parameter name = "query">'])
        tmp.append(self._encode(triples))
        tmp.extend(['</parameter></SSAP_message>'])
        return "".join(tmp)

    def wql_values_query(self, start_node, path, report_type = True):
        """Perform a wql query on the information stored in a smart
           space. Parameter start_node is the starting node as a (node,
           literal?) pair, parameter path is the wql path expression
           as a string. The method returns a list of rdf nodes
           matching the query.  If report_type is set to True, the
           list contains pairs (node, literal?) where literal? is
           True if the rdf node is a Literal, and literal? is False if
           the rdf node is a URI.
        """
        self.tr_id = get_tr_id()
        xml_msg = self._create_wql_values_msg(self.tr_id, start_node, path)
        self.conn.connect()
        self.conn.send(xml_msg)
        response = self.conn.receive()
        if "status" in response:
            if response["status"] != M3_SUCCESS:
                return []
        else:
            return []
        if "results" in response:
            node_list = []
            rsp_parser = make_parser()
            rsp_mh = UriListHandler(node_list)
            rsp_parser.setContentHandler(rsp_mh)
            rsp_parser.parse(StringIO.StringIO(response["results"]))
            if report_type:
                return node_list
            else:
                return [i[0] for i in node_list]
        else:
            return []

    def wql_related_query(self, start_node, end_node, path):
        """Query information stored in a smart space using a wql
           related query. Parameter start_node is the starting node as
           a (node, literal?) pair, parameter end_node is (node,
           literal?) pair, and parameter path is the wql path
           expression as a string. The method returns True if the end
           node can be reached from the start node via the path
           specified in parameter path.
        """
        self.tr_id = get_tr_id()
        xml_msg = self._create_wql_related_msg(self.tr_id, 
                                               start_node, end_node, path)
        self.conn.connect()
        self.conn.send(xml_msg)
        response = self.conn.receive()
        if "status" in response:
            if response["status"] != M3_SUCCESS:
                return None
        else:
            return None
        if "results" in response:
            if response["results"] == "TRUE":
                return True
            else:
                return False
        else:
            return None

    def wql_nodetypes_query(self, node):
        """Queries information stored in a smart space using a wql
           nodetypes query. Parameter node is (node, literal?) pair,
           where node is the node whose types are queried as a
           string. The method returns a list of (node, literal?) pairs
           matching the query.  Note that literals can not have an RDF
           type.
        """
        self.tr_id = get_tr_id()
        node_value, node_type = node
        if node_type:
            return [] # Node can't be literal
        xml_msg = self._create_wql_nodetypes_msg(self.tr_id, node_value)
        self.conn.connect()
        self.conn.send(xml_msg)
        response = self.conn.receive()
        if "status" in response:
            if response["status"] != M3_SUCCESS:
                return []
        else:
            return []
        if "results" in response:
            node_list = []
            rsp_parser = make_parser()
            rsp_mh = UriListHandler(node_list)
            rsp_parser.setContentHandler(rsp_mh)
            rsp_parser.parse(StringIO.StringIO(response["results"]))
            return node_list
        else:
            return []

    def wql_istype_query(self, node, type):
        """Query information stored in a smart space using a wql
           istype query. Parameter node is the instance node as a 
           string, and parameter type is the type node as a string. The
           method returns True if the instance node is of the type specified
           by the type node.
        """
        self.tr_id = get_tr_id()
        node_value, node_type = node
        type_value, type_type = type
        if node_type or type_type:
            return None # No literals allowed here
        xml_msg = self._create_wql_istype_msg(self.tr_id, 
                                              node_value, 
                                              type_value)
        self.conn.connect()
        self.conn.send(xml_msg)
        response = self.conn.receive()
        if "status" in response:
            if response["status"] != M3_SUCCESS:
                return None
        else:
            return None
        if "results" in response:
            if response["results"] == "TRUE":
                return True
            else:
                return False
        else:
            return None

    def wql_issubtype_query(self, subtype, supertype):
        """Subscribe to information stored in a smart space using a wql
           issubtype query. Parameter subtype is the subtype node as 
           a string, and parameter supertype is the supertype node as a 
           string. The method returns True if the subtype node is a
           subtype of the supertype node.
        """
        self.tr_id = get_tr_id()
        subtype_value, subtype_type = subtype
        supertype_value, supertype_type = supertype
        if subtype_type or supertype_type:
            return None # No literals allowed here
        xml_msg = self._create_wql_issubtype_msg(self.tr_id, 
                                                 subtype_value, 
                                                 supertype_value)
        self.conn.connect()
        self.conn.send(xml_msg)
        response = self.conn.receive()
        if "status" in response:
            if response["status"] != M3_SUCCESS:
                return None
        else:
            return None
        if "results" in response:
            if response["results"] == "TRUE":
                return True
            else:
                return False
        else:
            return None

    def rdf_query(self, triple, return_subj_type = False):
        """Perform a pattern (triple matching) query on the information
        stored in a smart space. The parameter triple is a tuple
        ((s, p, o), obj_type) where obj_type is either 'uri' or 'literal',
        and any of s, p, o may be None, specifying a wildcard. If o is
        None, obj_type does not have any meaning.

        If parameter return_subj_type is False, the method returns a list 
        of matching triples [((s, p, o), object_type),...], where the 
        object_type is either "URI" or "literal"

        If parameter return_subj_type is True, the method returns a list 
        of matching triples [((s, p, o), subject_type, object_type),...],
        where subject_type is always "URI" and the object_type is 
        either "URI" or "literal"
        """
        if type(triple) != list:
            triple = [triple]
        self.tr_id = get_tr_id()
        xml_msg = self._create_rdf_msg(self.tr_id, triple)
        self.conn.connect()
        self.conn.send(xml_msg)
        response = self.conn.receive()
        if "status" in response:
            if response["status"] != M3_SUCCESS:
                return []
        else:
            return []
        if "results" in response:
            triple_list = []
            rsp_parser = make_parser()
            rsp_mh = NodeM3RDFHandler(triple_list, {}, return_subj_type)
            rsp_parser.setContentHandler(rsp_mh)
            rsp_parser.parse(StringIO.StringIO(response["results"]))
            return triple_list
        else:
            return []

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
    
    def _encode(self, triples):
        tmp = ['<triple_list>']
        for t in triples:
            # triple, obj_type = t
            # Hack to get Vesa's ssls working
            if len(t) == 3:
                triple, subj_type, obj_type = t
            else: # len(t) == 2
                triple, obj_type = t
            s, p, o = triple
            if s == None:
                s = "http://www.nokia.com/NRC/M3/sib#any"
            if p == None:
                p = "http://www.nokia.com/NRC/M3/sib#any"
            if o == None:
                o = "http://www.nokia.com/NRC/M3/sib#any"
                obj_type = 'URI'
            elif o == 'sib:any' or o == "http://www.nokia.com/NRC/M3/sib#any":
                obj_type = 'URI'
            tmp.extend(['<triple><subject>%s</subject><predicate>%s</predicate><object type = "%s"><![CDATA[%s]]></object></triple>'%(s, p, obj_type.lower(), o)])
        tmp.append('</triple_list>')
        return "".join(tmp)

    def _create_msg_header(self, tr_id):
        tmp = ["<SSAP_message><transaction_type>SUBSCRIBE</transaction_type>",
               "<message_type>REQUEST</message_type>"]
        tmp.extend(["<transaction_id>", str(tr_id),"</transaction_id>"])
        tmp.extend(["<node_id>", str(self.node_id), "</node_id>"])
        tmp.extend(["<space_id>", str(self.targetSS), "</space_id>"])
        return tmp

    def _create_wql_values_msg(self, tr_id, start_node, path):
        s_node, sn_type = start_node
        if sn_type:
            sn_type = "literal"
        else:
            sn_type = "URI"
        tmp = self._create_msg_header(tr_id)
        tmp.extend(['<parameter name = "type">WQL-VALUES</parameter>'])
        tmp.extend(['<parameter name = "query">', '<wql_query>',
                    '<node name="start" type = "%s">'%sn_type, 
                    str(s_node), '</node>'])
        tmp.extend(['<path_expression>', str(path),
                    '</path_expression>', '</wql_query>', '</parameter>',
                    '</SSAP_message>'])
        return "".join(tmp)

    def _create_wql_related_msg(self, tr_id, start_node, end_node, path):
        s_node, sn_type = start_node
        e_node, en_type = end_node
        if sn_type:
            sn_type = "literal"
        else:
            sn_type = "URI"
        if en_type:
            en_type = "literal"
        else:
            en_type = "URI"
        tmp = self._create_msg_header(tr_id)
        tmp.extend(['<parameter name = "type">WQL-RELATED</parameter>'])
        tmp.extend(['<parameter name = "query">', '<wql_query>',
                    '<node name="start" type = "%s">'%sn_type, 
                    str(s_node), '</node>'])
        tmp.extend(['<node name="end" type= "%s">'%en_type,
                    str(e_node), '</node>'])
        tmp.extend(['<path_expression>', str(path),
                    '</path_expression>', '</wql_query>', '</parameter>',
                    '</SSAP_message>'])
        return "".join(tmp)

    def _create_wql_nodetypes_msg(self, tr_id, node):
        tmp = self._create_msg_header(tr_id)
        tmp.extend(['<parameter name = "type">WQL-NODETYPES</parameter>'])
        tmp.extend(['<parameter name = "query"><wql_query>',
                    '<node>', str(node), '</node>'])
        tmp.extend(['</wql_query>', '</parameter>', '</SSAP_message>'])
        return "".join(tmp)

    def _create_wql_istype_msg(self, tr_id, node, type):
        tmp = self._create_msg_header(tr_id)
        tmp.extend(['<parameter name = "type">WQL-ISTYPE</parameter>'])
        tmp.extend(['<parameter name = "query">', '<wql_query>',
                    '<node name="start">',
                    str(node), '</node>'])
        tmp.extend(['<node name="type">', str(type),
                    '</node>','</wql_query>', '</parameter>',
                    '</SSAP_message>'])
        return "".join(tmp)

    def _create_wql_issubtype_msg(self, tr_id, subtype, supertype):
        tmp = self._create_msg_header(tr_id)
        tmp.extend(['<parameter name = "type">WQL-ISSUBTYPE</parameter>'])
        tmp.extend(['<parameter name = "query"><wql_query>',
                    '<node name = "subtype">',
                    str(subtype), '</node>'])
        tmp.extend(['<node name = "supertype">',
                    str(supertype), '</node>', '</wql_query>', 
                    '</parameter>', '</SSAP_message>'])
        return "".join(tmp)

    def _create_rdf_msg(self, tr_id, triples):
        tmp = self._create_msg_header(tr_id)
        tmp.extend(['<parameter name = "type">RDF-M3</parameter>'])
        tmp.extend(['<parameter name = "query">'])
        tmp.append(self._encode(triples))
        tmp.extend(['</parameter></SSAP_message>'])
        return "".join(tmp)

    def subscribe_rdf(self, triple, msg_handler, return_subj_type = False):
        """Subscribe to the information stored in a smart space using
           a pattern (triple matching) query. The parameter triple is a
           tuple (s, p, o) where any component may be None, specifying a
           wildcard.

           The method returns a list of matching triples 
           [((s, p, o), object_type),...]
           
           When the query results change in the smart space the subscription
           calls msg_handler.handle(added, removed) where added is a list
           of triples added to the query results since last callback and
           removed is a list of triples removed from the query results after
           last callback
        """
        if type(triple) != list:
            triple = [triple]
        self.msg_handler = msg_handler
        self.tr_id = get_tr_id()
        xml_msg = self._create_rdf_msg(self.tr_id, triple)
        #print "SUBSCRIBE sending:", xml_msg
        self.conn.connect()
        self.conn.send(xml_msg)
        cnf = self.conn.receive()
        if cnf["status"] == M3_SUCCESS:
            self.sub_id = cnf["subscription_id"]
            initial_result = []
            rsp_parser = make_parser()
            rsp_mh = NodeM3RDFHandler(initial_result, {}, return_subj_type)
            rsp_parser.setContentHandler(rsp_mh)
            rsp_parser.parse(StringIO.StringIO(cnf["results"]))
            #self.msg_handler.handle(initial_result)            
            sub_h = RDFSubscribeHandler(
                self.node_id,
                self.tr_id,
                self.conn,
                msg_handler,
                return_subj_type)
            sub_h.start()
            return initial_result
        else:
            # Throw exception
            return None

    def subscribe_wql_values(self, start_node, path, 
                             msg_handler, report_type = True):
        """Subscribe to information stored in a smart space using a
        wql values query. Parameter start_node is the starting node as a
        pair (node, literal?), parameter path is the wql path
        expression as a string or list of strings (e.g. "['seq',
        'rdf:type']" or ['seq', 'rdf:type']). The method returns a
        list of rdf nodes matching the query and makes a callback each
        time that the query results change in the smart space. The
        msg_handler callback should be a class having a method
        handle(added, removed) where added is a list of (node,
        literal?) pairs added to the result set after previous
        callback, and removed is a list of (node, literal?) removed
        from the result set after previous callback.
        """
        self.msg_handler = msg_handler
        self.tr_id = get_tr_id()
        xml_msg = self._create_wql_values_msg(self.tr_id, start_node, path)
        self.conn.connect()
        self.conn.send(xml_msg)
        cnf = self.conn.receive()
        if cnf["status"] == M3_SUCCESS:
            self.sub_id = cnf["subscription_id"]
            initial_result = []
            rsp_parser = make_parser()
            rsp_mh = UriListHandler(initial_result)
            rsp_parser.setContentHandler(rsp_mh)
            rsp_parser.parse(StringIO.StringIO(cnf["results"]))
            #self.msg_handler.handle(initial_result)            
            sub_h = WQLNodeSubscribeHandler(self.node_id, self.tr_id,
                                        self.conn, msg_handler,
                                        report_type)
            sub_h.start()
            if report_type:
                return initial_result
            else:
                return [i[0] for i in initial_result]
        else:
            # Throw exception
            return None

    def subscribe_wql_related(self, start_node, end_node, path, 
                             msg_handler, report_type = False):
        """Subscribe to information stored in a smart space using a
           wql related query. Parameter start_node is the starting
           node as a pair (node, literal?), parameter end_node is the
           end node as a pair (node, literal?), and parameter path is
           the wql path expression as a string. The method returns
           True if the end node can be reached from the start node via
           the path specified in parameter path, and makes a callback
           each time that the query results change in the smart
           space. The msg_handler callback should be a class having a
           method handle(added, removed) where both are boolean
           values.
        """
        self.msg_handler = msg_handler
        self.tr_id = get_tr_id()
        xml_msg = self._create_wql_related_msg(self.tr_id, start_node,
                                               end_node, path)
        self.conn.connect()
        self.conn.send(xml_msg)
        cnf = self.conn.receive()
        if cnf["status"] == M3_SUCCESS:
            self.sub_id = cnf["subscription_id"]
            #self.msg_handler.handle(initial_result)            
            sub_h = WQLBooleanSubscribeHandler(self.node_id, self.tr_id,
                                               self.conn, msg_handler)
            sub_h.start()
            if cnf["results"] == "TRUE":
                return True
            else:
                return False
        else:
            # Throw exception
            return None

    def subscribe_wql_nodetypes(self, node, 
                             msg_handler, report_type = False):
        """Subscribe to information stored in a smart space using a
           wql nodetypes query. Parameter node is the node whose types
           are queried as a pair (node, literal?), where the literal?
           should be False (indicating an URI). The method returns a
           list of (node, literal?) pairs matching the query and makes
           a callback each time that the query results change in the
           smart space. The msg_handler callback should be a class
           having a method handle(added, removed) where added is a
           list of (node, literal?) pairs added to the result set
           after previous callback, and removed is a list of (node,
           literal?) removed from the result set after previous
           callback.
        """
        node_value, node_type = node
        if node_type:
            # Throw exception
            return None # Node can't be literal
        self.msg_handler = msg_handler
        self.tr_id = get_tr_id()
        xml_msg = self._create_wql_nodetypes_msg(self.tr_id, node)
        self.conn.connect()
        self.conn.send(xml_msg)
        cnf = self.conn.receive()
        if cnf["status"] == M3_SUCCESS:
            self.sub_id = cnf["subscription_id"]
            initial_result = []
            rsp_parser = make_parser()
            rsp_mh = UriListHandler(initial_result)
            rsp_parser.setContentHandler(rsp_mh)
            rsp_parser.parse(StringIO.StringIO(cnf["results"]))
            #self.msg_handler.handle(initial_result)            
            sub_h = WQLNodeSubscribeHandler(self.node_id, self.tr_id,
                                        self.conn, msg_handler,
                                        report_type)
            sub_h.start()
            if report_type:
                return initial_result
            else:
                return [i[1] for i in initial_result]
        else:
            # Throw exception
            return None
        

    def subscribe_wql_istype(self, node, type, 
                             msg_handler):
        """Subscribe to information stored in a smart space using a
           wql istype query. Parameter node is the instance node as a
           pair (node, literal?) where literal? should be False
           (indicating an URI), and parameter type is the type node as
           a string. The method returns True if the instance node is
           of the type specified by the type node, and makes a
           callback each time that the query results change in the
           smart space. The msg_handler callback should be a class
           having a method handle(added, removed) where both are
           boolean values.
        """
        node_value, node_type = node
        type_value, type_type = type
        if node_type or type_type:
            return None # No literals allowed here
        self.msg_handler = msg_handler
        self.tr_id = get_tr_id()
        xml_msg = self._create_wql_istype_msg(self.tr_id, node,
                                               type)
        self.conn.connect()
        self.conn.send(xml_msg)
        cnf = self.conn.receive()
        if cnf["status"] == M3_SUCCESS:
            self.sub_id = cnf["subscription_id"]
            #self.msg_handler.handle(initial_result)            
            sub_h = WQLBooleanSubscribeHandler(self.node_id, self.tr_id,
                                               self.conn, msg_handler)
            sub_h.start()
            if cnf["results"] == "TRUE":
                return True
            else:
                return False
        else:
            # Throw exception
            return None

    def subscribe_wql_issubtype(self, subtype, supertype, 
                             msg_handler):
        """Subscribe to information stored in a smart space using a
           wql issubtype query. Parameter subtype is the subtype
           node as a pair (node, literal?), and parameter supertype is
           the supertype node as a pair (node, literal?). Both
           literal? values in the parameters should be False
           (indicating an URI). The method returns True if the subtype
           node is a subtype of the supertype node, and makes a
           callback each time that the query results change in the
           smart space. The msg_handler callback should be a class
           having a method handle(added, removed) where both are
           boolean values.
        """
        subtype_value, subtype_type = subtype
        supertype_value, supertype_type = supertype
        if subtype_type or supertype_type:
            return None # No literals allowed here
        self.msg_handler = msg_handler
        self.tr_id = get_tr_id()
        xml_msg = self._create_wql_issubtype_msg(self.tr_id, 
                                                 subtype, supertype)
        self.conn.connect()
        self.conn.send(xml_msg)
        cnf = self.conn.receive()
        if cnf["status"] == M3_SUCCESS:
            self.sub_id = cnf["subscription_id"]
            #self.msg_handler.handle(initial_result)            
            sub_h = WQLBooleanSubscribeHandler(self.node_id, self.tr_id,
                                               self.conn, msg_handler)
            sub_h.start()
            if cnf["results"] == "TRUE":
                return True
            else:
                return False
        else:
            # Throw exception
            return None

    def close(self):
        tmp = "<SSAP_message>"
        tmp = tmp + "<transaction_type>UNSUBSCRIBE</transaction_type>"
        tmp = tmp + "<message_type>REQUEST</message_type>"
        tmp = tmp + "<node_id>" + self.node_id + "</node_id>"
        tmp = tmp + "<space_id>" + self.targetSS + "</space_id>"
        tmp = tmp + "<transaction_id>" + str(get_tr_id()) + "</transaction_id>"
        tmp = tmp + '<parameter name = "subscription_id">' + self.sub_id + "</parameter>"
        tmp = tmp + "</SSAP_message>"
        self.unsub_conn.connect()
        self.unsub_conn.send(tmp)
        self.unsub_conn.close()

class RDFSubscribeHandler(threading.Thread):
    def __init__(self, node_id, tr_id, connector,
                 msg_handler, return_subj_type):
        threading.Thread.__init__(self)
        self.node_id = node_id
        self.tr_id = tr_id
        self.conn = connector
        self.msg_handler = msg_handler
        self.return_subj_type = return_subj_type
        
    def run(self):
        while True:
            msg = self.conn.receive()
            # print "SUBSCRIBE RECEIVED:", msg
            if msg["transaction_type"] == "SUBSCRIBE":
                if msg["message_type"] == "INDICATION":
                    added_list = []
                    removed_list = []

                    add_parser = make_parser()
                    add_mh = NodeM3RDFHandler(added_list, {}, 
                                              self.return_subj_type)
                    add_parser.setContentHandler(add_mh)
                    add_parser.parse(StringIO.StringIO(msg["new_results"]))
                    
                    # TODO: Check if the parser instance can be reused

                    rem_parser = make_parser()
                    rem_mh = NodeM3RDFHandler(removed_list, {}, 
                                              self.return_subj_type)
                    rem_parser.setContentHandler(rem_mh)
                    rem_parser.parse(StringIO.StringIO(msg["obsolete_results"]))

                    self.msg_handler.handle(added_list, removed_list)
                    #sink = RDFSink()
                    #rdfxml.parseRDF(str(msg["results"]), None, sink)
                    #self.msg_handler.handle(sink.triples)

                elif msg["message_type"] == "CONFIRMATION":
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
                # Erroneous msg, raise exception
                return

class WQLNodeSubscribeHandler(threading.Thread):
    def __init__(self, node_id, tr_id, connector,
                 msg_handler, report_type):
        threading.Thread.__init__(self)
        self.node_id = node_id
        self.tr_id = tr_id
        self.conn = connector
        self.msg_handler = msg_handler
        self.report_type = report_type
        
    def run(self):
        report_type = self.report_type
        while True:
            msg = self.conn.receive()
            #print "SUBSCRIBE RECEIVED: ", msg
            if msg["transaction_type"] == "SUBSCRIBE":
                if msg["message_type"] == "INDICATION":
                    if "new_results" in msg and "obsolete_results" in msg:
                        add_node_list = []
                        rem_node_list = []

                        add_parser = make_parser()
                        add_mh = UriListHandler(add_node_list)
                        add_parser.setContentHandler(add_mh)
                        add_parser.parse(StringIO.StringIO(msg["new_results"]))

                        rem_parser = make_parser()
                        rem_mh = UriListHandler(rem_node_list)
                        rem_parser.setContentHandler(rem_mh)
                        rem_parser.parse(StringIO.StringIO(msg["obsolete_results"]))
                        if report_type:
                            self.msg_handler.handle(add_node_list, rem_node_list)
                        else:
                            self.msg_handler.handle([i[1] for i in add_node_list], 
                                                    [i[1] for i in rem_node_list])
                elif msg["message_type"] == "CONFIRMATION":
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
                # Erroneous msg, raise exception
                return

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
                        # This might be confusing, but...
                        # If subscription query returns a boolean value
                        # the "added" and "removed" fields will just
                        # have changed. I.e. if added is "True" then removed
                        # will always be "False" and vice versa
                        if msg["new_results"] == "TRUE":
                            self.msg_handler.handle(True, False)
                        else:
                            self.msg_handler.handle(False, True)
                elif msg["message_type"] == "CONFIRMATION":
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
                # Erroneous msg, raise exception
                return

class Connector:
    def connect(self):
        """Open a connection to SIB"""
        pass

    def send(self, msg):
        """Sends a message to SIB. Parameter msg is the
        XML serialization of the message"""
        pass

    def receive(self):
        """Receives a message from SIB. Returns a parsed message"""
        pass

    def close(self):
        """Close the connection to SIB"""
        pass

    def _parse_msg(self, msg):
        # print "_PARSE_MSG: parsing:"
        # print msg
        parsed_msg = {}
        parser = make_parser()
        ssap_mh = SSAPMsgHandler(parsed_msg)
        parser.setContentHandler(ssap_mh)        
        parser.parse(StringIO.StringIO(msg))
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
        # print "TCPCONNECTOR.send(): sending"
        # print msg
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
        ######
        #
        # TODO: Modify to have a single return point
        #
        ######
        msg_received = False
        msg_end_index = self.msg_buffer.find(END_TAG)
        # Check if msg_buffer contains a complete message
        # If so, return it without receiving from network
        if msg_end_index != -1: 
            msg = self.msg_buffer[:msg_end_index + ETL]
            self.msg_buffer = self.msg_buffer[msg_end_index + ETL:]
            print "RECEIVE: got"
            print msg
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
                #    -> return empty string, discard incomplete message if any
                # 2: Socket was closed, complete message received
                #    -> return message
                if msg_end_index == -1:
                    # print "SIB has closed socket"
                    # print "Incomplete Message remaining in buffer:"
                    # print msg
                    return {}
                else:
                    # print "TCPConnector: socket closed, got message:"
                    # print msg
                    return self._parse_msg(msg)
            msg = msg + chunk
            msg_end_index = msg.find(END_TAG)
            if msg_end_index == -1: # Received partial msg
                continue
            elif msg_end_index + ETL < len(msg):
                # Received also start of next msg
                self.msg_buffer += msg[msg_end_index + ETL:]
                # print "TCPConnector: More than one message received:"
                # print msg
                return self._parse_msg(msg[:msg_end_index + ETL])
            elif msg_end_index + ETL == len(msg): # Complete msg received
                # print "TCPConnector: got message:"
                # print msg
                return self._parse_msg(msg)
           
    def close(self):
        self.s.close()

class NodeM3RDFHandler(ContentHandler):
    "Handler for SAX events from M3 RDF encoded triples"
    def __init__(self, triple_list, bnodes, subj_type = False):
        # After parsing, template_list will contain tuples of form
        # (literal, [triple_id1, triple_id2,...], (s, p, o)) where 
        # literal is True (value, not str) if object is a literal
        # literal is False if object is a URI
        # triple_ids are the ids pointing to the triple
        self.triple_list = triple_list
        self.inSubject = False
        self.inPredicate = False
        self.inObject = False
        self.inTriple = False
        self.literal = False
        self.bNodeMap = bnodes
        self.subj_type = subj_type

    def startElement(self, name, attrs):
        if name == "subject":
            self.inSubject = True
            self.subject = []
            return
        elif name == "predicate":
            self.inPredicate = True
            self.predicate = []
            return
        elif name == "object":
            self.inObject = True
            self.object = []
            if attrs.get('type', None).lower() == "literal":
                self.literal = True
            return
        elif name == "triple":
            self.inTriple = True
            return
        
    def characters(self, ch):
        if self.inSubject:
            self.subject.append(ch)
        elif self.inPredicate:
            self.predicate.append(ch)
        elif self.inObject:
            self.object.append(ch)
            
    def endElement(self, name):
        if name == "subject":
            self.inSubject = False
            self.subject_str = "".join(self.subject)
            return
        elif name == "predicate":
            self.inPredicate = False
            self.predicate_str = "".join(self.predicate)
            return
        elif name == "object":
            self.inObject = False
            self.object_str = "".join(self.object)
            return
        elif name == "triple":
            self.inTriple = False
            if self.subj_type:
                self.triple_list.append(((self.subject_str, 
                                          self.predicate_str, 
                                          self.object_str), 
                                         False,
                                         self.literal))
            else:
                self.triple_list.append(((self.subject_str, 
                                          self.predicate_str, 
                                          self.object_str), 
                                         self.literal))
            self.literal = False
            return

class SSAPMsgHandler(ContentHandler):
    # Parser for received messages
    # Specification of different messages
    # Can be found from Smart Space wiki
    # http://swiki.nokia.com/SmartSpaces/AccessProtocol
    
    def __init__(self, array):
        self.array = array
        self.inNodeId = False
        self.inSpaceId = False
        self.inTransactionId = False
        self.inTransactionType = False
        self.inMessageType = False
        self.inParameter = False
        self.inICV = False
        self.parameterName = ""
        self.object_literal = False

    def startElement(self, name, attrs):
        if name == "SSAP_message":
            self.array["timestamp"] = "123"
            self.array["msg_id"] = str(uuid.uuid4())
            return
        elif name == "node_id":
            self.inNodeId = True
            self.nodeId = ""
            return
        elif name == "space_id":
            self.inSpaceId = True
            self.spaceId = ""
            return
        elif name == "transaction_id":
            self.inTransactionId = True
            self.transactionId = ""
            return
        elif name == "transaction_type":
            self.inTransactionType = True
            self.transactionType = ""
            return
        elif name == "message_type":
            self.inMessageType = True
            self.messageType = ""
            return
        elif name == "parameter":
            self.inParameter = True
            self.parameterName = attrs.get("name",None)
            self.parameter_strings = []
            return
        elif name == "ICV":
            self.inICV = True
            self.ICV = ""
            return
        else:
            if self.inParameter:
                self.parameter_strings.extend(["<", name])
                for i in attrs.items():
                    self.parameter_strings.extend([" ", str(i[0]), '="', str(i[1]), '"'])
                self.parameter_strings.append(">")
                if name == 'literal' or (name == 'object' and attrs.get('type', None) == 'literal'):
                    self.object_literal = True
                    self.parameter_strings.append('<![CDATA[')
            return

    def characters(self, ch):
        if self.inNodeId:
            self.nodeId = self.nodeId + ch
        elif self.inSpaceId:
            self.spaceId = self.spaceId + ch
        elif self.inTransactionId:
            self.transactionId = self.transactionId + ch
        elif self.inTransactionType:
            self.transactionType = self.transactionType + ch
        elif self.inMessageType:
            self.messageType = self.messageType + ch
        elif self.inParameter:
            self.parameter_strings.append(ch)
        elif self.inICV:
            self.ICV = self.ICV + ch
        
            
    def endElement(self, name):
        if name == "node_id":
            self.array["node_id"] = self.nodeId
            self.inNodeId = False
            return
        if name == "space_id":
            self.array["space_id"] = self.spaceId
            self.inSpaceId = False
            return
        elif name == "transaction_id":
            self.array["transaction_id"] = self.transactionId
            self.inTransactionId = False
            return
        elif name == "transaction_type":
            self.array["transaction_type"] = self.transactionType
            self.inTransactionType = False
            return
        elif name == "message_type":
            self.array["message_type"] = self.messageType
            self.inMessageType = False
            return
        elif name == "ICV":
            self.array["ICV"] = self.ICV
            self.inICV = False
            return
        elif name == "parameter":
            self.array[self.parameterName] = "".join(self.parameter_strings)
            self.inParameter = False
            return
        elif name == "SSAP_message":
            return
        else:
            if self.inParameter:
                if self.object_literal and (name == 'literal' or name == 'object'):
                    self.parameter_strings.append(']]>')
                    self.object_literal = False
                self.parameter_strings.extend(["</", name, ">"])
            return


class UriListHandler(ContentHandler):
    def __init__(self, node_list):
        self.node_list = node_list
        self.inURI = False
        self.inLit = False
            
    def startElement(self, name, attrs):
        if name == "uri":
            self.URI = ""
            self.inURI = True
            return
        elif name == "literal":
            self.Literal = ""
            self.inLit = True
        else:
            return
            
    def characters(self, ch):
        if self.inURI:
            self.URI += ch
            return
        elif self.inLit:
            self.Literal += ch
            return
        else:
            return

    def endElement(self, name):
        if name == "uri":
            self.inURI = False
            self.node_list.append((self.URI, False))
            return
        elif name == "literal":
            self.inLit = False
            self.node_list.append((self.Literal, True))
            return
        else:
            return

class BNodeUriListHandler(ContentHandler):
    def __init__(self, bnode_map):
        self.bNodeMap = bnode_map
        self.bNode = ""
        self.inURI = False
            
    def startElement(self, name, attrs):
        if name == "uri":
            self.URI = ""
            self.inURI = True
            self.bNode = attrs.get("tag", "")
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
            if not self.bNode in self.bNodeMap:
                self.bNodeMap[self.bNode] = self.URI
            return


