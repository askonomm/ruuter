(ns ruuter.core-test
  #?(:clj (:require [clojure.test :refer :all]
                    [ruuter.core :as ruuter]))
  #?(:cljs (:require [cljs.test :refer-macros [deftest testing is]]
                     [ruuter.core :as ruuter])))

(deftest path+uri->path-params-test
  (let [testfn #'ruuter/path+uri->path-params]
    (testing "No params returns an empty map"
      (is (= {}
             (testfn "/hello/world" "/hello/world"))))
    (testing "Having a param returns a map accordingly"
      (is (= {:who "world"}
             (testfn "/hello/:who" "/hello/world"))))
    (testing "Multiple params returns a map accordingly"
      (is (= {:who "world"
              :why "because"}
             (testfn "/hello/:who/:why" "/hello/world/because"))))
    (testing "Multiple params, but one is optional"
      (is (= {:who "world"}
             (testfn "/hello/:who/:why?" "/hello/world")))
      (is (= {:who "world"
              :why "because"}
             (testfn "/hello/:who/:why?" "/hello/world/because"))))
    (testing "Multiple params, but all are optional"
      (is (= {:who "world"
              :why "because"}
             (testfn "hello/:who?/:why?" "/hello/world/because")))
      (is (= {:who "world"}
             (testfn "/hello/:who?/:why?" "/hello/world"))))
    (testing "Wildcard param"
      (is (= {:everything "this/means/literally/everything"}
             (testfn "/hello/:everything*" "/hello/this/means/literally/everything"))))
    (testing "Normal params and wildcard param in the end"
      (is (= {:id "123"
              :path "foo.txt"}
             (testfn "/user/:id/file/:path*" "/user/123/file/foo.txt")))
      (is (= {:id "123"
              :path "a/b/c/foo.txt"}
             (testfn "/user/:id/file/:path*" "/user/123/file/a/b/c/foo.txt")))
      (is (= {:id "123"
              :path "a/b/c/foo.txt"
              :sub-path "b/c/foo.txt"}
             (testfn "/user/:id/file/:path*/:sub-path*" "/user/123/file/a/b/c/foo.txt"))))))

(deftest match-route-test
  (let [testfn #'ruuter/match-route]
    (testing "Find a route that exists"
      (is (= {:path "/hello"
              :method :get
              :response {:status 200
                         :body "Hello."}}
             (testfn [{:path "/hello"
                       :method :get
                       :response {:status 200
                                  :body "Hello."}}] "/hello" :get))))
    (testing "No route found"
      (is (= nil
             (testfn [] "/hello" :get))))))

(deftest route+req->response-test
  (let [testfn #'ruuter/route+req->response]
    (testing "Returns a map when the response is a direct map"
      (is (= {:status 200
              :body "Hello."}
             (testfn {:path "/hello"
                      :response {:status 200
                                 :body "Hello."}}
                     {:uri "/hello"}))))
    (testing "Returns a map via a fn when the response is a fn"
      (is (= {:status 200
              :body "Hello, world."}
             (testfn {:path "/hello/:who"
                      :response (fn [req]
                                  {:status 200
                                   :body (str "Hello, " (:who (:params req)) ".")})}
                     {:uri "/hello/world"}))))
    (testing "Returns an error map when route is invalid"
      (is (= {:status 404
              :body "Not found."}
             (testfn nil {:uri "/hello"}))))

    (testing "Returns a combined :params map"
      (let [params (atom nil)]
        (testfn {:path "/hello/:who"
                 :response (fn [req]
                             (reset! params (:params req))
                             {:status 200
                              :body ""})}
                {:uri "/hello/world"
                 :params {:some-params :from-elsewhere}})
        (is (= {:who "world"
                :some-params :from-elsewhere}
               @params))))

    (testing "Overwrites :params if needed"
      (let [params (atom nil)]
        (testfn {:path "/hello/:who"
                 :response (fn [req]
                             (reset! params (:params req))
                             {:status 200
                              :body ""})}
                {:uri "/hello/world"
                 :params {:who "overwritten"}})
        (is (= {:who "overwritten"}
               @params))))))
