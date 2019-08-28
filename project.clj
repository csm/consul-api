(defproject com.github.csm/consul-api "0.1.0-SNAPSHOT"
  :description "Swagger definition of the Consul HTTP API."
  :url "https://github.com/csm/consul-api"
  :license {:name "MPL-2.0"
            :url "https://www.mozilla.org/en-US/MPL/2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [metosin/compojure-api "2.0.0-alpha30"]]
  :profiles {:eg {:dependencies [[aleph "0.4.6"]]}}
  :aliases {"gen-swagger" ["run" "-m" "consul-api.gen/gen-swagger-json"]}
  :repl-options {:init-ns consul-api.repl})
