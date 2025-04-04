# `missionary.core/?>`

## Usage
* `(?> flow)`
* `(?> parallelism flow)`

`parallelism` must be a positive number, default is `1`.

## Description
The discrete flattening [synchronizer](/synchronizers.html). A `flow` process is spawned, then for each transfer the
value is returned and evaluation is resumed in a fresh evaluation context derived from the current one. If the `flow`
process crashes, the error is rethrown. The current evaluation context terminates when all of its derived ones are
terminated along with the `flow` process itself. `parallelism` defines the maximal count of derived evaluation contexts
allowed to run concurrently before pausing `flow` process transfers, i.e. applying backpressure.

## Examples
Transform successive values of an input flow :
```clojure
(require '[missionary.core :as m])

(def input (m/seed (range 10)))
(def input-inc (m/ap (inc (m/?> input))))
(def main (m/reduce + input-inc))

(def ps (main #(prn :success %) #(prn :failure %)))
:success 55
```

Asynchronous transformation with parallelism :
```clojure
(require '[missionary.core :as m])

(def input (m/seed (range 10)))
(defn async-inc [x] (m/sleep 1000 (inc x)))
(def input-inc (m/ap (m/? (async-inc (m/?> 5 input)))))
(def main (m/reduce + input-inc))

(def ps (main #(prn :success %) #(prn :failure %)))
;; after 2 seconds
:success 55
```

## Synchronicity
* `flow` spawn is synchronous with the concat call
* each concat return is synchronous with the `flow` transfer causing it

## See also
* [`amb`](/api/missionary.core/amb.html)
* [`amb=`](/api/missionary.core/amb=.html)
* [`ap`](/api/missionary.core/ap.html)