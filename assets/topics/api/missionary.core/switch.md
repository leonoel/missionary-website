# `missionary.core/?<`

## Usage
* `(?< flow)`

`flow` must be [continuous](/continuous-time.html).

## Description
The continuous switching [synchronizer](/synchronizers.html). A `flow` process is spawned, then for each state change
the new state is returned and evaluation is resumed in a fresh evaluation context derived from the current one. If the
`flow` process crashes, the error is rethrown. The current evaluation context terminates when all of its derived ones
are terminated along with the `flow` process itself. After the initial state is returned, every following state change
interrupts the evaluation context associated with the previous state along with those derived from it. When the `flow`
process transfer returns a duplicate, i.e. a state `=` to the previous one, the current state is considered unchanged,
therefore the switch is not triggered and no value is returned. If the `flow` process steps synchronously with a
transfer, the transferred value is discarded and another transfer is performed synchronously.

## Examples
Derive the state of one atom out of two, depending on the state of a third one :
```clojure
;; define 3 continuous inputs
(def !x (atom false))
(def !y (atom :foo))
(def !z (atom 0))

;; reflect the state of !y or !z, depending on the state of !x
(def y-or-z
  (m/cp (let [!target (if (m/?< (m/watch !x)) !y !z)]
          (m/?< (m/watch !target)))))

;; print successive states
(def main (m/reduce (fn [_ x] (prn :> x)) nil y-or-z))

(def ps (main #(prn :success %) #(prn :failure %)))
:> 0
(swap! !z inc)     ;; !z is being watched, cp state is updated
:> 1
(reset! !y :bar)   ;; no effect, !y is not watched at this point
(swap! !x not)     ;; stop watching !z, start watching !y
:> :bar
(reset! !y :baz)   ;; !y is being watched, cp state is updated
:> :baz
```

Duplicate states :
```clojure
(require '[missionary.core :as m])

(def !x (atom 0))

(def inc-x
  (m/cp
    (let [x (m/?< (m/watch !x))]
      (prn :> [:before x])
      (inc x))))

(def main (m/reduce (fn [_ x] (prn :> [:after x])) nil inc-x))

(def ps (main #(prn :success %) #(prn :failure %)))
:> [:before 0]
:> [:after 1]
(swap! !x inc)       ;; !x state changed, cp recomputes state
:> [:before 1]
:> [:after 2]
(swap! !x identity)  ;; !x state unchanged, cp reuses previous state
:> [:after 2]
```

## Synchronicity
* `flow` spawn is synchronous with switch call
* each switch return is synchronous with the `flow` transfer causing it

## See also
* [`cp`](/api/missionary.core/cp.html)
* [`ap`](/api/missionary.core/ap.html)