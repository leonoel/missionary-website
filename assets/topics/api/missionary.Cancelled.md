# `missionary.Cancelled`

Usage :
* `(import 'missionary.Cancelled)`
* `(try ,,, (catch Cancelled e ,,,))`
* `(Cancelled. "Operation interrupted.")`

A throwable type returned by missionary operators to indicate that the process crashed due to cancellation prior to
normal completion. This type can be used in `catch` clauses to recover from process cancellation.

Example : run a side effect when a task terminates due to cancellation
```clojure
(require '[missionary.core :as m])
(import 'missionary.Cancelled)

(def my-sleep
  (m/sp
    (try (m/? (m/sleep 1000))
         (catch Cancelled e
           (prn :sleep-cancelled)
           (throw e)))))

(m/? (m/timeout my-sleep 500 :timeout))    ;; :sleep-cancelled
:= :timeout
```

Third party asynchronous operations may have a different behavior in reaction to cancellation. For instance, on the JVM
it is common to throw an instance of `java.lang.InterruptedException` when a thread running a blocking method call is
interrupted. You may want to turn this exception to an instance of `Cancelled` to standardize your effects.

Example :
```clojure
(require '[missionary.core :as m])
(import 'missionary.Cancelled)

(defn jvm-sleep [d]
  (m/via m/blk
    (try (Thread/sleep d)     
         (catch InterruptedException _
           (throw (Cancelled. "Sleep cancelled."))))))

(def my-sleep
  (m/sp
    (try (m/? (jvm-sleep 1000))
         (catch Cancelled e
           (prn :sleep-cancelled)
           (throw e)))))

(m/? (m/timeout my-sleep 500 :timeout))    ;; :sleep-cancelled
:= :timeout
```