#!/usr/bin/perl 
#
# An example of an external program interfacing 
# with ssls to modify contents of SIB, to be called
# from a subscription. Each inserted number is incremented
# by one. 
# Assuming you have a subscription 'a', from ssls do 
#  
# subs a | ./loopback_sub.pl # ssls 
#
# which should feed the changes to this program
# and then parse the results back. 
#
# Alternatively you can start a subscription so that
# for each change the loopback is executed:
# 
# sub b | ./loopback_sub.pl # ssls 
#
# Warning: This ends up in an infinite loop as we update 
# each number which causes another firing of subscription
# etc.
# 
# Assuming this file has execution rights and is in the 
# same directory as ssls. 
#
# $Id: loopback_sub.pl,v 1.1 2010/07/21 07:23:41 vluukkal Exp $ 
# 

# To get the number detection
use POSIX; 

while(<>) {
    # print $_;

    if($_ =~ m/\d+: \+ ([^,]+),([^,]+),(\S+)/) {
	$s = $1;
	$p = $2;
	$o = $3;
	if($o =~ m/"(.*)"/) {
	    $o = $1;
	}
	# If object of an added triple is a number, we increment it by 
	# one and update the SIB. 
	if(isdigit $o) {
	    print "d $s,$p,$o\n";
	    $o++;
	    print "i $s,$p,$o\n";
	} 
    }
}
