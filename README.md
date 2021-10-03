# Ruuter

A tiny HTTP router that operates with a simple data structure where each route is a map inside a vector. Yup, that's it. No magic, no bullshit. 

## Installation

[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.askonomm/ruuter.svg)](https://clojars.org/org.clojars.askonomm/ruuter)

## Usage

### Setting up

Require the namespace `ruuter.core` and then pass your routes to the `route` function along with the current request map, like this:

```clojure
(ns myapp.core
  (:require [ruuter.core :as ruuter]))

(def routes [{:path "/"
              :method :get
              :response {:status 200
                         :body "Hi there!"}}])

(def request {:uri "/"
              :request-method :get})

(ruuter/route routes request) ; => {:status 200
                              ;     :body "Hi there!"}
```

This will attempt to match a route with the request map and return the matched route' response. If no route was found, it will attempt to find a route that has a `:path` that is `:not-found`, and return its response instead. But if not even that route was found, it will simply return a built-in 404 response instead.

Note that the `request-method` doesn't have to be a keyword, it can be anything that your HTTP server returns. But it does have to be called `request-method` for the router to know where to look for. 

### Setting up with [http-kit](https://github.com/http-kit/http-kit)

Now, obviously on its own the router is not very useful as it needs an actual HTTP server to return the responses to the world, so here's an example that uses [http-kit](https://github.com/http-kit/http-kit):

```clojure
(ns myapp.core
  (:require [ruuter.core :as ruuter]
            [org.httpkit.server :as http]))

(def routes [{:path "/"
              :method :get
              :response {:status 200
                         :body "Hi there!"}}
             {:path "/hello/:who"
              :method :get
              :response (fn [req]
                          {:status 200
                           :body (str "Hello, " (:who (:params req)))})}])

(defn -main []
  (http/run-server #(ruuter/route routes %) {:port 8080}))
```

### Setting up with [Ring + Jetty](https://github.com/ring-clojure/ring)

[Ring + Jetty](https://github.com/ring-clojure/ring) set-up is almost identical to the one of http-kit, and looks like this:

```clojure
(ns myapp.core
  (:require [ruuter.core :as ruuter]
            [ring.adapter.jetty :as jetty]))

(def routes [{:path "/"
              :method :get
              :response {:status 200
                         :body "Hi there!"}}
             {:path "/hello/:who"
              :method :get
              :response (fn [req]
                          {:status 200
                           :body (str "Hello, " (:who (:params req)))})}])

(defn -main []
  (jetty/run-jetty #(ruuter/route routes %) {:port 8080}))
```

### Creating routes

Like mentioned above, each route is a map inside of a vector - the order is important only in that the route matcher will return the first result it finds according to `:path`. 

Each route consists of three items:

#### `:path`

A string path starting with a forward slash describing the URL path to match. 

To create parameters from the path, prepend a colon (:) in front of a path slice like you would with a Clojure keyword. For example a string such as `/hi/:name` would match any string that matches the `/hi/.*` regex. The `:name` itself will then be available with its value from the `request` passed to the response function, like this:

```clojure
(fn [req]
  (let [name (:name (:params req))]
    {:status 200
     :body (str "Hi, " name)}))
```

#### `:method`

The HTTP method to listen for when matching the given path. This can be whatever the HTTP server uses. For example, if you're using http-kit for the HTTP server then the accepted values are:

- `:get`
- `:post`
- `:put`
- `:delete`
- `:head`
- `:options`
- `:patch`

#### `:response`

The response can be a direct map, or a function returning a map. In case of a function, you will also get passed to you the `request` map that the HTTP server returns, with added-in `:params` that contain the values for the URL parameters you use in your route's `:path`.

Thus, a `:response` can be a map:

```clojure
{:status 200
 :body "Hi there!"}
 ```

Or a function returning a map:

```clojure
(fn [req]
  {:status 200
   :body "Hi there!"})
 ```

What the actual map can contain that you return depends again on the HTTP server you decided to use Ruuter with. The examples I've noted here are based on [http-kit](https://github.com/http-kit/http-kit) & [ring + jetty](https://github.com/ring-clojure/ring), but feel free to make a PR with additions for other HTTP servers.

## Changelog

### 1.1.0

- Made Ruuter server-agnostic, which means now it really is just a router and nothing else, and can thus be used with just about any HTTP server you can throw at it. It also means there are now zero dependencies! ZERO!