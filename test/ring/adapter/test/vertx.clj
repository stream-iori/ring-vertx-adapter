(ns ring.adapter.test.vertx
  (:import (java.io ByteArrayInputStream))
  (:use clojure.test
        ring.adapter.vertx
        [ring.util.io :only (string-input-stream)]
        ring.middleware.params
        ring.middleware.cookies)
  (:require [clj-http.client :as http-client]
            [ring.middleware.params :as ring-params]
            [vertx.embed :as vertx]
            [vertx.buffer :as buf]
            [vertx.http :as http]))

(vertx/set-vertx! (vertx/vertx))

(defn- hello-world [request]
  {:status  200
   :headers {"Content-Type" "text/plain"}
   :body    "Hello World"})

(defn- content-type-handler [content-type]
  (constantly
   {:status  200
    :headers {"Content-Type" content-type}
    :body    ""}))

(defn- echo-handler [request]
  {:status 200
   :headers {"request-map" (str (dissoc request :body))}
   :body (:body request)})

(defmacro with-server [server app host port & body]
  `(let [http-server# (run-vertx-web ~server ~app ~host ~port)]
     (try
       ~@body
       (finally (http/close http-server#)))))

(defn with-server-fn [server app host port complete-h]
  (run-vertx-web server app host port complete-h))

(defn- get-http-server
  ([]
     (http/server))
  ([properties]
     (http/server properties)))



(defn html-escape [string]
  (str "<pre>" (clojure.string/escape string {\< "&lt;", \> "&gt;"}) "</pre>"))

(defn format-request [name request]
  (with-out-str
    (println "-------------------------------")
    (println name)
    (clojure.pprint/pprint request)
    (println "-------------------------------")))

(defn wrap-spy [handler spyname include-body]
  (fn [request]
    (let [incoming (format-request (str spyname ":\n Incoming Request:") request)]
      (println incoming)
      (let [response (handler request)]
        (let [r (if include-body response (assoc response :body "#<?>"))
              outgoing (format-request (str spyname ":\n Outgoing Response Map:") r)]
          (println outgoing)
          (update-in response  [:body] (fn[x] (str (html-escape incoming) x  (html-escape outgoing)))))))))


(defn handler [request]
  {:headers {"cookie" "a=\"b=c;e=f\""}})

(def app-handler
  (-> handler
      (wrap-spy "handler incomming" true)
      (wrap-cookies)
      (ring-params/wrap-params)
      (wrap-spy "server incomming" true))
  )


(deftest test-run-vertx-web
  (testing "HTTP server"
    (with-server (get-http-server) hello-world "localhost" 4347
      (let [response (http-client/get "http://localhost:4347")]
        (is (= (:status response) 200))
        (is (.startsWith (get-in response [:headers "content-type"])
                         "text/plain"))
        (is (= (:body response) "Hello World")))))

  (testing "default character encoding"
    (with-server (get-http-server) (content-type-handler "text/plain")
      "localhost" 4347
      (let [response (http-client/get "http://localhost:4347")]
        (is (.contains
             (get-in response [:headers "content-type"])
             "text/plain")))))

  (testing "custom content-type"
    (with-server (get-http-server) (content-type-handler "text/plain;charset=UTF-16;version=1")
      "localhost" 4347
      (let [response (http-client/get "http://localhost:4347")]
        (is (= (get-in response [:headers "content-type"])
               "text/plain;charset=UTF-16;version=1")))))

  (testing "request translation"
    (with-server (get-http-server) echo-handler "localhost" 4347
      (let [response (http-client/get "http://localhost:4347/foo/bar/baz?surname=jones&age=123" {:body "hello"})]
        (is (= (:status response) 200))
        (is (= (:body response) "hello"))
        (let [request-map (read-string (get-in response [:headers "request-map"]))]
          (is (= (:query-string request-map) "surname=jones&age=123"))
          (is (= (:uri request-map) "/foo/bar/baz"))
          (is (= (:content-length request-map) 5))
          (is (= (:character-encoding request-map) "UTF-8"))
          (is (= (:request-method request-map) :get))
          (is (= (:content-type request-map) "text/plain; charset=UTF-8"))
          (is (= (:remote-addr request-map) "127.0.0.1"))
          (is (= (:scheme request-map) :http))
          (is (= (:server-name request-map) "localhost"))
          (is (= (:server-port request-map) 4347))
          (is (= (:ssl-client-cert request-map) nil))))))

  (testing "HTTPS server"
    (with-server (get-http-server {:SSL true
                                   :key-store-path "test/keystores/server-keystore.jks"
                                   :key-store-password "wibble"
                                   :trust-store-path "test/keystores/server-truststore.jks"
                                   :trust-store-password "wibble"
                                   :client-auth-required true})
      hello-world "localhost" 4347
      (let [response (http-client/get "https://localhost:4347"
                                      {:keystore "test/keystores/client-keystore.jks"
                                       :keystore-pass "wibble"
                                       :trust-store "test/keystores/client-truststore.jks"
                                       :trust-store-pass "wibble"})]
        (is (= (:status response) 200))
        (is (= (:body response) "Hello World")))))

  )
