Random generators
=================

`(random-between low high)` -- Generates a random number uniformly chosen between `low` and `high`, inclusive.

`(random-natural)` -- Generates a random natural number (above zero).

`(random-positive)` -- Generates a random positive number (above one).

`(random-integer)` -- Generates a random integer.

`(random-rational)` -- Generates a random rational number (by dividing two random integers)

`(random-complex)` -- Generates a random complex number (using two random rationals)

`(random-number)` -- Randomly generates either an integer, a rational, or a complex number.

`(random-data-size)` -- Generates a random natural number, but favors smaller numbers. Appropriate if this value is going to determine the amount of data in a structure.

`(random-natural-list)` -- Generates a list of `(random-natural)` values, using a length determined by `(random-data-size)`.

`(random-natural-list-of-length ln)` -- Generates a list of `(random-natural)` values of a specified length. `ln` must be either a fixed natural number (such as 10) or a variable. So, for instance, `(random-natural-list-of-length (random-natural))` will give an error. Instead, generate the length separately: `(n :value (random-natural) xs :value (random-natural-list-of-length n))`

`(random-integer-list)` and `(random-integer-list-of-length ln)` -- similar to above

`(random-digit-list)` and `(random-digit-list-of-length ln)` -- random lists of numbers between 0 and 9

`(random-between-list low high)` and `(random-between-list-of-length low high ln)` -- similar to above; generates values between `low` and `high` inclusive (using `(random-between low high)`).

`(random-increasing-list)` -- A list of values that strictly go up (a sorted list).

**Note:** The below `random-list-of` versions of the above functions are included in Proof Pad for compatibility with DrACuLa; They don't add any functionality over the above forms. Additionally, unlike in DrACuLa, the parameters to `random-list-of` must match one of the below templates, or they won't work. I recommend that you use the above generators instead.

`(random-list-of (random-natural))` -- Alternate syntax for `(random-natural-list)`

`(random-list-of (random-natural) :size ln)` -- Alternate syntax for `(random-natural-list-of-length ln)`

`(random-list-of (random-integer))` -- Alternate syntax for `(random-integer-list)`

`(random-list-of (random-integer) :size ln)` -- Alternate syntax for `(random-integer-list-of-length ln)`

`(random-list-of (random-between low high))` -- Alternate syntax for `(random-between-list low high)`

`(random-list-of (random-between low high) :size ln)` -- Alternate syntax for `(random-between-list-of-length low high ln)`
