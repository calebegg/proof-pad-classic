; Random data generating functions using the same API as DrRacket's doublecheck.
(in-package "ACL2")
(include-book "namespace")

(push-namespace defrandom)

(defmacro% %scramble-step (n magic op1 op2 shift)
  `(logand #xffffffff (,op2 (,op1 ,n ,magic) (ash ,n ,shift))))

;; Robert Jenkins' 32 bit integer hash function. http://www.concentric.net/~ttwang/tech/inthash.htm
(defun% %scramble (n)
  (let* ((n (%scramble-step n #x7ed55d16 + + 12))
         (n (%scramble-step n #xc761c23c logxor logxor -19))
         (n (%scramble-step n #x165667b1 + + 5))
         (n (%scramble-step n #xd3a2646c + logxor 9))
         (n (%scramble-step n #xfd7046c5 + + 3))
         (n (%scramble-step n #xb55a4f09 logxor logxor -16)) )
    n))

(defun% %hash (x y)
  (%scramble (logxor x y)))


(defun% %symbol-append (prefix suffix)
  (intern (concatenate 'string
                       (symbol-name prefix)
                       (symbol-name suffix))
          "ACL2"))

(defun% %call-with-random-seed (name args)
  `(,name ,@args random-seed))


(defun% %replace-placeholders (tree so-far)
  (if (consp tree)
      (cons (%replace-placeholders (car tree) (ash so-far 1))
            (%replace-placeholders (cdr tree) (1+ (ash so-far 1))))
      (if (equal tree :defrandom-placement) so-far tree)))

(set-state-ok t)

(defun% %expand (form state)
  (declare (xargs :mode :program))
  (mv-let
   (flg val bindings state)
   (translate1 form :stobjs-out '((:stobjs-out . :stobjs-out)) t 'top-level (w state) state)
   (declare (ignore bindings))
   (mv flg val state)))

(defun% %placify (x state)
  (declare (xargs :mode :program))
  (mv-let (er x-prime state)
          (%expand x state)
          (mv er (%replace-placeholders x-prime 1) state)))


(defun% %defrandom-fn (name args expr)
  (let ((fn-name (%symbol-append name '-fn))
        (call-name (%symbol-append name '!)))
    `(progn
      
      
      ;; The actual logic of the function. Note that the expr here is already 
      ;; macroexpanded, and has :defrandom-placement replaced with actual placements.
      (defun ,fn-name ,(append args (list 'random-seed))
        ,expr)
      
      ;; It is useful to be able to call these random functions standalone for testing.
      ;; (my-random-thing! 42) does that.
      (defmacro ,call-name ,args
        `(mv-let (seed state)
                 (random$ #xffffffff state)
                 (mv nil (,',fn-name ,@(list ,@args) seed) state))))))

(defmacro% defrandom (name args expr)
  (let ((fn-name (%symbol-append name '-fn)))
    `(progn
      ;; The macro that other defrandoms will call passes on whatever arguments
      ;; were given as well as the seed, updated based on this call's placement.
      ;; We have to define this before the make-event in case this is a recursive
      ;; function - the function body will need to macroexpand the macro entry 
      ;; point, in that case.
      (defmacro ,name ,args
        `(,',fn-name ,@(list ,@args) (%hash random-seed :defrandom-placement)))
      
      (make-event
       (er-let* (;; Make a stub so that a if it macroexpands MY-RANDOM and finds
                 ;; MY-RANDOM-FN, it will not complain.
                 (defun-result (defun-fn '(,fn-name (,@args random-seed-arg) (list ,@args random-seed-arg)) state nil))
                 ;; Actually macroexpand
                 (placed-expr (%placify ',expr state)))
                (mv nil (%defrandom-fn ',name ',args placed-expr) state))))))

(def-namespace-form defrandom)
; For when you want to deal with the seed directly. A user of this library will typically
; not do this. The call will look like
; (defrandom-raw my-raw-random-function
;   (+ seed 5))
(defmacro% defrandom-raw (name expr)
  (let ((fn-name (%symbol-append name '-fn))
        (call-name (%symbol-append name '!)))
    `(progn (defun ,fn-name (seed)
              ,expr)
            (defmacro ,name () 
              `(,',fn-name (%hash random-seed :defrandom-placement)))
            
            (defmacro ,call-name ()
              `(mv-let (seed state)
                       (random$ *random-seed-ceiling* state)
                       (mv nil (,',fn-name seed) state))))))

(defrandom-raw random-boolean
  (logtest seed #x004000))

(defrandom% %random-below-2^n (pow so-far)
  (if (zp pow) so-far
      (%random-below-2^n (- pow 1)
                        (logior (ash so-far 1) 
                                (if (random-boolean) 1 0))))) 

(defrandom% %random-below-attempts (n bits tries)
  (if (zp tries)
      0
      (let ((try (%random-below-2^n bits 0)))
        (if (< try n)
            try
            (%random-below-attempts n bits (- tries 1))))))


(defrandom% %random-below (n)
  (%random-below-attempts n (integer-length n) 50))

; low <= result <= high
(defrandom% random-between (low high)
  (+ low (%random-below (+ high (- low) 1))))

(defrandom% random-element-of (xs)
  (nth (random-between 0 (- (length xs) 1)) xs))

(defrandom% random-char ()
  (code-char (random-between (char-code #\a) (char-code #\z))))

(defrandom% %random-char-list (n)
  (if (zp n)
      nil
      (cons (random-char) (%random-char-list (1- n)))))

(defconst% *%string-length-distribution*
   '(179 879 1120 952 559 289 87 19 11 1))

(make-event
   `(defconst% *%string-length-distribution-sum*
   		(+ ,@*defrandom%string-length-distribution* )))

(defun% %pick-from-distribution (distribution n idx )
   (if (or (endp distribution) (< idx (first distribution)))
       n
       (%pick-from-distribution (rest distribution)
                                (1+ n)
                                (- idx (first distribution)))))

(defrandom% %random-string-length ()
   (%pick-from-distribution *%string-length-distribution* 0 
                            (%random-below *%string-length-distribution-sum*)))

(defrandom% %string-of-length (n)
   (coerce (%random-char-list n) 'string))

(defrandom% random-string ()
  (%string-of-length (%random-string-length)))

(defrandom% random-symbol ()
  (intern (string-upcase (%string-of-length (1+ (%random-string-length)))) "ACL2"))

(defun% %number-cases (cases n)
  (if (endp cases)
      nil
      (cons (list n (first cases))
            (%number-cases (rest cases) (1+ n)))))


(defmacro% random-case (&rest cases)
  `(case (random-between 1 ,(length cases))
     ,@(%number-cases cases 1)))


(defrandom% %random-diminishing-interp (steps low high)
  (if (or (zp steps)
          (<= high low))
      low
      (let ((cut (+ low (ash (- high low) -2))))
        (if (random-boolean)
            (%random-diminishing-interp (- steps 1) low cut)
            (%random-diminishing-interp (- steps 1) (1+ cut) high)))))

(defrandom% %random-diminishing-doubling (steps sust so-far)
  (if (or (zp steps)
          (random-boolean))
      (+ so-far
         (%random-diminishing-interp steps 1 sust))
      (%random-diminishing-doubling (- steps 1) sust (+ so-far sust))))

; This is not any nice mathematical distribution, but in general larger numbers
; are less likely than smaller numbers. It generates at least 1. sust controls
; how quickly it falls off; higher sust means higher numbers.
(defrandom% %random-diminishing (sust)
  (%random-diminishing-doubling 1000 sust 0))

(defrandom% random-positive-integer ()
  (%random-diminishing 512))

(defrandom random-natural ()
  (- (random-positive-integer) 1))

(defrandom random-integer ()
  (random-case
   (random-natural)
   (- (random-positive-integer))))

(defrandom random-rational ()
  (random-case
   (random-integer)
   (/ (random-natural)
      (random-positive-integer))))           

(defrandom random-number ()
  (complex (random-rational)
           (random-rational)))

(defrandom random-atom ()
  (random-case
   (random-boolean)
   (random-symbol)
   (random-string)
   (random-char)
   (random-number)))

(defrandom% random-data-size ()
  (%random-diminishing 4))

(defun% %ensure-less (x upper-bound)
  (nfix (if (< x upper-bound)
            x
            0)))

(encapsulate%
 nil
 (local 
   (defthm ensure-less-works
     (implies (not (zp upper-bound))
              (< (%ensure-less x upper-bound)
                 upper-bound))))
 
 
 (local (in-theory (disable binary-logxor %hash lognot %ensure-less)))
 
 (defrandom %random-sexp-of-size (size)
   (if (zp size)
       (random-atom)
       (let* ((left (random-between 0 (- size 1)))
              (right (+ size (- left)  -1)))
         (cons (%random-sexp-of-size (%ensure-less left size))
               (%random-sexp-of-size (%ensure-less right size)))))))

(defrandom% random-sexp ()
  (%random-sexp-of-size (random-data-size)))

(pop-namespace defrandom)

