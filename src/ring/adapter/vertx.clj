(ns ring.adapter.vertx
  "Adapter for the vertx webserver."
  (:import (java.io File InputStream FileInputStream
                    OutputStream ByteArrayOutputStream
                    ByteArrayInputStream))
  (:require [vertx.http :as http]
            [vertx.stream :as stream]
            [vertx.buffer :as buf]
            [clojure.java.io :as io]
            [clojure.string :as string]))

(defn- get-headers
  "Creates a name/value map of all the request headers."
  [req]
  (let [headers (.headers req)]
    (into {} (for [m (.entries headers)]
               {(string/lower-case (key m)) (val m)}))))

(defn set-status
  "Update a HttpServerResponse with a status code."
  [resp status]
  (.setStatusCode resp status))

(defn set-headers
  "Update a HttpServerResponse with a map of headers."
  [resp headers]
  (doseq [[key val-or-vals] headers]
    (.putHeader resp key val-or-vals)))

(defn- set-body
  "Update a HttpServerResponse body with a String, ISeq, File or InputStream."
  [resp body]
  (cond
   (string? body) (http/end resp body)

   (seq? body) (do (.setChunked resp true)
                   (doseq [chunk body]
                     (stream/write resp chunk))
                   (http/end resp))

   (instance? InputStream body)
   (with-open [^InputStream in body
               ^OutputStream out (ByteArrayOutputStream.)]
     (io/copy in out)
     (http/end resp (buf/buffer (.toByteArray out))))

   (instance? File body) (let [^File f body]
                           (with-open [stream (FileInputStream. f)]
                             (set-body resp stream)))
   (nil? body) nil
   :else
   (throw (Exception. ^String (format "Unrecognized body: %s" body)))))

(defn- get-content-type
  "Get the content type from header."
  [header]
  (or (get header "content-type")
      "text/plain; charset=UTF-8"))

(defn- get-char-encoding
  "Get the character encoding"
  [content-type]
  (if (>= 0 (.indexOf content-type "charset"))
    (second (string/split content-type #"="))
    "UTF-8"))

(defn- update-response
  "Update ring response to vertx response"
  [resp, {:keys [status headers body]}]
  (when-not resp
    (throw (Exception. "Null response given.")))
  (when status
    (set-status resp status))
  (doto resp
    (set-headers headers)
    (set-body body)))

(defn- build-request-map
  "Return ring request with Vertx's Web parameter"
  [req data]
  (let [header (get-headers req)
        content-type (get-content-type header)]
    {:server-port        (-> req (.absoluteURI) (.getPort))
     :server-name        (-> req (.absoluteURI) (.getHost))
     :remote-addr        (-> req (.remoteAddress) (.getAddress) (.getHostAddress))
     :uri                (.path req)
     :query-string       (.query req)
     :scheme             (keyword (-> req (.absoluteURI) (.getScheme)))
     :request-method     (keyword (.toLowerCase (.method req)))
     :headers            header
     :content-type       content-type
     :content-length     (or (.length data) nil)
     :character-encoding (get-char-encoding content-type)
     :ssl-client-cert    (first (.peerCertificateChain req))
     :body               (ByteArrayInputStream. (buf/get-bytes data))}))

(defn- request-handler
  "Vertx Handler implementation for the given Ring handler."
  [handler req]
  (http/on-body req
                (fn [data]
                  (when-let [response-map
                             (handler (build-request-map req data))]
                    (update-response (http/server-response req) response-map)))))

(defn run-vertx-web
  "Start Vertx Web Servce with given ring-handler"
  ([http-server handler host port]
     (run-vertx-web http-server handler host port nil))

  ([http-server handler host port complete-handler]
     (doto http-server
       (http/on-request (partial request-handler handler))
       (http/listen port host complete-handler))
     http-server))
