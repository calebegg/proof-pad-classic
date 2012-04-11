(in-package "ACL2")

(include-book "testing" :dir :teachpacks)

(set-state-ok t)

(defun sum (xs)
  (if (endp xs)
      0
      (+ (first xs)
         (sum (rest xs)))))

(defun factorial (n)
  (if (zp n)
    1
    (* n (factorial (- n 1)))))

(defthm factorial-minus-one
   (implies (and (natp n) (> n 1))
            (= (* n (factorial (- n 1)))
               (factorial n))))

(check-expect (factorial 5) 120)
(check-expect (factorial 2) 2)
(check-expect (factorial 0) 1)

(defun main (state)
  (mv (cw "~x0~%" (factorial 20)) state))
