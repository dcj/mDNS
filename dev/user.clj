(ns user
  (:require
   [clojure.set :as set]
   [mdns.client]))


(defn format-span-mdns-event
  [m]
  (-> m
      (select-keys [:name :ipv4 :ipv6 :sn])
      (set/rename-keys {:sn :id :name :mdns-name})))


(defn update-item
  [items {:keys [mdns-name] :as item}]
  (update-in items [mdns-name] merge item))


(defmulti process-span-mdns
  (fn
    [_ event-type _]
    event-type))


(defmethod process-span-mdns :added
  [store _ parsed-event]
  (println "===============\nADDED:")  
  (let [event (format-span-mdns-event parsed-event)]
    (swap! store update-item event)))


(defmethod process-span-mdns :resolved
  [store _ parsed-event]
  (println "===============\nRESOLVED:")  
  (let [event (format-span-mdns-event parsed-event)]
    (swap! store update-item event)))


(defmethod process-span-mdns :removed
  [store _ parsed-event]
  (println "n===============\nREMOVED:"))


(comment

  (def mdns-panels (atom (hash-map)))  

  (mdns.client/register-service "_http._tcp.local." pprint)

  (mdns.client/register-service "_span._tcp.local." (partial process-span-mdns mdns-panels))

  (def my-ipaddr (byte-array [(byte 192) (byte 168) (byte 128) (byte 228)]))

  )



