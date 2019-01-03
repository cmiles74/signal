(ns cmiles74.signal.cli
  (:gen-class)
  (:require
   [taoensso.timbre :as timbre
    :refer (log  trace  debug  info  warn  error  fatal  report
                 logf tracef debugf infof warnf errorf fatalf reportf
                 spy get-env log-env)]
   [taoensso.timbre.profiling :as profiling
    :refer (pspy pspy* profile defnp p p*)]
   [cmiles74.common.config :as config])
  (:use [slingshot.slingshot :only [try+ throw+]]))

;; default configuration
(def DEFAULT-CONFIG
  {:signal
   {:user-agent "cmiles74-signal"
    :signal-url "https://textsecure-service.whispersystems.org"
    :cdn-url "https://cdn.signal.org"}})

;; default name of the configuration file
(def DEFAULT-CONFIG-FILE ".cmiles74-signal.yml")

(defn generate-password
  "Randomly generates a password of the specified length or with the le of
  16 if no length is specified."
  ([] (generate-password 16))
  ([length]
   (let [chars (map char (range 33 127))
         password (take length (repeatedly #(rand-nth chars)))]
     (apply str password))))

(defn store-config-file
  "Stores the provided map of configuration data to to the provided path with
  the specified file name."
  ([config-map] (store-config-file (System/getProperty "user.home") DEFAULT-CONFIG-FILE config-map))
  ([path-in filename config-map]
   (try+
    (config/save-config-file path-in filename config-map)
    (catch Exception exception
      (throw+ {:type :config-save-fail :message (.getMessage exception)})))))

(defn load-config-file
  "Loads the configuration file from the provided path, returns a map with the
  configuration values."
  [path-in]
  (let [config (merge (config/load-config-file path-in
                                               DEFAULT-CONFIG-FILE
                                               DEFAULT-CONFIG)
                      DEFAULT-CONFIG)]
    (if (nil? (get-in config [:account :username]))
      (throw+ {:type :no-config :message "No configuration file found"}))
    config))

(defn store-user-config
  "Stores the customer's configuration to the default configuration file. A
  country code may be provided and the phone number is required. If no country
  code is provided then the US (\"+1\") is used."
  ([phone-number] (store-user-config "+1" phone-number))
  ([country-code phone-number]
   (let [user-config {:account {:username (str country-code phone-number)
                                :password (generate-password)}}]
     (store-config-file user-config)
     (merge DEFAULT-CONFIG user-config))))

(defn load-user-config
  "Loads the customer's configuration from the default configuration file."
  []
  (try+
   (load-config-file nil)
   (catch [:type :no-config] {:keys [message]}
     (warn message))))

;;
;; Bootstrapping functions
;;

(defn main
  "Bootstraps the application and handles command line arguments."
  [& args]
  (info "Hello from the Signal Client!"))

(defn -main
  "The bootstrapping function used to start the application."
  [& args]
  (apply main args))
