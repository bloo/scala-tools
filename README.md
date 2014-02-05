
Scala Tools
===========

These are a collection of Scala utilities that I commonly use across projects. They were
also the earlier pieces of code I cut my Scala teeth on, and continue to develop as I
learn what are best practices, and what I'm doing that are terrible practices.

I build everything using Maven so that I get workspace dependency resolution in Eclipse via m2e plugin. SBT is great, but until I get better IDE dependency management with it, it hurts me more than it helps me.

[![Build Status](https://travis-ci.org/bloo/scala-tools.png?branch=master)](https://travis-ci.org/bloo/scala-tools) Travis-CI build status

[![Stories in Ready](https://badge.waffle.io/bloo/scala-tools.png?label=ready)](http://waffle.io/bloo/scala-tools) Waffles.io backlog

common
------

Basic, common utilities for use in any application.

* __email__ easy SMTP mailer and ```javax.mail.Session``` factory
* __errors__ application error manager that allows you to define unique error codes and messages
* __log__ Logger trait, depends on ```slf4j```
* __scalate__ [Scalate](http://scalate.fusesource.org/) templating engine and rendering manager, used by other utlities in this suite

slick
-----

Comprehensive toolkit for bootstrapping an application with [Slick from Typesafe](http://slick.typesafe.com/). Includes driver
abstractions for PostgreSQL, H2, and MySQL, and simple to use traits to enable transactional scopes (read-only, read-write) in your code.

Lots of concepts from this [42go](http://eng.42go.com/scala-slick-database-drivers-type-safing-session-concurrency/)
engineering blog post.

unfiltered-core
---------------

Basic set of helpers for the [unfiltered web toolkit](http://unfiltered.databinder.net/Unfiltered.html), which contains:
* __errors__ extension of ```common.errors``` that allows for http status codes
* __params__ common request parameter extractors
* __scalate__ a Scalate template engine that returns unfiltered ```ResponseWriter``` instances and defines default template locations

unfiltered-api-core
------------------

The bulk of this project surrounds the ```Resource``` class, which constructs an __unfiltered__ plan that handles __RESTful__ resource definitions.

unfiltered-api-auth
-------------------

An extension of __unfiltered-api-core__ that defines an authorized ```Session``` resource, for web applications that require it.

unfiltered-wro
--------------

An __unfiltered__ plan that services [WRO4J](https://github.com/alexo/wro4j) web resources. I use this to serve SASS, Coffeescript, and group together external libraries as a single resource URL.

specs2-core
-----------

Base utilities for [Specs](http://etorreborre.github.io/specs2/) 2 testing infrastructure  (unimplemented).

specs2-api
----------

Base harness for testing my __unfiltered-api-core__ ```api.Resource``` plans.

specs2-slick
------------

Base harness for testing __slick__ code against a database, and a way to rollback changes.

specs2-wro
----------

Base harness for testing __WRO__ code (unimplemented).

