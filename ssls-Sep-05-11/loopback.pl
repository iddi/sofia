#!/usr/bin/perl 
#
# An example of an external program interfacing 
# with ssls to modify contents of SIB. 
# From ssls do
# 
# ls | ./loopback.pl # ssls 
#
# For each triple whose object is a number, we increment
# that number by one. 
# 
# Assuming this file has execution rights and is in the 
# same directory as ssls. 
#
# $Id: loopback.pl,v 1.2 2010/07/23 16:16:40 vluukkal Exp $ 
# 

# To get the number detection
use POSIX; 

while(<>) {
    # print $_;

    if($_ =~ m/([^,]+),([^,]+),(\S+)/) {
	$s = $1;
	$p = $2;
	$o = $3;
	if($o =~ m/"(.*)"/) {
	    $o = $1;
	}
	# If object is a number, we increment it by 
	# one and update the SIB. 
	if(isdigit $o) {
	    print "d $s,$p,$o\n";
	    $o++;
	    print "i $s,$p,$o\n";
	} 
    }
}
