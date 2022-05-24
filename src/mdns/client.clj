(ns mdns.client
  (:require
   [clojure.string :as string]
   [medley.core :refer [assoc-some]])
  (:import java.net.Socket
           java.lang.StringBuilder
           [java.io File BufferedReader BufferedWriter IOException]
           (javax.jmdns JmDNS ServiceListener)
           (java.net URI InetAddress NetworkInterface Inet4Address)
           (org.apache.commons.lang3 StringEscapeUtils)))


(defn ^:private split-on-java-literals
  [s]
  (map #(StringEscapeUtils/unescapeJava %)
       (clojure.string/split s #"\\031")))


(defn ^:private split-nice-text-string
  [s]
  (mapcat split-on-java-literals (clojure.string/split s #"\\020")))


(defn ^:private ip->str
  [ip]
  (let [ip-str (str ip)]
    (if (string/blank? ip-str)
      ip-str
      (subs ip-str 1))))


(defn ^:private nice-text-string->hash-map
  [s]
  (if (string? s)
    (try
      (->> s
           split-nice-text-string
           (map clojure.string/trim)
           (map #(clojure.string/split % #"="))
           (into {})
           clojure.walk/keywordize-keys)
      (catch Exception e
        (hash-map))) ;; return empty hashmap on exception
    (hash-map))) ;; return empty hashmap if input is not a string
      

(defn ^:private parse-event
  [event]
  (let [{:keys [name inet6Address inet4Address niceTextString] :as event-bean} (bean (.getInfo event))]
    (merge
     (assoc-some event-bean
                 :name name
                 :ipv6 (ip->str inet6Address)
                 :ipv4 (ip->str inet4Address))
     (nice-text-string->hash-map niceTextString))))


(defn register-service
  "callback - fb of 2 args, first is keyword (:added, :removed, :resolved), second is parsed event"
  ([service-type callback]
   (register-service (InetAddress/getLocalHost) service-type callback))
  ([binding-inet-addr service-type callback]
   (let [listener
         (reify
           ServiceListener

           (serviceAdded [this added-event]
             (callback :added (parse-event added-event)))

           (serviceRemoved [this removed-event]
             (callback :removed (parse-event removed-event)))

           (serviceResolved [this resolved-event]
             (callback :resolved (parse-event resolved-event))))]
     (.addServiceListener
      (JmDNS/create  binding-inet-addr)
      service-type
      listener))))
