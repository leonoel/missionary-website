(ns client.dom
  (:require [missionary.core :as m]
            [goog.events :as e]))

(defn element [t]
  (.createElement js/document t))

(defn text [s]
  (.createTextNode js/document s))

(defn set-text! [n s]
  (set! (.-nodeValue n) s))

(defn get-text! [n]
  (.-nodeValue n))

(defn spine-mount [s k]
  (fn [e]
    (m/observe
      (fn [_]
        (s k {} e)
        #(do (s k {} nil))))))

(defn events [e t]
  (m/observe
    (fn [!]
      (e/listen e t !)
      #(e/unlisten e t !))))

(defn clock [d]
  (m/ap
    (loop []
      (m/amb nil
        (do (m/? (m/sleep d))
            (recur))))))

(def document-changes
  (if js/MutationObserver
    (m/observe
      (fn [on-change]
        (on-change nil)
        (let [obs (js/MutationObserver. on-change)]
          (.observe obs js/document
            (js-obj
              "childList" true
              "subtree" true))
          #(.disconnect obs))))
    (clock 100)))

(defn wait-for [f]
  (->> document-changes
    (m/sample (fn [_] (f)))
    (m/eduction (remove nil?))
    (m/reduce (comp reduced {}) nil)))
