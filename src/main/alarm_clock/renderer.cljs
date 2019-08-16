(ns alarm-clock.renderer
  (:require [reagent.core :as reagent :refer [atom]]
            [goog.date :as gdate :refer [DateTime]]
            [cljs.core.async :as async :refer [go go-loop <! >! chan timeout]]
            [clojure.string :as str]
            [re-com.core :as rc]
            [re-frame.core :as rf]
            [alarm-clock.subs :as s]
            [alarm-clock.events :as events]
            [thi.ng.geom.types :as types]
            [thi.ng.math.core :as math]
            [thi.ng.geom.core :as geom]
            [thi.ng.geom.vector :as vecs]
            [thi.ng.geom.svg.core :as svg]
            [thi.ng.geom.svg.adapter :as svg-adapter]))

(defn create-alarm
  ([name alarm-time active?]
   {:id (random-uuid)
    :name name
    :alarm-time alarm-time
    :active? active?})
  ([name alarm-time]
   (create-alarm name alarm-time false)))

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
  (let [name       (atom "")
        alarm-time (atom 0)
        active? (atom false)]
    (fn []
      [rc/v-box
       :children
       [[rc/label :label "name"]
        [rc/input-text
         :model @name
         :on-change #(reset! name %)]
        [rc/gap :size "1em"]
        [rc/label :label "alarm time"]
        [rc/input-time
         :model @alarm-time
         :show-icon? true
         :on-change #(reset! alarm-time %)]
        [rc/gap :size "1em"]
        [rc/checkbox :model @active? :on-change #(reset! active? %) :label "Activated?"]
        [rc/h-box
         :size "auto"
         :children
         [[rc/button
           :label "Add Alarm"
           :on-click #(do
                        (rf/dispatch [::events/add-alarm (create-alarm @name @alarm-time @active?)])
                        (rf/dispatch [::events/pop-modal]))
           :class "btn-primary"]
          [rc/button
           :label "cancel"
           :on-click #(rf/dispatch [::events/pop-modal])
           :class "btn-outline-warning"]]]]])))
(defn- number-str [n]
  (str (if (< n 10) "0" "") n))

(defn time-component [time-atom]
  (when-let [{:keys [hours minutes seconds]} @time-atom]
    (let [time-str (str  (number-str hours) ":" (number-str minutes) ":" (number-str seconds))]
      [rc/title
       :class "time-component"
       :level :level1
       :label time-str])))

(defn arm [length rot thickness]
  (types/->Triangle2
   (map (comp #(geom/rotate % rot) vecs/vec2)
        [[0 thickness]
         [0 (* -1 thickness)]
         [length 0]])))

 
(defn alarm-row [a over?]         
  [:tr {:on-mouse-over #(reset! over? true)
        :on-mouse-out #(reset! over? false)}
   [:td [rc/checkbox :model (:active? a) :on-change identity]]
   [:td (:name a)]
   [:td (let [t (:alarm-time a)
              h (int (/ t 100))
              m (mod t 100)]
          (str h ":" m))]
   [:td [rc/row-button :md-icon-name "zmdi-plus" :mouse-over-row? @over?]]])

(defn alarms []
  [:table.table
   [:thead
    [:tr
     [:th "Enabled"][:th "Name"] [:th "Time"] [:th "action"]]]
   [:tbody
    (doall
     (for [[i a] (map-indexed vector @(rf/subscribe [::s/alarms]))
           :let [over? (atom false)]]
       ^{:key (str "row_" i)} [alarm-row a over?]))]])

(defn clock [radius seconds minutes hours]
  (svg/svg {:viewBox      [(* -1 radius 1.2) (* -1 radius 1.2) (* radius 1.2 2) (* radius 1.2 2)]
            :stroke       "black"
            :stroke-width (/ radius 100)
            :font-size    (/ radius 10)
            :width        400
            :height       400}
           ;; (concat
           (svg/as-svg (types/->Circle2 [0 0] (* radius 1.0250))
                       {:fill "none"
                        :key  "circle"})
           (svg/as-svg
            (arm (* radius 0.4)
                 (+
                  (* (mod (+  @hours 9) 12)
                     math/SIXTH_PI)
                  (* (/ math/SIXTH_PI 5)
                     @minutes
                     (/ 1 60)))
                 (/ radius 50))
            {:id     "hours"
             :key    "hours"
             :stroke "grey"
             :fill   "darkgrey"})
           (svg/as-svg
            (arm (* 0.7 radius) (* (mod (+ @minutes 45) 60)
                                   (/ math/SIXTH_PI 5))
                 (/ radius 50))
            {:id     "minutes"
             :key    "minutes"
             :stroke "grey"
             :fill   "darkgrey"})

           (svg/as-svg
            (arm (* radius 0.9) (* (/ math/SIXTH_PI 5) (mod (+ @seconds 45) 60)) (/ radius 50))
            {:id     "seconds"
             :key    "seconds"
             :stroke "grey"
             :fill   "darkgrey"})
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
              (svg/text (vecs/vec2 (* (:x p2) 1.1) (* (:y p2) 1.1))
                        (mod (+ i 3) 12)
                        {:transform-origin "0.5 0.5"
                         :stroke-width    1
                         :transform       "translate(-2 4)"
                         :stroke          "red"
                         :key             (str "sec_text_" i)})))))
(defn modals []
  (when-not  (empty? @(rf/subscribe [::s/modals]))
    [rc/modal-panel
     :backdrop-on-click #(rf/dispatch [::events/pop-modal])
     :child (first @(rf/subscribe [::s/modals]))]))

(defn app-component []
  [rc/v-box
   :align :center
   :justify :around
   :class "app-component container"
   :style {:display          :flex
           :background-color "rgba(255,255,255, 0.7)"}
   :children
   [[modals]
    [rc/button
     :label "Create Alarm"
     :on-click #(rf/dispatch [::events/push-modal [new-alarm-panel]])
     :class "btn-outline-primary"]
    [rc/h-box
     :children
     [[rc/box
       :child [alarms]]
      [rc/gap :size "1em"]
      [rc/v-box
       :children
       [[time-component (rf/subscribe [::s/time])]        
        (when-not (nil? (rf/subscribe [::s/time]))
          [rc/box
           :child
           [:div.media
             [clock 150
              (rf/subscribe [::s/seconds])
              (rf/subscribe [::s/minutes])
              (rf/subscribe [::s/hours])]]])]]]]]])

(defn mount-components! []
  (reagent/render
   app-component
   (. js/document (querySelector "#root"))))

(defn ^:export main []
  (println "hello rendererer")
  (mount-components!)
  (rf/dispatch-sync [::events/init-state]))
