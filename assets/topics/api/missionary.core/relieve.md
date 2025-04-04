# `missionary.core/relieve`

## Usage
* `(relieve flow)`
* `(relieve sg flow)`

The values produced by `flow` must form a [semigroup](https://en.wikipedia.org/wiki/Semigroup) with `sg`. In other
words, `sg` must be a two-argument function able to merge successive items produced by `flow` and respecting
associativity, i.e. `(= (sg (sg x y) z) (sg x (sg y z)))`. The default semigroup is `(fn [_ x] x)`, i.e. all values but
the latest are discarded.

## Description
A function [operator](/operators.html) returning a decoupled view of given `flow`. The `flow` process is consumed as
fast as possible no matter how fast the `relieve` process is consumed. If the `flow` process produces a value before
the previous one was consumed, the previous value is merged with the current one using `sg`. When the `relieve` process
is cancelled, the `flow` process is cancelled.

## Synchronicity
* `flow` spawn is synchronous with `relieve` spawn
* `flow` transfer is synchronous with `flow` step
* `relieve` step is synchronous with the `flow` step immediately following a `relieve` transfer
* `relieve` termination is synchronous with `flow` termination, unless it happens while a `relieve` transfer is pending
in which case it is synchronous with this final transfer.

## See also
* [Continuous time](/continuous-time.html)