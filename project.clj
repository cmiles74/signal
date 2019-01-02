(defproject cmiles74/signal "0.1-SNAPSHOT"
  :description "Library for Interacting with the Signal Messenging Service"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/core.async "0.4.490"]
                 [org.clojure/tools.cli "0.4.1"]
                 [clj-yaml "0.4.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [slingshot "0.12.2"]
                 [manifold "0.1.8"]
                 [org.slf4j/slf4j-simple "1.7.25"]
                 [clojure-msgpack "1.2.1"]
                 [org.bouncycastle/bcprov-jdk15on "1.60"]
                 [org.whispersystems/signal-service-java "2.12.2"]]
  :main cmiles74.signal.cli

  :profiles {:uberjar {:aot :all}
             :dev {:source-paths ["dev"]}})

