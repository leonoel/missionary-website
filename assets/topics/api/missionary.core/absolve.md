# `missionary.core/absolve`

## Usage
* `(absolve task)`

`task` must complete with a zero-argument function.

## Description
Returns a task completing with the value returned by the function provided by `task`. If the `task` process fails, or
if the function call throws an exception, the `absolve` process fails.

## Examples
Wait for a javascript promise to settle. A [`dfv`](/api/missionary.core/dfv.html) is essentially a promise without an
error status, so we emulate the error status with a thunk. Note they're both just communication ports that don't
supervise the underlying process, i.e. cancelling a `dfv` simply stops waiting for the result.
```clojure
(require '[missionary.core :as m])

(defn await-promise [promise]
  (let [result (m/dfv)]
    (.then promise
      (fn [x] (result #(-> x)))
      (fn [e] (result #(throw e))))
    (m/absolve result)))

(def response (js/fetch "https://clojure.org"))

(def main (m/sp (.-statusText (m/? (await-promise response)))))

(def ps (main #(prn :success %) #(prn :failure %)))
;; after server response
:success "OK"
```

## See also
* [`attempt`](/api/missionary.core/attempt.html)