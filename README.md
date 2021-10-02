# Ruuter

A tiny HTTP router that operates with a simple data structure where each route is a map inside a vector. Yup, that's it. No magic, no bullshit. 

## Usage

### Setting up

Require the namespace `ruuter.core` and then pass your routes to the `route!` function, like this:

```clojure
(ns myapp.core
  (:require [ruuter.core :as ruuter]))

(defn -main [& opts]
  (ruuter/route! [{:path "/"
                   :method :get
                   :response {:status 200
                              :body "Hi there!"}}]))
```

This will start an HTTP server on a default port of 9600 using [http-kit](https://github.com/http-kit/http-kit) under the hood.

The `route!` function also takes a second, optional argument, which is the [options map for http-kit](http://http-kit.github.io/http-kit/org.httpkit.server.html#var-run-server), allowing you to specify the port and so on, like this:

```clojure
(def routes [{:path "/"
              :method :get
              :response {:status 200
                         :body "Hi there!"}}])

(ruuter/route! routes {:port 8080})
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

The HTTP method to listen for when matching the given path.

Accepted values are:

- `:get`
- `:post`
- `:put`
- `:delete`
- `:head`
- `:options`
- `:patch`

#### `:response`

The response can be a direct map, or a function returning a map. In case of a function, you will also get passed to you the `request` object. For better information on what are all the things you could do with a response, check out [the http-kit documentation](https://http-kit.github.io/server.html).
