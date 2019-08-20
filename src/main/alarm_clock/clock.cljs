(ns alarm-clock.clock
  (:require [goog.date :as gdate :refer [DateTime]]
            [thi.ng.geom.types :as types]
            [thi.ng.geom.core :as geom]
            [thi.ng.math.core :as math]
            [thi.ng.geom.vector :as vecs]
            [thi.ng.geom.svg.core :as svg]
            [alarm-clock.protocols :as p]))

(defn arm
  "Draw a clock arm"
  [length rot thickness]
  (types/->Polygon2
   (map (comp #(geom/rotate % rot) vecs/vec2)
        [[0 thickness]
         [(- (* 2 thickness)) 0]
         [0 (- thickness)]
         [(* 0.95 length) (/ thickness -5)]
         [length 0]
         [(* 0.95 length) (/  thickness 5)]])))

(defn clock [radius gtime]
  (let [arms (p/create-arms @gtime)]
   (svg/svg {:viewBox      [(* -1 radius 1.2) (* -1 radius 1.2) (* radius 1.2 2) (* radius 1.2 2)]
             :stroke       "black"
             :stroke-width (/ radius 100)
             :font-size    (/ radius 10)
             :width        400
             :height       400}
            (svg/as-svg (types/->Circle2 [0 0] (* radius 1.185))
                        {:fill "rgb(255 128 128)"
                         :fill-opacity 0.3
                         :stroke-width 1
                         :stroke "black"
                         :key  "outer-circle"})
            (svg/as-svg (types/->Circle2 [0 0] (* radius 1.0250))
                        {:fill "rgb(255 128 128)"
                         :opacity "0.6"
                         :key  "circle"})
            (svg/as-svg
             (arm (* radius 0.4)
                  (p/arm-angle (:hours arms) (- math/HALF_PI))
                  (/ radius 20))
             {:id     "hours"
              :key    "hours"
              :stroke "white"
              :stroke-width 1
              :fill   "darkgrey"})
            (svg/as-svg
             (arm (* 0.7 radius)
                  (p/arm-angle (:minutes arms) (- math/HALF_PI))
                  (/ radius 20))
             {:id     "minutes"
              :key    "minutes"
              :stroke "white"
              :stroke-width 1
              :fill   "darkgrey"})

            (svg/as-svg
             (arm (* radius 0.9)
                  (p/arm-angle (:seconds arms) (- math/HALF_PI))
                  (/ radius 20))
             {:id     "seconds"
              :key    "seconds"
              :stroke "white"
              :stroke-width 1
              :fill   "darkgrey"})
            (svg/as-svg (types/->Circle2 [0 0] (/ radius 15))
                        {:id "clock-center"
                         :key "clock-center"
                         :stroke-width 1
                         :stroke "white"
                         :fill "rgb(255 128 128)"
                         :fill-opacity "0.6"})
            (for [[i r] (map-indexed vector (range 0 math/TWO_PI math/SIXTH_PI))
                  :let  [p1 (geom/rotate (vecs/vec2 (* radius 0.9) 0) r)
                         p2 (geom/rotate (vecs/vec2 radius 0) r)]]
              (svg/group
               {:key (str "gr_" i)}
               (svg/group
                {:key (str "gr2_" i)}
                (for [[j r2] (map-indexed vector (range r (+ r math/SIXTH_PI) (/ math/SIXTH_PI 5)))
                      :let   [p3 (geom/rotate (vecs/vec2 (* radius 0.95) 0) r2)
                              p4 (geom/rotate (vecs/vec2 radius 0) r2)]]
                  (svg/as-svg (types/->Line2 [p3 p4])
                              {:key (str "gr_" i "_" j)})))
               ^{:key (str "min_" i)} (svg/as-svg (types/->Line2 [p1 p2])
                                                  {:key (str "min_" i)})
               (svg/text (vecs/vec2 (* (:x p2) 1.11) (* (:y p2) 1.11))
                         (mod (+ i 3) 12)
                         {:transformorigin "0.5 0.5"
                          ;; :font-weight 100
                          :font-size "1.2em"
                          :style {:text-shadow "-3px 3px 1px white"}
                          :stroke-width    0.5
                          :transform       "translate(-6 4)"
                          :stroke          "none"
                          :fill "red"
                          :key             (str "sec_text_" i)}))))))
