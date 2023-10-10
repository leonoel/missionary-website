(ns client.main
  (:require [clojure.java.io :as io]))

(defmacro topics []
  (let [assets (io/file "assets")]
    (into {} (filter (comp #{:topic} :template val))
      (release/read-pages! assets))))