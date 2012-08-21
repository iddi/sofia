5.9.2011

ssls - A smart space shell (v 1.66)

1. What it is
2. What's new 
3. Requirements and installation
4. Starting up
5. Operations and tutorial 
5.1. Basic operations
5.2. Observing changes
5.3. Saving, loading and history
6. smodels integration
7. Known problems and todo

1. What it is 

A command line utility for accessing, monitoring and manipulating 
the contents of Smart-M3 Semantic Information Broker (SIB) on 
triple level. Command level access to SSA protocol and possiblity
of saving the commands to a file and replaying them later on. 

Integration with smodels answer set programming system, which allows 
executing Prolog-like rules over the contents of the SIB and communicating 
the results back. 

Limited possibility of accessing the unix shell commands. 

Possibilty of visualization of the RDF graph using IsaViz. 

Smart-M3 is available at:
http://sourceforge.net/projects/smart-m3/

Smodels is available at:
http://www.tcs.hut.fi/Software/smodels

IsaViz is available at:
http://www.w3.org/2001/11/IsaViz/ v.3.0 Alpha 

Cwm is available at (required for visualization):
http://www.w3.org/2000/10/swap/doc/cwm.html, 1.2.1 

This program is beta quality. 

The author welcomes feedback at: vesa.luukkala@nokia.com

2. What's new 

Since 1.58 (May-23-11)
- new command sleep to sleep a number of seconds 
- subsave will insert sleep's to the history file
- new commands ssls_resetfact and resetnmrules to remove facts
  from local namespaces 
- new commands ssls_nsmls, ssls_nsmlsns and ssls_nsmlsfile
- arguments sib:boguss,sib:bogusp,sib:boguso to any ls 
  means that no communication is done. This may be useful 
  in triggering smls commands from rules. 

Since 1.50 (Mar-23-11)
- Support for controlling the rule execution from rules, allowing 
  execution of specified rulefiles from the subscriptions --
  new primitives:
  ssls_view, ssls_delview, ssls_subfile, ssls_subns, ssls_nsub, ssls_fact
- corresponding new commands:
  nsub, nmrule, nmrules, nsmls (see tstrlcmd.lp)
- ssls_fact now admits scoping (see tstnmfact.lp)
- the nsmls and nsub are like sub and smls, but take 
  as arguments lists of namespaces and rulefiles
- no other documentation yet

Since 1.49 (Mar-21-11)
- fixed a bug in smls_sub

Since 1.39 (Jul-23-10)
- new command 'fls' to make a transitive traversal of the
  graph up to a predetermined length or the whole reachable
  subgraph. Will not continue after rdf:type or rdffs:subClassOf.
  If the depth is -1, full reachability from the node is shown. 
  Examples:
  > fls 3 3456
  > fls -1 3456
- new commands available for the rule interpretation: ssls_sub, ssls_unsub
  ssls_smls, which allow starting and stopping subscriptions from the 
  rules as well as requesting more information. See the rulecmd.lp file
  for an example. 
- First three lines of .sslslogin file in the executing directory 
  are interpreted as name, IP address and port of the SIB.

Since 1.37 (Jul-19-10)
- 'sub' command can now be piped to shell commands, so that 
  for each firing of the subscription, the shell command 
  is evoked:
  sub a | wc 
  The commandline parsing is supported, see the loopback_sub.pl file:
  sub b | ./loopback_sub.pl # ssls
  will cause each modified triple to passed to ./loopback_sub.pl
  and parse its results for insertions and deletions and then execute them
- 'sub' command can take name of a view as 
  argument, enabling subscriptions with multiple triple patterns,
  see the 'view' command

Since 1.33 (Jul-12-10)
- commandline flag -c causes ssls not to quit after executing a file.
  Up until now executing a file from commandline would always exit 
  ssls after finalizing the script.
- possiblity of reading in commands from stdin, indicated by filename 
  '-'. Example:
  echo "ls" | python ssls.py X::10010 - 
- the query performed after a subscription named 'smls' now uses
  the pattern given for the said subscription. This means that 
  smodels database is constructed for only those triples which 
  have triggered the execution of smodels execution. 
- 'sub' command now has -q flag, which makes the changes which 
  triggered the subscription not to be saved. More efficient when using smls
  or only observing the subscription triggerings and not interested 
  in checking the changes later on. 'subs' command for such subscriptions 
  will never show any triples. Example: 
  sub smls_3 -q *,*,* 
- 'subs' printing bug fixed 

Since 1.24 (Jun-02-10)
- possible to pipe results of 'ls' and 'subs' to shell commands, e.g:
  ls | grep WIFE 
  ls | cat > a_file.txt
  ls | wc 
  ls | less 
  subs a | less
  Quotation marks, ' and ", will cause problems, to escape 
  a single quotation you need to do 
  ls | grep \\\"Test
- 'timefile' command allows saving timing information of smls execution 
- experimentally possible to read back the results of shell commands 
  to modify SIB. If the shell commandline has a comment # ssls postfixed
  to it, ssls attempts to intepret the results. Example with the provided
  external file: 
  ls | ./loopback.pl # ssls
  No other documentation yet. 
- bug fixes in ls flag parsing and execution 
- bug fixes in rendering of literals, now namespaces are not collapsed
  inside them

Since 1.22 (May-03-10)
- saving of ntriples now escapes doublequotes, meaning IsaViz can show them
- new command 'subsave' allows saving the contents of a subscription buffer 
  directly to file

Since 1.16 (Feb-24-10)
- 'file' command to export the SIB contents to a file in ntriples format 
- 'viz' command to feed the contents to external IsaViz visualization 
  program 
- 'view','l','views','viewdel' commands to construct named views which 
  are essentially queries with multiple triple patterns 
- 'ssls_fact' to assert session persistent local facts from smodels 
  rules. 

3. Requirements and installation

Requires Smart-M3 0.9 or later. 

No specific installation is needed besides uncompressing the 
archive. ssls.py and tokenize_smodel.py must be in the same directory.

Assumes that this can import the Node.py file of the Smart-M3 
Python API. You may place this file to the directory where Node.py
resides. 
The existence of lparse and smodels programs is not checked, it is expected
that they are installed in path.

If you want to use visualization you need to install IsaViz and cwm 
and set the environment variables CWM_PATH and ISAVIZ_PATH to point at the 
directories where the executables cwm and run.sh are installed. 
Note that in IsaViz you need to set Edit/Preferences/Directories 
the directories for dot executable and the temporary directory. 

Tested with 
Ubuntu 8.0.7, 9.0.4, 10.04
Maemo Fremantle 1.3 
Python 2.6.2, 2.5.2 
Smart-M3 0.93,0.94
smodels: lparse 1.1.1, smodels 2.3.4 [optional]
IsaViz: 3.0 Alpha [optional]
cwm: 1.2.1 [optional]

4. Starting up

Let's assume that we have SIB running at localhost in port 10010
and under the name X. 

For the Smart-M3 release SIB, you need to start 
> sibd
> sib-tcp

You can start ssls on the same machine as follows:

> python ssls.py X::10010

Leaving out the X::10010 the program will prompt the same 
information. Leave the IP address empty, if running ssls
on the same machine. 

5. Operations and tutorial 

Type help to get a listing of internal documentation.

Below we walk through the available commands by means of 
examples. They are expected to be done in sequence, so earlier
constructs are used later on. 

5.1 Basic operations

The command 'ls' takes a triple as argument and prints out the 
results of the triple query. The wildcard * is expanded to sib:any. 
ssls will automatically create namespaces for URIs, they will be called 
ns_1,ns_2,... 

Below is an example session showing the use of SSAPI commands. The text
after the prompt '>' is what you're expected to type. 

Lists all classes: 
> ls *,rdf:type,rdfs:Class

To see how the last command is expanded, without executing the command.
You can do this at any time. 
> prev 

Insert some information -- in real world we want to use a generated
id instead of id123 below and this is shown below, but for examples 
sake we use id123 here: 
> ins id123,rdf:type,ns_1:Person
> ins id123,ns_1:name,"Matti"

Note, version 0.93 and earlier of SIB have bug when inserting
wildcards (*) causing the SIB to fail. 

Lists all information pertaining to id123, requires that id123 exists.
> ls id123,*,*

The same thing as shorthand, this will be expanded 
to same thing as above
> ls id123

Also, these are the same thing:
> ls id123,ns_1:name,*
> ls id123,ns_1:name

The same thing, but with expanded namespaces:
> ls -v id123,ns_1:name

Note thta on Unix like systems it is possible to pipe 
the results of ls to shell commands: 

> ls *,*,* | wc 

This enables simple searching:

> ls | grep Matti 

As well as pagination 

> ls | more 

The handling of quotations, " and ' requires extra 
escapes: 

> ls | grep \\\"Matti 

will expand to shell command: 

grep \"Matti

It is possible to introduce new namespaces or rename 
old ones by means of 'add_ns' command:

> add_ns can http://www.m3.com/2008/02/m3/canonical

This will rename the ns_1 namespace to can. You can 
view the defined namespaces by:

> ns 

Delete information:
> del id123,can:name,"Matti" 
See what happended:
> ls id123

Note for versions earlier than 0.93 of Smart-M3 you can't use wildcards 
here, you must have the complete triple. For later versions the 
wildcard is allowed. 

Make an atomic update of several triples
> i id123,can:fname,"Matti"
> i id123,can:lname,"Nykanen"
Now see the update buffer 
> u 
To commit the update buffer 
> update 
Now the update buffer is empty 
> u
Delete and insert at the same time, 
you can have several inserts and deletes. 
> d id123,can:lname,"Nykanen"
> i id123,can:lname,"Tapola"
> i id123,can:wifename,"Mervi"
Check the update buffer 
> u 
Commit the triples 
> update 

In practice we want to have unique ids,   
so we can generate UUIDs as follows:

> i $,rdf:type,can:Person
> i $$,can:fname,"Mervi"
> i $$,can:lname,"Tapola"
> update 

The $ sign will be expanded to an UUID and the $$
will be expanded to the latest generated UUID. 

Note, that if you did a mistake in adding he i's 
and d's you can reset the update buffer by: 

> u_res

To see the results:

> ls *,can:fname,"Mervi"

Or alternatively:

> ls $$

5.2 Observing changes

It is possible to make subscriptions and view the 
change history. The simplest use is to subscribe
to any change:

> sub a *,*,*

This creates a subscription named 'a'. Now any change 
in SIB will be recorded to subscription 'a'.
The syntax is similar to 'ls' so a shorthand for 
the same thing is:

> sub a 

It is possible to view the defined subscriptions
by 

> subs 

Let's make two changes to observe the subscription behaviour:

> i id123,can:lname,"Nykanen"
> d id123,can:lname,"Tapola"
> i $$,can:husband,id123
> update
> i id123,can:wife,$$
> update 

Note that the $$ should refer to the person instance 
"Mervi" created above, if you have used $ since, you need
to check the particular value from SIB. 
To get an overview of the changes:

> subs 

And to see the actual changes:

> subs a

Note here, that the RDF store itself generated content
for the husband property, but this behaviour may change in
future. 

Results of subs command can be piped to shell in the same
manner as for ls command. 

> subs a | grep Matti

If the previous changes are not interesting, it is possible
to reset the buffer: 

> subres a 

Now look at the subscription:

> subs a

Let's make another change and observe its changes:

> i id123,can:mood,"Happy"
> update
> subs a 

Note that only the net effect of one transaction 
triggers the subscription, so the following 
does not trigger any subscription:
> d id123,can:mood,"Happy"
> i id123,can:mood,"Happy"
> update
> subs a 

We can stop or unsubscribe the subscription, and then see the effects:

> unsub a 
> subs a

Note, that the buffer of changes is still available.
It is not possible to reactivate this subscription and
also it is not possible to have another one with the 
same name.
To erase the subscription completely so that the name 
can be reused:

> subdel a

Sometimes we are only interested in changes to particular 
pieces of information. We may also want to see immediately
when the subscription fires.
We can limit the subscription with same syntax as with ls
and the -v flag makes the subscription print out the changes
as they come:

> sub a id123,can:mood
> sub b -v $$
> d id123,can:mood,"Happy"
> i id123,can:mood,"Hungry"
> i $$,can:mood,"Nervous"
> update
> subs 
> subs a
> subs b

It is possible to save the results of all subscriptions 
to a file, and later re-execute them. See section 5.3 for this,
or directly:

> help subsave 
> subsave a test.ssls  

The -q flag for sub suppresses saving of subscription history. This
is useful for performance reasons when subscription changes are not 
interesting ads such (smls subscriptions for smodels integration or
when just checking the changes with -v subscriptions). 

We may also require a set of conditions or values and
give them a name. The name must be different from 
any subscription name:

> d id123,can:mood,"Hungry"
> i $$,can:mood,"Happy"
> expect z
> expects
> ecpects z

Every time changes occur, we check whether any of
them matches the entries in the expects and if yes,
they are marked a such. When all of the entries 
in an expect are found, the expect 'fires'.

> del id123,can:mood,"Hungry"
> expects z
> del $$,can:mood,"Nervous"
> expects z
> ins $$,can:mood,"Happy"
> expects z

The expect is no longer active, since it has 
been fulfilled. 
Note that the entries need to be fulfilled once,
we don't need them to be active at the same time.
E.g. in the example above an insert that would set mood
to "Hungry" would not make any difference. 
This may change in the future. 

Given a -v flag, the 'firing' of expect is printed
out immediately. 

To delete an expect use 

> expect_del z 

The wait command blocks until the specified expect 
fires:

> wait z

You can give a comma separated list of expects and 
if you don't give any parameters it continues when
enter is pressed. 

The wait command is useful when running in batch mode. 

It is possible to define named views, which can be queried.
You can specify a set of query triples, which follow the
ls syntax and then make a query which lists all matching 
triples:

> l id123
> l $$,can:mood

This defines two queries, first one lists all the 
properties of the first person, the second one 
all the moods of the second person.

You can see the 'view buffer' with the 'u' command.
Now the view can be named:

> view a 

Associates the name a with the view buffer and at the 
same time empties the view buffer. Now it is possible
to query the SIB according to the view:

> ls a

Note, that giving a view name which equals an existing
triple in the SIB (rdfs:Class, for instance) will block
listing the original name. 

It is possible to subscribe to a view:

> sub myview a 

It is possible to list the current views
> views
> views a 

To delete a view:

> viewdel a 

5.3 Saving, loading and history

Many, but not all of the commands executed on the 
command line are saved to the history. Some commands
can be instructed to place their results to history
and some of the values in history can be referred 
to in other commands. 

For subscriptions you can use the 'subsave' command 
to save the contents of a named subscription buffer 
to a file, which may later be executed by ssls. 

> subsave a test.ssls 

The 'file' command saves the contents of the SIB
to a file in nttriples format. It takes the same
arguments as 'ls' so you can control what is saved.

The piping mechanism of 'ls' can also be used to 
save the contents of SIB:

> ls | cat > file.txt

The piping mechanism of 'subs' can be used to 
save the subscriptions in an alternative way to 'subsave':

> subs | cat > file.txt

Next we discuss the history operations, which include
the possiblity of saving the command history to a file
which may be reloaded. 

You can see the command history by:

> hist

You can redo any command by referring to its number 
in history:

> redo 0

would execute the first command, for this example the first 
ls. You can redo many commands:

> redo 0,1,5-7,9

You can also give wildcard * to redo everything in history. 

For some commands (ins,del,i,d,sub) there is an undo command 
which works in otherwise same manner as redo. Note that update
can't be undone, but i and d can. 

The history can also be saved to a file

> save test.ssls 

The file is a text file and can be modified by hand.
Note that it may be easier to try the file handling 
with the two included example files.

The distribution contains two examples timo_2.ssls and 
wife_2.ssls which can be executed against each other. 

You need at least a SIB running and two terminal windows 
for these two programs. You may also want to have a third
terminal where you can observe the traffic by means of 
subscription in a third ssls. 

The ssls program can be started with a saved file as argument,
which causes the commands to be executed and after that the
program exits. In one terminal do:
1> python ssls.py X::10010 timo_2.ssls 

This will wait for pressing of enter. Now start the other
program:
2> python ssls.py X::10010 wife_2.ssls 

Now hit enter in terminal 1 and both programs will 
run their course and stop. Note that the commandline 
flag -c will cause ssls not to stop after executing 
a file leaving you in ssls command line. 

The filename '-' causes ssls to read the commands 
from stdin:

> echo "ls" | python ssls.py X::10010 - 

A saved file can be loaded or executed in an executing ssls. 
Loading reads in the file to history, replacing the previous 
contents of history. The commands can then be re-executed.
Start two ssls's in two terminals and do:

1> load timo_2.ssls 
1> redo * 

Note that the history index changes when loading.
Alternatively executing the file will cause the commands
to be executed as they are read:

2> exec wife_2.ssls

NOTE: the access to the history values is quite convoluted,
you can still do it, but this seems a bit of a design mistake.

Now in terminal 1 you should have 42 as history index and 
13 in terminal 2.  You can view the individual values in 
some of the entries in history by command h:

1> h 0:s
1> h 0:p
1> h 0:o
1> h -2:s 

The first three prints out the subject, predicate and 
object of the arguments to the first i command in the history. 
You can either give a direct index to the history or a negative
index, which counts backwards from current history state.

1> h 29:o 

Prints an empty string. 

You can refer to these in commands, for example:

1> ins !0:s,http://www.m3.com/2008/02/m3/canonical#loves,!4:s 

Note that having direct referrals to index counts are 
problematic when referring to results that have been generated 
into history by commands explained shortly below. 
For those commands you should refer to them by means of negative 
indices. Look at wife_2.ssls for an example and explanation. 

Some commands have flags which cause the results to be added
to history. The 'ls' command has flag -i which makes the resulting
triples added to history with ins command. 

NOTE: as of 1.23 the -i flag is depreciated, you can use the 
'subsave' command to achieve the same effect. 
The 'sub' command has flag -i which causes the updates to be added
to history as i's and d's. The update command is not written out,
though. This can be used in conjunction with a *,*,* subscription
to record every change to history and eventually the history 
can then be saved. The saved history can then be replayed. 

The example files have the command 'one' in them. It acts as 
'ls' with same arguments but if there are more than one triples 
as result, it asks for the user to pick one and then inserts 
this as ins to history. See wife_2.ssls for example use. 

The file format is not fixed; the line numbering can be altered 
without causing a parse error, but it may mess the references 
to history items. The revision format and starting and ending tags 
(b---,e---) are critical. Line starting with # is a comment and 
not handled. 

6. smodels integration

ssls is able to use an external program to enable logic programming
so that it is possible to execute Prolog-like rules over the contents 
of the SIB. We use the smodels system which provides a different,
from our perspective more suitable, model for rule execution when
compared to traditional Prolog. 

The basic use of smodels is that the rules are written in files,
which are then indicated to ssls. The command 'smls' will combine 
the files in one file for rules and then the contents of the SIB 
are read in to a local file and both are then fed to smodels
system. The results are read back in and generated facts of 
predetermined names are interpreted by ssls and trigger 
SSAPI operations. 

It is also possible to have an amount of reactivity by defining
subscriptions with names beginning with 'smls'. Whenever the 
subscriptions fire, the 'smls' command is executed automatically. 

Note that for efficiency reasons it may make sense to use the -q
flag with smls subscriptions as it will not save the changes 
to a buffer. 

There exists a rudimentary mechnism for defining rules on the 
ssls command line, but not all smodels features are supported. 

The specific fact names and the required formats which are 
recognized by ssls are:
- i(s,p,o) to insert a triple to update buffer, which will be
  updated after the rules have been executed
- d(s,p,o) like 'i', but puts the triple to delete buffer
- ssls_print(s,p,o) prints out a triple 
- ssls_print_n(a1,a2,a3,...,an), where n is an integer prints 
  out a tuple of n items 
- ssls_fact(x,a1,a2,...,an) creates a fact named 'x' with arity of n
- ssls_sub(name,s,p,o) creates a subscription named 'name' with the
  triple pattern in s,p,o. Multiple facts for the same 'name' result
  to a single subscription for all of the patterns (subscription to a 
  view, essentially)
- ssls_unsub(name) destroys a subscription named 'name'
- ssls_smls(s,p,o) runs the smls command with the triple pattern 
  in s,p,o. Essentially calls the rule-engine again with potentially
  differing output. Multiple facts result listing all of the patterns. 

We walk through an example here to show the concepts. 
The distribution should contain the file 'condition.lp',
which is to be used in conjunction with the example files
timo_2.ssls and wife_2.ssls. 

Please note that this example relies on having an empty 
SIB to work. If you want to redo the example, then you
should quit the SIB and delete the database file. Alternatively 
you could also delete all triples which refer to the instances
of type:
- http://sofia.org/msg#SMS
- can:Man
- can:Beer 

Start a SIB and then start three terminals 1,2 and 3. In the 
first two start the timo_2.ssls and wife_2.ssls:

1> python ssls.py X::10010 timo_2.ssls
2> python ssls.py X::10010 wife_2.ssls

time_2 will stop waiting for an enter keypress. Press
enter and then wife_2 will stop waiting for another keypress. 

In third teminal start ssls:

3> python ssls.py X::10010 

Inform about the rules and define the required namespace:

3> rulefile condition.lp
3> add_ns can http://www.m3.com/2008/02/m3/canonical

If you want to see what goes on you can also place a 
subscription for all changes and view it later by 
means of the subs a command.

3> sub a 

Now have a look at the condition.lp file. In principle
there is no distinction between facts which are defined
or inferred in the smodels rules, but in practice the 
URI names from the SIB contain symbols which must be 
wrapped inside quotation marks. Also, in the SIB the 
triples are of format subject, predicate, object:

123,rdf:type,can:Man.

To match these triples in the rules we use the convention 
of having the predicate as the fact name followed by subject 
and object. So to match the above triple we use:

"rdf:type"(X,"can:Man")

This will bind the variable X to any URI matching the type. 

When it is possible to derive a fact by the name of 'i','d'
or 'ssls_print' they trigger actions by ssls after the 
rules have been executed.
'i' puts a triple in the insert buffer,
'd' puts a triple in the delete buffer
'ssls_print' prints out the triple 

The update buffer is committed to the SIB after the rules have 
been executed.

Note that at the end of condition.lp file we hide all the 
visible facts, but in order to have the relevant facts
commi, they need to be explicitely shown.

Now you can execute the rules by 

> smls

You should see something like:

lparse /tmp/ssls-db-p0hCYf /tmp/ssls-rls-Cf7C1t | smodels | grep "Stable Model" | ./tokenize_smodel.py > /tmp/ssls-resultsBIofx6
# 1cbb3127-f2dc-4622-9cca-90df4ab77f3e should not drive
i 1cbb3127-f2dc-4622-9cca-90df4ab77f3e,http://www.m3.com/2008/02/m3/canonical#drive_status,"forbidden"
updating smls results

The lines starting with # are results of the printing and the lines 
starting with 'i' or 'd' are the actions. In this case one new triple
has been inserted to SIB. 
The file starting with /tmp/ssls-db- contains the contents of the 
SIB in fact form and /tmp/ssls-rls- contains the rules. 
In case of error you can rerun the commandline for debugging. 

To demonstrate the reactivity we define a subscription which 
will trigger the execution of smls:

3> sub smls_1 *,can:drinks,*

or 

3> sub smls_1 -q *,can:drinks,*

The -q flag turns off the buffering of the changes, which is more
efficient. In the first case you can always look at what changes
triggered the rules, but the latter will not fill the memory. 

The triggering subscription names need to have 'smls' as their
four first characters. Note that here we trigger to a particular
piece of information. We could have defined a subscription for 
all changes (sub smls_2 *,*,*) also. 

Now when we hit enter in timo_2 terminal, the triple 

1cbb3127-f2dc-4622-9cca-90df4ab77f3e,can:drinks,...

is removed, firing the subscription, which in place 
triggers an smls, where another rule is triggered 
with actions.

Please do note that in this example we count that only
one instance of can:Man and can:Beer are present. 

7. Known problems and todo

Problems:
- history handling for some commands is missing 
- literals in smodels rules are broken

Todo:
- dump a ntriples file to SIB via inserts
- get rid of tokenize_smodel.py 
- expect which requires all of the entries to be 
  valid at the same time, i.e. an entry may change to NOK
- support for ontologies 
  - generating an ontology from instances 
- variables for triples 
- add an example of ssls_fact 
- commandline access to previous results, maybe combine
  with variables

#EOF
