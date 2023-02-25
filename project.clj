(defproject org.clojars.askonomm/ruuter "1.3.3"
  :description "A tiny HTTP router"
  :url "https://github.com/askonomm/ruuter"
  :license {:name "MIT"
            :url  "https://raw.githubusercontent.com/askonomm/ruuter/master/LICENSE.txt"}
  :dependencies [[org.clojure/clojure "1.11.1"]]
  :deploy-repositories [["releases" {:sign-releases false
                                     :url "https://repo.clojars.org/"}]
                        ["snapshots" :clojars]]
  :plugins [[lein-cloverage "1.2.3"]]
  :profiles {:test {:dependencies [[org.clojure/clojurescript "1.11.60"]]}}
  :main ruuter.core
  :min-lein-version "2.0.0"
  :aot [ruuter.core]
  :repl-options {:init-ns ruuter.core})
