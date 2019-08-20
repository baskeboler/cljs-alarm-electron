(ns alarm-clock.timer
  (:require [cljs.core.async :as async :refer [go-loop <! >! timeout chan]]
            [re-frame.core :as rf]
            [goog.date :as gdate :refer [DateTime]]
            [alarm-clock.events :as events]
            [alarm-clock.subs :as s]))



(defn should-trigger-alarm? [hours minutes seconds]
  (let [next-trigger-time (:alarm-time @(rf/subscribe [::s/next-alarm]))
        alarm-h (int (/ next-trigger-time 100))
        alarm-m (mod next-trigger-time 100)]
    (= [alarm-h alarm-m 0] [hours minutes seconds])))

(go-loop [_ (<! (timeout 500))]
  (let [d (DateTime.)
        h (. d (getHours))
        m (. d (getMinutes))
        s (. d (getSeconds))]
    (when (should-trigger-alarm? h m s)
      (println "triggering alarm!!")
      (rf/dispatch-sync [::events/trigger-alarm]))
    (rf/dispatch-sync [::events/set-time d])
    (recur (<! (timeout 200)))))
