% To test viewhandling from rules

% # First some local facts with namespaces 
% add_ns wp1 http://sofia.org/personal
% rulefile tstnmfact.lp
% smls
% # reset the rulefiles leave only the 
% # facts generated above
% rulefiles reset 
% nmrules
%
% rulefile tstrlcmd.lp
% smls
% 
% views 
% ls Class
% # then 
% rulefiles reset
% # You can run the same thing from command line 
% nsmls [myns] [resource.lp] Class


% Make a subscription with a viewname to be 
% defined later. The name must begin with smls,
% but it is not enforced. 
ssls_nsub("smls_test","Class").

% Associate some files with the views
ssls_subfile("smls_test","resource.lp").
ssls_subfile("smls_test","activity.lp").



% Associate some namespaces with a subscription 
% These are defined in tstnmfact.lp

ssls_subns("smls_test","myns").
ssls_subns("smls_test","myns3").

% Define viewnames. Might have been before the 
% smls_test.  
ssls_view("Prop","sib:any","sib:any","rdf:Property").
ssls_view("Class","sib:any","sib:any","rdfs:Class").
ssls_view("Class","sib:any","sib:any","rdfs:Resource").


show ssls_view(_,_,_,_).
show ssls_subns(_,_).
show ssls_subfile(_,_).
show ssls_nsub(_,_).
