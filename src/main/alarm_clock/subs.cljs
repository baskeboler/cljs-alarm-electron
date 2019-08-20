(ns alarm-clock.subs
  (:require [re-frame.core :as rf]
            [goog.date :as gdate :refer [DateTime]]
            [alarm-clock.protocols :as p]))
(defn current-time []
  (let [dt (DateTime.)]
    (+ (* 100 (. dt (getHours))) (. dt (getMinutes)))))

(rf/reg-sub
 ::time
 (fn [db _]
   (:time db)))

(rf/reg-sub
 ::ready?
 (fn [db _]
   (:ready? db)))

(rf/reg-sub
 ::seconds
 :<- [::time]
 (fn [time _]
   (p/get-seconds time)))

(rf/reg-sub
 ::minutes
 :<- [::time]
 (fn [time _]
   (p/get-minutes time)))


(rf/reg-sub
 ::hours
 :<- [::time]
 (fn [time _]
   (p/get-hours time)))

(rf/reg-sub
 ::modals
 (fn [db _]
   (:modals db)))

(rf/reg-sub
 ::alarms
 (fn [db _]
   (:alarms db)))

(rf/reg-sub
 ::enabled-alarms
 :<- [::alarms]
 (fn [alarms _]
   (filter #(:active? %) alarms)))

(rf/reg-sub
 ::next-alarm
 :<- [::enabled-alarms]
 :<- [::hours]
 :<- [::minutes]
 :<- [::seconds]
 (fn [[alarms hours minutes seconds] _]
   (let [current-time (+ (* hours 100) minutes)
         sorted (sort-by :alarm-time alarms)
         filtered (filterv #(> (:alarm-time %) current-time) sorted)]
     (if-not (empty? filtered)
       (first filtered)
       (first sorted)))))

(rf/reg-sub
 ::alarm-triggered?
 (fn [db _] (:alarm-triggered? db)))
