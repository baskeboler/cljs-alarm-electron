(ns alarm-clock.subs
  (:require [re-frame.core :as rf]
            [goog.date :as gdate :refer [DateTime]]))

(defn- current-time []
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
   (:seconds time)))

(rf/reg-sub
 ::minutes
 :<- [::time]
 (fn [time _]
   (:minutes time)))


(rf/reg-sub
 ::hours
 :<- [::time]
 (fn [time _]
   (:hours time)))

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
 (fn [alarms _]
   (let [sorted (sort-by :alarm-time alarms)
         filtered (filter #(> (:alarm-time %) (current-time)) sorted)]
     (first filtered))))
