(ns gomosdg.scraper.sites)

(def reddit [{:id :reddit
              :start-urls ["https://www.reddit.com/r/SafeMoon/new"
                           "https://www.reddit.com/r/Mobox/new"]
              :page-handler :single-page-handler
             
              :entities
              [{:type :reddit-info
                :root "div._3ozFtOe6WpJEMUtxDOIvtU"
                :attributes
                [{:name :number-of-users
                  :selector [:text "div.nEdqRRzLEN43xauwtgTmj > div"]}]}]}])



(def propert24-sold-prices
  [{:id :property24
    :start-urls ["https://www.property24.com/property-values/kommetjie/western-cape/478"]
    :page-handler :single-page-handler
    :sink {:type :sqs
           :queue-name "property24-sold-prices"
           :encoder :json}
    :url-selectors [".p24_alphabet .col-4 [title]"
                    ".p24_pager a"]
    :entities
    [{:type :breadcrumb
      :root "body"
      :attributes
      [{:name :path
        :selector [:text "#breadCrumbContainer"]}]}
     {:type :price
      :root "tr.p24_markerRow"
      :attributes
      [{:name :address
        :selector [:text "div.p24_addressLink a"]}
       {:name :price-data-url 
        :selector [:attr "src" "img.p24_propertyPrice"]}
       {:name :year-data-url
        :selector [:attr "src" "img.p24_propertyYear"]}]}]}])

(def sites [{:id :property24
             :url-selectors      ["div.p24_pager > div > ul a"
                                  #_".js_searchbarSpacing + div.p24_areas  ul > li > a"]
             :start-urls        ["https://www.property24.com/houses-for-sale/roodepoort/gauteng/5"]
             :pagination {:next-selector "div.p24_pager > a.pull-right"
                          :end-selector  "div.p24_pager > a.pull-right.text-muted"}
             :page-handler :appending-paginate-handler
             :entities
             [{:type :property
               :root "div[data-listing-number] > div[data-listing-number]"
               :attributes
               [{:name :features
                 :selector [:name-value-map ".p24_icons > span"
                            [:attr "alt" "image"]
                            [:float "svg + span"]]}
                {:name :url
                 :selector [:attr "href" "a"]}
                {:name :type
                 :selector [:or [:re-group ".p24_title" #"Bedroom\s(\w+)" 1]
                                [:text ".p24_title"]]}
                {:name :id
                 :selector [:attr "data-listing-number" "[data-listing-number]"]}
                {:name :location
                 :selector [:text ".p24_location "]}
                {:name :size
                 :selector [:name-value-map ".p24_icons > .p24_size"
                            [:attr "alt" "img"]
                            [:number "img + span"]]}
                {:name :description
                 :selector [:or [:text ".p24_title"]
                                [:text ".p24_description"]]}
                {:name :reduced?
                 :selector [:flag ".p24_reducedBanner"]}
                {:name :sold?
                 :selector [:flag ".p24_soldBanner"]}
                {:name :under-offer?
                 :selector [:flag ".p24_underOfferBanner"]}
                {:name :address
                 :selector [:text ".p24_address"]}
                {:name :price
                 :selector [:number".p24_price"]}]}]}
            ;; Only Valhalla
            #_{:id :property24-valhalla
             :url-selectors      ["div.p24_pager > div > ul a"
                                  ".js_searchbarSpacing + div.p24_areas  ul > li > a"]
             :start-urls        ["https://www.property24.com/for-sale/gauteng/1"
                                 "https://www.property24.com/property-values/gauteng/1"]
             :pagination {:next-selector "div.p24_pager > a.pull-right"
                          :end-selector  "div.p24_pager > a.pull-right.text-muted"}
             :page-handler :appending-paginate-handler
             :entities
             [{:type :property
               :root "div[data-listing-number] > div[data-listing-number]"
               :attributes
               [{:name :features
                 :selector [:name-value-map ".p24_icons > span"
                            [:attr "alt" "image"]
                            [:number "svg + span"]]}
                {:name :erf-size
                 :selector [:number ".p24_icons .p24_size"]}
                {:name :url
                 :selector [:attr "href" "a"]}
                {:name :type
                 :selector [:or [:re-group ".p24_title" #"Bedroom\s(\w+)" 1]
                                [:text ".p24_title"]]}
                {:name :id
                 :selector [:attr "data-listing-number" "[data-listing-number]"]}
                {:name :location
                 :selector [:text ".p24_location "]}
                {:name :descriotion
                 :selector [:or [:text ".p24_title"]
                                [:text ".p24_description"]]}
                {:name :reduced?
                 :selector [:flag ".p24_reducedBanner"]}
                {:name :under-offer?
                 :selector [:flag ".p24_underOfferBanner"]}
                {:name :address
                 :selector [:text ".p24_address"]}
                {:name :price
                 :selector [:number".p24_price"]}]}]}
            #_{:id :takealot
             :url-selector ["div[class^=transition-horizontal-module_slide] a"]
             :paginate-selector "div[class^=search-listings-module_load-more-container] button:not(.loading)"
             :start-urls ["http://takealot.co.za/computers/all"]
             :page-handler :infite-scroll-paginate-handler
             :entities
             [{:type :product
               :root "div.product-card"
               :attributes
               [{:name     :name
                 :selector [:text "div[class^=product-card-module_title] .product-title span.shiitake-children + span"]}
                {:name     :id
                 :selector [:href-after-last "div.product-card a.product-anchor" #"/"]}]}]}])
