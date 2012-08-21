%
% Simple smodels example to determine whether 
% driving is permissible. 
% Expected to be working with 
% timo_2.ssls and wife_1.ssls or wife_2.ssls
%
% Requires that the namespace can exists:
%
% add_ns can http://www.m3.com/2008/02/m3/canonical
%
% Please do note that in this example we count that only
% one instance of can:Man and can:Beer are present. 
% 


%
% A simple rule that derives a new fact based on 
% the SIB contents. Note that we must put the 
% URIs with ':' inside quotation marks. 
%
dont_drive(X) :-
   "rdf:type"(X,"can:Man"),
   "can:number_of_drinks"(X,Y),
   not do_drive_again(X),
   neq(Y,0).

%
% We produce two new facts, i and ssls_print, 
% which are interpreted by ssls and produce
% actions. 
%
% These could already been deduced instead of 
% dont_drive, but this allows for a contrived example.
%
2{i(X,"can:drive_status","forbidden"),ssls_print(X,"should not","drive")}2 :- 
   "rdf:type"(X,"can:Man"),
   dont_drive(X).

do_drive_again(X) :-
   "rdf:type"(X,"can:Man"),
   "rdf:type"(Y,"can:Beer"),
   not "can:drinks"(X,Y).
   % "can:drive_status"(X,"forbidden")
   % dont_drive(X).
   % eq(Y,"0").

2{d(X,"can:drive_status","forbidden"),ssls_print(X,"can drive","without worry")}2 :- 
   "rdf:type"(X,"can:Man"),
   do_drive_again(X).

% This means that we dont show any of the deduced
% facts.
hide. 
% Now we can selectively indicate which facts are 
% shown. At least the three following ones should
% be shown so that ssls can operate. It may 
% be useful for debugging purposes to show others 
% as well. 
show i(_,_,_).
show d(_,_,_).
show ssls_print(_,_,_).

% You can show as many as you want here
show dont_drive(_).
show do_drive_again(_).