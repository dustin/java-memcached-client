# Building

Spymemcached can be compiled using Apache Ant by running the following
command:

    ant

This will generate binary, source, and javadoc jars in the build
directory of the project.

To run the Spymemcached tests against Membase Server run the
following command:

    ant test -Dserver.type=membase

To test Spymemcached against Membase running on a different host
use the following command:

    ant test -Dserver.type=membase \
        -Dserver.address_v4=ip_address_of_membase

# Testing

The latest version of spymemcached has a set of command line arguments
that can be used to configure the location of your testing server. The
arguments are listed below.

    -Dserver.address_v4=ipv4_address_of_testing_server

This argument is used to specify the ipv4 address of your testing
server. By default it is set to localhost.

    -Dserver.address_v6=ipv6_address_of_testing_server

This argument is used to set the ipv6 address of your testing server.
By default it is set to ::1. If an ipv6 address is specified then an
ipv4 address must be specified otherwise there may be test failures.

    -Dserver.port_number=port_number_of_memcached

This argument is used when memcahched is started on a port other than
11211

    -Dtest.type=ci

This argument is used for CI testing where certain unit tests might
be temporarily failing.

# More Information

For more information about Spymemcached see the links below:

## Project Page The

[Spymemcached Project Home](http://code.google.com/p/spymemcached/)
contains a wiki, issue tracker, and downloads section.

## Github

[The gitub page](http://github.com/dustin/java-memcached-client)
contains the latest Spymemcached source.

## Couchbase.org

At [couchbase.org](http://www.couchbase.org/code/couchbase/java) you
can find a download's section for the latest release as well as an
extensive tutorial to help new users learn how to use Spymemcached.
