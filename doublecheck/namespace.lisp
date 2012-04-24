(in-package "ACL2")

(defun namespace-replace (x prefix)
   (cond ((symbolp x)
          (let ((name (symbol-name x)))
             (cond ((zp (length name)) x)
                   ((not (equal (char name 0) #\%)) x)
                   ((and (<= 2 (length name))
                         (equal (char name 1) #\%))
                    (intern-in-package-of-symbol (subseq name 1 (- (length name) 1)) x))
                   (t  
                    (intern-in-package-of-symbol 
                     (concatenate 'string prefix name) x)))))
         ((consp x)
          (cons (namespace-replace (car x) prefix)
                (namespace-replace (cdr x) prefix)))
         (t x)))

(defun namespace-subst (namespace form)
   (namespace-replace form (string-upcase (symbol-name namespace))))

;(defmacro in-namespace (namespace form)
;   (namespacify namespace form))


(defmacro push-namespace (namespace-name)
   `(table namespace 'stack 
       (cons ',namespace-name
             (cdr (assoc-eq 'stack (table-alist 'namespace world))))))

(defun pop-namespace-error-message (expected-namespace actual-namespace)
   (concatenate 'string
                "POP-namespace attempted to pop "
                (symbol-name expected-namespace)
                ", but " (symbol-name actual-namespace)
                " was on the top of the stack!"))


(defmacro pop-namespace (namespace-name)
   `(make-event
      (mv-let (err stack state)
              (table namespace 'stack)
         (cond (err (mv err nil state))
               ((equal (car stack) ',namespace-name)
            	 (mv nil `(table namespace 'stack ',(cdr stack)) state))
               (t (mv (pop-namespace-error-message ',namespace-name (car stack)) nil state))))))

; TODO - check for empty stack
(defmacro in-current-namespace (form)
   `(make-event
   	(mv-let (er stack state)
     	   (table namespace 'stack)
             (mv er
                 (namespace-subst (car stack) ',form)
                 ;(namespace-replace ,form (string-upcase (symbol-name active-namespace))))
                 state))))

(defmacro def-namespace-form (form-name)
   (let ((form-name% (intern-in-package-of-symbol
                      (concatenate 'string (symbol-name form-name) "%") 'namespace-replace)))
   `(defmacro ,form-name% (&rest args)
     `(in-current-namespace 
                 (,',form-name ,@args)))))

(def-namespace-form defun)
(def-namespace-form defmacro)
(def-namespace-form encapsulate)
