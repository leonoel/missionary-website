# Synchronizers

Synchronizers are syntactic elements allowing to define a sequential composition of asynchronous effects with the full
expressive power of imperative programming, while keeping precise control over boundaries with the purely functional
space.

Missionary has 4 primitive synchronizers :
* [`!`](/api/missionary.core/check.html) - interruption check
* [`?`](/api/missionary.core/park.html) - park on asynchronous result
* [`?<`](/api/missionary.core/switch.html) - switch from successive states
* [`?>`](/api/missionary.core/flatten.html) - flatten nested events streams

Macros can be used to derive synchronizers from the primitive ones. A derived synchronizer inherits the properties of
the synchronizer it desugars to. Missionary provides the following derived synchronizers :
* [`holding`](/api/missionary.core/holding.html) - sugar over [`?`](/api/missionary.core/park.html)
* [`amb`](/api/missionary.core/amb.html) - sugar over [`?>`](/api/missionary.core/flatten.html)
* [`amb=`](/api/missionary.core/amb%3D.html) - sugar over [`?>`](/api/missionary.core/flatten.html)

## Evaluation contexts
A synchronizer interacts with the current evaluation context. The host platform provides a default evaluation context,
and missionary coroutines provide additional evaluation contexts :
* [`sp`](/api/missionary.core/sp.html) - sequential process
* [`cp`](/api/missionary.core/cp.html) - continuous process
* [`ap`](/api/missionary.core/ap.html) - ambiguous process

Each synchronizer has the same behavior in every evaluation context supporting it, but each evaluation context only
supports its own subset of synchronizers, according to this compatibility matrix :

|                                           | Single-threaded (JS) | Multi-threaded (JVM) | [`sp`](/api/missionary.core/sp.html) | [`cp`](/api/missionary.core/cp.html) | [`ap`](/api/missionary.core/ap.html) |
|-------------------------------------------|----------------------|----------------------|--------------------------------------|--------------------------------------|--------------------------------------|
| [`!`](/api/missionary.core/check.html)    |                      | ✔️                    | ✔️                                    | ✔️                                    | ✔️                                    |
| [`?`](/api/missionary.core/park.html)     |                      | ✔️                    | ✔️                                    |                                      | ✔️                                    |
| [`?<`](/api/missionary.core/switch.html)  |                      |                      |                                      | ✔️                                    | ✔️                                    |
| [`?>`](/api/missionary.core/flatten.html) |                      |                      |                                      |                                      | ✔️                                    |

## Coroutines
The association of a coroutine context with its subset of synchronizers defines an extension of the clojure syntax. The
resulting language is a strict superset of clojure that is fully compatible with standard clojure, which means all
valid clojure expressions are still valid with the same meaning in all evaluation contexts. The purpose of syntactic
extensions is to augment the evaluation rules with extra capabilities.

Missionary coroutines are *stackless* : synchronizers within nested function calls are not considered part of the
coroutine execution context.

Example : calling [`?`](/api/missionary.core/park.html) from [`sp`](/api/missionary.core/sp.html) via a nested function
call. Don't do this.
```clojure
(require '[missionary.core :as m])

(defn my-sleep [d]
  (m/? (m/sleep d)))

;; undefined behavior, m/? is called from a nested function
(m/sp (my-sleep 1000))
```

The common workarounds to this limitation are :
* make `my-sleep` return an effect and use another synchronizer after the call to run the effect.
* turn `my-sleep` into a macro. The macro will expand in the `sp` body, making `?` a direct call.

## Parking & Forking
[`?`](/api/missionary.core/park.html) is the parking synchronizer. When called, evaluation is suspended and the task
passed as argument is run. Evaluation is resumed when the task process terminates, the result is returned from the
synchronizer call.

[`?<`](/api/missionary.core/switch.html) and [`?>`](/api/missionary.core/flatten.html) are the forking synchronizers.
Forking is a generalization of parking, the evaluation is also suspended, but it can resume many times. The synchronizer
takes a flow instead of task and returns a result for each transfer. These two operators have different behavior when a
new input is available - the former invalidates the current evaluation, the latter propagates backpressure.

Example : backpressure propagation with [`?>`](/api/missionary.core/flatten.html) and
[`?`](/api/missionary.core/park.html).
```clojure
(require '[missionary.core :as m])

(defn now []
  #?(:clj (System/currentTimeMillis)
     :cljs (.now js/Date)))

;; a fixed-rate clock emitting `nil` every second
(def clock
  (m/ap
    ;; fork on an infinite sequence of timestamps.
    ;; ?> propagates backpressure : the next iteration is
    ;; not consumed until the current sleep is completed
    (let [timestamp (m/?> (m/seed (iterate (fn [previous] (+ previous 1000)) (now))))]
      ;; park on a sleep for each timestamp
      (m/? (m/sleep (- timestamp (now)))))))

(def ps
  ((m/reduce (fn [_ _] (prn :tick)) nil clock)
   (partial prn :success)
   (partial prn :failure)))
;; after 1s
:tick
;; after 1s
:tick
;; after 1s
:tick
;; cancellation
(ps)
:failure #error{,,,}
```

Example : switch from indefinite evaluations with [`?<`](/api/missionary.core/switch.html).
```clojure
(require '[missionary.core :as m])

(def !active (atom true))
(def !result (atom 0))

(def active-result
  (m/cp
    ;; fork on successive !active states.
    ;; ?< invalidates current evaluation
    (when (m/?< (m/watch !active))
      ;; fork again to get successive !result states.
      (m/?< (m/watch !result)))))

(def ps
  ((m/reduce (fn [_ x] (prn :> x)) nil active-result)
   (partial prn :success)
   (partial prn :failure)))
:> 0
(swap! !result inc)
:> 1
(swap! !active not)
:> nil
;; cancellation
(ps)
:failure #error{,,,}
```

## Interruption
JVM threads and missionary coroutines support cooperative interruption. The evaluation is always guaranteed to have run
to completion when the context terminates, therefore a context interruption will not stop evaluation immediately but
instead inform the program to promptly terminate.

An interrupted context cancels all of its parking and forking processes, which means the default behavior is to delegate
interruption handling to its children. Alternatively, the program can periodically check context interruption state
using [`!`](/api/missionary.core/check.html).

A coroutine context becomes interrupted when its process gets cancelled. Unlike JVM threads, this state cannot be
changed by the coroutine body.

Example : internal resource allocation & cleanup, with interruption polling in a loop.
```clojure
(require '[missionary.core :as m])
(require '[clojure.java.io :as io])

(defn print-chars [path]
  (m/sp
    ;; open a file and close it before completing
    (with-open [is (io/input-stream path)]
      (loop []
        ;; read file asynchronously
        (let [c (m/? (m/via m/blk (.read is)))]
          (when-not (== c -1)
            (print (char c))
            ;; check interruption state
            (m/!)
            (recur)))))))

(def ps
  ((print-chars "myfile.txt")
   (partial prn :success)
   (partial prn :failure)))
;; cancellation
(ps)
:failure #error{,,,}
```

Tip : If the cleanup procedure requires an asynchronous operation, you may not want it to be cancelled along with the
evaluation context. In this case, use [`compel`](/api/missionary.core/compel.html) to make sure the task completes
properly.

## Memory consistency (JVM only)
In standard clojure, the evaluation of an expression is always bound to a single thread. In missionary evaluation
contexts, this is not always the case. Multiple threads can successively take ownership of evaluation, which has
implications for shared memory access.

Example : the REPL thread starts the process, a cpu pooled thread finishes it.
```clojure
(require '[missionary.core :as m])

(def ps
  ((m/sp
     (identical? (Thread/currentThread)
       (do (m/? (m/via m/cpu))
           (Thread/currentThread))))
   (partial prn :success)
   (partial prn :failure)))
:success false
```

For this reason, synchronizers define additional rules :
* actions preceding a call to `?`, `?<` or `?>` in the evaluation context *happen-before* actions taken by the process.
* actions taken by a task process prior to completion *happen-before* actions following the return from `?` in the evaluation context.
* actions taken by a flow process prior to a transfer *happen-before* actions following the return from `?<` or `?>` in the evaluation context.

These rules basically allow the developer to reason about shared memory as if the evaluation was single-threaded.

Example : unsynchronized heap access across asynchronous boundaries. Safe.
```clojure
(require '[missionary.core :as m])

(def ps
  ((m/sp
     (let [arr (long-array 1)]
       (aset arr 0 6)
       (m/? (m/via m/cpu
              (aset arr 0 (inc (aget arr 0)))))
       (* 6 (aget arr 0))))
   (partial prn :success)
   (partial prn :failure)))
:success 42
```
