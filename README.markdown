Building:

	Spymemcached can be compiled using Apache buildr and requires a
	running memcached or Membase instance in order for all unit tests
	to pass successfully. To build Spymemcached with a memcached instance
	running on localhost on port 11211 use the following command:

	buildr package

	This will generate binary source, and javadoc jars in the target
	directory of the project.

	To compile the Spymemcached against Membase running on localhost use
	the following command:

	buildr package SPYMC_SERVER_TYPE="membase"

	To compile Spymemcached against Membase running on a different host
	use the following command:

	buildr package SPYMC_SERVER_TYPE="membase" \
	SPYMC_TEST_SERVER_V4="ip_address_of_membase"

	To compile Spymemcached on localhost against memcached and skip all
	tests run the following command:

	buildr TEST=no

Testing:

	The latest version of spymemcached has a set of command line
	arguments that can be used to configure the location and type of your
	testing server. The arguments are listed below.

	SPYMC_SERVER_TYPE="server_type"

	This argument is used to specify the type of testing server you are
	using. By default this argument is set to memcached. It can be set to
	either "memcached" or "membase". Invalid testing server types will
	default to memcached.

	SPYMC_TEST_SERVER_V4="ip_address_of_testing_server"

	This argument is used to specify the ipv4 address of your testing
	server. By default it is set to localhost.

	SPYMC_TEST_SERVER_V6

	This argument is used to set the ipv6 address of your testing server.
	By default it is set to ::1. This argument does not need to be
	specified if SPYMC_TEST_SERVER_V4 is specified. If an ipv4 address is
	specified, but an ipv6 address is not specified then the ipv6 address
	will default to the ipv4 address. If an ipv6 address is specified
	then an ipv4 address must be specified otherwise there may be test
	failures.

More Information:

	For more information about Spymemcached see the following links
	below:

	Spymemcached Project Home:

	http://code.google.com/p/spymemcached/

	Contains a wiki, issue tracker, and downloads section.

	Github:

	http://github.com/dustin/java-memcached-client

	Contains the latest Spymemcached source.

	Couchbase.org:

	http://www.couchbase.org/products/sdk/membase-java

	Contains a download's section for the latest release as well as an
	extensive tutorial to help new users learn how to use Spymemcached.