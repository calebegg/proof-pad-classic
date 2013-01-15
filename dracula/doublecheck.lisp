(in-package "ACL2")

(set-state-ok t)

(defconst *default-repeat* 50)
(defconst *default-limit* 60)

;; Data generators

(defun geometric-from-uniform (un mx p)
   (if (< un (* mx p))
       0
       (+ 1 (geometric-from-uniform
             (- un (* mx p))
             (- mx (* mx p))
             p))))

(defun random-geometric-fn (p state)
   (mv-let (un state)
           (random$ 1000000 state)
      (mv (geometric-from-uniform un 1000000 p) state)))

(defmacro random-geometric (p)
  `(random-geometric-fn ,p state))

(defun random-between-fn (low high state)
  (mv-let (random state)
    (random$ (- (+ 1 high) low) state)
    (mv (+ random low) state)))

(defmacro random-between (low high)
   `(random-between-fn ,low ,high state))

(defabbrev random-natural ()
   (random-geometric 1/32))

(defun random-positive-fn (state)
   (mv-let (rnd state)
           (random-natural)
      (mv (+ 1 rnd) state)))

(defmacro random-positive ()
   `(random-positive-fn state))

(defun random-integer-fn (state)
   (mv-let (rnd state)
           (random$ 2 state)
      (if (= rnd 1)
          (random-natural)
          (mv-let (rnd state)
                  (random-natural)
             (mv (- rnd) state)))))

(defmacro random-integer ()
   `(random-integer-fn state))

(defmacro random-rational ()
   `(mv-let (a state)
            (random-integer)
       (mv-let (b state)
               (random-integer)
          (mv (/ a (if (= b 0) 1 b)) state))))

(defmacro random-complex ()
   `(mv-let (a state)
            (random-rational)
       (mv-let (b state)
               (random-rational)
          (mv (complex a b) state))))

(defabbrev random-data-size ()
   (random-geometric 1/4))

(defmacro random-number ()
   `(mv-let (which state)
            (random-between 0 3)
       (cond ((= which 0)
              (random-integer))
             ((= which 1)
              (random-rational))
             (t
              (random-complex)))))

(defun random-natural-list-of-length-fn (n state)
   (if (zp n)
       (mv nil state)
       (mv-let (xs state)
               (random-natural-list-of-length-fn (1- n) state)
          (mv-let (val state)
                  (random-natural)
             (mv (cons val xs) state)))))

(defmacro random-natural-list ()
   `(mv-let (ln state)
            (random-data-size)
       (random-natural-list-of-length-fn ln state)))

(defmacro random-natural-list-of-length (ln)
   `(random-natural-list-of-length-fn ,ln state))

(defun random-integer-list-of-length-fn (n state)
   (if (zp n)
       (mv nil state)
       (mv-let (xs state)
               (random-integer-list-of-length-fn (1- n) state)
          (mv-let (val state)
                  (random-integer)
             (mv (cons val xs) state)))))

(defmacro random-integer-list ()
   `(mv-let (ln state)
            (random-data-size)
       (random-integer-list-of-length-fn ln state)))

(defmacro random-integer-list-of-length (ln)
   `(random-integer-list-of-length-fn ,ln state))

(defun random-digit-list-of-length-fn (n state)
   (if (zp n)
       (mv nil state)
       (mv-let (xs state)
               (random-digit-list-of-length-fn (1- n) state)
          (mv-let (val state)
                  (random-between 0 9)
             (mv (cons val xs) state)))))

(defmacro random-digit-list ()
   `(mv-let (ln state)
            (random-data-size)
       (random-digit-list-of-length-fn ln state)))

(defun random-between-list-fn (lo hi ln state)
   (if (zp ln)
       (mv nil state)
       (mv-let (xs state)
               (random-between-list-fn lo hi (1- ln) state)
          (mv-let (rnd state)
                  (random-between lo hi)
             (mv (cons rnd xs) state)))))

(defmacro random-between-list (lo hi)
   `(mv-let (ln state)
           (random-data-size)
       (random-between-list-fn ,lo ,hi ln state)))

(defmacro random-between-list-of-length (lo hi ln)
   `(random-between-list-fn ,lo ,hi ,ln state))

(defun random-increasing-list-fn (ln state)
   (if (zp ln)
       (mv nil state)
       (mv-let (xs state)
               (random-increasing-list-fn (1- ln) state)
          (mv-let (rnd state)
                  (let ((prev (if (consp xs)
                                  (first (last xs))
                                  0)))
                       (random-between prev (+ 50 prev)))
             (mv (append xs (list rnd)) state)))))

(defmacro random-increasing-list ()
   `(mv-let (ln state)
            (random-data-size)
       (random-increasing-list-fn ln state)))

(defmacro random-list-of (&rest args)
   (let ((size (if (and (>= (len args) 3)
                        (eq (second args) ':size))
                   (third args)
                   -1)))
     (cond ((eq (first (first args)) 'random-natural)
            (if (= size -1)
                `(random-natural-list)
                `(random-natural-list-of-length ,size)))
           ((eq (first (first args)) 'random-integer)
            (if (= size -1)
                `(random-integer-list)
                `(random-integer-list-of-length ,size)))
           ((eq (first (first args)) 'random-between)
            (if (= size -1)
                `(random-between-list ,(second (first args))
                                      ,(third (first args)))
                `(random-between-list-of-length
                  ,(second (first args))
                  ,(third (first args))
                  ,size)))
           (t (hard-error nil "Only certain random
                           generators are currently
                           supported in (random-list-of).
                           Go to \"Help > Index\" and click
                           \"doublecheck\" for the full
                           list" nil)))))

;; Defproperty and friends

(defmacro repeat-times (times limit body)
  (if (zp limit)
    `(mv state (hard-error nil
                           "Wasn't able to generate enough
                            data. Check your :where clauses
                            (make sure they are satisfiable)
                            and try increasing the :limit
                            for the property"
                           nil))
    `(if (zp ,times)
         (mv state nil)
         (mv-let (state result assignments)
            ,body
            (mv-let (state rs)
                 (repeat-times (- ,times 
                                  (if (eql result
                                           'where-not-matched)
                                           0 1))
                               ,(- limit 1) ,body)
                 (mv state
                     (cons (cons result assignments)
                           rs)))))))

(defmacro expand-vars (vars body)
  (if (endp vars)
    `(mv state ,body nil)
    `(mv-let (,(first vars) state)
       ,(cond ((eql (second vars) ':value)
               (third vars))
              ((eql (fourth vars) ':value)
               (fifth vars))
              (t (hard-error
                   nil
                   "Missing :value parameter for ~xn"
                   (list (cons #\n (first vars))))))
       (if ,(cond ((eql (second vars) ':where)
                   (third vars))
                  ((eql (fourth vars) ':where)
                   (fifth vars))
                  (t t))
         (mv-let (state result assignments)
           (expand-vars ,(cond ((member (fourth vars)
                                        '(:value :where))
                                (nthcdr 5 vars))
                               (t (nthcdr 3 vars)))
                        ,body)
           (mv state result
               (cons (cons (quote ,(first vars))
                           ,(first vars))
                     assignments)))
         (mv state 'where-not-matched nil)))))

(defun eager-and (x y)
  (and x y))

(defun doublecheck-print-args (args)
   (if (endp args)
       nil
       (prog2$ (cw "  ~p0 = ~p1~%" (first (first args)) (rest (first args)))
               (doublecheck-print-args (rest args)))))

(defun condense-results (rs)
  (if (endp rs)
    t
    (eager-and (let ((success (first (first rs))))
           (prog2$
             (if (not success)
               (prog2$ (cw "Failure case: ~%")
                       (doublecheck-print-args (rest (first rs))))
               (prog2$ (cw "Success case: ~%")
                       (doublecheck-print-args (rest (first rs)))))
             success))
         (condense-results (rest rs)))))

(defmacro defproperty-program (name &rest args)
  (let ((repeat (cond ((eql (first args) ':repeat)
                       (second args))
                      ((eql (third args) ':repeat)
                       (fourth args))
                      (t *default-repeat*)))
        (limit (cond ((eql (first args) ':limit)
                      (second args))
                     ((eql (third args) ':limit)
                      (fourth args))
                     (t *default-limit*)))
        (vars (cond ((<= (len args) 3) (first args))
                    ((<= (len args) 5) (third args))
                    ((<= (len args) 7) (fifth args))))
        (body (cond ((<= (len args) 3) (second args))
                    ((<= (len args) 5) (fourth args))
                    ((<= (len args) 7) (sixth args)))))
    `(mv-let (state results)
       (repeat-times ,repeat ,limit
                     (expand-vars ,vars ,body))
       (if (prog2$
            (cw "DoubleCheck Test Results:~%")
            (condense-results results))
         (mv nil nil state)
         (mv (hard-error nil "Test ~xn failed."
                         (list (cons #\n (quote ,name))))
             nil
             state)))))

;; Code from Dracula begins here
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

(defmacro defproperty-logic (name &rest rest)
  `(defthm ,name ,@(expand-defproperty-body rest)))
;; Code from Dracula ends here

(defmacro defproperty (name &rest args)
   `(mv-let (er val state)
            (table acl2-defaults-table :defun-mode)
       (declare (ignore er))
       (if (eq val :logic)
           (defproperty-logic ,name ,@args)
           (defproperty-program ,name ,@args))))