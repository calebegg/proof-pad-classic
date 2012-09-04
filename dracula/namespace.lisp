(in-package "ACL2")

; Prepend a prefix onto all symbols in x starting with a %. 
(defun namespace%replace (x prefix)
   (cond ((symbolp x)
          (let ((name (symbol-name x)))
             (cond ;; %SYMBOL -> PREFIX%SYMBOL
                   ((and (<= 1 (length name))
                         (equal (char name 0) #\%))
                    (intern-in-package-of-symbol 
                     (concatenate 'string prefix name) x))
                   
                   ;; *%SYMBOL* -> *PREFIX%SYMBOL*
                   ((and (<= 3 (length name))
                         (equal (char name 0) #\*)
                         (equal (char name 1) #\%)
                         (equal (char name (1- (length name))) #\*))
                    (intern-in-package-of-symbol 
                     (concatenate 'string 
                                  "*" 
                                  prefix
                                  (subseq name 1 (- (length name) 1))
                                  "*") x))
                   
                   ;; SYMBOL -> SYMBOL
                   (t x))))
         ((consp x)
          (cons (namespace%replace (car x) prefix)
                (namespace%replace (cdr x) prefix)))
         (t x)))

(defun namespace%subst (namespace form)
   (namespace%replace form (string-upcase (symbol-name namespace))))

(defmacro push-namespace (namespace-name)
   (if (not (symbolp namespace-name))
       (er hard 'pop-namespace "PUSH-NAMESPACE expects a symbol")
   	  `(table namespace 'stack 
               (cons ',namespace-name
               (cdr (assoc-eq 'stack (table-alist 'namespace world)))))))

(defun pop-namespace-error-message (expected-namespace actual-namespace)
   (concatenate 'string
                "POP-NAMESPACE attempted to pop "
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

(defmacro in-namespace (form &optional namespace)
   (if namespace
       (namespace%subst namespace form)
   	`(make-event
   		(mv-let (er stack state)
     	   	(table namespace 'stack)
             	(mv (or er
                      (and (null stack)
                          "No namespace has been entered. Use PUSH-NAMESPACE."))
                 	(namespace%subst (car stack) ',form)
                 	state)))))

(defmacro def-namespace-form (form-name)
   (let ((form-name% (intern-in-package-of-symbol
                      (concatenate 'string (symbol-name form-name) "%") 'namespace-replace)))
   `(defmacro ,form-name% (&rest args)
     `(in-namespace (,',form-name ,@args)))))

(def-namespace-form defconst)
(def-namespace-form defun)
(def-namespace-form defmacro)
(def-namespace-form encapsulate)
