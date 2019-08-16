(ns alarm-clock.events
  (:require [re-frame.core :as rf]
            [day8.re-frame.tracing]))
(def initial-state
  {:time   {:hours   0
            :minutes 0
            :seconds 0}
   :ready? false
   :modals []
   :alarms []})

(rf/reg-event-db
 ::init-state
 (fn [_ _]
   (-> initial-state
       (assoc :ready? true))))

(defn reg-attr-evt [evt path]
  (rf/reg-event-db
   evt
   (fn [db [_ value]]
     (cond
       (vector? path)
       (-> db
           (assoc-in path value))
       (keyword? path)
       (-> db
           (assoc path value))))))

(reg-attr-evt ::set-time :time)
(reg-attr-evt ::set-ready? :ready?)

(rf/reg-event-db
 ::push-modal
 (fn [db [_ modal]]
   (-> db
       (update :modals conj modal))))

(rf/reg-event-db
 ::pop-modal
 (fn [db _]
   (-> db
       (update :modals pop))))

(rf/reg-event-db
 ::add-alarm
 (fn [db [_ alarm]]
   (-> db
       (update :alarms conj alarm))))
