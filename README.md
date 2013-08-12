ring-vertx-adapter
==================

The adapter of Ring for Vert.x

This project dependency lib of [io.vertx/clojure-api](https://github.com/vert-x/mod-lang-clojure)

Simply example:

    ;;we use Compojure as router 
    (defroutes app
       (GET "/" [] "<h1>Hello World</h1>")
       (route/not-found "<h1>Page not found</h1>"))
       
    (ring-vertx/run-vertx-web (http/server) app "localhost" 8080)

full example see [src/example/http_server](src/example/http_server.clj).
