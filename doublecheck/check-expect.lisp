;;;; Caleb Eggensperger

(in-package "ACL2")

(set-state-ok t)

(program)

(defun check-expect-fn (left right state)
  (if (equal left right)
    (mv t (cw "check-expect succeeded~%") state)
    (er soft nil "check-expect failed.
    Expected: ~x0
    Actual:   ~x1" left right)))

(defmacro check-expect (left right)
  `(check-expect-fn ,left ,right state))

(logic)