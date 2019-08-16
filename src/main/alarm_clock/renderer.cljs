(ns alarm-clock.renderer
  (:require [reagent.core :as reagent :refer [atom]]
            [goog.date :as gdate :refer [DateTime]]
            [cljs.core.async :as async :refer [go go-loop <! >! chan timeout]]
            [clojure.string :as str]
            [re-com.core :as rc]
            [re-frame.core :as rf]
            [alarm-clock.subs :as s]
            [alarm-clock.events :as events]
;; (defonce the-time (atom nil))
            [thi.ng.geom.types :as types]
            [thi.ng.math.core :as math]
            [thi.ng.geom.core :as geom]
            [thi.ng.geom.vector :as vecs]
            [thi.ng.geom.svg.core :as svg]
            [thi.ng.geom.svg.adapter :as svg-adapter]))
(defn notification [title body]
  (let [n (js/Notification. title)]))

(go-loop [_ (<! (timeout 500))]
  (let [d (DateTime.)
        h (. d (getHours))
        m (. d (getMinutes))
        s (. d (getSeconds))]
    (rf/dispatch [::events/set-time
                  {:hours h
                   :minutes m
                   :seconds s}])
    (recur (<! (timeout 500)))))

(defn new-alarm-panel []
  (let [name (atom "")
        alarm-time (atom 0)]
    (fn []
      [rc/v-box
       :children
       [[rc/label :label "name"]
        [rc/input-text
         :model @name
         :on-change #(reset! name %)]
        [rc/gap :size "10px"]
        [rc/label :label "alarm time"]
        [rc/input-time
         :model @alarm-time
         :on-change #(reset! alarm-time %)]
        [rc/gap :size "1em"]
        [rc/button
         :label "Add Alarm"
         :on-click identity
         :class "btn-primary"]]])))

(defn- number-str [n]
  (str (if (< n 10) "0" "") n))

(defn time-component [time-atom]
  (when-let [{:keys [hours minutes seconds]} @time-atom]
    (let [time-str (str  (number-str hours) ":" (number-str minutes) ":" (number-str seconds))]
      [rc/title
       :class "time-component"
       :level :level1
       :label time-str])))

(defn clock [radius seconds minutes hours]
  (svg/svg {:viewBox [(* -1 radius 1.2) (* -1 radius 1.2) (* radius 1.2 2) (* radius 1.2 2)]
            :stroke "black"
            :stroke-width (/ radius 100)
            :font-size (/ radius 10)
            :width 600
            :height 600}
           ;; (concat

           (svg/as-svg (types/->Line2 [(vecs/vec2 0 0)
                                       (geom/rotate
                                        (vecs/vec2 (* radius 0.4) 0)
                                        (+
                                         (* (mod (+  @hours 9) 12)
                                            math/SIXTH_PI)
                                         (* (/ math/SIXTH_PI 5)
                                            @minutes
                                            (/ 1 60))))])
                       {:id "hours"
                        :key "hours"})
           (svg/as-svg (types/->Line2 [(vecs/vec2 0 0)
                                       (geom/rotate
                                        (vecs/vec2 (* radius 0.7) 0)
                                        (* (mod (+ @minutes 45) 60)
                                           (/ math/SIXTH_PI 5)))])
                       {:id "minutes"
                        :key "minutes"})
           
           (svg/as-svg (types/->Line2 [(vecs/vec2 0 0)
                                       (geom/rotate
                                        (vecs/vec2 (* radius 0.9) 0)
                                        (* (mod (+ @seconds 45) 60)
                                           (/ math/SIXTH_PI 5)))])
                       {:id "seconds"
                        :key "seconds"})
           (for [[i r] (map-indexed vector (range 0 math/TWO_PI math/SIXTH_PI))
                 :let [p1 (geom/rotate (vecs/vec2 (* radius 0.9) 0) r)
                       p2 (geom/rotate (vecs/vec2 radius 0) r)]]
               (svg/group
                 {:key (str "gr_" i)}
                 (svg/group
                  {:key (str "gr2_" i)}
                  (for [[j r2] (map-indexed vector (range r (+ r math/SIXTH_PI) (/ math/SIXTH_PI 5)))
                        :let [p3 (geom/rotate (vecs/vec2 (* radius 0.95) 0) r2)
                              p4 (geom/rotate (vecs/vec2 radius 0) r2)]]
                    (svg/as-svg (types/->Line2 [p3 p4])
                                {:key (str "gr_" i "_" j)})))
                 ^{:key (str "min_" i)} (svg/as-svg (types/->Line2 [p1 p2])
                                                    {:key (str "min_" i)})
                 (svg/text (vecs/vec2 (* (:x p2) 1.1) (* (:y p2) 1.1))
                           (mod (+ i 3) 12)
                           {:transform-origin "50% 50%"
                            :stroke-width 1
                            :transform "translateY(-50%)"
                            :stroke "red"
                            :key (str "sec_text_" i)})))))
                                       ;; :transform (str "rotate(" (/ r math/RAD) ")")}))))))
             ;; (for [r (range 0 math/TWO_PI math/SIXTH_PI)
                   ;; :let [p1 (geom/rotate (vecs/vec2 4 0) r)
                         ;; p2 (geom/rotate (vecs/vec2 6 0) r))
               ;; (svg/text p2 "texto")))))

(defn app-component []
  [rc/v-box
   :align :center
   :justify :around
   :class "app-component container"
   :style {:display :flex
           :background-color "rgba(255,255,255, 0.7)"}
   :children
   [[rc/h-box
     :align :center
     :justify :around
     :children [[time-component (rf/subscribe [::s/time])]]]
    #_[new-alarm-panel]
    (when-not (nil? (rf/subscribe [::s/time]))
      [clock 150
       (rf/subscribe [::s/seconds])
       (rf/subscribe [::s/minutes])
       (rf/subscribe [::s/hours])])]])

(defn mount-components! []
  (reagent/render
   app-component
   (. js/document (querySelector "#root"))))

(defn ^:export main []
  (println "hello rendererer")
  (mount-components!)
  (rf/dispatch-sync [::events/init-state]))
