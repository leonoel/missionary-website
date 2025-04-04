# `missionary.core/watch`

## Usage
* `(watch reference)`

`reference` must support `deref`, `add-watch`, and `remove-watch`.

A function [operator](/operators.html) returning a [continuous](/continuous-time.html) flow reflecting the current
state of `reference`. The `watch` process never terminates successfully. Cancelling the `watch` process makes it crash
with an instance of [`Cancelled`](/api/missionary.cancelled.html).

## Synchronicity
* `add-watch` call is synchronous with `watch` spawn
* each `watch` step preceding cancellation is synchronous with the watch callback notification
* each `watch` transfer is synchronous with the `deref` call returning the current state
* if `watch` is not ready to transfer when cancelled, it steps synchronously to transfer the error
* `remove-watch` call and the `watch` termination are synchronous with the final `watch` transfer