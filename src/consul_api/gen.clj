(ns consul-api.gen
  (:require [clojure.java.io :as io]
            [consul-api.core :as a]))

(defn gen-swagger-json
  ([] (gen-swagger-json "resources/consul-api/swagger.json"))
  ([output-path]
   (let [api (a/consul-api)
         swagger (api {:uri "/swagger.json"
                       :request-method :get})]
     (with-open [out (io/writer (io/file output-path))]
       (io/copy (io/reader (:body swagger)) out)))))