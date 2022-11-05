(ns gomosdg.scraper.handlers
  {:clj-kondo/config {:lint-as {promesa.core/let clojure.core/let}}}
  (:require [promesa.core :as p]
            ["apify" :as apify]
            [cljs.core.async :refer [go >!]]))

(defmulti handle-page (fn [site _] (:page-handler site)))

(defmethod handle-page :appending-paginate-handler
  [{:keys [entities request-queue url-selectors pagination out]
    :as site}
   context]
  "ada"
  (p/let [page (. ^js context -page)
          puppeteer (-> (js/require "apify")
                        (.-utils)
                        (.-puppeteer))
          #_:clj-kondo/ignore
          block-requests (.blockRequests ^js puppeteer page)
          req (.-request ^js context)
          dom (.content page)
          entities (map #(assoc {}
                                :dom dom
                                :selector %
                                :site-id (:id site)
                                :date (js/Date.now)
                                :out out)
                        entities)]

    (js/console.log "Scraping: " (.-url req))

    (doseq [e entities]
      (go (>! out e)))

    ;; Enqueue links
    (p/chain (.$$ ^js page (:end-selector pagination))
             #(when (= (.-length %) 0)
                (.enqueueLinks apify/utils (js-obj "page" page
                                                   "requestQueue" request-queue
                                                   "selector" (:next-selector pagination)))))

    (p/all (map #(.enqueueLinks apify/utils (js-obj "page" page
                                                    "requestQueue" request-queue
                                                    "selector" %))
                url-selectors))))

(defmethod handle-page :single-page-handler
  [{:keys [entities out request-queue url-selectors]
    :as site}
   context]
  (p/let [page (. ^js context -page)
          puppeteer (-> (js/require "apify")
                        (.-utils)
                        (.-puppeteer))
          #_:clj-kondo/ignore
          block-requests (.blockRequests ^js puppeteer page)
          req (.-request ^js context)
          dom (.content page)
          
          entities (map #(assoc {}
                                :dom dom
                                :selector %
                                :site-id (:id site)
                                :url (.-url req)
                                :date (js/Date.now)
                                :out out)
                        entities)]

    (js/console.log "Scraping: " (.-url req))

    (doseq [e entities]
      (go (>! out e)))
    
    ;; Enqueue URLs
    (p/all (map #(.enqueueLinks apify/utils (js-obj "page" page
                                                    "requestQueue" request-queue
                                                    "selector" %))
                url-selectors))))

(defmethod handle-page :infite-scroll-paginate-handler
  [{:keys [entities request-queue url-selectors paginate-selector out]
    :as site}
   context]
  (p/let [page (. ^js context -page)
          req (.-request ^js context)
          puppeteer (-> (js/require "apify")
                        (.-utils)
                        (.-puppeteer))
          #_:clj-kondo/ignore
          log       (js/console.log "Scraping: " (.-url req))
          #_:clj-kondo/ignore
          scroll    (.infiniteScroll ^js puppeteer page (js-obj "timeoutSecs" 60
                                                                "waitForSecs" 4
                                                                "buttonSelector" paginate-selector))
          dom (.content page)
          entities (map #(assoc {}
                                :dom dom
                                :selector %
                                :site-id (:id site)
                                :out out)
                        entities)]

    (doseq [e entities]
      (go (>! out e)))

    ;; Enqueue links

    (js/console.log "SLCTRS" url-selectors)

    (p/all (map #(.enqueueLinks apify/utils (js-obj "page" page
                                                    "requestQueue" request-queue
                                                    "selector" %))
                url-selectors))))
