(ns release
  (:require [cljs.build.api :as cljs]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [garden.core :as garden]
            [glow.html :as g.html]
            [glow.parse :as g.parse]
            [hiccup.util :as util]
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

(defn write-js! [release {:keys [dev?]}]
  (cljs/build
    (merge
      {:main          "client.main"
       :output-to     (str (io/file release "app.js"))}
      (if dev?
        {:asset-path "/js"
         :output-dir (str (io/file release "js"))}
        {:optimizations :advanced}))))

(defn write-css! [release {:keys [assets]}]
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

(defn page-head [dev?]
  [:head
   [:meta {:charset "utf-8"}]
   [:link {:type "text/css"
           :href (util/to-uri "/clojure.css")
           :rel "stylesheet"}]
   [:script {:type  "text/javascript"
             :src   (util/to-uri "/app.js")
             :async (not dev?)}]])

(def search-input
  [:input#search {:type "search"}])

(def template
  {:home      (fn [{:keys [assets dev?]}]
                (html5
                  (page-head dev?)
                  [:body
                   [:header [:a {:href "/about.html"} "About"] search-input]
                   [:h1 "Missionary"]
                   [:section (md->hiccup assets "fragments/one-sentence")]
                   [:section (md->hiccup assets "fragments/one-code-block")]
                   [:section (md->hiccup assets "fragments/one-paragraph")]
                   [:section (md->hiccup assets "fragments/documentation")]
                   [:section (md->hiccup assets "fragments/source")]
                   [:section (md->hiccup assets "fragments/support")]
                   [:section (md->hiccup assets "fragments/quote")]]))
   :not-found (fn [{}]
                (html5
                  [:head]
                  [:body
                   [:h1 "Not found"]]))
   :topic     (fn [{:keys [assets path dev?]}]
                (html5
                  (page-head dev?)
                  [:body
                   [:header [:a {:href "/"} "Home"] search-input]
                   [:article
                    (md->hiccup assets (str "topics/" path))]
                   [:footer (md->hiccup assets "fragments/footer")]]))})

(defn read-pages! [assets]
  (edn/read-string (read! assets "pages.edn")))

(defn write-pages! [release opts]
  (reduce-kv (fn [_ path page]
               (write! release (str path ".html")
                 ((template (:template page)) (assoc opts :path path :page page))))
    nil (read-pages! (:assets opts))))

(defn build [release opts]
  (doto release
    (clean!)
    (write-js! opts)
    (write-css! opts)
    (write-pages! opts)))

(defn -main [& _]
  (build (io/file "release") {:assets (io/file "assets")}))