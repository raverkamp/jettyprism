* JettyPrism
Roland Averkamp, 2016-4-23

JettyPrims is a bundle of the Jetty servlet engine/webserver and the Prism System
of Marcelo F. Ochoa..

As Prism is quite old (Oracle 7 support) I removed a lot of code that is not
needed anymore. A lot of customization is gone.

You can run this system on your computer and access procedures on an Oracle database
which use the oracle PL/SQL Web Toolkit to create HTML output for browsers. The system
can also serve local files.

So you do need to ask your DBA to start the HTTP listener on the databse server.
Or install Weblogic.

It is meant for development purposes.
The system uses basic authentication!  Do not use it in production.

It works fine for me.


** Building
The system is developed with Netbeans.
But there is an Ant build file "build-jettyprism.xml" which you can use to build
the system.
The ant files creates a directory jettyprism in the dist folder and places the
generated jar file "main.jar" and the libraries into this folder.
The file "main.jar" is executable. So copy the directory jettyprism to the 
installation folder and call
java -e jettyprism/main.jar args ...

** Running
The program takes one argument, the name a configuration file. It must be in the form
of a java properties file.


All files are resolved local to the directory of the configuration file, i.e. "." will be the
directory opf the file.

*** Properties:
In general: if a property is prefixed with "$", the "$" will be removed and the value
will be used for look up in the environment. If a property value starts with "$$", 
then the first "$" is removed and the rest is used as value.

log4jconfig

the name of the log4j property file. this maybe empty, then the default 
"log4j.properties" is taken.

Static file serving

static.a.dir : the directory that will be served
static.a.wdir : the path under which is accessible
static.a.cache-control :
   the CacheControl Line that will be served
   ex. max-age=86400, public means anyone may cache, check again in 86400 seconds 
    which is one day

There can be more than one directory for static files.

port
the port to listen

use_local
If this is not falsish (0,N, NO, F, false) then the server will listen only on the
loopback device (127.0.0.1). Otherwise the server will listen on all IP addresses.
The default is Y, meaning that it is not possible to access the server from
other computers.


The properties for the DBprism servlet start with "general." e.g.
general.alias: x y

Properties general 

alias
  the list of dads (space separated)

Porpeties per DAD
For each DAD the properties are prefixed with "DAD_<dadname>."
If no dbusername for a DAD is given, then the user has to authenticate as a 
database user. If a username is given, then also a password has to be given.

connectString
  the jdbs connect string, e.g. jdbc:oracle:thin:@localhost:1521:xe

dbusername
  the user name in the databse

dbpassword
  the password of the user

dynamicLoginRealm
  the realm for basic authentication, this will be displayed in the login dialog

allowed_packages
  a list of allowed packages whcih may be called in this DAD. The format is a
  space separated list of <owner>.<package> . The owner and package name must 
  be uppercase. The check is done after resolving synonyms!

current_schema
  the schema for resolving the called package. This results in a call to
  "alter session set current_schema" when initializing the connection.

*** Logging
log4j.properties setze 
log4j.rootLogger=INFO, A1
oder
log4j.rootLogger=DEBUG, A1

Use the INFO level, there is much less output
