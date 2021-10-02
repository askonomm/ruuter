(defproject ruuter "1.0.0"
  :description "A tiny HTTP router"
  :url "https://github.com/askonomm/ruuter"
  :license {:name "MIT"
            :url  "https://raw.githubusercontent.com/askonomm/ruuter/master/LICENSE.txt"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [http-kit "2.5.3"]]
  :main ruuter.core
  :min-lein-version "2.0.0"
  :repl-options {:init-ns ruuter.core})
