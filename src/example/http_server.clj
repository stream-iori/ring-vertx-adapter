(ns example.http-server
  (:use compojure.core)
  (:require [compojure.route :as route]
            [vertx.embed :as vertx]
            [vertx.http :as http]
            [ring.adapter.vertx :as ring-vertx]))

(vertx/set-vertx! (vertx/vertx))

(defroutes app
  (GET "/" [] "<h1>Hello World</h1>")
  (route/not-found "<h1>Page not found</h1>"))

(ring-vertx/run-vertx-web (http/server) app "localhost" 8080)
