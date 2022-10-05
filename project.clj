(defproject org.clojars.askonomm/ruuter "1.3.0"
  :description "A tiny HTTP router"
  :url "https://github.com/askonomm/ruuter"
  :license {:name "MIT"
            :url  "https://raw.githubusercontent.com/askonomm/ruuter/master/LICENSE.txt"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [org.clojure/clojurescript "1.11.60"]]
  :deploy-repositories [["releases" :clojars]
                        ["snapshots" :clojars]]
  :plugins [[lein-cloverage "1.2.3"]]
  :main ruuter.core
  :min-lein-version "2.0.0"
  :aot [ruuter.core]
  :repl-options {:init-ns ruuter.core})
