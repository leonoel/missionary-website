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

(defn touch! [root path]
  (doto (io/file root path)
    (-> .getParentFile .mkdirs)))

(defn read! [root path]
  (slurp (io/file root path)))

(def app "app.js")
(def style "style.css")
(def github-markdown "github-markdown.css")

(defn write-js! [release {:keys [dev?]}]
  (cljs/build
    (merge
      {:main          "client.main"
       :output-to     (str (io/file release app))}
      (if dev?
        {:asset-path "/js"
         :output-dir (str (io/file release "js"))}
        {:optimizations :advanced}))))

(defn write-css! [release {:keys [assets]}]
  (spit (touch! release github-markdown)
    (slurp "https://raw.githubusercontent.com/sindresorhus/github-markdown-css/gh-pages/github-markdown-light.css"))
  (let [{:keys [max-width padding header-font-size footer-font-size link-color
                highlight-color code-special code-literal code-comment code-string code-symbol]}
        (edn/read-string (read! assets "style.edn"))]
    (spit (touch! release style)
      (garden/css
        ;; reset http://meyerweb.com/eric/tools/css/reset/
        [:html :body :div :span :applet :object :iframe :h1 :h2 :h3 :h4 :h5 :h6 :p :blockquote :pre :a :abbr :acronym
         :address :big :cite :code :del :dfn :em :img :ins :kbd :q :s :samp :small :strike :strong :sub :sup :tt :var
         :b :u :i :center :dl :dt :dd :ol :ul :li :fieldset :form :label :legend :table :caption :tbody :tfoot :thead
         :tr :th :td :article :aside :canvas :details :embed :figure :figcaption :footer :header :hgroup :menu :nav
         :output :ruby :section :summary :time :mark :audio :video
         {:border 0
          :margin 0
          :padding 0
          :font 'inherit}]
        [:article :aside :details :figcaption :figure :footer :header :hgroup :menu :nav :section
         {:display 'block}]
        [:blockquote :q {:quotes 'none}
         [:&:before :&:after {:content "''"}]
         [:&:before :&:after {:content 'none}]]
        [:table {:border-collapse 'collapse
                 :border-spacing 0}]

        [:html :body {:height "100%"}]
        [:header :footer
         {:width "100%"
          :max-width max-width
          :margin 'auto
          :box-sizing 'border-box}
         [:hr {:margin-bottom 0}]
         [:a {:color link-color
              :font-weight 600
              :text-decoration 'none}
          [:&:hover {:text-decoration 'underline}]]]
        [:header {:font-size header-font-size
                  :padding   (str padding " " padding " 0 " padding)}]
        [:div.autocomplete {:position 'absolute}]
        [:div.suggestion {:cursor 'pointer}]
        [:span.highlight {:background-color highlight-color}]
        [:body {:display   'flex
                :flex-flow 'column}]
        [:main {:overflow 'auto
                :flex      1
                :display   'flex
                :flex-flow 'column}]
        [:article.markdown-body
         {:flex 1
          :box-sizing 'border-box
          :width "100%"
          :max-width max-width
          :margin "0 auto"
          :padding padding}]
        [:footer {:font-size footer-font-size
                  :padding   (str "0 " padding " " padding " " padding)}
         [:hr {:margin-top 0}]
         [:ul {:display 'flex
               :list-style 'none
               :justify-content 'space-between}]]
        [:pre
         [:.exception
          :.repeat
          :.conditional
          :.core-fn
          :.definition
          :.special-form
          :.macro
          {:color code-special}]
         [:.number
          :.boolean
          :.nil
          :.keyword
          {:color code-literal}]
         [:.comment
          {:color code-comment}]
         [:.string
          :.character
          :.regex
          {:color code-string}]
         [:.variable
          :.reader-char
          :.s-exp
          :.symbol
          {:color code-symbol}]]))))

(def hiccup-renderers
  (assoc md.transform/default-hiccup-renderers
    :code (fn [_ctx {:keys [language content]}]
            (assert (= language "clojure") "Unsupported language.")
            (into [:pre] (map (comp g.html/hiccup-transform g.parse/parse :text)) content))
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
    (into [:html {:lang "en"}] hiccup)))

(defn page-head [dev?]
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
   [:link {:type "text/css"
           :href (util/to-uri (str "/" github-markdown))
           :rel "stylesheet"}]
   [:link {:type "text/css"
           :href (util/to-uri (str "/" style))
           :rel "stylesheet"}]
   [:script {:type  "text/javascript"
             :src   (util/to-uri (str "/" app))
             :async (not dev?)}]])

(def search-input
  [:input#search {:type "search"}])

(def template
  {:home      (fn [{:keys [assets dev?]}]
                (html5
                  (page-head dev?)
                  [:body
                   [:header [:a {:href "/about.html"} "About"] search-input [:hr]]
                   [:main
                    [:article {:class "markdown-body"}
                     (next (md->hiccup assets "topics/home"))]]]))
   :not-found (fn [{}]
                (html5
                  [:head]
                  [:body
                   [:h1 "Not found"]]))
   :topic     (fn [{:keys [assets path dev?]}]
                (html5
                  (page-head dev?)
                  [:body
                   [:header [:a {:href "/"} "Home"] search-input [:hr]]
                   [:main
                    [:article {:class "markdown-body"}
                     (next (md->hiccup assets (str "topics/" path)))]
                    [:footer (md->hiccup assets "fragments/footer")]]]))})

(defn read-pages! [assets]
  (edn/read-string (read! assets "pages.edn")))

(defn write-pages! [release opts]
  (reduce-kv (fn [_ path page]
               (spit (touch! release (str path ".html"))
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