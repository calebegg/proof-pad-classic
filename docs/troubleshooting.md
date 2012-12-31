---
layout: doc
title: Troubleshooting
category: /docs/
---

Troubleshooting and Known Issues
================================

*If you have an issue that can't be fixed after trying the steps on this page,
feel free to [contact the developer.](mailto:calebegg@gmail.com)*

Invalid or corrupted jar
------------------------

Proof Pad requires an up-to-date installation of Java 7 (or Java 6 on OS X).
Make sure your version of Java is up-to-date. If it is, try [reinstalling
Java](http://java.com).

ACL2 terminates
---------------

If you get a message about ACL2 terminating when you start up Proof Pad, then
the version of ACL2 that comes with Proof Pad is not compatible with your
system. One thing to try is installing a separate version of ACL2 from [the ACL2
website](http://www.cs.utexas.edu/~moore/acl2/). After that, point Proof Pad to
your separate installation by going to <span class="win-show">Tools >
Options</span><span class="osx-show">Proof Pad > Preferences...</span><span
class="linux-show">Edit > Preferences</span><span
class="none-show">Preferences</span>, check "Use custom ACL2", and direct Proof
Pad to your custom ACL2 installation.

There's an older Windows version of ACL2
[here](http://www.cs.utexas.edu/users/moore/acl2/v3-6/distrib/windows/) that's
been especially reliable.  It's pretty out of date, but should work fine as a
last resort.
