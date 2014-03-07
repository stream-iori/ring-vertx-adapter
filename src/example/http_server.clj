(ns example.http-server
  (:use compojure.core)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [vertx.embed :as vertx]
            [vertx.http :as http]
            [ring.adapter.vertx :as ring-vertx]))

(vertx/set-vertx! (vertx/vertx))

(defroutes app
  (GET "/" [] {:cookies {"name" "stream"}})
  (route/not-found "<h1>Page not found</h1>"))

(defn start-http-server []
  (ring-vertx/run-vertx-web (handler/site #'app) "localhost" 8080))

(def repl-server-id (atom nil))
(reset! repl-server-id (repl/start))



