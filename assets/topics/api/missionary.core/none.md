# `missionary.core/none`

## Usage
* `none`

## Description
A value [operator](/operators.html) describing a flow performing no action and producing no value. The `none` process
terminates immediately. Cancelling a `none` process has no effect.

## Examples
```clojure
(def ps
  ((m/reduce conj m/none)
   (partial prn :success)
   (partial prn :failure)))
:success []
```

## Synchronicity
* `none` terminates synchronously with spawn