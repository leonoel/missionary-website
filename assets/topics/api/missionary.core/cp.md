# `missionary.core/cp`

## Usage
* `(cp & body)`

## Description
A macro [operator](/operators.html) returning a [continuous](/continuous-time.html) flow evaluating `body` in an
implicit `do`, in a context supporting interruption check and continuous switch. On transfer, the switch sequence is
updated according to child process state changes, then the final evaluation context returns the current state and
terminates. Cancelling `cp` process interrupts the root evaluation context.

## Synchronicity
* `cp` initial step is synchronous with `cp` spawn
* `cp` termination is synchronous with termination of the last child process, unless the last child process terminates
while a transfer is still pending in which case the termination is synchronous with this final transfer.
* any `cp` step following a transfer is synchronous with the first step of any child process
* any `cp` transfer is synchronous with all operations required to compute the current state

## See also
* [Synchronizers](/synchronizers.html)
* [!](/api/missionary.core/!.html)
* [?<](/api/missionary.core/_<.html)