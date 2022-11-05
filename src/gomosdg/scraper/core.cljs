(ns gomosdg.scraper.core
  #_:clj-kondo/ignore
  {:clj-kondo/config {:lint-as {promesa.core/let clojure.core/let}}}
  (:require ["apify" :as apify] 
            [cljs.core.async :refer [chan pipeline]]
            [gomosdg.scraper.handlers :as handler]
            [gomosdg.scraper.pipelines :as pl]
            [gomosdg.scraper.sites :as s]
            [gomosdg.scraper.sinks :as sinks]
            [promesa.core :as p]
            [clojure.core.async :refer [<! go]]
            [cljs.core.async.interop :refer-macros [<p!]])) 


(defn get-launch-context
  "Puppeteer launch options https://pptr.dev/#?product=Puppeteer&version=v5.5.0&show=api-puppeteerlaunchoptions"
  []
  (js-obj "launchOptions"
          (js-obj "headless" true
                  "args" (into-array "--headless"))
          "useChrome" true
          "stealth"   false))

(defn process-entities [source sink]
  (let [c (chan)
        xf (comp (mapcat pl/get-entities-specs)
                 (map pl/entity-spec->attribute-specs)
                 (map pl/parse-attributes)
                 (map pl/build-entity)
                 (map pl/decorate-with-id)
                 (map pl/decorate-with-type)
                 (map :entity))]

    (pipeline 4
              c
              xf
              source)

    (sinks/listen sink c)))

(defn make-crawler [{:keys [start-urls sink] :as site}]
  (p/let [sink (sinks/get-sink sink)
          request-queue    (apify/openRequestQueue (-> (random-uuid)
                                                       (.toString)))
          request-list     (apify/openRequestList "start-urls" (into-array (map #(js-obj "url" %) start-urls)))
          out              (chan)]


    (process-entities out sink)

    (apify/PuppeteerCrawler. (js-obj "requestQueue"       request-queue
                                     "requestList"        request-list
                                     "handlePageFunction" (partial handler/handle-page (assoc site
                                                                                              :request-queue request-queue
                                                                                              :out out))
                                     "handlePageTimeoutSecs" 6000
                                     "launchContext"
                                     (get-launch-context)))))

(defn start-crawler [^js crawler]
  (identity (.run crawler)))

(defn crawl-sites [sites]
  (p/all 
   (for [s sites]
    (p/then (make-crawler s)
            start-crawler))))


(defn -main [& args]
  (println "Running Scraper") 
  (crawl-sites s/propert24-sold-prices))