(ns ruuter.core
  (:require
    [clojure.string :as string])
  #?(:clj (:gen-class)))

(defn- path->regex-path
  "Takes in a raw route `path` and turns it into a regex pattern to
  match against the request URI."
  [path]
  (cond (= "/" path)
        "\\/"

        (re-find #"\*" path)
        (-> (string/replace path #"\:.*?\*" ".*?")
            (string/replace #"/" "\\/"))

        :else
        (->> (string/split path #"/")
             (map #(cond
                     ; matches anything, and must be present
                     ; for example `:name`
                     (and (string/starts-with? % ":")
                          (not (string/ends-with? % "?")))
                     ".*"
                     ; matches anything, but is optional
                     ; for example `:name?`
                     (and (string/starts-with? % ":")
                          (string/ends-with? % "?"))
                     "?.*?"
                     :else
                     ; what comes around, goes around
                     %))
             (string/join "\\/"))))

(defn- path+uri->path-params
  "Takes a raw route `path` and the actual request `uri`, which it then
  turns into a map of k:v, if any parameters were used in the `path`."
  [path uri]
  (cond (= "/" path)
        {}

        (re-find #"\*" path)
        (let [index-of-k-start (string/index-of path ":")
              k (-> (subs path (+ 1 index-of-k-start))
                    drop-last
                    string/join
                    keyword)
              v (subs uri index-of-k-start)]
          {k v})

        :else
        (let [split-path (->> (string/split path #"/")
                              (remove empty?)
                              vec)
              split-uri (->> (string/split uri #"/")
                             (remove empty?)
                             vec)]
          (into {} (map-indexed
                     (fn [idx item]
                       (cond
                         ; required parameter
                         (and (string/starts-with? item ":")
                              (not (string/ends-with? item "?")))
                         {(keyword (subs item 1)) (get split-uri idx)}
                         ; optional parameter
                         (and (string/starts-with? item ":")
                              (string/ends-with? item "?")
                              (get split-uri idx))
                         {(keyword (-> item
                                       (subs 0 (- (count item) 1))
                                       (subs 1)))
                          (get split-uri idx)}))
                     split-path)))))

(defn- match-route
  "For a collection of `route`, will attempt to find one that matches
  the given `uri` and `request-method`. If none is matched, `nil` will
  be returned instead."
  [routes uri request-method]
  (let [route (->> routes
                   (filter #(not (= :not-found (:path %))))
                   (map #(merge % {:regex-path (path->regex-path (:path %))}))
                   (filter #(and (re-matches (re-pattern (:regex-path %)) uri)
                                 (= (:method %) request-method)))
                   first)]
    (when route
      (dissoc route :regex-path))))

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

(defn route
  "For a given collection of `routes` and the current HTTP request as
  `req`, will attempt to match a route with the HTTP request, which it
  will then try to return a response for. The only requirement for `req`
  is to contain both a `uri` and `request-method` key. First should match
  the request path (like the paths defined in routes) and the second
  should match the request method used by the HTTP server you pass this fn to.

  If no route matched for a given HTTP request it will try to find a
  route with `:not-found` as its `:path` instead, and return the response
  for that, and if that route was also not found, will return a built-in
  404 response instead."
  [routes {:keys [uri request-method] :as req}]
  (if-let [route (match-route routes uri request-method)]
    (route+req->response route req)
    (route+req->response (->> routes
                              (filter #(= :not-found (:path %)))
                              first) req)))
