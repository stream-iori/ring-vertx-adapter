(ns ring.adapter.utils
  "Adapter for the vertx webserver."
  (:import (java.io File InputStream OutputStream FileInputStream
                    ByteArrayInputStream ByteArrayOutputStream)
           (org.vertx.java.core Handler MultiMap)
           (org.vertx.java.core.buffer Buffer)
           (org.vertx.java.core.http HttpServerRequest HttpServerResponse))
  (:require [clojure.java.io :as io]
            [clojure.string :as string]))


(defn- get-headers
  "Creates a name/value map of all the request headers."
  [^HttpServerRequest req]
  (let [^MultiMap headers (.headers req)]
    (into {} (for [m (.entries headers)]
               {(key m) (string/split (val m) #",")}))))

(defn set-status
  "Update a HttpServerResponse with a status code."
  [^HttpServerResponse resp, status]
  (.setStatusCode resp status))

(defn set-headers
  "Update a HttpServerResponse with a map of headers."
  [^HttpServerResponse resp, headers]
  (doseq [[key val-or-vals] headers]
    (.putHeader resp key val-or-vals)))

(defn- set-body
  "Update a HttpServerResponse body with a String, ISeq, File or InputStream."
  [^HttpServerResponse resp, body]
  (cond
   (string? body)
   (.end resp body)
   (seq? body)
   (do
     (.setChunked true resp)
     (doseq [chunk body]
       (.end resp chunk)))
   (instance? InputStream body)
   (with-open [^InputStream in body
               ^OutputStream out (ByteArrayOutputStream.)]
     (io/copy in out)
     (.end resp (Buffer. (.toByteArray out))))
   (instance? File body)
   (let [^File f body]
     (with-open [stream (FileInputStream. f)]
       (set-body resp stream)))
   (nil? body)
   nil
   :else
   (throw (Exception. ^String (format "Unrecognized body: %s" body)))))

(defn- get-content-type [header]
  (let [ct (first (get header "Accept"))]
    (if (nil? ct) "text/plain" ct)))

(defn- get-char-encoding [header]
  (let [e (first (get header "Accept-Language"))]
    (if (nil? e) "UTF-8" e)))

(defn build-request-map
  "Return ring request with Vertx's Web parameter"
  [^HttpServerRequest req ^Buffer buf]
  (let [header (get-headers req)]
    {:server-port        (-> req (.absoluteURI) (.getPort))
     :server-name        (-> req (.absoluteURI) (.getHost))
     :remote-addr        (-> req (.remoteAddress) (.getHostName))
     :uri                (.path req)
     :query-string       (.query req)
     :scheme             (keyword (-> req (.absoluteURI) (.getScheme)))
     :request-method     (keyword (.toLowerCase (.method req)))
     :headers            header
     :content-type       (get-content-type header)
     :content-length     (.length buf)
     :character-encoding (get-char-encoding header)
     :ssl-client-cert    (first (.peerCertificateChain req))
     :body               (ByteArrayInputStream. (.getBytes buf))}))

(defn update-response
  "Update ring response to vertx response"
  [^HttpServerResponse resp, {:keys [status headers body]}]
  (when-not resp
    (throw (Exception. "Null response given.")))
  (when status
    (set-status resp status))
  (doto resp
    (set-headers headers)
    (set-body body)))
