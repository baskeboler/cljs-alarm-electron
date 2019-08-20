(ns alarm-clock.protocols
  (:require [goog.date :as gdate :refer [DateTime]]
            [thi.ng.math.core :as math]))

(defprotocol PDateTime
  (get-seconds [this])
  (get-minutes [this])
  (get-hours [this])
  (get-millis [this]))

(extend-protocol PDateTime
  nil
  (get-seconds [this] 0)
  (get-minutes [this] 0)
  (get-hours [this] 0)
  (get-millis [this] 0)
  DateTime
  (get-seconds [this] (.getSeconds this))
  (get-minutes [this] (.getMinutes this))
  (get-hours [this] (.getHours this))
  (get-millis [this] (.getMilliseconds this)))

(defprotocol ArmAngle
  (arm-angle [this] [this angle-offset]))

(defn with-offset [f offset]
  (fn [a]
    (+ offset (f a))))
(defn with-mod [f value]
  (fn [a]
    (mod (f a) value)))
(defn with-offset-modded [f offset mod-val]
  (with-mod (with-offset f offset) mod-val))

(deftype SecondsArm [datetime]
  ArmAngle
  (arm-angle [this]
    (let [s (+ (/ (get-millis datetime) 1000) (get-seconds datetime))
          q (/ s 60)]
      (* math/TWO_PI q)))
  (arm-angle [this offset]
    ((with-offset-modded arm-angle offset math/TWO_PI) this)))

(deftype MinutesArm [datetime]
  ArmAngle
  (arm-angle [this]
    (let [m (+ (/ (get-seconds datetime) 60) (get-minutes datetime))
          q (/ m 60)]
      (* math/TWO_PI q)))
  (arm-angle [this offset]
    ((with-offset-modded arm-angle offset math/TWO_PI) this)))

(deftype HoursArm [datetime]
  ArmAngle
  (arm-angle [this]
    (let [h (+ (/ (get-minutes datetime) 60) (get-hours datetime))
          q (/ (mod h 12) 12)]
      (* math/TWO_PI q)))
  (arm-angle [this offset]
    ((with-offset-modded arm-angle offset math/TWO_PI) this)))

(defn create-arms [gtime]
  {:hours (HoursArm. gtime)
   :minutes (MinutesArm. gtime)
   :seconds (SecondsArm. gtime)})
