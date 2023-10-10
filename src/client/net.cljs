(ns client.net
  (:require [goog.events :as e])
  (:import missionary.Cancelled
           (goog.net EventType ErrorCode XhrIo)))

(defn fetch [path type]
  (fn [s f]
    (let [xhr (XhrIo.)]
      (.setResponseType xhr type)
      (e/listen xhr (.-COMPLETE EventType)
        (fn [_]
          (if (.isSuccess xhr)
            (s (.getResponse xhr))
            (f (let [code (.getLastErrorCode xhr)]
                 (if (== code (.-ABORT ErrorCode))
                   (Cancelled. "XHR aborted.")
                   (.getLastError xhr)))))))
      (.send xhr path)
      #(.abort xhr))))