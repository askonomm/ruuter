# Ruuter

A tiny, zero dependency, system-agnostic router for Clojure, ClojureScript, Babashka and NBB that operates with a simple data structure where each route is a map inside a vector. Yup, that's it. No magic, no bullshit. 

## Installation

[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.askonomm/ruuter.svg)](https://clojars.org/org.clojars.askonomm/ruuter)

## Articles

- [Routing with Ruuter in a Reagent / Re-frame project](https://omma.ee/routing-with-ruuter-in-a-reagent-re-frame-project/)

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

Note that the `request-method` doesn't have to be a keyword, it can be anything that your HTTP server returns. But it does have to be called `request-method` for the router to know where to look for. That said, you do not have to provide neither `method` in the route, nor `request-method` in the request if you don't want to. You can skip both of them and let Ruuter route based on the `:uri` alone if you want.

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

### Setting up with [Babashka](https://github.com/babashka/babashka)

You can also use Ruuter with [Babashka](https://github.com/babashka/babashka), by using the built-in http-kit server, for example. Either add the dependency in your `bb.edn` file or if you want to make the whole thing one-file-rules-them-all, then load it in with `deps/add-deps`, like below:

```clojure
#!/usr/bin/env bb

(deps/add-deps '{:deps {org.clojars.askonomm/ruuter {:mvn/version "1.3.2"}}})

(require '[org.httpkit.server :as http]
         '[babashka.deps :as deps]
         '[ruuter.core :as ruuter])

(def routes [{:path "/"
              :method :get
              :response {:status 200
                         :body "Hi there!"}}])

(http/run-server #(ruuter/route routes %) {:port 8082})

@(promise)
```

### Creating routes

Like mentioned above, each route is a map inside a vector - the order is important only in that the route matcher will return the first result it finds according to `:path`. 

Each route consists of three items:

#### `:path`

A string path starting with a forward slash describing the URL path to match. 

To create parameters from the path, prepend a colon (:) in front of a path slice like you would with a Clojure keyword. 

##### Required parameters

A required parameter with a string such as `/hi/:name`, which would match any string that matches the `\/hi\/.*` regex in the URI, in its own slice. The `:name` itself will then be available with its value from the `request` passed to the response function, like this:

```clojure
(fn [req]
  (let [name (:name (:params req))]
    {:status 200
     :body (str "Hi, " name)}))
```

##### Optional parameters

A optional parameter with a string such as `/hi/:name?`, which would match any string that matches the `\/hi\/?.*?` regex in the URI, in its own slice. If there is a `:name` provided in the URI then it will then be available with its value from the `request` passed to the response function, like this:

```clojure
(fn [req]
  (let [name (:name (:params req))]
    {:status 200
     :body (str "Hi, " name)}))
```

##### Wildcard parameters

The above-mentioned `:name` and `:name?` only match in its own path slice, e.g inside a space surrounded by two forward slashes. They cannot, by design, match the whole URL path. If you need wildcard matching, instead use `:name*`, which will match everything, including forward slashes.

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

### 1.3.4

- Fixes an issue where if used with middlewares (like Ring), or really anything that passes a `:params` key from outside of Ruuter in the request, Ruuter overwrites the `:params` key with its own parametarization. It now does a deep merge instead, with the outside parameters having priority. This means that Ruuter parameters will remain unless overwritten, and should co-exist with outside parameters nicely. [Issue #6](https://github.com/askonomm/ruuter/issues/6).

- Added and fixed some tests

### 1.3.3

- Removed ClojureScript from dependencies to make the bundle size smaller in case you want to use Ruuter with nbb.

### 1.3.2

- When using wildcard parameters, the keyword returned in ´:params´ of a request was ´:name*´, but aiming for consistency with an optional parameter where we remove the question mark ´?´, the asterisk has been removed. 

### 1.3.1 

- A small bugfix related to wildcard parameters losing the first character in the result.

### 1.3.0

- Fixed an issue with optional parameters not matching correctly when there were multiple optional paremeters in use.
- Implemented wildcard parameters in the form of `:name*`, which will match everything including forward slashes.

### 1.2.2

- Fixed an issue where CLJS compilation would fail because of the `(:gen-class)` that is JVM-only. 

- Tests are now runnable for CLJS as well, via `clojure -Atest`. 

### 1.2.1

- Fixed an issue with regex parsing. Sorry about that.

### 1.2.0

- Implemented optional route parameters, so now you can do paths like `/hi/:name?` in your routes, and it would match the route even if the `:name` is not present. All you have to do is add a question mark to the parameter, and that's it.

- Changed Ruuter from a .clj file to a .cljc file, so it would also work with ClojureScript. Although it would probably require a more hands-on set-up than just a drop-in to an HTTP server like http-kit or ring + jetty, there is no reason that the router itself wouldn't work as it does not rely on any platform-specific code.

- Ruuter also works with [Babashka](https://github.com/babashka/babashka), and I've created a "Setting up with Babashka" section in this README to show that.

### 1.1.0

- Made Ruuter server-agnostic, which means now it really is just a router and nothing else, and can thus be used with just about any HTTP server you can throw at it. It also means there are now zero dependencies! ZERO!
