(ns release
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [garden.core :as garden]
            [glow.html :as g.html]
            [glow.parse :as g.parse]
            [hiccup2.core :as h]
            [hiccup.page :as h.page]
            [nextjournal.markdown :as md]
            [nextjournal.markdown.transform :as md.transform]
            [nextjournal.markdown.parser :as md.parser])
  (:import (java.io File)))

(defn clean! [^File f]
  (when (.isDirectory f)
    (run! clean! (.listFiles f)))
  (.delete f))

(defn write! [root path content]
  (spit (doto (io/file root path)
          (-> .getParentFile .mkdirs))
    content))

(defn read! [root path]
  (slurp (io/file root path)))

(defn write-css! [release assets]
  (let [{:keys [glow code-font-size pre-padding]} (edn/read-string (read! assets "style.edn"))]
    (write! release "clojure.css"
      (garden/css
        [:code {:font-size code-font-size}]
        [:pre
         (assoc (select-keys glow [:background])
           :overflow-x "auto"
           :padding pre-padding)
         (into [] (map g.html/css-color glow))]))))

(def hiccup-renderers
  (assoc md.transform/default-hiccup-renderers
    :code (fn [_ctx {:keys [language content]}]
            (assert (= language "clojure") "Unsupported language.")
            [:pre (into [:code] (map (comp g.html/hiccup-transform g.parse/parse :text)) content)])
    :plain (fn [ctx node]
             (or (:text node)
               (keep (partial md.transform/->hiccup
                       (assoc ctx ::md.transform/parent node))
                 (:content node))))
    :internal-link (fn [_ctx {:keys [text attrs]}]
                     [:a.internal {:href (str (:href attrs) ".html")} text])))

(def md-doc
  (update md.parser/empty-doc :text-tokenizers conj
    md.parser/internal-link-tokenizer))

(defn md->hiccup [assets path]
  (md.transform/->hiccup hiccup-renderers
    (md/parse md-doc (read! assets (str path ".md")))))

(defn html5 [& hiccup]
  (h/html (h.page/doctype :html5)
    (into [:html] hiccup)))

(def clojure-css (h.page/include-css "clojure.css"))

(def template
  {:home      (fn [assets path page]
                (html5
                  [:head clojure-css]
                  [:body
                   [:header [:a {:href "/about.html"} "About"]]
                   [:h1 "Missionary"]
                   [:section (md->hiccup assets "fragments/one-sentence")]
                   [:section (md->hiccup assets "fragments/one-code-block")]
                   [:section (md->hiccup assets "fragments/one-paragraph")]
                   [:section (md->hiccup assets "fragments/documentation")]
                   [:section (md->hiccup assets "fragments/source")]
                   [:section (md->hiccup assets "fragments/support")]
                   [:section (md->hiccup assets "fragments/quote")]]))
   :not-found (fn [assets path page]
                (html5
                  [:head]
                  [:body
                   [:h1 "Not found"]]))
   :topic     (fn [assets path page]
                (html5
                  [:head clojure-css]
                  [:body
                   [:header [:a {:href "/"} "Home"]]
                   (md->hiccup assets (str "topics/" path))
                   [:footer (md->hiccup assets "fragments/footer")]]))})

(defn write-pages! [release assets]
  (reduce-kv (fn [_ path page]
               (write! release (str path ".html")
                 ((template (:template page)) assets path page)))
    nil (edn/read-string (read! assets "pages.edn"))))

(defn build [release assets]
  (doto release
    (clean!)
    (write-css! assets)
    (write-pages! assets)))

(defn -main [& _]
  (build (io/file "release") (io/file "assets")))