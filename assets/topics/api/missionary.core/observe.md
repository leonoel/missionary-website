# `missionary.core/observe`

## Usage
* `(observe subject)`

`subject` is a function taking a callback as argument, registering the callback and returning a deregistering function
taking zero-argument.

## Description
A function [operator](/operators.html) returning a flow providing a callback and producing the values passed to this
callback. The `observe` process never terminates successfully. Cancelling the `observe` process makes it crash with an
instance of [`Cancelled`](/api/missionary.cancelled.html).

## Examples
Observer pattern in the browser :
```clojure
(require '[missionary.core :as m])

(def click-events
  (m/observe
    (fn [listener]
      (.addEventListener js/window "click" listener)
      #(.removeEventListener js/window "click" listener))))

(def ps
  ((m/reduce (fn [_ _] (prn :clicked)) nil click-events)
   (partial prn :success)
   (partial prn :failure)))
;; after a click
:clicked
;; after another click
:clicked
(ps)
:failure #error{}
;; next clicks have no effect
```

## Synchronicity
* the `subject` function call is synchronous with `observe` spawn
* each `observe` step preceding cancellation is synchronous with the callback invocation
* if `observe` is not ready to transfer when cancelled, it steps synchronously to transfer the error
* the deregistering function call and the `observe` termination are synchronous with the final `observe` transfer

## See also
* [`relieve`](/api/missionary.core/relieve.html)