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

## Run with Vert.x ##

Well, default example of running ring-vertx-adapter is embeded.
if you wanna it run with vertx's command such as `vertx run core.clj`. you should know something.

* Includes some lib to your verticle
I recommend run this adapter with vert.x module, you could clone this project then install it with
`lein install`, get the jar of ring-vertx-adapter to your lib of module, as well as some other lib
your dependecy.

* Use repl to startup your project.
It is very common that run project of clojure with repl in lein, But i have not yet enought time to make
a plugin to wrap vert.x's clojure as a module in lein. so you could refer [example of repl](https://github.com/vert-x/mod-lang-clojure/blob/master/examples/repl/server.clj) to startup it.

Well, you also could use command run directly if you set classpath.

`vertx run core.clj -cp classes:lib/ring-vertx-adapter.jar`
