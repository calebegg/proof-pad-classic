;; Welcome to ACL2_IDE_NAME

;; This tutorial will give you a brief introduction to
;; the features of ACL2 and the IDE itself.

;; You are editing a copy of the original tutorial, so
;; feel free to edit or save it.

;; First of all, try admitting this ACL2 function call
;; by clicking the grey "proof bar" to the left:

(+ 2 2)

;; After admitting it, the proof bar turns green, to
;; indicate that it admitted successfully. In this case,
;; "admitted" just means that ACL2 could run it without any
;; errors.

;; Admitting (+ 2 2) doesn't seem very useful. This top area
;; is known as the "definitions" pane, and it is where you
;; can put your definitions and events. Events are functions
;; that change the logical world, such as defun.

;; Here's a function definition. Try admitting it:

(defun fac (n)
  (if (zp n)
    1
    (* n (fac (- n 1)))))

;; Now that fac has been admitted, you can execute it. But
;; instead of executing it up here, in the defintiions area,
;; try executing it below, in the read-eval-print loop
;; (REPL). The REPL allows you to type arbitrary function
;; calls and shows the results. Type:

; (fac 20)

;; into the REPL and click "Run" (or press enter) to run it.

;; Built-in functions

;; Built-in types

;; Defun

;; Theorems

;; IO

;; Build button

(defun main (state)
  (write "Hello, World!" "helloworld.txt"))