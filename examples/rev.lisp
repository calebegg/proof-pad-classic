(in-package "ACL2")

(defun app (xs ys)
   (if (endp xs)
       ys
       (cons (first xs)
             (app (rest xs) ys))))

(defun rev (xs)
   (if (endp xs)
       nil
       (app (rev (rest xs))
            (list (first xs)))))

(defthm rev-rev
   (implies (true-listp xs)
            (equal (rev (rev xs))
                   xs)))