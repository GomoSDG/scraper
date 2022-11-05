(ns gomosdg.scraper.explorations.vuma
  (:require [promesa.core :as p]))


(def b (atom nil))


(p/let [puppeteer (js/require "apify")
        browser   (.launchPuppeteer puppeteer)] 
       (js/console.log "Launching!") 
       (js/console.log browser) 
       (reset! b browser))


(p/let [page (.newPage @b)
        _ (.goto page "https://vumatel.co.za/product/reach"
                 (js-obj "waitUntil" "networkidle2"))
        ;;_ (.waitForNetworkIdle page)
        _ (js/console.log "Typing..")
        _ (.$eval page "#map-search" (fn [el] (set! (.-value el) "4 Randskroon Drive, Horison, Roodepoort")))
        _ (-> (.-keyboard page)
              (.press "Enter"))]
       
       (js/console.log "Done!"))




