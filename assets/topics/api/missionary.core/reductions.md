# `missionary.core/reductions`

## Usage
* `(reductions rf flow)`
* `(reductions rf init flow)`

## Description
A function [operator](/operators.html) returning a flow producing the successive reduction states of `flow` values by
function `rf` with optional initial state `init`. If initial state is not provided, `rf` must provide it via its zero
arity. The `flow` process is consumed at the same rate as the `reductions` process. When the reducing function returns
a `reduced` state or throws, the `flow` process is cancelled and its subsequent values are ignored.

## Examples
```clojure
(def input (m/seed (range 10)))
(def input-sums (m/reductions + input))

(def ps
  ((m/reduce conj input-sums)
   (partial prn :success)
   (partial prn :failure)))
:success [0 1 3 6 10 15 21 28 36 45]
```

## Synchronicity
* `flow` spawn is synchronous with `reductions` spawn
* initial `reductions` step is synchronous with `reductions` spawn
* subsequent `reductions` steps are synchronous with `flow` steps
* `flow` transfers are synchronous with `reductions` transfers following initial one
* `reductions` termination is synchronous with `flow` termination, unless it happens while the initial transfer is
pending in which case it is synchronous with this final transfer.
* `flow` cancellation is synchronous with `reductions` cancellation or any `flow` transfer returning a value causing
the reduction step to early terminate, whichever comes first