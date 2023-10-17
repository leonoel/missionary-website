# `missionary.core/sp`

## Usage
* `(sp & body)`

A macro [operator](/operators.html) returning a task evaluating `body`, in an implicit `do`, in a context supporting
asynchronous parking and interruption checking. `sp` process completes with the evaluation result, or crashes if an
exception is thrown. Cancelling `sp` process interrupts the evaluation context.

## Synchronicity
* `sp` completion is synchronous with evaluation of final `body` expression
* any parking task spawn is synchronous with evaluation of its parking expression
* evaluation of any `body` expression is synchronous with the completion of the parking task preceding it in program
order, if it exists. Otherwise, it is synchronous with `sp` spawn.

## See also
* [synchronizers](/synchronizers.html)
* [`!`](/api/missionary.core/!.html)
* [`?`](/api/missionary.core/?.html)