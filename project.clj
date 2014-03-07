(defproject ring-vertx-adapter "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[ring/ring-core "1.2.1"]
                 [io.vertx/clojure-api "1.0.0.RC1"]]

  :profiles
  {:dev {:dependencies [[org.clojure/clojure "1.5.1"]
                        [compojure "1.1.6"]
                        [clj-http "0.6.4"]]}})

