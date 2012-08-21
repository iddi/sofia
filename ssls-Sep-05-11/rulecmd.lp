%
% Sample rules to test the ssls command interface.
%
% $Id$ 
%

%
% Look for new classes, use this to trigger 
% an smls to obtain the instances of the class.
% Can be triggered by 
% smls *,rdf:type,rdfs:Class
% 
% Will trigger a *,rdf:type,C query 
%
ssls_smls("sib:any","rdf:type",C) :- 
  "rdf:type"(C,"rdfs:Class"). 

% This will be reached by means of the smls
% above. 

ssls_print("New instance of", C, I) :- 
  % neq(C,"http://www.w3.org/2000/01/rdf-schema#Class"),
  "rdf:type"(I,C).

ssls_sub("Boo", I,"rdf:type",C) :- 
  % neq(C,"http://www.w3.org/2000/01/rdf-schema#Class"),
  "rdf:type"(I,C).

hide.
show ssls_smls(_,_,_).
show ssls_print(_,_,_).
show ssls_sub(_,_,_,_).
