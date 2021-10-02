(ns ruuter.core
  (:require [clojure.string :as string]
            [org.httpkit.server :as http]))


(def routes [{:path "/"
              :method :get
              :response {:status 200
                         :body "Hello, World."}}
             {:path "/some/page/goes/here"
              :method :get
              :response {:status 200
                         :body ":)"}}
             {:path "/hi/:name"
              :method :get
              :response (fn [req]
                          {:status 200
                           :body (str "Hi, " (:name (:params req)))})}
             {:path :not-found
              :response {:status 404
                         :body "Not found."}}])


(defn- path->regex-path
  [path]
  (if (= "/" path)
    path
    (->> (string/split path #"/")
         (map (fn [piece]
                (if (string/starts-with? piece ":")
                  ".*"
                  piece)))
         (string/join "/"))))


(defn- path+uri->path-params
  [path uri]
  (if (= "/" path)
    {}
    (let [split-path (string/split path #"/")
          split-uri (string/split uri #"/")]
      (into {} (map-indexed
                 (fn [idx item]
                   (when (string/starts-with? item ":")
                     {(keyword (subs item 1)) (get split-uri idx)}))
                 split-path)))))


(defn- match-route
  [routes uri request-method]
  (->> routes
       (filter #(not (= :not-found (:path %))))
       (map #(merge % {:regex-path (path->regex-path (:path %))}))
       (filter #(and (re-matches (re-pattern (:regex-path %)) uri)
                     (= (:method %) request-method)))
       first))


(defn- route+req->response
  [{:keys [path response]} {:keys [uri] :as req}]
  (cond
    ; responses are maps, so there's no reason they can't be
    ; direct maps.
    (map? response)
    response
    ; responses can also be functions that return maps, and
    ; when using a function, you get the whole `req` and params
    ; with it as well.
    (fn? response)
    (response (->> {:params (path+uri->path-params path uri)}
                   (merge req)))
    ; if by whatever reason we make it here it must mean the
    ; route is invalid, or doesn't exist, in which case we return
    ; an error message.
    :else
    {:status 404
     :body "Not found."}))


(defn- router
  [routes {:keys [uri request-method] :as req}]
  (if-let [route (match-route routes uri request-method)]
    (route+req->response route req)
    (route+req->response (->> routes
                              (filter #(= :not-found (:path %)))
                              first) req)))


(defn route!
  ([routes]
   (route! routes {:port 9600}))
  ([routes opts]
   (http/run-server #(router routes %) opts)))


(defn -main [& opts]
  (route! routes))