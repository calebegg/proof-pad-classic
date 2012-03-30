
(in-package "ACL2")

(defun expand-defproperty-implication (hyps test)
  `(implies (and ,@hyps) ,test))

(defun expand-defproperty-hypotheses (vars)
  (case-match vars
    ((':where hyp . rest)
     (cons hyp (expand-defproperty-hypotheses rest)))
    ((':value & . rest)
     (expand-defproperty-hypotheses rest))
    ((':limit & . rest)
     (expand-defproperty-hypotheses rest))
    ((& . rest) (expand-defproperty-hypotheses rest))
    (nil nil)))

(defun expand-defproperty-body (body)
  (case-match body
    ((':repeat & . rest) (expand-defproperty-body rest))
    ((':limit & . rest) (expand-defproperty-body rest))
    ((vars test . options)
     (cons
      (expand-defproperty-implication
       (expand-defproperty-hypotheses vars) 
       test)
      options))))

(defmacro defproperty (name &rest rest)
  `(defthm ,name ,@(expand-defproperty-body rest)))

(defmacro defrandom (name args body)
  `(encapsulate () (program) (defun ,name ,args ,body)))

(defrandom random-boolean () t)
(defrandom random-symbol () 'symbol)
(defrandom random-char () #\c)
(defrandom random-string () "string")
(defrandom random-number () 0)
(defrandom random-rational () 0)
(defrandom random-integer () 0)
(defrandom random-natural () 0)
(defrandom random-data-size () 1)
(defrandom random-between (lo hi) (min lo hi))
(defrandom random-atom () nil)
(defrandom random-sexp () (cons 'sexp nil))
(defrandom random-element-of (xs) (first xs))

(defmacro random-case (first second &rest rest)
  (declare (ignore rest))
  `(if (random-boolean) ,first ,second))

(defmacro random-sexp-of (atom &key size)
  (declare (ignore size))
  `(if (random-boolean) (cons ,atom ,atom) ,atom))

(defmacro random-list-of (elem &key size)
  (declare (ignore size))
  `(if (random-boolean) (list ,elem) nil))

(defmacro check-properties () '(progn))
