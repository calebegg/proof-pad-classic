(defun rep (n x)
   (if (zp n)
       nil
       (cons x (rep (- n 1) x))))

(defun allequal (xs)
   (if (endp (rest xs))
       t
       (and (equal (first xs) (second xs))
            (allequal (rest xs)))))

(defthm rep-lemma
   (implies (consp (rep n x))
            (equal (car (rep n x)) x)))

(defthm rep-allequal
   (implies (natp n)
            (allequal (rep n x))))

(defthm member-rep
   (implies (and (natp n) (> n 0))
            (iff (member y (rep n x))
                 (member y (list x)))))