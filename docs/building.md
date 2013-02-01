---
layout: doc
title: Building executables
category: /docs/
---

Building executables
====================

Proof Pad allows you to build native executable files from the ACL2 code you
write.

1.  At the top of your file (but below your book imports), add the line:

        :set-state-ok t

    This allows you to use the special variable `state` in your code. `state` is
    required for many types of input and output in ACL2. Programming with
    `state` is challenging, but necessary if you want to really take advantage
    of ACL2, especially as a non-interactive executable. You can find more
    information about `state` at [the ACL2
    documentation](http://www.cs.utexas.edu/users/moore/acl2/current/STATE.html).

2.  Create a `main` function that takes one argument, `state`. If you don't need
    to use `state`, use `(declare (ignore state))` to ignore it:

        (defun main (state)
           (declare (ignore state))
           (...))

    In any case, the body of `main` should contain the code you want to run when
    the executable is run. It should be the entry point to your application.

3.  Click "Tools > Build" or the hammer button on the toolbar to build. If you
    have not saved the file, you will be prompted to do so; otherwise, you will
    be prompted to save the executable. Building should take a few seconds for
    small files, but can take much longer for large projects.

Tips
----

* Test and debug your code in Proof Pad before building it; built executables do
not give very good error feedback.
