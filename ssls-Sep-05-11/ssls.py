#
# Smart Space ls
#
# Copyright (C) 2009, 2010, 2011 Nokia Corporation
# All rights reserved. 
#
#  Redistribution and use in source and binary forms, with or without
#  modification, are permitted provided that the following conditions
#  are met:
#  
#    * Redistributions of source code must retain the above copyright
#    notice, this list of conditions and the following disclaimer.  
#    * Redistributions in binary form must reproduce the above copyright
#    notice, this list of conditions and the following disclaimer in
#    the documentation and/or other materials provided with the
#    distribution.  
#    * Neither the name of Nokia nor the names of its contributors 
#    may be used to endorse or promote products derived from this 
#    software without specific prior written permission.
#
#  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
#  "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
#  LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
#  FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
#  COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
#  INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
#  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
#  SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
#  HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
#  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
#  OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
#  EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
#
#
# Author(s): Vesa Luukkala
#
# $Id: ssls.py,v 1.66 2011/08/18 18:07:48 vluukkal Exp $
# 

try:
    # Backward compatible
    import Node
except:
    from smart_m3 import Node

import time
import sys
import os

import cmd
import shlex
import csv

import getopt

import urlparse
import uuid

import threading

import copy
import tempfile 

use_yp = True

def is_str_lit(s):
    """
    This is a hack to decide whether a given string is interpreted
    as an literal. We decide it on the content of the string, which 
    is not a general solution.
    The rules are:
    - no ':' in the string

    This means that what appears to be an uuid generated 
    URI ID is always a literal. 
    If we change things so that URIs are always URIs syntactically,
    maybe enforced by SIB, this problem disappears as we always 
    have a namespace which identifies an URI by prefix. 
    """
    res = False
    if s.find(':') == -1:
        res = True
    # Detect a UUID, it has 8-4-4-4-12 
    # An ugly hack again to circumvent lack of proper literals. 
    if len(s) > 28 and s[-13] == '-' and s[-18] == '-' and s[-23] == '-' and s[-28] == '-':
        res = False
    return res


class Rule:
    def __init__(self,cmdln,n,args,t):
        self.cmdln = cmdln
        self.name = n
        self.args = args
        self.arity = len(args)
        self.ready = False
        self.body = []
        self.type = t

    def set_one_body(self,n,arity,l):
        #accu = []
        #for (nm,a,vars) in self.body:
        #    if nm == n:
        #        print 'redefining previous', n 
        #    else:
        #        accu.append((nm,a,vars))
        #self.body = accu
        self.body.append((n,arity,l))

    def need_quoting(self,s):
        """
        Returns True if writing out the string as an smodels
        token/variable requires quotes around it.
        At first we check whether it has any 'special' characters
        in it.
        """
        res = not(s.isalnum())
        if res:
            # Check that it is not just underscore
            # coerce potenital unicode to str for translate to work 
            tmp = str(s)
            tmp = tmp.translate(None,'_')
            tmp = tmp.translate(None,'-')
            # tmp = s.translate('_')
            res = not(tmp.isalnum())
            # print '\t\t1', s, 'needs quoting:', res
        else:
            # print '\t\t1', s, 'is not alphanum'
            pass
        if s[0] == '"' and s[-1] == '"':
            res = False
        return res

    def smstr(self,s, dont_expand_in_quotes=False):
        if self.need_quoting(s):
            return '"' + s + '"'
        else:
            # Now we need to make the decision whether we expand 
            # or not. 
            if self.cmdln.is_lit(s):
                tmp = s[1:-1]
            else:
                tmp = s
            if dont_expand_in_quotes: 
                tmp = self.cmdln.mk_short(tmp, False)
            # print 'XXX smstr in', s, 'out', tmp
            if self.cmdln.is_lit(s):
                return '"' + tmp + '"'
            else:
                return tmp

    def to_str(self,short=True,v=False,smodel=False,dont_expand_in_quotes=False):
        buf = ''
        # print 'to_str, short:', short, 'v:', v, 'smodel:', smodel
        if not self.ready:
            return ('Rule ' + self.name + ' under definition')
        else:
            # (_,b) = self.expand_ns(a)
            b = self.cmdln.mk_short(self.name,False)
            if smodel:
                # b = '"' + b + '" '
                b = self.smstr(b,dont_expand_in_quotes)
            #print b
            buf = buf + b + ' ' # + '\n'

            if not short:
                tmp_arg = []
                for a in self.args:
                    if v: 
                        b = a
                        #print 'XXX 1', b
                    else: 
                        #print 'XXX 2', a
                        b = self.cmdln.mk_short(a, False)
                        # b = self.cmdln.mk_short(a, True)
                        #print 'XXX 2', b
                    if smodel:
                        #b = '"' + b + '"'
                        # Now check whether the stuff inside of quotes 
                        # needs 
                        b = self.smstr(b,dont_expand_in_quotes)
                        #print 'XXX 3', b
                    tmp_arg.append(b)
                    #print (','.join(tmp_arg)),  
                buf = buf + '(' + (','.join(tmp_arg)) + ')'
                # if self.type == 'bwd':
                mylen = len(self.body)
                if mylen > 0:
                    #print ' :-'
                    buf = buf + ' :-' + '\n'
                else:
                    buf = buf + '.\n'
                ctr = 1
                for (n,arity,l) in self.body:
                    if self.cmdln.is_var_uri(n):
                        b = self.cmdln.var_uri_name(n)
                    else:
                        if v:
                            b = n
                        else: 
                            b = self.cmdln.mk_short(n,False)
                            #print ctr, '  ', b, 

                        if smodel:
                            # b = '"'+ b + '"'
                            b = self.smstr(b,dont_expand_in_quotes)

                    buf = buf + '  ' + b + ' '

                    tmp_arg_2 = []
                    for j in l:
                        if self.cmdln.is_var_uri(j):
                            b = self.cmdln.var_uri_name(j)
                        else:
                            if v:
                                b = j
                            else:
                                b = self.cmdln.mk_short(j,False)
                            if smodel:
                                #b = '"' + b + '"'
                                b = self.smstr(b,dont_expand_in_quotes)

                        tmp_arg_2.append(b)
                        #print (','.join(tmp_arg)),  
                        #print b,',',
                        #print (','.join(tmp_arg_2))
                    buf = buf + '(' + (','.join(tmp_arg_2)) + ')' 
                    # Ugly way of detecting the last item 
                    if ctr == mylen:
                        buf = buf + '.\n' 
                    else:
                        buf = buf + ',\n' 
                    ctr = ctr + 1
            else:
                # print '/', self.arity, '(',self.type,')'
                # print '/', self.arity
                buf = buf + '/ ' + str(self.arity)
            return buf 

    def printr(self,short=True,v=False,smodel=False):
        print (self.to_str(short,v,smodel))


    def eval(self):
        if not self.ready:
            print self.name, "not fully defined yet - use 'end'"
            return 
        # print 'eval', self.body
        # self.eval_rec(self.body,[])
        print 'eval not implemented, use smls'

    def eval_rec(self,l,accu):
        """
        Not implemented yet for now refer to smls and smodels 
        integration. 
        """
        pass

class DefaultHandler:
    def __init__(self,cmdln,trid,silent=True):
        self.cmdln = cmdln
        self.subs_id = trid
        self.silent = silent

    def handle(self, added, removed):
        # We acquire the master lock of the shell
        # and release it at the end of this function. 
        # Only two return paths, save fatal exceptions, 
        # are in this function. (ret1,ret2)
        # This does not rely on executing another callback
        # even if the may call functions inside the interpreter. 
        # No other place in this code accesses this lock. 
        self.cmdln.cblock.acquire()

        if not self.silent:
            print "Subscription for", self.subs_id.tr_id
        try: 
            (rs,l,(ss,pp,oo),status,tobuf,shellcmd,loopback,quiet,ctr,nslist,rlist) = self.cmdln.subs[self.subs_id.tr_id]
        except:
            print 'Subscription error for key', self.subs_id.tr_id
            for err in self.cmdln.subs.keys():
                print '\t', err, ':', self.cmdln.subs[err]
            self.cmdln.cblock.release()
            return # ret1

        # First remove stuff 
        # print 'Remove'
        need_stopping = False
        for i in removed:
            #print "Removed:", str(i)
            ((s,p,o),x,y) = i 
            need_stopping = self.handle_expects(s,p,o,x,y,'-', need_stopping)
            ctr = ctr + 1
            if not self.silent:
                self.cmdln.render_t(s,p,o, '- ',x,y)
            if tobuf:
                if y:
                    oo = '"' + o + '"'
                else:
                    oo = o
                self.cmdln.add_to_hist(('d',(s,p,oo),(s,p,oo),None,x,y))

            #self.vals.remove_triple(i)
        # print 'Add'
        for i in added:
            #print "Added:", str(i)
            ((s,p,o),x,y) = i 
            need_stopping = self.handle_expects(s,p,o,x,y,'+', need_stopping)
            ctr = ctr + 1
            if not self.silent:
                self.cmdln.render_t(s,p,o,'+ ',x,y)
            #self.vals.add_triple(i)
            if tobuf:
                if y:
                    oo = '"' + o + '"'
                else:
                    oo = o
                self.cmdln.add_to_hist(('i',(s,p,oo),(s,p,oo),None,x,y))
        
        if tobuf and (len(removed) > 0 or len(added) > 0):
            self.cmdln.add_to_hist(('update',('','',''),('','',''),None,None,None))

        # We record the ascii time as well as the time in seconds
        latest = [(time.asctime(),time.time(),removed,added)]
        if not quiet: 
            l = l + latest
            # l.append((time.asctime(),removed,added))

        if not need_stopping: 
            # Only if expects haven't completed
            self.cmdln.subs[self.subs_id.tr_id] = (rs,l,(ss,pp,oo),status,tobuf,shellcmd,loopback,quiet,ctr,nslist,rlist)

        # Any subscription starting with smls will execute
        # smls later on.
        subname = self.cmdln.id_to_name(self.subs_id.tr_id)
        #print '---- ID', self.subs_id.tr_id, 'Name', subname 
        if subname != None and len(subname) > 3 and subname[0:4] == 'smls' and rlist == []:
            # XXX Will this deadlock?
            #     Should not as only another callback may be triggered
            #     and nothing in the interpreter loop accesses the lock. 
            # self.cmdln.do_smls('')
            smlscmd = ss + ',' + pp + ',' + oo
            # self.cmdln.do_smls(smlscmd)
            self.cmdln.onecmd('smls ' + smlscmd)
        elif subname != None and len(subname) > 3 and subname[0:4] == 'smls' and rlist != []:
            nsmlscmd = '['+ ','.join(nslist) + '] [' + ','.join(rlist) + '] ' + ss
            print 'nslscmd via sub:', nsmlscmd
            # self.cmdln.do_nsmls(nsmlscmd)
            self.cmdln.onecmd('nsmls ' + nsmlscmd)

            # Now after this we may have to iterate as this 
            # may trigger another nsmls. 

            # self.cmdln.onecmd('\n')
            # self.cmdln.postcmd(None,None) # Urgh?
        if shellcmd != '' and len(latest) > 0:
            b = []
            # Pass the latest history item to shell
            b = self.cmdln.sub_printer(b,latest,ctr)
            self.cmdln.pipe(b,shellcmd,loopback)

        self.cmdln.cblock.release() # ret2

    
    def check_expects(self,l,s,p,o,x,y):
        tmp = []
        found = False
        for (isdone,((ss,pp,oo),xx,yy)) in l:
            #print 'Checking incoming ', ss,pp,oo,xx,yy
            #print 'Checking against  ', s,p,o,x,y
            if isdone or found:
                tmp.append((isdone,((ss,pp,oo),xx,yy)))
            else:
                # if ((ss,pp,oo),xx,yy) == ((s,p,o),x,y):
                #print ('subj_i:' + ss + '-')
                #print ('subj_a:' + s + '-')
                #print
                #print ('pred_i:' + pp + '-')
                #print ('pred_a:' + p + '-')
                #print
                #print ('obje_i:' + oo + '-')
                #print ('obje_a:' + o + '-')

                if (ss,pp,oo) == (s,p,o):
                    # print 'Yes we matched ', ((s,p,o),x,y)
                    found = True
                    tmp.append((True,((ss,pp,oo),xx,yy)))
                else:
                    # tmp.append((isdone,((ss,pp,oo),xx,yy)))
                    tmp.append((False,((ss,pp,oo),xx,yy)))

        return (found,tmp)

    def handle_expects(self, s,p,o,x,y,op,need_stopping):
        for i in self.cmdln.expect_bufs.keys():
            (ok,to_add,to_rem,done,tobedone,noise,stop) = self.cmdln.expect_bufs[i]
            #print 'Looking at expect', i, 'OK?', ok
            #print to_add
            #print to_rem
            #print '---------------------'
            if ok:
                return need_stopping 
            else:
                found = False
                if op == '+':
                    (found,to_add) = self.check_expects(to_add,s,p,o,x,y)
                else:
                    (found,to_rem) = self.check_expects(to_rem,s,p,o,x,y)

                if found:
                    done = done + 1
                if done == tobedone:
                    ok = True
                    self.cmdln.do_subdel(i,addtohist=False)
                    need_stopping = True
                    if noise:
                        print 'Now to stop', i, 'vals',stop,need_stopping
                        self.cmdln.do_expects(i, verbose=False)
                    self.cmdln.expect_locks[i].release()
                    del(self.cmdln.expect_locks[i])

                self.cmdln.expect_bufs[i] = (ok,to_add,to_rem,done,tobedone,stop,noise)

            if stop and need_stopping:
                # Since we are at a callback sys.exit won't work. 
                # But needs reset from shell afterwards. 
                # os._exit(0)
                self.cmdln.stop_now = True



        return need_stopping

class Ss(cmd.Cmd):
    def __init__(self,node,handle,torun='',use_yp=True,just_exec=False):
        cmd.Cmd.__init__(self)
        self.use_yp = use_yp
        self._node = node
        self._handle = handle 
        self.nsp = { 'http://www.w3.org/2000/01/rdf-schema': 'rdfs', 'http://www.w3.org/1999/02/22-rdf-syntax-ns': 'rdf', 'http://www.m3.org/exp': 'exp', 'http://www.w3.org/2001/XMLSchema': 'xsd', 'http://www.w3.org/2002/07/owl': 'owl', 'http://www.nokia.com/NRC/M3/sib': 'sib'}
        self.just_exec = just_exec
        self.previous_cmd = None
        self.history = []
        self.nsnum = 0
        self.subs = dict()
        self.update_i = [] 
        self.update_d = [] 
        self.update_l = [] 
        self.views = {}
        self.expect_bufs = {}
        self.expect_locks = {}
        self.names = {}
        self.curr_name = None
        self.input_to_buf = False
        self.stop_now = False
        self.torun = torun
        self.error = ''
        self.cblock = threading.Lock()
        self.wql = []
        self.vars = {}
        self.rules = {}
        self.named_rules = {}
        self.rels = {}
        self.rule_under_def = None
        self.rulefiles = []
        self.timefile = None
        self.trigger_smls = False
        self.no_trigger_smls = False
        self.trigger_smls_cmd = ''
        self.trigger_nsmls = False
        self.trigger_nsmls_cmds = {}

    def add_to_hist(self,e):
        self.previous_cmd = e
        self.history.append(e)
        i = len(self.history)
        self.prompt = str(node.member_of) + " " + str(i) +" > "

    def modify_hist(self,e,idx):
        # print 'Modifying history at', idx, 'with', e
        self.history[idx] = e

    def postcmd(self,stop,line):
        """
        This exists for a callback to quit the program. 
        Needed by expect. 
        """
        if self.stop_now:
            print 
            sys.exit(0)

        # print 'postcmd trigger_smls, trigger_nsmls', self.trigger_smls, self.trigger_nsmls

        if self.trigger_smls:
            """
            This is only triggered from do_smls and by the 
            rules. They've set up a view and expect this 
            view to disappear
            """
            self.trigger_smls = False
            viewnm = self.trigger_smls_cmd
            print 'smls triggered by the rules', viewnm 
            # We need to prevent double smls execution
            self.no_trigger_smls = True
            self.do_smls(viewnm)
            self.do_viewdel(viewnm)
            self.no_trigger_smls = False

        if self.trigger_nsmls:
            """
            This is only triggered from do_nsmls and by the 
            rules. They've set up a view and expect this 
            view to disappear. 
            """
            self.trigger_nsmls = False
            #print '-------------------------'
            #print 'postcmd trigger_nsmls_cmds', len(self.trigger_nsmls_cmds.keys())
            #print self.trigger_nsmls_cmds
            for viewnm in self.trigger_nsmls_cmds.keys():
                nsmlscmd = self.trigger_nsmls_cmds[viewnm]
                print 'nsmls triggered by the rules', nsmlscmd
                # We need to prevent double nsmls execution
                self.no_trigger_nsmls = True
                self.do_nsmls(nsmlscmd)
                # Clean up the view for this one 
                self.do_viewdel(viewnm)
                self.no_trigger_nsmls = False

    def preloop(self):
        """
        This exists so that we can run a batch file. 
        """
        if self.torun != '':
            self.do_exec(self.torun)
            if self.error == '':
                if self.just_exec:
                    sys.exit(0)
            else:
                print 'Execution stopped due to error in last command'
                self.error = ''

    def expand(self,s,relative):
        if s == '*':
            return (s,'sib:any')
        if s == '$' and len(s) == 1:
            res = str(uuid.uuid4())
            self.lastid = res
            return ('$',res)
        if s == '$$':
            if self.lastid == None:
                 self.lastid = str(uuid.uuid4())
            return ('$$',self.lastid)
        if s[0] == '$':
            return self.expand_true_var(s[1:])
        if s[0] == '!': 
            return self.expand_var(s[1:],relative)
        # Remove the enclosing "s if any
        # Why, what would break?
        #if s[0] == '"' and s[-1] == '"':
        #    s = s[1:-1]
        return self.expand_ns(s)
    
    def expand_var(self,s,relative):
        (res,val) = self.get_h(s,relative)
        # print 'Expanding', '!',s, 'to', val
        return ('!' + s,val)

    def expand_true_var(self,s):
        (res,val) = self.get_h(s)
        # print 'Expanding', '$',s, 'to', val
        return ('!' + s,val)

    def is_var(self,s):
        if s[0] == '!' or s[0] == '$':
            return True
        else:
            return False

    def handle_history_args(self,str):
        """
        Takes in a string of of form a,b-c,d or just *
        where numbers delimited by commas indicate single
        history items and b-c indicate a range of history 
        items. 
        Returns a list of indices for the history list that 
        are to be repeated. 
        """
        last = 0
        res = []
        if str == '':
            print 'eval needs an argument'
            return None
        if str == '*':
            return range(len(self.history))
        items = str.split(',')
        for i in items:
            ii = i.split('-')
            if len(ii) == 2:
                mini = int(ii[0])
                maxi = int(ii[1])
                if mini > len(self.history):
                    print 'No such history item', repr(mini)
                if maxi >= len(self.history):
                    print 'No such history item', repr(maxi)
                if mini > maxi:
                    print 'Min of range larger than max', i
                    return None
                res = res + range(mini,maxi)
            elif len(ii) == 1:
                val = int(ii[0])
                if val >= len(self.history):
                    print 'No such history item', repr(val)
                    return None
                else:
                    res.append(val)
            else:
                print 'ranges only for two values, got:',i 
        return res

    def handle_opts(self,l,s,p,o):
        """
        Urgh, only handles one at the time. If you have more, its
        undefined.
        """
        verbose = False
        ins = False
        no_p = False
        quiet = False
        if l == []:
            pass
        for (i,j) in l:
            if i == '-c':
                if p != 'sib:any' or o != 'sib:any':
                    print 'Ignoring -c'
                else:
                    p = 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'
                    o = 'http://www.w3.org/2000/01/rdf-schema#Class'
            if i == '-t':
                if p != 'sib:any':
                    print 'Ignoring -t'
                else:
                    p = 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'
                    no_p = True
            if i == '-s':
                if p != 'sib:any':
                    print 'Ignoring -s'
                else:
                    p = 'http://www.w3.org/2000/01/rdf-schema#subClassOf'
            if i == '-p':
                if p != 'sib:any' or o != 'sib:any':
                    print 'Ignoring -p', p, o 
                else:
                    p = 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'
                    o = 'http://www.w3.org/1999/02/22-rdf-syntax-ns#Property'
            if i == '-v':
                verbose = True
                # break
            if i == '-i':
                ins = True
                # break
            if i == '-q':
                quiet = True
                # break
        return (s,p,o,verbose,ins,no_p,quiet)

    def parse_expect(self,args,allow_many=True):
        als = args.split()
        res = False
        stop = False
        noise = False
        try:
            optlist, a = getopt.getopt(als,'vs')
        except getopt.GetoptError(s):
            print s
            return (False,False,False)

        if len(a) > 1 and not allow_many:
            print 'expect needs one name only'
            return (False,False,False)

        if len(a) > 1:
            a = ' '.join(a)
        else:
            a = a[0]

        for (i,j) in optlist:
            if i == '-v':
                noise = True
            elif i == '-s':
                stop = True

        res = True
        return (res,a, stop,noise)

    def parse_wql(self,args,allow_any=True):
        res = False
        l = []
        ll = []
        if args == None:
            print 'need arguments'
            return (res,l,ll)
        al = args.split()
        for i in al: 
            (v,v_full) = self.expand(i)
            l.append(v)
            ll.append(v_full)
        return(True,l,ll)

    def parse_ls(self,args,allow_any=True,relative_expand=True):
        #print "Args -" + repr(args) + "-"
        s = 'sib:any'
        p = 'sib:any'
        o = 'sib:any'
        name = None
        orig_s = 'sib:any'
        orig_p = 'sib:any'
        orig_o = 'sib:any'
        piped = False
        loopback = False
        shell = []
        als = args.split()
        try:
            optlist, a = getopt.getopt(als,'ctpsviq')
        except getopt.GetoptError(s):
            print s
            return (False,('','',''),('','',''),False,'',False)
        
        (s,p,o,verbose,hist,nop,quiet) = self.handle_opts(optlist,s,p,o)
        #self.input_to_buf = hist
        # aa = self.patch_quotes(a)
        tmp = ' '.join(a)
        aa = [tmp]
        # print optlist
        if len(aa) > 1:
            print 'Only one argument allowed, got:', a
            return (False,(s,p,o),(s,p,o),verbose,hist,'',False,False)
        if len(aa) > 0 and aa[0] != '':
            spo = aa[0].split(',')
            #print 'Still', spo
            #spo = aa.split(',')
            # A smarter way for this?
            if len(spo) == 3:
                # Check that last argument for shell commands 
                (piped,my_o,shell,loopback) = self.get_piped(spo[2])
                (orig_s,s) = self.expand(spo[0],relative_expand)
                if not nop:
                    (orig_p,p) = self.expand(spo[1],relative_expand)
                (orig_o,o) = self.expand(my_o,relative_expand)
                #print 'Then', s,p,o
            elif len(spo) == 2: 
                (piped,my_o,shell,loopback) = self.get_piped(spo[1])
                (orig_s,s) = self.expand(spo[0],relative_expand)
                if not nop:
                    (orig_p,p) = self.expand(my_o,relative_expand)
                else:
                    (orig_o,o) = self.expand(my_o,relative_expand)
            elif len(spo) == 1: 
                (piped,my_o,shell,loopback) = self.get_piped(spo[0])
                # Check for an empty shell command 
                if piped and shell == []:
                    return (False,(s,p,o),(s,p,o),False,hist,'',False,False)
                # print 'What now',my_o
                if my_o != '':
                    if not nop:
                        (orig_s,s) = self.expand(my_o,relative_expand)
                    else:
                        (orig_o,o) = self.expand(my_o,relative_expand)
            elif len(spo) > 3:
                print 'Only subject,predicate,object allowed, got:', spo
                return (False,(s,p,o),(s,p,o),False,hist,'',False,False)

        bd = 'sib:any'
        if (s == bd or p == bd or o == bd) and (not allow_any):
            print 'Not allowed to use wildcards here (sib:any or *), you gave', orig_s,orig_p,orig_o
            return (False,(s,p,o),(s,p,o),False,hist,'',False,False)
        shells = ' '.join(shell)
        return (True,(s,p,o),(orig_s,orig_p,orig_o),verbose,hist,shells,loopback,quiet)

    def get_piped(self, s):
        """
        Search for pipe character in s and return the 
        string after that to be fed to shell. 
        Make sure that pipe is not inside quotes. 
        Also look for # the shell comment followed by 
        ssls, signalling ssls to read in the results. 
        """
        res = False
        loopback = False
        nonshell = s
        sl = shlex.split(s)
        try:
            idx = sl.index('|')
            shell = sl[idx + 1:]
            nonshell = sl[:idx]
            nonshell = ' '.join(nonshell)
            if shell == []:
                print 'Must have a shell command following |'
                res = True
            else:
                res = True
                # print 'Shell command', shell

                # Next try for the hash 
                tmpshell = ' '.join(shell)
                sll = shlex.split(tmpshell)
                try:
                    cidx = sll.index('#')
                    loop = sll[cidx + 1:]
                    nushell = sll[:cidx]
                    if loop[0] == 'ssls':
                        # print 'loopback!'
                        # print 'Now?', nushell
                        loopback = True
                        shell = nushell
                    else:
                        print 'Only directive ssls interpreted after #'
                except:
                    # No changes, loopback is false
                    pass
        except:
            shell = []

        return (res,nonshell,shell,loopback)

    def help_i(self):
        print 'i subj,pred,obj'
        print '   adds a triple to update buffer, like ins which is not committed yet.'
        print '   Use command u to view the update buffer and command d to add triples'
        print '   to be deleted in the buffer.'
        print '   Commit the buffer with command update. This command empties the buffer.'

    def help_d(self):
        print 'd subj,pred,obj'
        print '   adds a triple to update buffer, like del which is not committed yet.'
        print '   Use command u to view the update buffer and command i to add triples'
        print '   to be added in the buffer.'
        print '   Commit the buffer with command update. This command empties the buffer.'

    def help_l(self):
        print 'l [subj[,pred[,obj]]'
        print '   adds a query triple to view buffer.'
        print '   Each query triple is like a command for the ls command.'
        print '   Use command v to view the triples in the view buffer.'
        print '   Make a view for all the triples in the buffer by view command.'
        print '   view command empties the view buffer.'

    def help_view(self):
        print 'view name'
        print '   creates a named view which can be an argument to ls or sub'
        print '   command and other similar commands (viz, file).'
        print '   Use command v to view the triples in the view buffer.'
        print '   Use command l to add query triples to the view buffer.'
        print '   Use command views to see the defined views.'
        print '   Use command viewdel remove views.'
        print '   This command empties the view buffer.'

    def help_views(self):
        print 'views [name]'
        print '   lists the views that have been created by view command.'

    def help_viewdel(self):
        print 'viewdel name'
        print '   deletes a named view that have been created by view command.'

    def help_u(self):
        print 'u'
        print '   Shows the triples that are to be updated with the next update command.'
        print '   Use commands i and d to add triples to the buffer.'
        print '   Commit the buffer with command update. This command empties the buffer.'

    def help_update(self): 
        print 'update'
        print '   Commits the update buffer to SIB.'
        print '   Use commands i and d to add triples to the buffer and u to view the contents'
        print '   of the buffer.'
        print '   This command empties the buffer.'

    def help_expect(self): 
        print 'expect [-vs] name'
        print '   Waits for fulfillment of the update buffer.'
        print '   Starts a *,*,* subscription called name.'
        print '   Each change is compared to triples in buffer.'
        print '   Use commands i and d to add triples to the buffer and u to view the contents'
        print '   -v causes an indication to be printed when the expect is fulfilled'
        print '   -s causes the ssls to quit and print the expected triples when'
        print '      the expect is fulfilled'
        print '   Use expects command to view active expects.'
        print '   Also see the i, d, u, update and wait commands.'
        print '   This command empties the update buffer.'

    def help_expects(self): 
        print 'expects [name]'
        print '   Lists the status of expects. More details available by specifying'
        print '   a single name.'

    def help_wait(self): 
        print 'wait [name[ name2 [name 3...]]]'
        print '   Blocks until all named expects have been fulfilled.'
        print '   If no name is given waits for user pressing enter.'
        print '   Also see the expect command.'


    def do_save(self,a,v='$Revision: 1.66 $'):
        i = 0
        if a == '':
            print 'Need a filename for save'
            return 
        try:
            f = open(a,'w')
            f.write('ssls ' + v + '\n')
            f.write('b---\n')
            for (cmd, (s,p,o),(ss,pp,oo),id) in self.history:
                cmds = self.hist_to_str((cmd, (s,p,o),(ss,pp,oo),id,None,None), True,True) # XXXX
                f.write( str(i) + ':' + 'a' + ': ' + cmds + '\n')
                i = i + 1
            f.write('e---\n')
            f.close()
        except IOError:
            print 'Could not save to file', a, ':', sys.exc_info()[0]

    def exec_stdin(self):
        while (1):
            i = sys.stdin.readline()
            if not i:
                break
            if i[0] != '#':
                # print 'reading', i
                self.onecmd(i)
            

    def do_exec(self,a,v=''):
        i = 0
        if a == '':
            print 'Need a filename for exec'
            return 
        try:
            if a == '-':
                # it's stdin
                check_header = False
                self.exec_stdin()
                return 
            else:
                f = open(a,'r')
                # print 'Loading history from', a, f
                hist = f.readlines()
                f.close()
        except IOError:
            print 'File', a, 'not found'
            return 
        print 'Exec read', len(hist), 'lines'

        header = hist[0]
        if header.split()[0] != 'ssls':
            print 'Aborting, Cannot recognize file header:', header
            return 
        rev = header.split('$')[1]
        if v != '' and v != rev:
            print 'Aborting, Expected version', v, 'got ', rev
            return 
        in_content = False
        payload = hist[1:]

        # Reset the old history 
        self.history = []
        for i in payload:
            # print i 
            if i[-2:] == '\r\n':
                # print 'converting crlf'
                i = i[:-2]
                i = i + '\n'
                
            if self.error != '':
                # print 'ERROR', self.error
                break
            if i[0:5] == 'b---\n': # or i[0:5] == 'b---\r':
                in_content = True
            elif i[0:5] == 'e---\n': # or i[0:5] == 'e---\r':
                in_content = False
            else:
                if in_content:
                    if i[0] != '#':
                        #print 'reading', i
                        h = i.split(':')
                        if len(h) < 3:
                            print 'Skipping malformed line', i
                        else:
                            idx = int(h[0])
                            direction = h[1]
                            # Reconstruct the command 
                            # If no items past the two above this returns ''
                            cmdargs = ':'.join(h[2:])
                            cmdargs = cmdargs.strip()
                            print cmdargs
                            # Now do the command 
                            # print 'Doing', cmdargs 
                            self.onecmd(cmdargs)

    def do_load(self,a,v=''):
        i = 0
        if a == '':
            print 'Need a filename for load'
            return 
        try:
            f = open(a,'r')
            #print 'Loading history from', a, f
            hist = f.readlines()
        except IOError:
            print 'File', a, 'not found'
            return 
        f.close()
        # print 'Print read', len(hist), 'lines'
        # Check the first line 
        header = hist[0]
        if header.split()[0] != 'ssls':
            print 'Aborting, Cannot recognize file header:', rev
            return 
        rev = header.split('$')[1]
        if v != '' and v != rev:
            print 'Aborting, Expected version', v, 'got ', rev
            return 
        in_content = False
        # Reset the old history 
        self.history = []
        for i in hist[1:]:
            if i == 'b---\n':
                in_content = True
            elif i == 'e---\n':
                in_content = False
            else:
                if in_content:
                    #print 'reading', i
                    if i[0] != '#':
                        h = i.split(':')
                        if len(h) < 3:
                            print 'Skipping malformed line', i
                        else:
                            idx = int(h[0])
                            direction = h[1]
                            # Reconstruct the command 
                            # If no items past the two above this returns ''
                            cmdargs = ':'.join(h[2:])
                            cmds = cmdargs.split()
                            cmd = cmds[0]
                            args = ' '.join(cmds[1:])
                            # And now get the arguments 
                            # print 'loading', cmd, args
                            if cmd == 'expect' or cmd == 'wait' or cmd == 'redo' or cmd == 'unsub' or cmd == 'view':
                                if args != '':
                                    (res, a, stop, noise) = self.parse_expect(args)
                                else:
                                    stop = False
                                    noise = False
                                    res = True
                                    a = ''
                                if stop:
                                    s = '-s'
                                    ss = '-s'
                                if noise:
                                    p = '-v'
                                    pp = '-v'
                                # print 'expect args', a
                                tmp = a 
                                s,p,o = '','',''
                            elif (cmd == 'inv' or cmd == 'seq'):
                                (res, s,ss) = self.parse_wql(args)
                                print 's', s, 'ss',s
                                # self.add_to_hist((cmd,(l,'',''),(l,'',''),l,None,None))
                                tmp = None
                            elif cmd == 'add_ns':
                                als = args.split(' ')
                                s,p,o = '','',''
                                ss,pp,oo = '','',''
                                # tmp = [als[0],als[1]]
                                if len(als) != 2:
                                    res = False
                                else:
                                    res = True
                                    tmp = als
                            else:
                                # print 'parsing', args
                                (res, (s,p,o),(ss,pp,oo),verbose,tobuf,_,_,_) = self.parse_ls(args,True,False)
                                tmp = None
                            if not res:
                                print 'Ignoring malformed command', i
                            else:
                                # print 'Adding', (cmd,(s,p,o),(ss,pp,oo),None)
                                self.add_to_hist((cmd,(s,p,o),(ss,pp,oo),tmp,None,None)) # XXXX


    def do_i(self,a):
        (res, (s,p,o),(ss,pp,oo),verbose,tobuf,_,_,_) = self.parse_ls(a)
        self.add_to_hist(('i',(s,p,o),(ss,pp,oo),None,None,None)) # XXXX
        if not res:
            return 
        if self.is_lit(o):
            l = [((s,p,self.delit(o)),'uri','literal')]
        else:
            l = [((s,p,o),'uri','uri')]
        self.update_i = self.update_i + l

    def do_d(self,a):
        # (res, (s,p,o),(ss,pp,oo),verbose,tobuf) = self.parse_ls(a,False)
        (res, (s,p,o),(ss,pp,oo),verbose,tobuf,_,_,_) = self.parse_ls(a)
        self.add_to_hist(('d',(s,p,o),(ss,pp,oo),None)) # XXXX
        if not res:
            return 
        #print 'do_d: ', s,p,o, '=', ss,pp,oo
        if self.is_lit(o):
            l = [((s,p,self.delit(o)),'uri','literal')]
        else:
            l = [((s,p,o),'uri','uri')]
        self.update_d = self.update_d + l

    def do_l(self,a):
        (res, (s,p,o),(ss,pp,oo),verbose,tobuf,_,_,_) = self.parse_ls(a)
        self.add_to_hist(('l',(s,p,o),(ss,pp,oo),None)) # XXXX
        if not res:
            return 
        #print 'do_d: ', s,p,o, '=', ss,pp,oo
        if self.is_lit(o):
            l = [((s,p,self.delit(o)),'uri','literal')]
        else:
            l = [((s,p,o),'uri','uri')]
        self.update_l = self.update_l + l

    def do_u(self,a):
        for ((ss,pp,oo),x,y) in self.update_i:
            self.render_t(ss,pp,oo, '+ ',x,y)
        for ((ss,pp,oo),x,y) in self.update_d:
            self.render_t(ss,pp,oo, '- ',x,y)
        for ((ss,pp,oo),x,y) in self.update_l:
            self.render_t(ss,pp,oo, '< ',x,y)

    def do_v(self,a):
        for ((ss,pp,oo),x,y) in self.update_l:
            self.render_t(ss,pp,oo, '- ',x,y)


    def do_u_res(self,a):
        self.update_i = []
        self.update_d = []

    def do_wql_res(self,a):
        self.wql = []

    def do_update(self,a):
        if len(self.update_d) == 0 and len(self.update_i) == 0:
            print 'No updates in buffer, use i and d to fill the buffer'
            return 
        self.add_to_hist(('update',('','',''),('','',''),None)) # XXXX 
        ui = self.preprocess_triples('i', self.update_i)
        ud = self.preprocess_triples('d', self.update_d)

        # a.update(self.update_i, "RDF-M3", self.update_d, "RDF-M3", confirm = True)
        if len(ud) != 0 or len(ui) != 0:
            a = self._node.CreateUpdateTransaction(self._handle)
            a.update(ui, "RDF-M3", ud, "RDF-M3", confirm = True)
            self._node.CloseUpdateTransaction(a)
        self.update_i = []
        self.update_d = []

    def do_view(self,a):
        if len(self.update_l) == 0:
            print 'No queries in view buffer, use l to fill the buffer'
            return 
        self.add_to_hist(('view',('','',''),('','',''),a,None,None))

        # Get the first space delimited entry 
        r = a.find(' ')
        if r < 0 and len(a) == 0:
            print 'view expects the first argument to be the name always'
            return
        elif r < 0 and len(a) > 0:
            name = a
            a = ''
        else:
            name = a[0:r]
            a = a[r+1:]

        if self.views.has_key(name):
            print 'view', name, 'already existing, not defined'
            return 

        self.views[name] = self.update_l
        self.update_l = []

    def do_views(self,a):
        print 'Views'
        for i in self.views.keys():
            if i==a or a == '':
                l = self.views[i]
                print i, ':'
                for ((ss,pp,oo),x,y) in l:
                    self.render_t(ss,pp,oo, '< ',x,y)

    def do_viewdel(self,a,addtohist=True):
        found = False
        for i in self.views.keys():
            if i == a:
                del self.views[i]
                found = True
                break
        if not found:
            print 'No such view', a


    def do_resetnmrules(self,a):
        if a == '':
            print 'resetnmrules needs one argument, the scope'
            return 
        if self.named_rules.has_key(a):
            del self.named_rules[a]
        else:
            print 'resetnmrules: No such scope', a



    def do_expect(self,a):
        if len(self.update_d) == 0 and len(self.update_i) == 0:
            print 'No content in buffer, use i and d to fill the buffer'
            return 

        (res,a,stop,noise) = self.parse_expect(a)

        if not res:
            # error printed by options parser
            return 

        if self.expect_bufs.has_key(a):
            print 'Expect named', a, 'already exists'
            return 

        if self.get_name(a) != None:
            print 'name', a, 'already existing as subscription'
            return

        # Create a related subscription 
        self.do_sub((a + ' *,*,*'), addtohist=False)
        if noise:
            my_v = '-v'
        else:
            my_v = ''
        if stop:
            my_s = '-s'
        else:
            my_s = ''
        self.add_to_hist(('expect',(my_v,my_s,''),(my_v,my_s,''),a,None,None))
        to_add = []
        to_rem = []
        for i in self.update_i:
            to_add.append((False,i))
        for i in self.update_d:
            to_rem.append((False,i))
        # (is_fulfilled, to_ins,to_del,progress,togo)
        lock = threading.Lock()
        lock.acquire(False)
        self.expect_locks[a] = lock 
        self.expect_bufs[a] = (False,to_add,to_rem,0,len(to_add) + len (to_rem),noise,stop)
        self.update_i = []
        self.update_d = []

    def help_sleep(self,a):
        print 'sleep seconds'
        print '      Waits for the amount of "seconds", which can be floating point number'
    def do_sleep(self,a):
        names = a.split(' ')

        if len(names) != 1 or a == '': 
            print 'sleep needs one argument: time in seconds'
            return 

        st = float(names[0])
        time.sleep(st)

    def do_print(self,a):
        print a

    def do_wait(self,a):
        names = a.split(' ')

        if len(names) == 1 and names[0] == '':
            # raw_input('waiting for keypress:')
            sys.stdout.write('waiting for user input -- press enter ')
            sys.stdin.readline()
            return 

        for i in names:
            if not self.expect_bufs.has_key(i):
                print 'No such expect:', i
                return 

        self.add_to_hist(('wait',('','',''),('','',''),a,None,None))

        for i in names:
            (ok,to_add,to_rem,done,tobedone,_,_) = self.expect_bufs[i]
            if ok:
                # It was already done 
                print 'expect', i, 'ready'
                break
            else:
                # We know that 
                self.expect_locks[i].acquire()
                print 'expect', i, 'ready'

        return 
                


    def do_expects(self,a,verbose=True):
        if verbose:
            print 'Expects'
        for i in self.expect_bufs.keys():
            if i==a or a == '':
                (ok,to_add,to_rem,done,tobedone,_,_) = self.expect_bufs[i]
                n = ''
                if verbose:
                    print i, ':', done, '/', tobedone
                else:
                    print '\n', i, ': completed'
                # return 
                if a != '':
                    ctr = 0
                    for (tbd,((s,p,o),x,y)) in to_add:
                        if tbd:
                            myok = 'OK'
                        else:
                            myok = 'NOK'
                        if not verbose:
                            myok = ''
                                   
                        pre = str(ctr) + ' ' + myok + ' : +'
                        self.render_t(s,p,o, pre,x,y)
                        ctr = ctr + 1 

                    for (tbd,((s,p,o),x,y)) in to_rem:
                        if tbd:
                            myok = 'OK'
                        else:
                            myok = 'NOK'
                        if not verbose:
                            myok = ''
                                   
                        pre = str(ctr) + ' ' + myok + ' : -'
                        self.render_t(s,p,o, pre,x,y)
                        ctr = ctr + 1 
                        
    def do_expectdel(self,a,addtohist=True):
        found = False
        for i in self.expect_bufs.keys():
            if i == a:
                del self.expect_bufs[i]
                found = True
                self.do_subdel(a,addtohist=False)
                break
        if not found:
            print 'No such expect', a


    def help_ls(self):
        print 'ls [-ctpsvi] [viewname][subj[,pred[,obj]]] [| shellcmds]'
        print '   performs a query to the smart space, if no value given for subj,pred,obj'
        print '   it is assumed to be * (sib:any).'
        print '   If the first non-switch argument, viewname, matches with an existing'
        print '   view (as defined by view command), those triples will be'
        print '   used instead.'
        print '   Results can be piped to arbitrary shell commands by having'
        print '   the commandline followed by "|" and the string to pass to shell.'
        print '   If the shell command has a comment #ssls, the results of the shell'
        print '   command are parsed for insertions and deletions.'
        print '   The quotation of quotes needs care, see the example below.'
        print '   -v expands namespaces fully, i.e URIs are complete'
        print '   -c shows all classes, shorthand for *,rdf:type,rdfs:Class'
        print '   -p shows all classes, shorthand for *,rdf:type,rdf:Property'
        print '   -t shows all types, shorthand for *,rdf:type,*'
        print '   -s shows all subclasses, shorthand for *,rdfs:subClassOf,*'
        print '   -i adds the items to history as inserts'
        print '   Examples:'
        print '   ls              = ls *,*,*'
        print '   ls rdf:Property = ls rdf:Property,*,*'
        print '   ls *,rdf:type   = ls *,rdf:type,*'
        print '   ls | less       = ls *,*,* | less'
        print '   ls | grep \\\\\\"foo   = ls *,*,* | grep \\"foo'

    def help_one(self):
        print 'one [-ctpsvi] [subj[,pred[,obj]]]'
        print '   performs a query to the smart space and makes the user choose one of'
        print '   The resulting triples, which is inserted in history.'
        print '   Similar to ls command.'
        print '   If no value given for subj,pred,obj it is assumed to be * (sib:any).'
        print '   -v expands namespaces fully, i.e URIs are complete'
        print '   -c shows all classes, shorthand for *,rdf:type,rdfs:Class'
        print '   -p shows all classes, shorthand for *,rdf:type,rdf:Property'
        print '   -t shows all types, shorthand for *,rdf:type,*'
        print '   -s shows all subclasses, shorthand for *,rdfs:subClassOf,*'
        print '   Examples:'
        print '   one              = one *,*,*'
        print '   one rdf:Property = one rdf:Property,*,*'
        print '   one *,rdf:type   = one *,rdf:type,*'


    def help_load(self):
        print 'load filename'
        print '   loads commands from file "filename" and replace the current'
        print '   history with these commands without executing them.'
        print '   Use exec to execute a file.'
        print '   Use save to save a history to file that can be loaded.'
        print '   Use redo to execute commands in history.'

    def help_exec(self):
        print 'exec filename'
        print '   loads and executes commands from file "filename"'
        print '   replacing the current history.'
        print '   Use save to save a history to file that can be executed.'
        print '   Use load to load commands from a file without'
        print '   executing them.'

    def help_save(self):
        print 'save filename'
        print '   saves the current history to "filename".'
        print '   Use load or exec to load commands from this file.'
        print '   Use "file" command to dump contents of the SIB to a file.'

    def do_one(self, a, buffer=None,hist=None):
        (res, (s,p,o),(ss,pp,oo),verbose,tobuf,_,_,_) = self.parse_ls(a)

        if not res:
            return 
        a = self._node.CreateQueryTransaction(self._handle)
        l = [((s,p,o),'uri','uri')]
        res = a.rdf_query(l,return_subj_type=True)
        self._node.CloseQueryTransaction(a)
        #res = self.preprocess_result(res)
        if len(res) > 0:
            ((s,p,o),(ss,pp,oo)) = self.select_one(res)
            #print '\t adding to hist',s,p,o
            # Horrible kludge in case we are redoing a one
            if hist != None:
                # We expect to modify the history item at hist
                self.modify_hist(('ins',(s,p,o),(ss,pp,oo),None),hist)
            # We add this command to current history anyway 
            self.add_to_hist(('ins',(s,p,o),(ss,pp,oo),None,None,None)) # XXXX
            # print 'latest in history', self.history[-1]
        else:
            print 'No result'
            self.error = 'one: no result'
            return 

    def select_one(self,l):
        ctr = 0
        tmp = []
        print
        for ((s,p,o),x,y) in l:
            pre = str(ctr) + '::'
            self.render_t(s,p,o,pre,x,y,True,None)
            tmp.append(((s,p,o),(s,p,o)))
            ctr = ctr + 1
            
        ok = False
        if len(tmp) == 1:
            ok = True
            res = tmp[0]
        while not ok:
            print
            sys.stdout.write('pick one number: ')
            idx = sys.stdin.readline()
            try:
                r = int(idx)
                if r < len(tmp):
                    res = tmp[r]
                    ok = True
                    break
            except:
                print sys.exc_info()[0]
                pass
        return res 

    def ls_common(self,a,cmdname):
        l = []

        cmd = ''
        # We need to do this first to get the switches
        (res, (s,p,o),(ss,pp,oo),verbose,tobuf,cmd,loopback,quiet) = self.parse_ls(a)
        if not res:
            return ([],None,None,cmd,quiet)
        # First check whether update_l buffer 
        # has stuff in it and if it has, use that 
        # instead of commandline parsed triple.
        if len(self.update_l) > 0:
            for ((s,p,o),x,y) in self.update_l:
                l.append(((s,p,o),'uri','uri'))
            # l = self.update_l
            self.update_l = []
        else:
            # Check whether the first argument was 
            # a name of a view.
            if self.views.has_key(s):
                for ((s,p,o),x,y) in self.views[s]:
                    # l.append(((s,p,o),'uri','uri'))
                    l.append(((s,p,o),x,y))
            else:
                #self.previous_cmd = ('ls',(s,p,o),(ss,pp,oo))
                l = [((s,p,o),'uri','uri')]

        l = self.preprocess_triples(cmdname, l)
        
        # Now go through the triples and if there's a single 
        # triple of sib:bogus, sib:bogus, sib:bogus
        # then do not communicate to SIB. 
        l = self.detect_bogus(l)
        res = []
        if len(l) > 0: 
            a = self._node.CreateQueryTransaction(self._handle)
            res = a.rdf_query(l,return_subj_type=True)
            self._node.CloseQueryTransaction(a)

        return (res,verbose,tobuf,cmd,loopback,quiet)

        

    def do_ls(self, a, buffer=None):
        # print 'my ls', a

        (res,verbose,tobuf,shcmd,loopback,_) = self.ls_common(a,'ls')

        if shcmd != '':
            # This will cause render_t to put everything in this buffer
            buffer = []

        for ((s,p,o),x,y) in res:
            self.render_t(s,p,o,'',x,y,verbose,buffer) # XXXX
            #print s,p,o
            if y:
                oo = '"' + o + '"'
            else:
                oo = o
            #if self.input_to_buf:
            if tobuf:
                self.add_to_hist(('ins',(s,p,oo),(s,p,oo),None,x,y))
            
        if shcmd != '':
            self.pipe(buffer,shcmd,loopback)

    def build_tree(self,current,path,depth,deny,allow,back=False):
        """
        We return a list of lists starting from 'node'
        [[(node,p,o),(node,p,o')],[(o,p,o''),(o,p,o'''),(o',p,o''''),...]]
        But this is the rendered list. We should build a tree. 
        [((node,p,o),[((o,p,o''),[]),((o,p,o'''),[])])]
        """

        #print 'build_tree'
        #print '\t current', current 
        #print '\t path', path
        #print '\t depth', depth
        if depth == 0:
            return []

        if back:
            l = [(('sib:any','sib:any',current),'uri','uri')]
        else:
            if self.is_lit(current):
                return []
            l = [((current,'sib:any','sib:any'),'uri','uri')]
        l = self.preprocess_triples('ls', l)
        # print '\querying for', l
        if len(l) > 0: 
            a = self._node.CreateQueryTransaction(self._handle)
            res = a.rdf_query(l,return_subj_type=True)
            self._node.CloseQueryTransaction(a)

        next = []
        for ((s,p,o),x,y) in res:
            # For each triple in result, we create 
            # another
            # Fill in this triple to the tree
            tmpres = ((s,p,o),x,y)
            children = []
            if back:
                ptochk = s
            else:
                ptochk = o

            # print '\tChecking', ptochk

            if self.not_in_result(path,ptochk) and (not self.denied(p,deny,allow)):
                # Add to the path this triple 
                tmppath = path[:]
                tmppath.append(((s,p,o),x,y))
                # branch = self.build_tree(o,tmppath, (depth - 1),deny,allow)
                branch = self.build_tree(ptochk,tmppath, (depth - 1),deny,allow)
                children = children + branch
            next.append((tmpres,children))

        # now next contains the tree to be returned
        # for each new entry, we return it
        return next
                                               

    def not_in_result(self,r,c):
        for ((s,p,o),x,y) in r:
            if c == s:
                return False
            #if c == o:
            #    return False
        return True

    def denied(self,p,deny,allow):
        for i in deny:
            # print '\tComparing', p, 'to denial', i
            if i == p:
                return True
        return False

    def recls_closure(self,arg,depth,verbose,deny,allow,back):
        """
        Returns the buffer associated with the recls command.
        """
        # Construct the triples for the first query
        # l = [((arg,'sib:any','sib:any'),'uri','uri')]
        # res = self.transitive_query(l,[], depth,deny,allow)
        res = self.build_tree(arg,[], depth,deny,allow,back)               
        # print res
        return ([],res)

        buffer = []
        ares = []
        for i in res:
            for ((s,p,o),x,y) in i:
                ares.append(((s,p,o),x,y))
                self.render_t(s,p,o,'',x,y,verbose,buffer)

        return (buffer,ares)

    def do_bls(self, a, buffer=None):
        self.do_fls(a,buffer,True)

    def do_fls(self, a, buffer=None, back=False):

        if back:
            cmdname = 'bls'
        else:
            cmdname = 'fls'
        r = a.find(' ')
        (piped,a,shell,loopback) = self.get_piped(a)
        # print 'a',a, 'shell', shell
        verbose = False
        shcmd = ''
        if r < 0 and len(a) == 0:
            print cmdname + ': No arguments'
            # print 'Syntax: fls depth [-p pred1,pred2,] [subj|pred|obj]'
            print 'Syntax:', cmdname, 'depth [subj|pred|obj]'
            print 'Where depth is the length of the chain'
            return
        elif r < 0 and len(a) > 0:
            try:
                depth = int(a)
                arg = 'sib:any'
            except:
                print 'fls: depth must be an integer'
                print 'Syntax: recls depth [subj|pred|obj]'
                print 'Where depth is the length of the chain'
                return 
        else:
            try:
                depth = int(a[0:r])
                arg = a[r+1:]
            except:
                print 'fls: depth must be an integer'
                print 'Syntax: recls depth [subj|pred|obj]'
                print 'Where depth is the length of the chain'
                return 
        
        deny = ['http://www.w3.org/1999/02/22-rdf-syntax-ns#type','http://www.w3.org/2000/01/rdf-schema#subClassOf']
        allow = []
        (buffer,res) = self.recls_closure(arg,depth,verbose,deny,allow,back)

        tmp = self.tree_to_triples(buffer,res)
        mybuf = []
        for ((s,p,o),x,y) in tmp:
            # for ((s,p,o),x,y) in i:
            self.render_t(s,p,o,'',x,y,verbose,mybuf)

        if piped:
            shcmd = ' '.join(shell)
            if shcmd == 'viz':
                self.feed_to_viz(tmp,verbose)
            else:
                self.pipe(mybuf,shcmd,loopback)
        else:
            for i in mybuf:
                 print i
            

    def tree_to_triples(self,b,t):
        tmp = []
        for (((s,p,o),x,y),rest) in t:
            #print '\t appending ', ((s,p,o),x,y) ,'\n'
            tmp.append(((s,p,o),x,y))
            # tmp.append(self.tree_to_triples(b,rest))
            tmp = tmp + self.tree_to_triples(b,rest)
            #print '\t tree now\n', tmp
            #print '\t --------------------------------\n'
            # for (t,rest) in res:
        return tmp

    def help_fls(self):
        print 'fls depth uri'
        print '   starts a transitive traversal of the graph starting'
        print '   from node "uri" up to "depth" steps.'
        print '   Hardcoded not to continue after rdf:type or rdfs:subClassOf.'
        print '   If "depth" is -1, the traversal prints out all the'
        print '   triples reachable from "uri".'

            
    def pipe(self,buffer,shell,loopback):
        """
        Takes in 'buffer' a list of strings, dumps them 
        to a tmp file and pipes that to the shell command 'shell'.
        """
        f = tempfile.NamedTemporaryFile(prefix='ssls-out-',delete=False)
        for i in buffer:
            f.write(i+'\n')
        f.close()
        # Now run the command line 
        if loopback:
            f2 = tempfile.NamedTemporaryFile(prefix='ssls-results',delete=False)
            f2.close()
            cmd = 'cat ' + f.name + ' | ' + shell + '>' + f2.name
        else:
            cmd = 'cat ' + f.name + ' | ' + shell
        print cmd
        res = os.system(cmd)
        if res == 0 and loopback:
            (update_needed,nsmls,smls) = self.parse_ext_res(f2.name)
            if update_needed:
                self.do_update('')

        # os.unlink(f2.name)
        # Delete it now 
        os.unlink(f.name)

    def do_rls(self, a, buffer=None):
        # The rule ls, inserts everything as relations 
        if not self.use_yp:
            print 'Rules not usable'
            return 
        (res, (s,p,o),(ss,pp,oo),verbose,tobuf,_,_,_) = self.parse_ls(a)
        #self.previous_cmd = ('ls',(s,p,o),(ss,pp,oo))
        self.add_to_hist(('rls',(s,p,o),(ss,pp,oo),None,None,None))
        if not res:
            return 
        l = [((s,p,o),'uri','uri')]
        l = self.preprocess_triples('rls', l)
        res = []
        if len(l) > 0: 
            a = self._node.CreateQueryTransaction(self._handle)
            res = a.rdf_query(l,return_subj_type=True)
            self._node.CloseQueryTransaction(a)

        for ((s,p,o),x,y) in res:
            self.render_t(s,p,o,'',x,y,verbose,buffer)
            self.make_rel(s,p,o,'',x,y,verbose,buffer)
            #print s,p,o
            if y:
                oo = '"' + o + '"'
            else:
                oo = o
            # Now make a dynamic YP rulebase for this

    def do_dumprules(self,a):
        r = a.find(' ')
        if r < 0 and len(a) == 0:
            print 'dumprules expects the first argument to be the name always'
            return
        elif r < 0 and len(a) > 0:
            name = a
            a = ''
        else:
            name = a[0:r]
            a = a[r+1:]

        # Now check the file 
        try:
            f = open(name,'w')
        except IOError:
            print 'Could not write to file', name, ':', sys.exc_info()[0]
        
        # Now dump the rules there 
        for i in self.rules.keys():
            r = self.rules[i]
            for j in r:
                tmp = ''
                tmp = j.to_str(short=False,v=verbose,smodel=True,dont_expand_in_quotes=True)
                f.write(tmp+'\n')
        f.write('hide.\n')
        for i in self.rules.keys():
            # n = self.rules[i].name
            r = self.rules[i]
            for j in r:
                showstr = 'show ' + j.smstr(j.name) + '(' + ','.join(j.arity*'_') + ').\n'
                f.write(showstr)
        if len(self.rulefiles) == 0:
            print 'No rulefiles defined, use "rulefile" command to define them'

        if len(self.rulefiles) == 0 and len(self.rules.keys()) == 0:
            print 'WARNING: No rulefiles or rules defined, no results. This may not be what you want.' 
            print '         Use "rulefile" and "rule" commands to define files containing rules and rules.'

        for r in self.rulefiles:
            try:
                tmpf = open(r,'r')
                tmplines = tmpf.readlines()
                f.write(('\n% Begin ' + r + '\n'))
                for ff in tmplines:
                    f.write(ff)
                tmpf.close()
                f.write(('\n% EOF ' + r + '\n'))
            except IOError:
                print 'Rulefile', r, 'not found'
        
        f.close()


    def do_smls(self, a, buffer=None):
        # The smodels rule ls, inserts everything as relations to a file 
        if not self.use_yp:
            print 'smls not usable'
            return 

        t0 = time.time()

        (res,verbose,tobuf,_,_,quiet) = self.ls_common(a,'smls')

        lines = []
        t1 = time.time()
        for ((s,p,o),x,y) in res:
            # self.render_t(s,p,o,'',x,y,verbose,buffer)
            # z = self.make_smodel_fact(s,p,o,'',True,False,True,None)
            z = self.make_smodel_fact(s,p,o,'',x,y,verbose,buffer,quiet)
            lines.append(z)
        # Create a temp file 
        f = tempfile.NamedTemporaryFile(prefix='ssls-db-',delete=False)
        #print 'created', f.name
        for i in lines:
            f.write(i+'\n')
        f.close()
        t2 = time.time()
        f2 = tempfile.NamedTemporaryFile(prefix='ssls-rls-',delete=False)
        #rls = ''
        for i in self.rules.keys():
            r = self.rules[i]
            for j in r:
                tmp = ''
                # tmp = j.to_str(short=False,v=verbose,smodel=True)
                tmp = j.to_str(short=False,v=False,smodel=True,dont_expand_in_quotes=True)
                #print 'XXX Printing rule as', tmp
                #tmp2 = j.to_str(short=False,v=True,smodel=True)
                #print 'XXX Printing verbose rule', tmp2
                f2.write(tmp+'\n')
        f2.write('hide.\n')
        for i in self.rules.keys():
            # n = self.rules[i].name
            r = self.rules[i]
            for j in r:
                showstr = 'show ' + j.smstr(j.name) + '(' + ','.join(j.arity*'_') + ').\n'
                f2.write(showstr)
        # Now is the time to take into account the 
        # other existing rules
        t3 = time.time()
        if len(self.rulefiles) == 0:
            print 'No rulefiles defined, use "rulefile" command to define them'

        if len(self.rulefiles) == 0 and len(self.rules.keys()) == 0:
            print 'WARNING: No rulefiles or rules defined, no results. This may not be what you want.' 
            print '         Use "rulefile" and "rule" commands to define files containing rules and rules.'

        for r in self.rulefiles:
            try:
                tmpf = open(r,'r')
                tmplines = tmpf.readlines()
                f2.write(('\n% Begin ' + r + '\n'))
                for ff in tmplines:
                    f2.write(ff)
                tmpf.close()
                f2.write(('\n% EOF ' + r + '\n'))
            except IOError:
                print 'Rulefile', r, 'not found'

        f2.close()
        t4 = time.time()

        #print 'created', f.name, f2.name 
        # Create the result file 
        f3 = tempfile.NamedTemporaryFile(prefix='ssls-results',delete=False)
        f3.close()
        cmd = 'lparse ' + f.name + ' ' + f2.name + ' | smodels 1 | grep "Stable Model" | ./tokenize_smodel.py'
        cmd = cmd + ' > ' + f3.name
        res = os.system(cmd)
        t5 = time.time()
        print cmd
        #print 'Result in', f3.name, 'it is', res
        if res == 0:
            t6 = time.time()
            (update_needed,nsmls,smls) = self.parse_ext_res(f3.name,'smodels')
            t7 = time.time()
            if update_needed:
                print 'updating smls results'
                self.do_update('')
            else:
                print 'no updates'
            # f4.close()
            t8 = time.time()

            tstr = 'Whole command (ms): ' + str((t8 - t0) * 1000.0)
            tstr = tstr + '\n  communication (ms): ' + str((t1 - t0) * 1000.0)
            tstr = tstr + '\n  db creation (ms): ' + str((t2 - t1) * 1000.0)
            tstr = tstr + '\n  internal rule creation (ms): ' + str((t3 - t2) * 1000.0)
            tstr = tstr + '\n  file rule creation (ms): ' + str((t4 - t3) * 1000.0)
            tstr = tstr + '\n  smodels execution (ms): ' + str((t5 - t4) * 1000.0)
            tstr = tstr + '\n  result parsing (ms): ' +  str((t7 - t6) * 1000.0)
            tstr = tstr + '\n  result updating (ms): ' +  str((t8 - t7) * 1000.0)

            if self.timefile == None:
                print tstr + '\n'
                # exit here, we call postcmd, which may diverge, 
                # that is, come back here. 
                self.postcmd(None,None)
            else:
                print tstr + '\n'
                sep = '------------------------\n'
                t = time.asctime()
                tstr = sep + t + '\n' + tstr
                try: 
                    f = open(self.timefile,'a')
                    f.write(tstr + '\n')
                    f.close()
                    # exit here, we call postcmd, which may diverge, 
                    # that is, come back here. 
                    self.postcmd(None,None)
                except IOError:
                    print 'Could not save to file', self.timefile, ':', sys.exc_info()[0]

        else:
            tstr = 'smls fail (ms): ' +  cmd + ' resulted to ' + str(res)
            tstr = tstr + '\nWhole command (ms): ' + str((t8 - t0) * 1000.0)
            tstr = tstr + '\n  communication (ms): ' + str((t1 - t0) * 1000.0)
            tstr = tstr + '\n  db creation (ms): ' + str((t2 - t1) * 1000.0)
            tstr = tstr + '\n  internal rule creation (ms): ' + str((t3 - t2) * 1000.0)
            tstr = tstr + '\n  file rule creation (ms): ' + str((t4 - t3) * 1000.0)
            tstr = tstr + '\n  smodels execution (ms): ' + str((t5 - t4) * 1000.0)
            tstr = tstr + '\n  result parsing (ms): ' +  str((t7 - t6) * 1000.0)
            tstr = tstr + '\n  result updating (ms): ' +  str((t8 - t7) * 1000.0)

            if self.timefile == None:
                print tstr + '\n'
            else:
                print tstr + '\n'
                sep = '------------------------\n'
                t = time.asctime()
                tstr = sep + t + '\n' + tstr
                try:
                    f = open(self.timefile,'a')
                    f.write(tstr + '\n')
                    f.close()
                except IOError:
                    print 'Could not save to file', self.timefile, ':', sys.exc_info()[0]

    def nmls_list(self,a):
        tmp = a.split(']', 1)
        if len(tmp) != 2:
            print 'nmls: bad arguments, no ] found', a
            return (False, None,None,None)
        rest = tmp[1]
        rawitems = tmp[0]
        try:
            i = rawitems.index('[')
            rawitems = rawitems[i+1:]
        except:
            print 'nmls mismatched list format, no [.'
            return (False, None,None,None)
        myreader = csv.reader([rawitems],skipinitialspace=True)
        myitems = []
        for z in myreader:
            for x in z:
                myitems.append(x)
            
        return(True, rest, None, myitems)


    def nmls_args(self,a):
        """
        Strips off the namespace and list of potential rulefiles to be
        executed for smls. The commandline format is 
        > nsmls [ns1,ns2] [f1,f2,f3] {flags} s,p,o
        We strip off anything up to the flags and especially look 
        for the list.
        """
        (res,rest,_,mynslist) = self.nmls_list(a)
        if not res:
            print 'nsmls: problem parsing namespace list'
            return (False, None,None,None)

        (res,rest,_,myflist) = self.nmls_list(rest)
        if not res:
            print 'nsmls: problem parsing file list'
            return (False, None,None,None)

        return(True, rest, mynslist,myflist)


    def nmls_args_old(self,a):
        """
        Strips off the namespace and list of potential rulefiles to be
        executed for smls. The commandline format is 
        > nsmls [ns1,ns2] [f1,f2,f3] {flags} s,p,o
        We strip off anything up to the flags and especially look 
        for the list.
        """
        tmp = a.split(']', 1)
        if len(tmp) != 2:
            print 'nmls: bad arguments, no ] found', a
            return (False, None,None,None)
        rest = tmp[1]
        tmp = tmp[0]
        myargs = tmp.split(' ',1)
        if len(myargs) != 2:
            print 'nmls: bad arguments, no namespace found', tmp
            return (False, None,None,None)
        ns = myargs[0]
        rawfiles = myargs[1]
        if rawfiles[0] == '[':
            rawfiles = rawfiles[1:]
        myreader = csv.reader([rawfiles],skipinitialspace=True)
        myfiles = []
        for z in myreader:
            for x in z:
                myfiles.append(x)
            
        return(True, rest, ns, myfiles)
        
    def do_nsmls(self, a, buffer=None):
        """
        The command line wrapper which does not concern with 
        return value. 
        """
        _ = self.nsmls(a,buffer)

    def nsmls(self, a, buffer=None):
        # The smodels rule ls, inserts everything as relations to a file 
        # Contains the namespace as well. An example:
        # nsmls namespace name_of_view
        # or 
        # nsmls s,p,o 
        # May trigger other nsmls instances, so this may diverge. 

        if not self.use_yp:
            print 'nsmls not usable'
            return 

        t0 = time.time()

        # Before passing to ls_common, we need to parse
        # the namespace and the potential rulefiles. 
        # nsmls ns [f1,f2,f3] {flags} s,p,o

        (res, a,nslist,flist) = self.nmls_args(a)
        if not res:
            return 


        # print 'XXXXX About to ls_common with', a # XA
        (res,verbose,tobuf,_,_,quiet) = self.ls_common(a,'smls')

        lines = []
        t1 = time.time()

        for ((s,p,o),x,y) in res:
            # self.render_t(s,p,o,'',x,y,verbose,buffer)
            # z = self.make_smodel_fact(s,p,o,'',True,False,True,None)
            z = self.make_smodel_fact(s,p,o,'',x,y,verbose,buffer,quiet)
            lines.append(z)
        # Create a temp file 
        f = tempfile.NamedTemporaryFile(prefix='ssls-db-',delete=False)
        #print 'created', f.name
        for i in lines:
            f.write(i+'\n')
        f.close()


        t2 = time.time()
        f2 = tempfile.NamedTemporaryFile(prefix='ssls-rls-',delete=False)
        #rls = ''

        for nscand in nslist:
            if not self.named_rules.has_key(nscand):
                print 'No namespace', nscand, 'for local rules. This may not be what you want.'
            else:
                lrules = self.named_rules[nscand]

            
                # for i in self.rules.keys():
                for i in lrules.keys():
                    # r = self.rules[i]
                    r = lrules[i]
                    for j in r:
                        tmp = ''
                        # tmp = j.to_str(short=False,v=verbose,smodel=True)
                        tmp = j.to_str(short=False,v=False,smodel=True,dont_expand_in_quotes=True)
                        #print 'XXX Printing rule as', tmp
                        #tmp2 = j.to_str(short=False,v=True,smodel=True)
                        #print 'XXX Printing verbose rule', tmp2
                        f2.write(tmp+'\n')
        f2.write('hide.\n')

        for nscand in nslist:
            if self.named_rules.has_key(nscand):
                lrules = self.named_rules[nscand]
                for i in lrules.keys():
                    r = lrules[i]
                    for j in r:
                        showstr = 'show ' + j.smstr(j.name) + '(' + ','.join(j.arity*'_') + ').\n'
                        f2.write(showstr)
                        
        #for i in self.rules.keys():
        #    # n = self.rules[i].name
        #    r = self.rules[i]
        #    for j in r:
        #        showstr = 'show ' + j.smstr(j.name) + '(' + ','.join(j.arity*'_') + ').\n'
        #        f2.write(showstr)


        # Now is the time to take into account the 
        # other existing rules
        t3 = time.time()
        if len(self.rulefiles) == 0 and len(flist) == 0:
            print 'No rulefiles defined, use "rulefile" command to define them or give an non-empty list as second argument'

        if len(self.rulefiles) == 0 and len(self.rules.keys()) == 0 and len(flist) == 0:
            print 'WARNING: No rulefiles or rules defined, no results. This may not be what you want.' 
            print '         Use "rulefile" and "rule" commands to define files containing rules and rules.'

        if len(flist) == 0:
            thefiles = self.rulefiles
        else:
            thefiles = flist

        # print 'Files', thefiles
        for r in thefiles:
            try:
                tmpf = open(r,'r')
                tmplines = tmpf.readlines()
                f2.write(('\n% Begin ' + r + '\n'))
                for ff in tmplines:
                    f2.write(ff)
                tmpf.close()
                f2.write(('\n% EOF ' + r + '\n'))
            except IOError:
                print 'Rulefile', r, 'not found'

        f2.close()
        t4 = time.time()

        #print 'created', f.name, f2.name 
        # Create the result file 
        f3 = tempfile.NamedTemporaryFile(prefix='ssls-results',delete=False)
        f3.close()
        cmd = 'lparse ' + f.name + ' ' + f2.name + ' | smodels 1 | grep "Stable Model" | ./tokenize_smodel.py'
        cmd = cmd + ' > ' + f3.name
        res = os.system(cmd)
        t5 = time.time()
        print cmd
        #print 'Result in', f3.name, 'it is', res

        # Up until this point we can only have exited
        # by means of an unhandled exception. 
        if res == 0:
            t6 = time.time()
            (update_needed,nsmls,smls) = self.parse_ext_res(f3.name,'smodels')
            t7 = time.time()
            if update_needed:
                print 'updating nsmls results'
                self.do_update('')
            else:
                print 'no updates'
            # f4.close()
            t8 = time.time()

            tstr = 'Whole command (ms): ' + str((t8 - t0) * 1000.0)
            tstr = tstr + '\n  communication (ms): ' + str((t1 - t0) * 1000.0)
            tstr = tstr + '\n  db creation (ms): ' + str((t2 - t1) * 1000.0)
            tstr = tstr + '\n  internal rule creation (ms): ' + str((t3 - t2) * 1000.0)
            tstr = tstr + '\n  file rule creation (ms): ' + str((t4 - t3) * 1000.0)
            tstr = tstr + '\n  smodels execution (ms): ' + str((t5 - t4) * 1000.0)
            tstr = tstr + '\n  result parsing (ms): ' +  str((t7 - t6) * 1000.0)
            tstr = tstr + '\n  result updating (ms): ' +  str((t8 - t7) * 1000.0)

            if self.timefile == None:
                # exit here 
                print tstr + '\n'
                # exit here, we call postcmd, which may diverge, 
                # that is, come back here. 
                self.postcmd(None,None)
            else:
                print tstr + '\n'
                sep = '------------------------\n'
                t = time.asctime()
                tstr = sep + t + '\n' + tstr
                try: 
                    f = open(self.timefile,'a')
                    f.write(tstr + '\n')
                    f.close()
                    # exit here, we call postcmd, which may diverge, 
                    # that is, come back here. 
                    self.postcmd(None,None)
                except IOError:
                    print 'Could not save to file', self.timefile, ':', sys.exc_info()[0]

        else:
            tstr = 'nsmls fail (ms): ' +  cmd + ' resulted to ' + str(res)
            tstr = tstr + '\nWhole command (ms): ' + str((t8 - t0) * 1000.0)
            tstr = tstr + '\n  communication (ms): ' + str((t1 - t0) * 1000.0)
            tstr = tstr + '\n  db creation (ms): ' + str((t2 - t1) * 1000.0)
            tstr = tstr + '\n  internal rule creation (ms): ' + str((t3 - t2) * 1000.0)
            tstr = tstr + '\n  file rule creation (ms): ' + str((t4 - t3) * 1000.0)
            tstr = tstr + '\n  smodels execution (ms): ' + str((t5 - t4) * 1000.0)
            tstr = tstr + '\n  result parsing (ms): ' +  str((t7 - t6) * 1000.0)
            tstr = tstr + '\n  result updating (ms): ' +  str((t8 - t7) * 1000.0)

            if self.timefile == None:
                print tstr + '\n'
            else:
                print tstr + '\n'
                sep = '------------------------\n'
                t = time.asctime()
                tstr = sep + t + '\n' + tstr
                try:
                    f = open(self.timefile,'a')
                    f.write(tstr + '\n')
                    f.close()
                except IOError:
                    print 'Could not save to file', self.timefile, ':', sys.exc_info()[0]

    def parse_ext_res(self,fname,myname='external'):
        update_needed = False
        f4 = open(fname,'r')
        models = f4.readlines()
        subs_to_start = {}
        localviews = {}
        viewfiles = {}
        viewns = {}
        nsmlsviewns = {}
        nsmlsfiles = {}
        nsmls = {}
        nsmsls = {}
        namedsubs = {}
        view_to_obtain = []
        facts_to_add = {}
        for i in models:
            if i != '':
                if i == 'None':
                    print 'No results'
                    break
                else:
                    i = i.strip()
                    (res,nm,body,oneliner) = self.parse_rule(i,all_literals=True)
                    if res:
                        # print nm, body
                        if nm == 'ssls_fact':
                            nmspc = body[0]
                            fact = body[1] + ' '
                            body = map(lambda i: '"' + i + '"',body[2:])
                            fact = fact + ','.join(body)
                            fact = fact + '.'
                            # print 'ssls_fact:', fact
                            # self.do_nmrule(fact,nmspc=nmspc)
                            tmpfacts = []
                            if facts_to_add.has_key(nmspc):
                                tmpfacts = facts_to_add[nmspc]
                            tmpfacts.append(fact)
                            facts_to_add[nmspc] = tmpfacts
                        elif nm == 'ssls_resetfact':
                            nmspc = body[0]
                            # del self.named_rules[nmspc]
                            self.do_resetnmrules(nmspc)
                        elif nm == 'ssls_view':
                            viewname = body[0]

                            if is_str_lit(body[-1]):
                                body[-1] = '"' + body[-1] + '"'

                            viewcmdln = ','.join(body[1:])
                            print 'l', viewcmdln
                            viewlist = []
                            if localviews.has_key(viewname):
                                viewlist = localviews[viewname]
                            viewlist.append(viewcmdln)
                            localviews[viewname] = viewlist
                        elif nm == 'ssls_subfile':
                            viewname = body[0]
                            fname = body[1]
                            tmpfiles = []
                            if viewfiles.has_key(viewname):
                                tmpfiles = viewfiles[viewname]
                            tmpfiles.append(fname)
                            viewfiles[viewname] = tmpfiles
                        elif nm == 'ssls_subns':
                            viewname = body[0]
                            nsname = body[1]
                            tmpns = []
                            if viewns.has_key(viewname):
                                tmpns = viewns[viewname]
                            tmpns.append(nsname)
                            viewns[viewname] = tmpns
                        elif nm == 'ssls_nsub':
                            subname = body[0]
                            viewname = body[1]
                            # tmpsubs = []
                            if namedsubs.has_key(subname):
                                print 'Cannot have two views for nsub', viewname
                                break
                            namedsubs[subname] = viewname 
                        elif nm == 'ssls_delview':
                            viewname = body[0]
                            self.do_viewdel(viewname)
                        elif nm == 'ssls_fact_old':
                            fact = body[0] + ' '
                            body = map(lambda i: '"' + i + '"',body[1:])
                            fact = fact + ','.join(body)
                            fact = fact + '.'
                            # print 'ssls_fact:', fact
                            self.do_rule(fact)
                        elif nm == 'i':
                            update_needed = True
                            # A horrible hack to make everything in object a
                            # literal. 
                            # body[-1] = '"' + body[-1] + '"'
                            # print 'Huuhaa', body[-1]
                            if is_str_lit(body[-1]):
                                body[-1] = '"' + body[-1] + '"'

                            print 'i', ','.join(body)
                            self.do_i(','.join(body))
                        elif nm == 'd':
                            update_needed = True
                            if is_str_lit(body[-1]):
                                body[-1] = '"' + body[-1] + '"'
                                # body[-1] = '"' + body[-1] + '"'
                            print 'd', ','.join(body)
                            self.do_d(','.join(body))
                        elif nm[0:10] == 'ssls_print':
                            print '#', ' '.join(body)
                        elif nm == 'ssls_sub':
                            # First one is the name of the sub 
                            sublist = []
                            if subs_to_start.has_key(body[0]):
                                sublist = subs_to_start[body[0]]
                            sublist.append(','.join(body[1:]))
                            subs_to_start[body[0]] = sublist 
                        elif nm == 'ssls_nsub':
                            # First one is the name of the sub 
                            sublist = []
                            if subs_to_start.has_key(body[0]):
                                sublist = subs_to_start[body[0]]
                            sublist.append(','.join(body[1:]))
                            subs_to_start[body[0]] = sublist 
                        elif nm == 'ssls_unsub':
                            # First one is the name of the sub 
                            self.do_subdel('smls_' + body[0])
                            self.do_viewdel(body[0])
                        elif nm == 'ssls_smls':
                            view_to_obtain.append(','.join(body))
                        elif nm == 'ssls_nsmlsns':
                            # To go with ssls_nsmls
                            # ssls_nssmlsns(mytag,ns)
                            nsmlsname = body[0]
                            nsname = body[1]
                            tmpns = []
                            if nsmlsviewns.has_key(nsmlsname):
                                tmpns = nsmlsviewns[nsmlsname]
                            tmpns.append(nsname)
                            nsmlsviewns[nsmlsname] = tmpns
                        elif nm == 'ssls_nsmlsfile':
                            # To go with ssls_nsmls
                            # ssls_nssmlsfile(mytag,filename)
                            nsmlsname = body[0]
                            fname = body[1]
                            tmpns = []
                            if nsmlsfiles.has_key(nsmlsname):
                                tmpns = nsmlsfiles[nsmlsname]
                            tmpns.append(fname)
                            nsmlsfiles[nsmlsname] = tmpns
                        elif nm == 'ssls_nsmls':
                            # ssls_nsmls(mytag,rest)
                            # First one is the name of the particular 
                            # nsmls run 
                            nsmlstagname = body[0]
                            viewlist = []
                            if nsmsls.has_key(nsmlstagname):
                                viewlist = nsmsls[nsmlstagname]
                            viewlist.append(','.join(body[1:]))
                            nsmsls[nsmlstagname] = viewlist 
                    else:
                        print 'Malformed ' + myname + ' result', i

        # Now create views, if needed
        lviewkeys = localviews.keys()
        if len(lviewkeys) > 0:
            for viewname in lviewkeys:
                vcmds = localviews[viewname]
                if len(vcmds) > 0:
                    for vcmd in vcmds:
                        self.do_l(vcmd)
                    self.do_view(viewname)

        # Now create the views for nsmls 
        if len(nsmsls.keys()) > 0: # and not self.no_trigger_smls: # XXX 27.6.
            for vname in nsmsls.keys():
                vlist = nsmsls[vname]
                for i in vlist:
                    self.do_l(i)
                self.do_view(vname)
                # smlshash = dict() # XA
                smlshash = '' # XB
                self.trigger_nsmls_cmd = vname 
                if self.trigger_nsmls_cmds.has_key(vname):
                    smlshash = self.trigger_nsmls_cmds[vname]
                    # print 'XXXXXXXX vname', vname, 'has existing trigger_nsmls_cmds', smlshash # XA
                # Now we construct the arguments for nsmls
                nspart = '[]'
                filepart = '[]'
                # First the namespaces 
                if nsmlsviewns.has_key(vname):
                    tmpns = nsmlsviewns[vname]
                    nspart = '[' + ','.join(tmpns) + ']'
                if nsmlsfiles.has_key(vname):
                    tmpfs = nsmlsfiles[vname]
                    filepart = '[' + ','.join(tmpfs) + ']'
                nsmlscmd = nspart + ' ' + filepart + ' ' + vname 
                # smlshash[vname] = nsmlscmd # XA
                # self.trigger_nsmls_cmds[vname] = smlshash # XA
                
                # When a subscription for this view triggers 
                # we want to execute the command nsmlscmd
                self.trigger_nsmls_cmds[vname] = nsmlscmd # XB

                # print 'XXXXXXXX setting vname', vname, 'in trigger_nsmls_cmds to', smlshash # XA
                self.trigger_nsmls = True

        # Now go through the new facts  
        for thens in facts_to_add.keys():
            flst = facts_to_add[thens]
            for fi in flst:
                self.do_nmrule(fi,nmspc=thens)

        # Now go through the named subs 
        for s in namedsubs.keys():
            # Now gather the list of namespaces
            if viewns.has_key(s):
                nsl = viewns[s]
            else:
                nsl = []
            # The list of rulefiles
            if viewfiles.has_key(s):
                fl = viewfiles[s]
            else:
                fl = []

            nameofview = namedsubs[s]

            # Now make a commandline for this sub 
            #print 'namedsub:', s
            #print 'nsl', nsl
            #print 'fl', fl
            #print 'nameofview', nameofview
            cmdln = '[' + ','.join(nsl) + '] [' + ','.join(fl) + '] ' + s + ' ' + nameofview
            print 'nsmls', cmdln 
            self.do_nsub(cmdln)


        # Now go through the subs_to_start, make 
        # views for them and subscribe to each of them 
        newsubs = subs_to_start.keys()
        if newsubs != []:
            for i in newsubs:
                bl = subs_to_start[i]
                # print 'YYY', i, ':', bl 
                for j in bl:
                    self.do_l(j)
                # Now make a view 
                self.do_view(i)
                self.do_sub('smls_' + i + ' ' + i)

        if len(view_to_obtain) > 0 and not self.no_trigger_smls:
            vname = 'smlsview' + str(uuid.uuid4())
            for i in view_to_obtain:
                self.do_l(i)
            self.do_view(vname)
            # We want to do 
            # self.do_smls(vname)
            # self.do_viewdel(vname)
            # but since this is called from do_smls 
            # we need to do this as post-processing of 
            # do_smls in postcmd. 
            self.trigger_smls_cmd = vname 
            self.trigger_smls = True
            
        f4.close()
        return (update_needed,self.trigger_nsmls,self.trigger_smls)
        

    def do_rulefile(self,a):
        if a == '':
            print 'Need a filename for load'
            return 
        if not os.path.isfile(a):
            print 'File', a, 'not found'
            return 
        self.rulefiles.append(a)

    def do_timefile(self,a):
        if a == '':
            if self.timefile == None: 
                print 'No timefile specified'
            else:
                print 'timefile', self.timefile
            return 
        if self.timefile == None: 
            self.timefile = a
        else:
            print 'timefile redefined to', self.timefile
            self.timefile = a

    def do_rulefiles(self,a):
        if a == 'reset':
            self.rulefiles = []
            return 
        print 'Rulefiles:'
        for i in self.rulefiles:
            if i==a or a == '':
                print i

    def help_file(self):
        print 'file filename [subj[,pred[,obj]]]'
        print '   dumps the content of the SIB to file "filename"'
        print '   The subj,pred,obj handling is as in "ls" command,'
        print '   if no value given for subj,pred,obj all of the SIB'
        print '   contents are dumped to file.'

    def help_timefile(self):
        print 'timefile filename'
        print '   dumps timing information of smls to "filename"'

    def do_file(self, a, buffer=None):
        # Before checking at the arguments get the first space delimited
        # entry 
        r = a.find(' ')
        if r < 0 and len(a) == 0:
            print 'file expects the first argument to be the name always'
            return
        elif r < 0 and len(a) > 0:
            name = a
            a = ''
        else:
            name = a[0:r]
            a = a[r+1:]

        # Now check the file 
        try:
            f = open(name,'w')
        except IOError:
            print 'Could not write to file', name, ':', sys.exc_info()[0]

        (res,verbose,tobuf,_,_,_) = self.ls_common(a,'file')

        lines = []

        # Render the results as N-triples 
        for ((s,p,o),x,y) in res:
            z = self.make_ntriple(s,p,o,'',x,y,verbose,buffer)
            lines.append(z)

        for i in lines:
            f.write(i+'\n')
        f.close()

    def help_viz(self):
        print 'viz [-ctpsvi] [viewname][[subj[,pred[,obj]]]'
        print '   uses external program IsaViz, if installed to visualize'
        print '   contents of SIB.'
        print '   The subj,pred,obj handling is as in "ls" command,'
        print '   if no value given for subj,pred,obj all of the SIB'
        print '   contents are visualized.'
        print '   If the first non-switch argument, viewname, matches with an existing'
        print '   view (as defined by view command), those triples will be'
        print '   used instead.'
        print '   Requires that CWM_PATH and ISAVIZ_PATH environment'
        print '   variables point to the installation of cwm and IsaViz tools. '

    def feed_to_viz(self,res,verbose):
        lines = []
        for ((s,p,o),x,y) in res:
            # self.render_t(s,p,o,'',x,y,verbose,buffer)
            # z = self.make_smodel_fact(s,p,o,'',True,False,True,None)
            z = self.make_ntriple(s,p,o,'',x,y,verbose,buffer)
            lines.append(z)
        # Create a temp file 
        f = tempfile.NamedTemporaryFile(prefix='ssls-viz-',delete=False)
        #print 'created', f.name
        for i in lines:
            f.write(i+'\n')
        f.close()
        # Now we should run IsaViz on this file, but 
        # as a hack we need to change the result to RDF
        # so that it can be given as commandline. 
        # cwmpath = '/home/vluukkal/src/cwm-1.2.1/cwm'
        cwmpath = os.environ['CWM_PATH'] + '/cwm'
        if not os.path.isfile(cwmpath):
            print cwmpath, 'does not exist, maybe a faulty CWM_PATH'
            return 
        newname = f.name + '.owl'
        cmdln = cwmpath + ' --ntriples ' + f.name + ' --rdf 2> /dev/null > ' + newname
        res = os.system(cmdln)
        if res == 0:
            # Now we run the actual IsaViz 
            # vizpath = '/home/vluukkal/src/IsaViz/run.sh'
            vizpath = os.environ['ISAVIZ_PATH'] + '/run.sh'
            if not os.path.isfile(vizpath):
                print vizpath, 'does not exist, maybe a faulty VIZ_PATH'
                return 
            cmdln = vizpath + ' ' + newname 
            res = os.system(cmdln)
        else:
            print 'cwm failed with command\n', cmdln
        

    def do_viz(self, a, buffer=None):
        # First check whether environment variables 
        # are set for cwm and isaviz
        if not os.environ.has_key('ISAVIZ_PATH'):
            print 'No path for IsaViz, ISAVIZ_PATH environment variable must be set'
            return 

        if not os.environ.has_key('CWM_PATH'):
            print 'No path for cwm, CWM_PATH environment variable must be set'
            return 

        (res,verbose,tobuf,_,_,_) = self.ls_common(a,'viz')

        self.feed_to_viz(res,verbose)
        return 

        lines = []
        for ((s,p,o),x,y) in res:
            # self.render_t(s,p,o,'',x,y,verbose,buffer)
            # z = self.make_smodel_fact(s,p,o,'',True,False,True,None)
            z = self.make_ntriple(s,p,o,'',x,y,verbose,buffer)
            lines.append(z)
        # Create a temp file 
        f = tempfile.NamedTemporaryFile(prefix='ssls-viz-',delete=False)
        #print 'created', f.name
        for i in lines:
            f.write(i+'\n')
        f.close()
        # Now we should run IsaViz on this file, but 
        # as a hack we need to change the result to RDF
        # so that it can be given as commandline. 
        # cwmpath = '/home/vluukkal/src/cwm-1.2.1/cwm'
        cwmpath = os.environ['CWM_PATH'] + '/cwm'
        if not os.path.isfile(cwmpath):
            print cwmpath, 'does not exist, maybe a faulty CWM_PATH'
            return 
        newname = f.name + '.owl'
        cmdln = cwmpath + ' --ntriples ' + f.name + ' --rdf 2> /dev/null > ' + newname
        res = os.system(cmdln)
        if res == 0:
            # Now we run the actual IsaViz 
            # vizpath = '/home/vluukkal/src/IsaViz/run.sh'
            vizpath = os.environ['ISAVIZ_PATH'] + '/run.sh'
            if not os.path.isfile(vizpath):
                print vizpath, 'does not exist, maybe a faulty VIZ_PATH'
                return 
            cmdln = vizpath + ' ' + newname 
            res = os.system(cmdln)
        else:
            print 'cwm failed with command\n', cmdln
            



    def do_no(self, a, buffer=None):
        # print 'my ls', a
        (res, (s,p,o),(ss,pp,oo),verbose,tobuf,_,_,_) = self.parse_ls(a)
        #self.previous_cmd = ('ls',(s,p,o),(ss,pp,oo))
        self.add_to_hist(('no',(s,p,o),(ss,pp,oo),None,None,None))
        if not res:
            return 
        p_old = p
        p = 'sib:any'
        l = [((s,p,o),'uri','uri')]
        l = self.preprocess_triples('no', l)
        res = []
        if len(l) > 0: 
            a = self._node.CreateQueryTransaction(self._handle)
            res = a.rdf_query(l,return_subj_type=True)
            self._node.CloseQueryTransaction(a)
        #for i in res:
        #     print 'Triple', i
        #res = self.preprocess_result(res)
        eligible = {}
        for ((s,p,o),x,y) in res:
            if p != p_old and not eligible.has_key(s):
                eligible[s] = True
            elif p != p_old and not eligible.has_key(s):
                eligible[s] = False
            elif p == p_old and eligible.has_key(s):
                eligible[s] = False

        for ((s,p,o),x,y) in res:
            if eligible[s] == True:
                self.render_t(s,p,o,'',x,y,verbose,buffer)
                #print s,p,o
                if y:
                    oo = '"' + o + '"'
                else:
                    oo = o
                #if self.input_to_buf:
                if tobuf:
                    self.add_to_hist(('ins',(s,p,oo),(s,p,oo),None,x,y))

    def do_seq(self,a):
        (res, l,ll) = self.parse_wql(a)
        self.add_to_hist(('seq',(l,'',''),(ll,'',''),ll,None,None))
        if not res:
            return 
        self.wql.append(('seq',l,ll))

    def do_inv(self,a):
        (res, l,ll) = self.parse_wql(a)
        self.add_to_hist(('inv',(l,'',''),(ll,'',''),ll,None,None))
        if not res:
            return 
        self.wql.append(('inv',l,ll))

    def do_seqp(self,a):
        (res, l,ll) = self.parse_wql(a)
        self.add_to_hist(('seqp',(l,'',''),(ll,'',''),ll,None,None))
        if not res:
            return 
        self.wql.append(('seqp',l,ll))

    def need_quoting(self,s):
        """
        Returns True if writing out the string as an smodels
        token/variable requires quotes around it.
        At first we check whether it has any 'special' characters
        in it.
        """
        res = not(s.isalnum())
        if res:
            # Check that it is not just underscore
            # print 'YYY', s
            tmp = s.translate(None,'_')
            tmp = tmp.translate(None,'-')
            # tmp = s.translate('_')
            res = not(tmp.isalnum())
            #print '\t\t2', s, tmp, 'needs quoting:', res
            #print '\t the first char:', tmp[0], ':'
        else:
            # print '\t\t2', s, 'is not alphanum'
            pass
        return res

    def smstr(self,s):
        if self.need_quoting(s):
            return '"' + s + '"'
        else:
            return s


    def render_wql(self,(cmd,sl,ll)):
        # Problem is that the sl and ll lists may contain 
        # sublists and I don't know how to go through 
        # lists of lists so that I can distinguish 
        # a string from a list. XXX
        res = str(ll)
        #if cmd == 'inv' or cmd == 'seq':
        #    res.append(cmd)
        return res

    def wql_his_to_str(self,(cmd,l,ll)):
        pass 

    def do_wql(self, a, buffer=None):
        res = []
        start = None
        for (cmd,cmprs,verbose) in self.wql:
            if cmd == 'node':
                if start != None:
                    print 'Overwriting wql starting point', start
                start = verbose
            else:
                print (cmd,cmprs,verbose)
            


    def do_wls(self, a, buffer=None):

        #(res, (s,p,o),(ss,pp,oo),verbose) = self.parse_ls(a)
        #self.add_to_hist(('ls',(s,p,o),(ss,pp,oo),None,None,None))
        #if not res:
        #    return 
        a = self._node.CreateQueryTransaction(self._handle)
        l = [((s,p,o),'uri','uri')]
        res = a.rdf_query(l,return_subj_type=True)
        #for i in res:
        #     print 'Triple', i
        self._node.CloseQueryTransaction(a)
        #res = self.preprocess_result(res)
        for ((s,p,o),x,y) in res:
            self.render_t(s,p,o,'',x,y,verbose,buffer)
            #print s,p,o
            if y:
                oo = '"' + o + '"'
            else:
                oo = o
            if self.input_to_buf:
                self.add_to_hist(('ins',(s,p,oo),(s,p,oo),None,x,y))
            
        # Kludge! Always reset this
        self.input_to_buf = False

    def patch_quotes(self,l):
        """
        Go through list l and if anyitem starts with \" join
        the following items until one ends with antoher \".
        This is because that strings are split()ted by space
        do not honor the quotes. 
        """
        res = []
        p = False
        tmp = ''
        for i in l:
            if i[0] == '"' and not p:
                p = True
                tmp = i
            elif p and i[-1] == '"':
                p = False
                tmp = tmp + i
                res.append(tmp)
                tmp = ''
            elif p:
                tmp = tmp + i
            else:
                res.append(i)
        return res

    def is_lit(self,o):
        if o == '':
            return True
        res = (o[0] == '"' and o[-1] == '"')
        # XXX Here we make every string literal which has 
        #     : as the first character. 
        #     Or sib:any inside the string. 
        if res and o[1] == ':':
            res = False
        if res and o[1:-1] == 'sib:any':
            res = False
        # print o, 'literal?', res
        return res 

    def delit(self,o):
        if o[0] == '"' and o[-1] == '"':
            #return o[1:-1]
            tmp = o[1:-1]
            if tmp[0:2] == '\\"' and tmp[-2:] == '\\"':
                return tmp[1:-2] + '"'
            else:
                return tmp
            
        else:
            return o

    def help_ins(self):
        print 'ins subj,pred,obj'
        print '    inserts the given triple to SIB'



    def set_var_val(self,n,v):
        self.vars[n] = v

    def create_var(self,n):
        self.vars[n] = None

    def preprocess_triples(self,cmd,l):
        """
        Takes in a list of triples and preprocesses them.
        May return an expanded or reduced list, but is empty 
        for now. 
        """
        res = []
        return l

        # Dead code below
        for ((s,p,o),x,y) in l:
            # Variables can only be created by means of 'ins'
            # and even then so that the variable name is in 
            # s and does not exist beforehand. 
            pass
        return l

    def detect_bogus(self,l):
        """
        Takes in a list of triples and looks for 
        a single predetermined triple, which should be 
        ignored. 
        """
        if len(l) != 1:
            return l

        ((s,p,o),x,y) = l[0]

        if s == 'sib:boguss' and p == 'sib:bogusp' and o == 'sib:boguso':
            return []
        else:
            return l


    # Replace cmdln.is_var_uri(a) with this 
    def uri_needs_preprocess(self, s):
        # Never 
        return False

    # Replace cmdln.var_uri_name(a) with this 
    def preprocess_uri_name(self, s):
        # None or a string 
        return None 
    
    # Eval_rec may need to go, all YP stuff may need to go...

    # Replace valid_var_uri
    #def valid_var_uri(self,s):
    #    return true




    def parse_rule(self,a,all_literals=False):
        # Find the first space
        oneliner = False
        if a[-1] == '.':
            # print '\t Ends with dot'
            a = a[:-1]
            oneliner = True
        idx = a.find(' ')
        if idx <= 0:
            print 'relation needs to have at least one name'
            return (False,None,None,oneliner)
        n = a[0:idx]
        (nn,n) = self.expand_ns(n)
        #print 'Defining rule name', n, '-', nn
        vars = a[idx+1:]
        #print idx, ':',n, ':',vars
        varl = vars.split(',')
        if len(varl) == 0:
            print 'relation needs to have at least one variable'
            return (False,None,None,oneliner)
        res = []
        for i in varl:
            # print '\t Rule variable --', i, '--'
            # Check that it is just a name, no special chars
            # We then create it with namespace of our own 
            # rulename. 
            (tp,net,varname,_,_,cn) = urlparse.urlparse(i)
            # We treat is as a literal
            (_,i) = self.expand_ns(i) 
            res.append(i)
            # print i, 'is not a suitable variable name'
            # return (False,None,None)

        return (True,n,res,oneliner)

    def rules_od(self,a):
        print 'Rules'
        for i in self.rules.keys():
            if i==a or a == '': 
                rls = self.rules[i]
                for r in rls:
                    if r.ready:
                        b = self.mk_short(r.name,False)
                        print b,
                        if a != '':
                            for a in r.args:
                                b = self.mk_short(a, False)
                                print b, ',', 
                            if r.type == 'bwd':
                                print ' :-'
                            else:
                                print ' -:'
                            for (n,arity,l) in r.body:
                                b = self.mk_short(n,False)
                                print '  ', b, 
                                for j in l:
                                    b = self.mk_short(j,False)
                                    print b,',',
                                print 
                        else:
                            print '/', r.arity, '(',r.type,')'

    def do_rules(self,a):
        als = a.split()
        try:
            optlist, a = getopt.getopt(als,'vi')
        except getopt.GetoptError(s):
            print 'Problem with arguments',
            print s
            return
        
        if a == []:
            a = ''
        elif len(a) == 1:
            a = a[0]
        else:
            print 'bad arguments', a
            return 
        (_,_,_,verbose,hist,_,_) = self.handle_opts(optlist,None,None,None)
        
        print 'Rules'
        for i in self.rules.keys():
            if i==a or a == '': 
                rls = self.rules[i]
                for r in rls:
                    r.printr(short=(a == ''),v=verbose)

    def do_nmrules(self,a):
        als = a.split()
        try:
            optlist, a = getopt.getopt(als,'vi')
        except getopt.GetoptError(s):
            print 'Problem with arguments',
            print s
            return
        
        if a == []:
            nmspc = ''
            rn = ''
        elif len(a) == 1:
            nmspc = a[0]
            rn = ''
        elif len(a) == 2:
            nmspc = a[0]
            rn = a[1]
        else:
            print 'bad arguments', a
            return 
        (_,_,_,verbose,hist,_,_) = self.handle_opts(optlist,None,None,None)
        
        print 'Scoped rules', nmspc
        for nm in self.named_rules.keys():
            if nm == nmspc or nmspc == '':
                for r in self.named_rules[nm].keys():
                    if rn == '' or r == rn:
                        # r.printr(short=(rn == ''),v=verbose)
                        arule = self.named_rules[nm][r]
                        for x in arule:
                            print nm, ':', 
                            x.printr(short=(rn == ''),v=verbose)

    def do_end(self, a):
        if self.rule_under_def == None:
            print 'No rule under definition - use \'rule\' to start defining'
            return 
        (name,idx) = self.rule_under_def
        if self.rules.has_key(name):
            # r = self.rules[self.rule_under_def]
            r = self.rules[name][idx]
            b = len(r.body)
            if not b > 0:
                # print 'Rule', name, 'did not have body defined'
                pass
            r.ready = True
            self.rule_under_def = None
        else:
            print 'No rule under definition - use \'rule\' to start defining'


    def rule(self, a, type):
        """
        Creates a rule head consisting of rule name and 
        a number of arguments, separated by comma. 
        Arguments are variables, which may exist already.
        If they don't exist, they are created under a different
        namespace. 
        """
        (res, n, l,isdone) = self.parse_rule(a)
        if not res:
            return 
        #print 'defining rule', n
        # self.rule_under_def = n
        r = Rule(self, n, l, type)
        # We may have > 1 rule for the same name
        if self.rules.has_key(n):
            tmp = self.rules[n]
            tmp.append(r)
            r_idx = len(tmp) - 1
        else:
            self.rules[n] = [r]
            r_idx = 0
        print 'defining rule', n, '(', r_idx,')'            
        if isdone:
            r.ready = True
        else:
            self.rule_under_def = (n,r_idx)

    def nmrule(self, a, type, nmspc):
        """
        Creates a rule head consisting of rule name and 
        a number of arguments, separated by comma. 
        Arguments are variables, which may exist already.
        If they don't exist, they are created under a different
        namespace. 
        This is similar to rule, but we maintain a hash of hashes
        indexed by the nmspc. 
        """
        (res, n, l,isdone) = self.parse_rule(a)
        if not res:
            return 
        #print 'defining rule', n
        # self.rule_under_def = n
        r = Rule(self, n, l, type)
        # First we get the namespace hash, and if there is 
        # none we create one.
        if self.named_rules.has_key(nmspc):
            rhash = self.named_rules[nmspc]
        else:
            rhash = {}

        # We may have > 1 rule for the same name
        if rhash.has_key(n):
            tmp = rhash[n]
            tmp.append(r)
            r_idx = len(tmp) - 1
        else:
            rhash[n] = [r]
            r_idx = 0
        print 'defining scoped rule', nmspc, ':', n, '(', r_idx,')'
        self.named_rules[nmspc] = rhash 

        if isdone:
            r.ready = True
        else:
            self.rule_under_def = (n,r_idx)

    def do_rule(self,a):
        if not self.use_yp:
            print 'Rules not usable'
            return 
        self.rule(a,'fwd')

    def do_nmrule(self,a,nmspc=''):
        if not self.use_yp:
            print 'Rules not usable'
            return 
        self.nmrule(a,'fwd',nmspc)

    def do_rule_b(self,a):
        if not self.use_yp:
            print 'Rules not usable'
            return 
        self.rule(a,'bwd')

    def do_r(self,a):
        """
        One element of rule bodies:
        rname v1,v2,v3
        """
        (res, n, l) = self.parse_rule(a)
        if not res:
            return 
        #print 'body of rule', self.rule_under_def
        #print self.rules
        (name,idx) = self.rule_under_def
        if self.rules.has_key(name):
            r = self.rules[name][idx]
            arity = len(l)
            r.set_one_body(n,arity,l)
        else:
            print 'No rule under definition - use rule_f or rule_b to start'

    def do_ins(self, a):
        (res, (s,p,o),(ss,pp,oo),verbose,tobuf,_,_,_) = self.parse_ls(a)
        #self.previous_cmd = ('ins',(s,p,o),(ss,pp,oo))
        self.add_to_hist(('ins',(s,p,o),(ss,pp,oo),None))
        if not res:
            return 
        if self.is_lit(o):
            l = [((s,p,self.delit(o)),'uri','literal')]
        else:
            l = [((s,p,o),'uri','uri')]
        l = self.preprocess_triples('ins', l)
        if len(l) > 0: 
            a = self._node.CreateInsertTransaction(self._handle)
            a.send(l,confirm=True)
            self._node.CloseInsertTransaction(a)

    def do_del(self, a):
        (res, (s,p,o),(ss,pp,oo),verbose,tobuf,_,_,_) = self.parse_ls(a)
        #self.previous_cmd = ('del',(s,p,o),(ss,pp,oo))
        self.add_to_hist(('del',(s,p,o),(ss,pp,oo),None))
        if not res:
            return 
        if self.is_lit(o):
            l = [((s,p,self.delit(o)),'uri','literal')]
        else:
            l = [((s,p,o),'uri','uri')]
        l = self.preprocess_triples('del', l)
        if len(l) > 0: 
            a = self._node.CreateRemoveTransaction(self._handle)
            a.remove(l,confirm=True)
            self._node.CloseRemoveTransaction(a)

    def help_del(self):
        print 'del subj,pred,obj'
        print '    deletes the given triple from SIB.'


    def mkns(self, ns, n=None):
        # print 'Making ns', ns, 'n=', n
        if self.nsp.has_key(ns):
            nsname = self.nsp[ns] 
            if n != None:
                print 'Overwriting namespace', nsname, '(', ns, ') with', n
                self.nsp[ns] = n
                nsname = n
        else:
            if n in self.nsp.values():
                for i in self.nsp.keys():
                    if self.nsp[i] == n:
                        print 'Redefining namespace', n, 'to', ns, 'from', i
                        del(self.nsp[i])
                        self.nsp[ns] = n
                        nsname = n
            else:
                if n == None:
                    self.nsnum = self.nsnum + 1
                    nsname = 'ns_' + repr(self.nsnum)
                    self.nsp[ns] = nsname
                else:
                    self.nsp[ns] = n
                    nsname = n
        return nsname
        
    def mk_short(self,x,v):
        (tp,net,path,_,_,cn) = urlparse.urlparse(x)
        # print '!!--- mk_short', x, tp, net, path, cn
        if tp == '' or tp == 'sib':
            # xx = ':' + x
            xx = x
        else:
            myns = urlparse.urlunparse((tp,net,path,'','',''))
            #print '--- mk_short', myns, x, tp, net, path, cn
            xns = self.mkns(myns)
            #print '--- xns', myns
            xx = xns + ':' + cn
            #print '--- xx', xx
        if v:
            xx = x
        return xx
        
    def is_quoted(self,s):
        if self.is_lit(s):
            return True
        # if s[]

        return False

    def expand_ns(self,x):
        # First do not look inside a quoted string 
        if self.is_quoted(x):
            return (x,x)
        y = x.split(':')
        # In case of empty namespace dont do anything 
        if y[0] == '':
            return (x,x)
        # If namespace is sib, dont do anything
        if y[0] == 'sib':
            return (x,x)
        if len(y) == 2:
            # Let's hope no one makes a namespace http:
            if y[0] == 'http' or y[0] == 'sib':
                res = x # no work, x is modified and missing the http
                # res = y[0] + ':' + y[1]
            else:
                ns = self.get_ns(y[0])
                if ns == None:
                    res = y[1]
                else:
                    if y[1] != '':
                        res = ns + "#" + y[1]
                    else:
                        res = ns
        else:
            res = x

        # print 'expanding', x, 'to', res
        return (x,res)

    def emptyline(self):
        """
        Otherwise empty line would repeat the previous command.
        """
        pass

    def make_rel(self,s,p,o,prefix='',pred=True,obj=False,verbose=False,buffer=None):
        ss = self.mk_short(s,verbose)
        pp = self.mk_short(p,verbose)
        oo = self.mk_short(o,verbose)

        #print 'Asserting', pp,ss,oo
        self.rels[p] = True
        YP.assertFact(Atom.a(p),
              [Atom.a(s),Atom.a(o)])

    def make_smodel_fact(self,s,p,o,prefix,pred,obj,verbose,buffer=None,hist=False):
        ss = self.mk_short(s,verbose)
        pp = self.mk_short(p,verbose)
        oo = self.mk_short(o,verbose)

        #print 'Asserting', pp,ss,oo
        self.rels[p] = True
        # res = '"' + p + '" ("' + s + '","' + o + '").' # verbose
        if not hist:
            res = '"' + pp + '" ("' + ss + '","' + oo + '").'
        else:
            res = '"' + pp + '" (1,"' + ss + '","' + oo + '").'
        # print res
        return res

    def make_ntriple(self,s,p,o,prefix,pred,obj,verbose,buffer=None):
        # ss = self.mk_short(s,verbose)
        ss = self.mk_short(s,True)
        # pp = self.mk_short(p,verbose)
        pp = self.mk_short(p,True)
        # oo = self.mk_short(o,verbose)
        oo = self.mk_short(o,True)

        pref = 'file:///sib/X#'

        #print '---------------'
        #print 'SS',ss,pp,oo

        if is_str_lit(s):
            # Subject can never be a literal, so this 
            # is a URI
            if s.find('#'):
                ss = '<' + s + '>'
            else:
                ss = '<' + pref + s + '>'
        else:
            ss = '<' + s + '>'

        if obj:
            # quote " charcaters in o 
            # o = o.replace('"','\\"')
            o = o.replace('"','\\\\\'')
            oo = '"' + o + '"'
        else:
            if is_str_lit(o):
                if o.find('#'):
                    oo = '<' + o + '>'
                else:
                    oo = '<' + pref + o + '>'
            else:
                oo = '<' + o + '>'

        # res = '<' + pp + '> <' + ss + '> <' + oo + '>.'
        res = ss + ' <' + p + '> ' + oo + '.'
        # print res
        return res

    def do_rels(self,a):
        print 'Relations:'
        for i in self.rels.keys():
            print i

    def do_run(self,a):
        for i in self.rules.keys():
            if i == a or i == self.mk_short(a,True):
                print 'Executing', i
                rls = self.rules[i]
                for r in rls:
                    r.eval()

    def render_t(self,s,p,o,prefix='',pred=True,obj=False,verbose=False,buffer=None):
        ss = self.mk_short(s,verbose)
        pp = self.mk_short(p,verbose)
        oo = self.mk_short(o,verbose)
        if obj:
            # We dont collapse strings
            res = (prefix + ss + ',' + pp + ',' + '"' + o + '"')
            # res = (prefix + ss + ',' + pp + ',' + '"' + oo + '"')
        else:
            res= (prefix + ss + ',' + pp + ',' +  oo)
            #print (prefix + ss + ',' + pp + ',' +  oo)
        if buffer == None:
            print res
        else:
            buffer.append(res)

    def render_t_str(self,s,p,o,prefix='',pred=True,obj=False,verbose=False):
        ss = self.mk_short(s,verbose)
        pp = self.mk_short(p,verbose)
        oo = self.mk_short(o,verbose)
        if obj:
            return (prefix + ss + ',' + pp + ',' + '"' + oo + '"')
        else:
            return (prefix + ss + ',' + pp + ',' +  oo)

    def get_ns(self,n):
        for i in self.nsp.keys():
            if self.nsp[i] == n:
                return i
        else:
            return n

    def help_ns(self):
        print 'ns [prefix]'
        print '   lists the prefixes that are defined for this session.'
        print '   if prefix given shows the corresponding value.'

    def help_add_ns(self):
        print 'add_ns prefix uri'
        print '   defines a new prefix or namespace for given URI.'
        print '   If the same URI has an existing namespace, the old namespace is overwritten.'

    def help_rulefile(self):
        print 'rulefile filename'
        print '   instructs ssls to use smodels rules in file filename.'
        print '   "smls" command will execute the rules.'
        print '   Files are added to an internal list.'
        print '   See the "rulefiles" command to remove files from this list.'

    def help_rulefiles(self):
        print 'rulefiles'
        print '   lists all files defined by "rulefile".'
        print '   giving "reset" as argument will empty this list.'

    def help_smls(self):
        print 'smls'
        print '   executes smodels rules against the SIB contents.'
        print '   Rules come from files defined by "rulefile" command or'
        print '   from rules defined by "rule" command.'

    def do_ns(self,a):
        if a == '':
            for i in self.nsp.keys():
                print self.nsp[i], ':', i
        else:
            for i in self.nsp.keys():
                if self.nsp[i] == a:
                    print self.nsp[i], ':', i

    def do_add_ns(self,a):
        if a == '':
            print 'add_ns needs a short name and a URI'
            return 
        else:
            als = a.split(' ')
            if len(als) != 2:
                print 'add_ns needs a short name and a URI'
            else:
                sn = als[0]
                uri = als[1]
                _ = self.mkns(uri,sn)
                self.add_to_hist(('add_ns',('','',''),('','',''),[sn,uri]))



    def help_prev(self):
        print 'prev'
        print '   shows the previous command and its full expansion'

    def do_prev(self,a):
        (cmd, (s,p,o),(ss,pp,oo),id) = self.previous_cmd
        print cmd + ' ' + ss + ',' + pp + ',' + oo + ' = ' + cmd + ' ' + s + ',' + p + ',' + o + ' (' + str(id) + ')'
            
    def help_hist(self):
        print 'hist'
        print '   lists the previous commands and their full expansion.'
        print '   The history number can be used as argument for "redo" to re-execute the sommand '

    def do_h(self,a):
        (res,val) = self.get_h(a)
        if res:
            print val
        else:
            # Errors have been printed
            pass

    def get_h(self,a,relative=True):
        if a == '':
            # print 'h needs an argument in the history: (name:)num:[spo*]'
            print 'h needs an argument in the history: (name:)num:[spo]'
            return (False,None)
        myhis = []
        args = a.split(':')
        if len(args) == 3:
            n = args[0]
            sub = self.get_name(n)
            if sub == None:
                print 'name', n, 'has not bee defined'
                return (False,None)
            args = args[1:]
            (_,subid,_) = sub 
            myhis = self.get_subs_hist(subid,0)
        else:
            myhis = self.history
        try:
            x = int(args[0])
        except ValueError:
            print 'Need a numeric history number, got', args[0]
            return (False,None)
        if len(args) != 2:
            print 'h needs an argument in the history: num:[spo*]'
            return (False,None)
        # if x > len(self.history):
        if x > len(myhis):
            print 'No such history item', args[0]
            return (False,None)
        if not (args[1] == 's', args[1] == 'p', args[1] == 'o', args[1] == '*'):
            print 'h needs an argument in the history: num:[spo]', args[1], 'not allowed'
            return (False,None)
            
        if x < 0:
            if relative:
                #print '\t negative index', x,
                x = len(myhis) + x 
                #print 'transformed to', x
                if x < 0 or x > len(myhis):
                    print 'No such history item', args[0]
            else:
                # print 'Not expanding', a
                return (True, '!' + a)

        i = 0
        # for (cmd, (s,p,o),(ss,pp,oo),id) in self.history:
        for (cmd, (s,p,o),(ss,pp,oo),id) in myhis:
            if x == i:
                # cmds = self.hist_to_str((cmd, (s,p,o),(ss,pp,oo),id))
                if args[1] == 's':
                    return (True,s)
                elif args[1] == 'p':
                    return (True,p)
                elif args[1] == 'o':
                    return (True,o)
                elif args[1] == '*':
                    if cmd == 'node':
                        return (True,self.hist_to_str(('', ('','',''),('','',''),id,None,None))) # XXXX
                    else:
                        print 'Expanding', cmd, args[0], args[1],s,ss
                        # Now s and ss contain lists
                        # This can only happen with wql commands

                        #res1 = ' '
                        #res2 = ' '
                        for z in s:
                            print '\t\tX', z
                        for z in ss:
                            print '\t\tX', z
                        #for z in ss:
                        #    res2 = res2 + z + ' ' 
                        # return (self.hist_to_str((cmd, ('','',''),('','',''),id),sure=True))

                        #res = self.hist_to_str((cmd, (res1,'',''),(res2,'',''),id),sure=False)
                        res = self.hist_to_str((cmd, (s,'',''),(ss,'',''),id,None,None),sure=True)
                        print '\t to', cmd, s, res
                        return (True,[ res ])

            i = i + 1

    def do_hist(self,a):
        i = 0
        for (cmd, (s,p,o),(ss,pp,oo),id) in self.history:
            if a != '':
                if int(a) == i:
                    cmds = self.hist_to_str((cmd, (s,p,o),(ss,pp,oo),id)) # XXXX
                    print str(i) + ': ' + cmds
            else:
                    cmds = self.hist_to_str((cmd, (s,p,o),(ss,pp,oo),id)) # XXXX
                    print str(i) + ': ' + cmds
            i = i + 1

    def pvar(self,v,vv, to_save):
        # print 'Variable v', v, 'expansion vv', vv
        if self.is_var(vv) and to_save:
            return vv
        else:
            return v

    def hist_to_str(self,(cmd, (s,p,o),(ss,pp,oo),id,slit,olit),sure=False,to_save=False):
        if cmd == 'ls' or cmd == 'del' or cmd == 'ins' or cmd == 'one':
            if not sure:
                res = cmd + ' ' + ss + ',' + pp + ',' + oo + ' = ' + cmd + ' ' + s + ',' + p + ',' + o
            else:
                s = self.pvar(s,ss,to_save)
                p = self.pvar(p,pp,to_save)
                o = self.pvar(o,oo,to_save)
                res = cmd + ' ' + s + ',' + p + ',' + o
        elif cmd == 'sub':
            if not sure:
                res = cmd + ' ' + ss + ',' + pp + ',' + oo + ' = ' + cmd + ' ' + s + ',' + p + ',' + o + ' (' + str(id) + ')'
            else:
                s = self.pvar(s,ss,to_save)
                p = self.pvar(p,pp,to_save)
                o = self.pvar(o,oo,to_save)
                res = cmd + ' ' + s + ',' + p + ',' + o
        elif cmd == 'unsub' or cmd == 'subres':
            if not sure:
                res = cmd + ' ' + str(id)
            else:
                res = cmd + ' ' + str(id)
        elif cmd == 'i' or cmd == 'd' or cmd == 'l':
            if not sure:
                res = cmd + ' ' + ss + ',' + pp + ',' + oo + ' = ' + cmd + ' ' + s + ',' + p + ',' + o
            else:
                s = self.pvar(s,ss,to_save)
                p = self.pvar(p,pp,to_save)
                o = self.pvar(o,oo,to_save)
                res = cmd + ' ' + s + ',' + p + ',' + o
        elif cmd == 'u' or cmd == 'update':
            res = cmd 
        elif cmd == 'redo' or cmd == 'let' or cmd == 'end':
            res = cmd + ' ' + id
        elif cmd == 'expect' or cmd == 'view':
            res = cmd + ' ' + s + ' ' + p + ' ' + id
        elif cmd == 'wait':
            res = cmd + ' ' + id
        elif cmd == 'add_ns':
            res = cmd + ' ' + id[0] + ' ' + id[1]
        elif cmd == 'node':
            # res = cmd + ' ' + id
            if not sure:
                res = cmd + ' ' + ss + ' = ' + cmd + ' ' + s
            else:
                s = self.pvar(s,ss,to_save)
                res = cmd + ' ' + s
        elif cmd == 'inv' or cmd == 'seq' or cmd == 'seqp':
            print 
            restt = ' ' # ' '.join(ss)
            rest = ' '
            resttt = ' '
            tmpres = self.render_wql((cmd,s,ss))
            #for i in ss:
            #    restt = restt + i + ' '
            #for i in s:
            #    rest = rest + i + ' '
            #for (i,j) in zip(s,ss):
            #    z = self.pvar(i,j,to_save) # j
            #    if to_save: # and ...
            #        resttt = resttt + z + ' '
            res = cmd + tmpres

            if not sure:
                res = cmd + ' ' + restt + ' = ' + cmd + ' ' + rest
            else:
                print 'To save rest', rest, 'restt', restt, 'resttt', resttt
                res = cmd + ' ' + restt
        else:
            if not sure:
                res = cmd + ' ' + ss + ',' + pp + ',' + oo + ' = ' + cmd + ' ' + s + ',' + p + ',' + o + ' (' + str(id) + ')'
            else:
                s = self.pvar(s,ss,to_save)
                p = self.pvar(p,pp,to_save)
                o = self.pvar(o,oo,to_save)
                res = cmd + ' ' + s + ',' + p + ',' + o + ' (' + str(id) + ')'
        return res 

    def help_redo(self):
        print 'redo num[,num][num1-num2]'
        print '   Redoes the command in the history given by the number(s).'
        print '   You can specify a range of numbers specified by commas or a range separated by "-"'
        print '   * indicates all history items.'
        print '   Example: redo 4,3,5-10'

    def help_undo(self):
        print 'undo num[,num][num1-num2]'
        print '   Undo the command in the history given by the number(s).'
        print '   You can specify a range of numbers specified by commas or a range separated by "-"'
        print '   * indicates all history items.'
        print '   Only the following commands are undoable:'
        print '      ins,del,i,d,sub'
        print '   Example: redo 4,3,5-10'

    def do_redo(self,a):
        i = 0
        hs = self.handle_history_args(a)
        if hs == None:
            return 
        b = ','.join(map(str,hs))
        # Redo does not go to history
        # self.add_to_hist(('redo',('','',''),('','',''),b))

        # Now hs lists all the indices that need to be 
        # redone. 
        #print 'Redoing', hs
        for i in hs:
            (cmd, (s,p,o),(ss,pp,oo),id) = self.history[i]
            mylen = len(self.history)
            cmdstr = self.hist_to_str((cmd, (s,p,o),(ss,pp,oo),id),True) # XXXX
            # cmdstr = self.hist_to_str((cmd, (s,p,o),(ss,pp,oo),id),True,True)
            print 'Redoing', i, 'at', mylen,':',cmdstr
            # if cmd == 'one':
            if False:
                # Kludge, an attempt at handling one. 
                myargs = (s+','+p+','+o)
                print 'one at', i, ':', myargs
                self.do_one(myargs,None,i)
            else:
                self.onecmd(cmdstr)

    def to_redo(self,s,id):
        if s == 'ins':
            return 'del'
        elif s == 'del':
            return 'ins'
        if s == 'i':
            return 'd'
        elif s == 'd':
            return 'i'
        elif s == 'sub':
            return 'unsub'
        elif s == 'update':
            return 'update'
        else:
            return None

    def do_undo(self,a):
        i = 0
        hs = self.handle_history_args(a)
        if hs == None:
            return 
        b = ','.join(map(str,hs))
        self.add_to_hist(('undo',('','',''),('','',''),b))

        # Now hs lists all the indices that need to be 
        # redone. 
        #print 'Undoing', hs
        for i in hs:
            (cmd, (s,p,o),(ss,pp,oo),id) = self.history[i]
            redocmd = self.to_redo(cmd,id)
            if redocmd != None:
                cmdstr = self.hist_to_str((redocmd, (s,p,o),(ss,pp,oo),id),True) # XXXX
                print 'Undoing', i, ': ',cmdstr
                self.onecmd(cmdstr)


    def help_sub(self):
        print 'sub name [-viq] [viewname][subj[,pred[,obj]]] [| shellcmds]'
        print '   Places a subscription in the SIB for the specified pattern.'
        print '   Pattern works in same way is in case of the ls command.'
        print '   The values can be shown via the subs command.'
        print '   If the first non-switch argument, viewname, matches with an existing'
        print '   view (as defined by view command), those triples will be'
        print '   used instead.'
        print '   Results can be piped to arbitrary shell commands by having'
        print '   the commandline followed by "|" and the string to pass to shell.'
        print '   In this case each time the subscription fire the shell command'
        print '   is executed.'
        print '   If the shell command has a comment #ssls, the results of the shell'
        print '   command are parsed for insertions and deletions.'
        print '   If the -v flag is given the values are also printed out as they arrive.'
        print '   If the -q flag is given the values are not stored for latter viewing.'
        print '   If the -i flag is given the updates are added to history.'
        print '   Do note that the name must always be the first argument.'

    def do_sub(self,a,addtohist=True):
        # Before checking at the arguments get the first space delimited
        # entry 
        r = a.find(' ')
        isview = False
        if r < 0 and len(a) == 0:
            print 'sub expects the first argument to be the name always'
            return
        elif r < 0 and len(a) > 0:
            name = a
            a = ''
        else:
            name = a[0:r]
            a = a[r+1:]

        if self.get_name(name) != None:
            print 'name', name, 'already existing'
            return

        (res, (s,p,o),(ss,pp,oo),verbose,tobuf,shellcmd,loopback,quiet) = self.parse_ls(a)
        #self.previous_cmd = ('sub',(s,p,o),(ss,pp,oo))
        #self.add_to_hist(('sub',(s,p,o),(ss,pp,oo)))
        if not res:
            return 

        l = []
        # Check whether the first argument was 
        # a name of a view.
        if self.views.has_key(s):
            isview = True
            for ((a,b,c),x,y) in self.views[s]:
                # print '    view sub::', a,b,c,'(',x,y,')'
                l.append(((a,b,c),x,y))
        else:
            # Now make the subscription
            if self.is_lit(o):
                l = [((s,p,self.delit(o)),'uri','literal')]
            else:
                l = [((s,p,o),'uri','uri')]

        self.cblock.acquire()
        rs1 = self._node.CreateSubscribeTransaction(self._handle)
        result_rdf = rs1.subscribe_rdf(l, DefaultHandler(self,rs1,(not verbose)),return_subj_type=True)
        # return rs1
        if isview:
            print 'Subscription ID', rs1.tr_id, ":", ss, "(view)"
        else:
            print 'Subscription ID', rs1.tr_id, ":",(ss,pp,oo)

        if addtohist:
            self.add_to_hist(('sub',(s,p,o),(ss,pp,oo),rs1.tr_id))

        # The subscription datastructure
        # (subhandle,changes,pattern,status,bufferflag,shellcmd,loopback,quiet,counter,nslist,rulefiles)
        # Where
        # 'subhandle' is the Node internal handle for the subscription
        # 'changes' is the list of changes which have triggered this subscription
        # 'pattern' is the subscription pattern, which may be a view
        # 'status' running, or stopped
        # 'bufferflag' is true if we dump the events to historybuffer
        # 'shellcmd' is the command which we execute on the changes
        # 'loopback' indicates whether we should parse the result of the shellcmd
        # 'quiet' is false if we want to record changes to 'changes'
        # 'counter' counts the number of events
        # 'nslist' has a list of internal namespaces associated with this 
        #          subscription and is only applicable when executing rules
        #          (i.e. there's loopback)
        # 'rulefiles' has a list of rulefiles which should be executed 
        #             when the subscription fires, see above.
        self.subs[rs1.tr_id] = (rs1, [], (s,p,o),'running',tobuf,shellcmd,loopback,quiet,0,[],[])
        # self.subs[rs1.tr_id] = (rs1, [], l,'running',tobuf,loopback,quiet)
        # If we have a current name associate the ID with it
        self.reg_name(name, len(self.history),rs1.tr_id)
        self.cblock.release()

    def do_nsub(self,a,addtohist=True):
        # Before checking at the arguments get the first space delimited
        # entry 

        isview = False
        (res, a,nslist,flist) = self.nmls_args(a)
        if not res:
            return 
        
        # print '\t a now', a
        a = a.lstrip()
        r = a.find(' ')
        if r < 0 and len(a) == 0:
            print 'nsub expects the third argument to be the name always'
            return
        elif r < 0 and len(a) > 0:
            name = a
            a = ''
        else:
            name = a[0:r]
            a = a[r+1:]

        if self.get_name(name) != None:
            print 'name', name, 'already existing'
            return

        (res, (s,p,o),(ss,pp,oo),verbose,tobuf,shellcmd,loopback,quiet) = self.parse_ls(a)
        #self.previous_cmd = ('sub',(s,p,o),(ss,pp,oo))
        #self.add_to_hist(('sub',(s,p,o),(ss,pp,oo)))
        if not res:
            return 

        l = []
        # Check whether the first argument was 
        # a name of a view.
        if self.views.has_key(s):
            isview = True
            for ((a,b,c),x,y) in self.views[s]:
                # print '    view sub::', a,b,c,'(',x,y,')'
                l.append(((a,b,c),x,y))
        else:
            # Now make the subscription
            if self.is_lit(o):
                l = [((s,p,self.delit(o)),'uri','literal')]
            else:
                l = [((s,p,o),'uri','uri')]

        self.cblock.acquire()
        rs1 = self._node.CreateSubscribeTransaction(self._handle)
        result_rdf = rs1.subscribe_rdf(l, DefaultHandler(self,rs1,(not verbose)),return_subj_type=True)
        # return rs1
        if isview:
            print 'Subscription ID', rs1.tr_id, ":", ss, "(view)"
        else:
            print 'Subscription ID', rs1.tr_id, ":",(ss,pp,oo)

        if addtohist:
            self.add_to_hist(('sub',(s,p,o),(ss,pp,oo),rs1.tr_id))

        # The subscription datastructure
        # (subhandle,changes,pattern,status,bufferflag,shellcmd,loopback,buffer,?,namespacelist,filelist)
        self.subs[rs1.tr_id] = (rs1, [], (s,p,o),'running',tobuf,shellcmd,loopback,quiet,0,nslist,flist)
        # self.subs[rs1.tr_id] = (rs1, [], l,'running',tobuf,loopback,quiet)
        # If we have a current name associate the ID with it
        self.reg_name(name, len(self.history),rs1.tr_id)
        self.cblock.release()


    def get_name(self,n):
        if self.names.has_key(n):
            return self.names[n]
        else:
            return None

    def del_name(self,n):
        if self.names.has_key(n):
            del self.names[n]
        else:
            print 'No such subscription name', n

    def reg_name(self,n,ln,id):
        if not self.names.has_key(n):
            self.names[n] = (ln,id,None)
        else:
            (lnb,oldid,lne) = self.names[n]
            newe = lne
            newid = oldid
            if oldid == None:
                newid = id
            if lne == None:
                newe = ln 
            self.names[n] = (lnb,newid,newe)

    def id_to_name(self,id):
        for i in self.names.keys():
            (lnb,idn,lne) = self.names[i]
            if id == idn:
                return i
        return None

    

    def do_let(self,a):
        if a == '':
            print 'let needs a name'
            return 
        else:
            self.reg_name(a,len(self.history)+1,None)
            self.curr_name = a
            self.add_to_hist(('let',('','',''),('','',''),a))

    def do_node(self,a):
        if a == '':
            print 'node needs a value'
            return 
        else:
            al = a.split()
            if len(al) > 1:
                print 'Only one value for node allowed'
                return 
        (res, l,ll) = self.parse_wql(a)
        self.add_to_hist(('node',(str(l[0]),'',''),(str(ll[0]),'',''),ll[0]))
        if not res:
            return 
        self.wql.append(('node',l[0],ll[0]))



    def do_names(self,a):
        for i in self.names.keys():
            (lnb,idn,lne) = self.names[i]
            if a != '' and a == i:
                print i, ':', idn, '(',lnb,'-',lne,')'
            else:
                print i, ':', idn, '(',lnb,'-',lne,')'
            
    def do_end_name(self,a):
        if a != '':
            print 'end does not need arguments'
            return 
        else:
            self.reg_name(self.curr_name,len(self.history),None)
            self.add_to_hist(('end',('','',''),('','',''),None))

    def do_subh(self,a):
        for i in self.subs.keys():
            if i==a or a == '' or self.id_to_name(i) == a:
                (rs,l,(s,p,o),status,tobuf,shellcmd,loopback,quiet,_) = self.subs[i]

                ctr = 0
                for (t,t2,rem,add) in l:
                    for ((ss,pp,oo),x,y) in rem:
                        pre = str(ctr) + ': - '
                        self.render_t(ss,pp,oo, pre,x,y)
                        ctr = ctr + 1 
                        self.add_to_hist(('d',(ss,pp,o),(ss,pp,oo),None))

                    for ((ss,pp,oo),x,y) in add:
                        pre = str(ctr) + ': + '
                        self.render_t(ss,pp,oo, pre,x,y)
                        ctr = ctr + 1 
                        self.add_to_hist(('i',(ss,pp,oo),(ss,pp,oo),None))

    def do_subsave(self,a,v='$Revision: 1.66 $'):
        if a == '':
            print 'subsave needs subscription name and filename'
            return 
        l = a.split(' ')
        if len(l) != 2:
            print 'subsave needs precisly two arguments: subscription name and filename separated by space'
            return 
            
        sn = l[0]
        fn = l[1]
        
        try:
            f = open(fn,'w')
            f.write('ssls ' + v + '\n')
            f.write('b---\n')

            for i in self.subs.keys():
                if self.id_to_name(i) == sn:
                    (rs,l,(s,p,o),status,tobuf,shellcmd,loopback,quiet,_,_,_) = self.subs[i]

                    ctr = 0
                    prevt = 0.0
                    for (t,t2,rem,add) in l:
                        f.write( '# ' + str(t) + '\n')
                        ctr = ctr + 1
                        f.write( str(ctr) + ':' + 'a' + ': ' + 'sleep ' + str(t2-prevt) + '\n')
                        prevt = t2
                        for ((ss,pp,oo),x,y) in rem:
                            cmds = self.hist_to_str(('d', (ss,pp,oo),(ss,pp,oo),None,x,y), True,True)
                            f.write( str(ctr) + ':' + 'a' + ': ' + cmds + '\n')
                            ctr = ctr + 1 

                        for ((ss,pp,oo),x,y) in add:
                            cmds = self.hist_to_str(('i', (ss,pp,oo),(ss,pp,oo),None,x,y), True,True)
                            f.write( str(ctr) + ':' + 'a' + ': ' + cmds + '\n')
                            ctr = ctr + 1 

                        cmds = 'update'
                        f.write( str(ctr) + ':' + 'a' + ': ' + cmds + '\n')
                        ctr = ctr + 1 
            f.write('e---\n')
            f.close()
        except IOError:
            print 'Could not save to file', a, ':', sys.exc_info()[0]




    def help_subs(self):
        print 'subs [id]'
        print '   Lists the subscription IDs that have been started by sub command.'
        print '   Listing shows the ID, the pattern, how many times it has fired and'
        print '   whether it is active or not.'
        print '   Giving a subscription ID as argument the changes to the SIB are listed.'
        print '   The command unsub stops the subscription and subres purges the events'
        print '   associated with the subscription.'
        print '   Results can be piped to arbitrary shell commands by having'
        print '   the commandline followed by "|" and the string to pass to shell.'
        print '   If the shell command has a comment #ssls, the results of the shell'
        print '   command are parsed for insertions and deletions.'
        print '   Examples:'
        print '   subs a | less'
        print '   subs a | ./loopback_sub.lp # ssls'


    def help_subsave(self):
        print 'subsave subname filename'
        print '   Saves the changes of subscription "subname" to file'
        print '   "filename" in ssls format, which can be later executed'
        print '   with "exec" command.'
        print '   An alternative is to use subs piped to shell:'
        print '   subs a | cat > file'

    def do_subs(self,a):
        print 'Subscriptions'
        (res,a,sh,loopback) = self.get_piped(a)
        shells = ' '.join(sh)
        mybuf = []
        for i in self.subs.keys():
            if i==a or a == '' or self.id_to_name(i) == a:
                (rs,l,(s,p,o),status,tobuf,shellcmd,loopback,quiet,myctr,nlist,rlist) = self.subs[i]
                n = ''
                tmp = self.id_to_name(i)
                if tmp != None:
                    n = '('+tmp+')'
                if quiet: 
                    qstr = ' (changes not recorded)'
                else:
                    qstr = ''
                if shellcmd != '':
                    qstr = qstr + ' ( | ' + shellcmd + ')'
                if self.views.has_key(s):
                    mypat = s + ' (view)'
                else:
                    mypat = self.render_t_str(s,p,o,'',True,False)

                tmp = str(i) + ' ' + n + ' :' + mypat + ' : #' + \
                    str(len(l)) + ' (' + str(myctr) + ')' + ' : ' + str(status) + qstr
                # print i, n, ':', self.render_t_str(s,p,o,'',True,False),': #', len(l),':', status
                mybuf.append(tmp)
                if a != '':
                    mybuf = self.sub_printer(mybuf,l)
                    if False:
                        ctr = 0
                        for (t,t2,rem,add) in l:
                            # print t
                            mybuf.append(t)
                            for ((ss,pp,oo),x,y) in rem:
                                pre = str(ctr) + ': - '
                                # self.render_t(ss,pp,oo, pre,x,y)
                                tmp = self.render_t_str(ss,pp,oo, pre,x,y)
                                mybuf.append(tmp)
                                ctr = ctr + 1 
                            for ((ss,pp,oo),x,y) in add:
                                pre = str(ctr) + ': + '
                                # self.render_t(ss,pp,oo, pre,x,y)
                                tmp = self.render_t_str(ss,pp,oo, pre,x,y)
                                mybuf.append(tmp)
                                ctr = ctr + 1 
        if mybuf != []:
            if shells == '':
                for i in mybuf:
                    print i
            else:
                # Pipe it to shell 
                self.pipe(mybuf,shells,loopback)

    def sub_printer(self,mybuf,l,start=0):
        """
        'mybuf' is the buffer where we put the stuff for later printing
        'l' is the list of changes
        """
        ctr = start
        t1 = 0.0
        for (t,t2,rem,add) in l:
            # print t
            mytime = str(t) + '(' + str(t2-t1) + ')'
            t1 = t2
            mybuf.append(t)
            for ((ss,pp,oo),x,y) in rem:
                pre = str(ctr) + ': - '
                # self.render_t(ss,pp,oo, pre,x,y)
                tmp = self.render_t_str(ss,pp,oo, pre,x,y)
                mybuf.append(tmp)
                ctr = ctr + 1 
            for ((ss,pp,oo),x,y) in add:
                pre = str(ctr) + ': + '
                # self.render_t(ss,pp,oo, pre,x,y)
                tmp = self.render_t_str(ss,pp,oo, pre,x,y)
                mybuf.append(tmp)
                ctr = ctr + 1 
        return mybuf

    def get_subs_hist(self,a,item):
        res = []
        for i in self.subs.keys():
            # print 'Looking history for',a, 'checking against', i
            if i==a or a == '' or self.id_to_name(i) == a:
                # print 'Found it',a , i, self.id_to_name(i)
                (rs,l,(s,p,o),status,tobuf,loopback,quiet,_,_,_) = self.subs[i]
                for (t,t2,rem,add) in l:
                    for ((ss,pp,oo),x,y) in rem:
                        res.append(('d',(ss,pp,oo), (ss,pp,oo), i))
                    for ((ss,pp,oo),x,y) in add:
                        res.append(('i',(ss,pp,oo), (ss,pp,oo), i))
                if res != []:
                    # Is this really needed?
                    res.append(('update',('','',''),('','',''),i ))
        return res 
                

    def help_unsub(self):
        print 'unsub id'
        print '   Stops the subscription correspondiong to the given ID.'
        print '   The occurred events are not purged, they can be viewed with subs still.'
        print '   The subscription cannot be restarted.'

    def do_unsub(self,a,addtohist=True,verbose=True):
        for i in self.subs.keys():
            if i == a or self.id_to_name(i) == a:
                (rs,l,(s,p,o),_,_,shells,loopback,quiet,ctr,nslist,rlist) = self.subs[i]
                if verbose:
                    print 'Unsubscribing', i
                if addtohist:
                    self.add_to_hist(('unsub',(s,p,o),(s,p,o),i))
                self._node.CloseSubscribeTransaction(rs)
                self.subs[i] = (rs,l,(s,p,o),'stopped',False,shells,loopback,quiet,ctr,nslist,rlist) 

    def help_subres(self):
        print 'subres id'
        print '   Resets the events that have fired the subscription related to the ID.'
        print '   Also see subdel and unsub'

    def help_subdel(self):
        print 'subdel id'
        print '   Removes all traces of subscription id.'
        print '   Also see subres and unsub'

    def help_subh(self):
        print 'subh id'
        print '   Adds to the history buffer the changes triggered by subscription id.'
        print '   This is done by means of i and d, which need to be re-executed '
        print '   with redo and committed with update.' 
        print '   Also see i,d,u,update and expect.'

    def do_subres(self,a):
        for i in self.subs.keys():
            if i == a or self.id_to_name(i) == a:
                (rs,_,(s,p,o),r,tobuf,shellcmd,loopback,quiet,ctr,nslist,rlist) = self.subs[i]
                self.add_to_hist(('subres',(s,p,o),('','',''),i))
                self.subs[i] = (rs,[],(s,p,o),r,False,shellcmd,loopback,quiet,0,nslist,rlist) 

    def do_subdel(self,a,addtohist=True):
        found = False
        for i in self.subs.keys():
            if i == a or self.id_to_name(i) == a:
                (rs,_,(s,p,o),r,tobuf,shellcmd,loopback,quiet,_,_,_) = self.subs[i]
                #if addtohist:
                #    self.add_to_hist(('subdel',('','',''),('','',''),a))
                if r == 'running':
                    # must stop it first
                    self.do_unsub(a,addtohist=False,verbose=False)
                del self.subs[i]
                self.del_name(self.id_to_name(i))
                found = True
                break
        if not found:
            print 'No such subscription ID or name', a

    def do_quit(self,a):
        sys.exit(0)
    def do_EOF(self,a):
        print 
        sys.exit(0)


if len(sys.argv) > 4:
    print 'ssls.py usage: ssls.py [-c] [ss_name:IP_addr:port] [file]'
    sys.exit(1)

ss_name = ''
ss_ip = ''
ss_port = ''
fname = ''
conn_dir = False
just_exec = False
just_cont = False

args = sys.argv[1:]
for i in args:
    j = i.split(':')
    if len(j) == 3:
        ss_name = j[0]
        ss_ip = j[1]
        ss_port = j[2]
        conn_dir = True
    elif len(j) == 1:
        if j[0] == '-c':
            just_cont = True
            if just_exec:
                just_exec = False
        else:
            fname = j[0]
            if not just_cont:
                just_exec = True

if fname == '':
    # Check for the existence of an init file in current dir
    try:
        f = open('.ssls','r')
        f.close()
        fname = '.ssls'
        just_exec = False
    except IOError:
        pass

# Check for the existence of a login file 
try:
    f = open('.sslslogin','r')
    logininfo = f.readlines()
    ss_name = logininfo[0].rstrip()
    ss_ip = logininfo[1].rstrip()
    ss_port = logininfo[2].rstrip()
    f.close()
    print 'Connecting to ' + ss_name + ':' + ss_ip + ":" + ss_port
except IOError:
    pass


# print 'ssls:', ss_name, ss_ip, ss_port, 'file', fname

node = Node.ParticipantNode("ssls")
# ss_handle = node.discover(method = "mDNS")
#ss_handle = node.discover()

if ss_name == '':
    ss_handle = node.discover()
else:
    # No need to instantiate, just pass the class
    if ss_ip == 'nota':
        Node.NotaInit()
        c = Node.NotaConnector
    else:
        c = Node.TCPConnector
    ss_handle = (ss_name,(c,(ss_ip,int(ss_port))))

try:
    res = node.join(ss_handle)
except:
    print sys.exc_info()[1]
    sys.exit('Could not join to Smart Space')

# print res
if not res:
    sys.exit('Could not join to Smart Space')

# print "--- Member of SS:", node.member_of

loop = Ss(node,ss_handle,fname,use_yp,just_exec)
loop.prompt = str(node.member_of) + " 0 > "
loop.cmdloop('Smart space list $Id: ssls.py,v 1.66 2011/08/18 18:07:48 vluukkal Exp $')

sys.exit(0)

