(ns alarm-clock.subs
  (:require [re-frame.core :as rf]))


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
