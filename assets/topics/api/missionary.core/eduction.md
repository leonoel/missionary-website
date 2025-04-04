# `missionary.core/eduction`

## Usage
* `(eduction & xfs flow)`

`xfs` must be a sequence of transducers.

## Description
A function [operator](/operators.html) returning a flow producing the successive values produced by the transformation
of `flow` values by the composition of `xfs`. The `flow` process is consumed when all values produced by the previous
transformation step have been consumed. When a transformation step returns a `reduced` state or throws, the `flow`
process is cancelled and its subsequent values are ignored.

## Examples
```clojure
(require '[missionary.core :as m])

(def input (m/seed (range 10)))

(def input-transform (m/eduction (filter odd?) (mapcat range) (partition-all 4) input))

(def ps
  ((m/reduce conj input-transform)
   (partial prn :success)
   (partial prn :failure)))
:success [[0 0 1 2] [0 1 2 3] [4 0 1 2] [3 4 5 6] [0 1 2 3] [4 5 6 7] [8]]
```

## Synchronicity
* `flow` spawn is synchronous with `eduction` spawn
* the transducer initialization is synchronous with `eduction` spawn
* `eduction` steps for the first value of each transformation step are synchronous with its `flow` transfer
* `eduction` steps for the next values are synchronous with the `eduction` transfer of previous value
* `flow` transfers are synchronous with `eduction` transfer of the last value produced by latest transformation step or
the following `flow` step, whichever comes last
* `eduction` termination is synchronous with `flow` termination or `eduction` transfer of the last value of the final
transformation step, whichever comes last
* `flow` cancellation is synchronous with `reductions` cancellation or any `flow` transfer returning a value causing
the transformation step to early terminate, whichever comes first