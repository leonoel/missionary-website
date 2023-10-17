# `missionary.core/via`

## Usage
* `(via executor & body)`

JVM only.

A macro [operator](/operators.html) returning a task evaluating `body`, in an implicit `do`, via a
`java.util.concurrent.Executor`. `via` process completes with the evaluation result, or crashes if an exception is
thrown. Cancelling `via` process interrupts evaluating thread.

Example : assign a cpu-intensive operation to a fixed thread pool
```clojure
(def fib42
  (m/via m/cpu
    ((fn fib [n]
       (case n
         0 0
         1 1
         (+ (fib (dec n))
           (fib (dec (dec n))))))
     42)))

(m/? fib42) := 267914296
```

## Synchronicity
* `via` completion is not synchronous with `via` spawn

## See also
* [`via-call`](/api/missionary.core/via-call.html)
* [`blk`](/api/missionary.core/blk.html)
* [`cpu`](/api/missionary.core/cpu.html)