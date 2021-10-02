(ns ruuter.core
  (:require [clojure.string :as string]
            [org.httpkit.server :as http])
  (:gen-class))


(defn- path->regex-path
  "Takes in a raw route `path` and turns it into a regex pattern to
  match against the request URI."
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
  "Takes a raw route `path` and the actual request `uri`, which it then
  turns into a map of k:v, if any parameters were used in the `path`."
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
  "For a collection of `route`, will attempt to find one that matches
  the given `uri` and `request-method`. If none is matched, `nil` will
  be returned instead."
  [routes uri request-method]
  (->> routes
       (filter #(not (= :not-found (:path %))))
       (map #(merge % {:regex-path (path->regex-path (:path %))}))
       (filter #(and (re-matches (re-pattern (:regex-path %)) uri)
                     (= (:method %) request-method)))
       first))


(defn- route+req->response
  "Given the current route and the current HTTP request, it will
   attempt to return a response, either directly if it's a map or
   indirectly if it's a function. In case of a function, it will also
   pass along the request map with added-in params that were parsed
   from the route path.

   If the response is invalid, or does not exist, a error message with
   status code 404 will be returned instead."
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
  "For a given collection of `routes` and the current HTTP request as
  `req`, will attempt to match a route with the HTTP request, which it
  will then try to return a response for.

  If no route matched for a given HTTP request it will try to find a
  route with `:not-found` as its `:path` instead, and return the response
  for that."
  [routes {:keys [uri request-method] :as req}]
  (if-let [route (match-route routes uri request-method)]
    (route+req->response route req)
    (route+req->response (->> routes
                              (filter #(= :not-found (:path %)))
                              first) req)))


(defn route!
  "Starts an HTTP server which will then try to find a matching route for
  each request from within the given collection of `routes`. Takes an
  optional `opts` map, which corresponds directly to HTTP-Kit's config,
  allowing you to specify things like `{:port 8080}` and so on."
  ([routes]
   (route! routes {:port 9600}))
  ([routes opts]
   (http/run-server #(router routes %) opts)))