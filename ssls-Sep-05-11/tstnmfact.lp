% To test rendering of ssls_fact 

% add_ns wp1 http://sofia.org/personal
% rulefile tstnmfact.lp
% smls
% rulefiles reset 
% Try in ssls commandline 
% nmrules 
% # If resource.lp would use them ... 
% nsmls [myns] [resource.lp]
% # To test deletion of them, requires delfact.lp 
% nsmls [myns,myns3] [delfact.lp]



ssls_fact(myns,abcd,"wp1:entry").
ssls_fact(myns2,xyz,"wp1:entry").
ssls_fact(myns3,abcd,"wp1:entry").
ssls_fact(myns,abcd,"wp1:second").
ssls_fact(myns3,xyz,"wp1:xyz").


show ssls_fact(_,_,_).
