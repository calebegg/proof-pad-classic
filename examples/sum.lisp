(defun fac (n)
   (if (zp n)
       1
       (* n (fac (- n 1)))))

(defun sum (xs)
   (if (endp xs)
       0
       (+ (first xs)
          (sum (rest xs)))))

(defthm sum-append-2
   (= (sum (append xs ys))
      (+ (sum xs) (sum ys))))

(defun concat (xss)
   (if (endp xss)
       nil
       (append (first xss)
               (concat (rest xss)))))

(defun sum-each (xss)
   (if (endp xss)
       nil
       (cons (sum (first xss))
             (sum-each (rest xss)))))

(defthm sum-append
   (= (sum (concat xss))
      (sum (sum-each xss))))

(defun nats-up-to (n)
   (if (zp n)
       nil
       (append (nats-up-to (- n 1))
               (list n))))

(include-book "arithmetic-3/top" :dir :system)

(defthm sum-nats
   (implies (natp n)
            (= (sum (nats-up-to n))
               (/ (* n (+ n 1)) 2))))
