(ns alarm-clock.svg
  (:require [alarm-clock.protocols :as p]
            [com.rpl.specter :as sp]
            [thi.ng.geom.svg.core :as svg]
            [thi.ng.geom.vector :as vecs]
            [thi.ng.geom.core :as geom]
            [instaparse.core :as insta :refer-macros [defparser]]
            [clojure.string :as str])
  (:require-macros [alarm-clock.macros :refer [svg-hiccup]]))

(defn element? [e]
  (and (vector? e) (<= 2 (count e))))

(defn children [e]
  (assert (element? e))
  (let [[_ _ & c] e]
    c))
(defn children? [e]
  (and (element? e)
       (not-empty (children e))))

(defn attrs [e]
  (assert (element? e))
  (second e))

(def ELEMENTS
  "element navigator"
  (sp/recursive-path [] p
                     (sp/cond-path
                      children? (sp/continue-then-stay [children sp/ALL p])
                      element? sp/STAY)))

(def other-clock (svg-hiccup "resources/public/images/clock4.svg"))
(def arms (svg-hiccup "resources/public/images/clock-arms.svg"))


(defn select-arm [name]
  #(= name (-> % attrs :id)))

(defn set-path-rotation [path rot origin]
  (let [[t attrs & ch] path
        new-attrs (-> attrs
                      (assoc :transform (str "rotate(" rot ")"))
                      (assoc "transform-origin" origin))]
    [t new-attrs ch]))


(defn- set-seconds [dtime]
  (fn [arm] (set-path-rotation arm (/ (p/arm-angle (p/->SecondsArm @dtime)) thi.ng.math.core/RAD) "128 128")))
(defn- set-hours [dtime]
  (fn [arm] (set-path-rotation arm (/ (p/arm-angle (p/->HoursArm @dtime)) thi.ng.math.core/RAD) "128 128")))
(defn- set-minutes [dtime]
  (fn [arm] (set-path-rotation arm (/ (p/arm-angle (p/->MinutesArm @dtime)) thi.ng.math.core/RAD) "128 128")))

(defn new-clock [dtime]
  (-> other-clock
      (conj (->> arms
                 (sp/transform [ELEMENTS (select-arm "hours")] (set-hours dtime))
                 (sp/transform [ELEMENTS (select-arm "minutes")] (set-minutes dtime))
                 (sp/transform [ELEMENTS (select-arm "seconds")] (set-seconds dtime))))))

(def int-or-double
  (insta/parser
   "ws = #'\\s*';
     Int = #'-?[0-9]+';
     coma = <ws> ',' <ws>;
     close = <ws> 'z'
     Double = #'-?[0-9]+\\.[0-9]*|\\.[0-9]+';
     <ConstExpr> = Int | Double;
     P = ConstExpr <coma> ConstExpr;
     move = 'm' <ws> P <ws>;
     Input = move (P <ws>)+ close"
   :start :Input))

(defmulti parse-input first)
(defmethod parse-input  :Input [args]
  {:result (mapv parse-input (rest args))})

(defmethod parse-input :move [args]
  (let [[_ _ p] args]
    (parse-input p)))

(defmethod parse-input :P [args]
  (let [[_ p1 p2] args]
    (vecs/vec2 (parse-input p1) (parse-input p2))))

(defmethod parse-input :Int [args]
  (let [[_ intstr] args]
    (js/Number.parseInt intstr)))

(defmethod parse-input :Double [args]
  (let [[_ dstr] args]
    (js/Number.parseFloat dstr)))

(defmethod parse-input :close [args]
  :end)
