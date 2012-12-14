Tutorial: reverse of reverse
============================

Step 1: Define reverse
----------------------

`(reverse xs)` is a built-in function that takes a list and returns a list with
all of the elements in the opposite order. We'll be defining another version of
it called `rev`.

In order to define a recursive function in ACL2, we need to think about what it
would return in a couple of different cases. The first case is when the
argument, `xs`, is empty (or `nil`). The reverse of an empty list is just an
empty list:

    (rev nil) = nil

Now, what if `xs` is not empty? If it's not empty, we can break it up into its
first element and the rest of the list, using `(first xs)` and `(rest xs)`,
respectively. We want to take these parts and assemble a new, reversed list. We
can just use `(rev (rest xs))` to reverse the rest of the list, but what do we
do with `(first xs)`?

Consider an example: `(rev (list 1 2 3 4 5))`. After we split it up into two
parts, `1` and `(list 2 3 4 5)`, we can reverse the list part to get `(list 5 4
3 2)`. And what we want for the whole list is `(list 5 4 3 2 1)`. So we need to
put `1` at the end of the reversed list.

    (rev xs) = (put-at-end (first xs) (rev (rest xs)))

For `(put-at-end x xs)`, we can use `(append)`:

    (defun put-at-end (x xs)
      (append xs (list x)))

Putting it all together, we get:

    (defun rev (xs)
      (if (endp xs) ; Test if xs is empty
          nil
          (put-at-end (first xs)
                      (rev (rest xs)))))

Step 2: Test reverse
--------------------

Now that we have a good working definition for reverse, we need to test it to
see that it works.

The quickest way to test a function you've defined is with the REPL. The REPL is
the part of Proof Pad below the definitions area. After you've typed the
definitions for `put-at-end` and `rev` into the main definitions area, type
`(rev (list 1 2 3 4 5))` into the text field at the very bottom of Proof Pad,
and click "Run" (or press "Enter"). You instantly see the result, which is `(5 4
3 2 1)`.

We can automate this process to make sure that `rev` continues to match our
expectations, even if we change or rewrite it. The simplest automatic test
provided by Proof Pad is `check-expect`. To use it, first include the "testing"
book:

    (include-book "testing" :dir :teachpacks)

Now write a test like this:

    (check-expect (rev (list 1 2 3 4 5)) (list 5 4 3 2 1))

`check-expect` will automatically run the test and show a green bar to the left
when it passes. A test passes when the two arguments to `check-expect` evaluate
to the same thing (in this case, the list `(5 4 3 2 1)`).

We could write some more check-expect style tests, but they can only get us so
far. It would be even better if we could write a test that will check several
types of lists to see that our function does what we want. To do this, we need
to write a property-style test. First, include the "doublecheck" book
(DoubleCheck is the name of the testing library):

    (include-book "doublecheck" :dir :teachpacks)

A doublecheck test has three parts: a name, a set of data generators, and a
body. The body is evaluated several times with different, randomly generated
data, and if it evaluates to `t` (which is Lisp's version of `true`), then the
test passes. If a single case fails, the test fails.

One property we can test with DoubleCheck is that reversing a list twice gives
you the same list you started with. I'll start by showing the test, then talk
about the parts:

    (defproperty rev-rev-test
      (xs :value (random-integer-list))
      (equal (rev (rev xs)) xs))

The name of the test is `rev-rev-test`. The name isn't important, except that
each one has to be unique. This test has one generator. It generates values
called `xs`, using the `(random-integer-list)` generator. You can see what kinds
of values this generator returns by typing it in the REPL.

Finally, we have the body: we want to show that `(rev (rev xs))` is equal to
just `xs`.

To run the test, just paste it into the definitions area. It will run, and if it
passes, a green bar will appear to the left of the test.

Try changing the `nil` in the definition of `rev` above to something else, like
`1`. You can see that this makes the test fail. When a test fails, it shows you
which cases it failed on. In this case, it fails all cases, but it might help
you to diagnose the problem if only some of the cases fail too. When a test
fails, it's often a good idea to go back to the REPL and try some of the failed
values to see what you get.

Step 3. Proving `rev-rev`
-------------------------

DoubleCheck properties are nice, but they aren't perfect. They only test the
types of values that you generate, and you might have a bug that impacts only a
small subset of cases, which means the generated test cases are unlikely to
expose the bug.

To strengthen our properties, we can run them through ACL2's theorem prover to
try to prove that it holds for all input values, not just the randomly selected
ones that DoubleCheck uses.

In this case, our test as written will fail. The reason for the failure is that
it doesn't quite hold for any value of `xs`. In particular, to ACL2's logic,
`(rev 1)` = `nil`. If you try to run `(rev 1)` in the REPL, you'll get a guard
error. Guards are restrictions on what values a function will take; in this
case, `(endp xs)` expects a list, and we gave it a number. However, ACL2's
internal logic will run the theorem just fine, with `(endp xs)` returning `t`
when `xs` is 1, making `(rev 1)` return `nil`.

In order to correct this, we need to add a hypothesis to our property:

    (defproperty rev-rev
      (xs :value (random-integer-list)
          :where (true-listp xs))
      (equal (rev (rev xs)) xs))

This way, ACL2 will know to only concern itself with values that satisfy
`true-listp` -- values that are lists.

With this correction in place, you can run the property through ACL2's logic by
clicking on the green bar to the left of it. If everything has been entered
correctly, ACL2 will succeed, and the bar will turn dark green (with a
checkmark), indicating that the property has been proven correct.

The whole file so far is:

    (include-book "testing" :dir :teachpacks)
    (include-book "doublecheck" :dir :teachpacks)

    (defun put-at-end (x xs)
      (append xs (list x)))

    (defun rev (xs)
      (if (endp xs) ; Test if xs is empty
          nil
          (put-at-end (first xs)
                      (rev (rest xs)))))

    (check-expect (rev (list 1 2 3 4 5))
                  (list 5 4 3 2 1))

    (defproperty rev-rev
      (xs :value (random-integer-list)
          :where (true-listp xs))
      (equal (rev (rev xs)) xs))
