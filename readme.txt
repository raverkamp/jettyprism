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


All files are resolved local to the directory of the file, i.e. "." will be the
directory opf the file.

*** Properties:
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


prismconf
The name of the config file for prims. The default is "prism.xconf".


*** Logging
log4j.properties setze 
log4j.rootLogger=INFO, A1
oder
log4j.rootLogger=DEBUG, A1

Use the INFO level, there is much less output
