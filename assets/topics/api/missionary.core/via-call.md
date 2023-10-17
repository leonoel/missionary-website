# `missionary.core/via-call`

## Usage
* `(via-call executor thunk)`

JVM only.

An [operator](/operators.html) returning a task evaluating `thunk`, a zero-argument function, via a
`java.util.concurrent.Executor`. `via-call` process completes with the evaluation result, or crashes if an exception is
thrown. Cancelling `via-call` process interrupts evaluating thread.

## Synchronicity
* `via-call` completion is not synchronous with `via-call` spawn

## See also
* [`via`](/api/missionary.core/via.html)
* [`blk`](/api/missionary.core/blk.html)
* [`cpu`](/api/missionary.core/cpu.html)