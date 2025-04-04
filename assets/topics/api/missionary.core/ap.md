# `missionary.core/ap`

## Usage
* `(ap & body)`

## Description
A macro [operator](/operators.html) returning a flow evaluating `body` in an implicit `do`, in a context supporting
interruption check, asynchronous park, continuous switch and discrete concat. `ap` flows have discrete semantics, they
produce successive evaluation results as soon as they become available. Due to concat parallelism, multiple results can
race for transfer, in this case they will be enqueued. Transferring a result terminates the evaluation context which
computed it. Cancelling `ap` process interrupts the root evaluation context.

## Synchronicity
* any `body` expression evaluation is synchronous with the return of the parking expression preceding it in program
  order, if it exists. Otherwise, it is synchronous with `ap` spawn.

## See also
* [Synchronizers](/synchronizers.html)
* [`!`](/api/missionary.core/!.html)
* [`?`](/api/missionary.core/?.html)
* [`?<`](/api/missionary.core/_<.html)
* [`?>`](/api/missionary.core/_>.html)