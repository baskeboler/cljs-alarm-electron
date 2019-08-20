(ns alarm-clock.renderer
  (:require [reagent.core :as reagent :refer [atom]]
            [goog.date :as gdate :refer [DateTime]]
            [cljs.core.async :as async :refer [go go-loop <! >! chan timeout]]
            [clojure.string :as str]
            [re-com.core :as rc]
            [re-frame.core :as rf]
            [alarm-clock.subs :as s]
            [alarm-clock.events :as events]
            [alarm-clock.clock :as cl :refer [clock]]
            [alarm-clock.timer]
            [alarm-clock.protocols :as p]
            [alarm-clock.svg :as mysvg]
            [thi.ng.geom.types :as types]
            [thi.ng.math.core :as math]
            [thi.ng.geom.core :as geom]
            [thi.ng.geom.vector :as vecs]
            [thi.ng.geom.svg.core :as svg]
            [thi.ng.geom.svg.adapter :as svg-adapter]
            [goog.dom :as gdom]
            [com.rpl.specter :as sp])
            ;; [alarm-clock.macros :as m])
 (:require-macros
   [alarm-clock.macros :refer [svg-hiccup ELEMENTS]]))


(defn create-alarm
  ([name alarm-time active?]
   {:id (random-uuid)
    :name name
    :alarm-time alarm-time
    :active? active?})
  ([name alarm-time]
   (create-alarm name alarm-time false)))

(defn other-clock []
  (svg-hiccup "resources/public/images/clock4.svg"))

(defn play-alarm []
  (let [a (. js/document (querySelector "#alarm-audio"))]
    (. a (play))))

(defn notification [title body]
  (let [n (js/Notification. title)]))



(defn next-alarm-panel []
  [rc/v-box
   :children
   [[rc/title
     :level :level2
     :label "Next Alarm"]
    (when-not (nil? @(rf/subscribe [::s/next-alarm]))
      [rc/h-box
       :gap "3px"
       :children
       [[rc/box
         :size "auto"
         :child [rc/label :label (:name @(rf/subscribe [::s/next-alarm]))]]
        [rc/box
         :size "auto"
         :child [rc/line :size "3px" :color "red"]]
        [rc/box
         :size "auto"
         :child [rc/label :label (:alarm-time @(rf/subscribe [::s/next-alarm]))]]]])
    (when (nil? @(rf/subscribe [::s/next-alarm]))
      [rc/box
       :size "auto"
       :child [rc/title
               :level :level3
               :label "No pending alarms"]])]])

(defn alarm-triggered-panel []
  [rc/v-box
   :gap "5px"
   :children [[rc/box
               :size "auto"
               :child [rc/title
                       :label "Alarm Triggered!"
                       :level :level1]]
              [rc/box
               :size "auto"
               :height "200px"
               :max-height "200px"
               :child [:div {:style {:width "100%"
                                     :height "100%"
                                     :background "url(images/alarm.png) no-repeat"
                                     :background-size :contain
                                     :background-position :center}
                             :height "100%"}]]
              [rc/button
               :label "Dismiss"
               :class "btn-primary btn-block"
               :on-click #(rf/dispatch [::events/stop-triggered-alarm])]]])

(defn alarm-modal []
  (when @(rf/subscribe [::s/alarm-triggered?])
    [rc/modal-panel
      :child [alarm-triggered-panel]]))

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
    (let [time-str (.toIsoTimeString ^js @time-atom)]
      [rc/title
       :class "time-component"
       :level :level1
       :label time-str])))


(defn alarm-row [a over?]
  [:tr {:on-mouse-over #(reset! over? true)
        :on-mouse-out  #(reset! over? false)}
   [:td [rc/checkbox :model (:active? a) :on-change #(rf/dispatch [::events/toggle-activated-alarm (:id a)])]]
   [:td (:name a)]
   [:td (let [t (:alarm-time a)
              h (int (/ t 100))
              m (mod t 100)]
          (str h ":" m))]
   [:td [rc/row-button :md-icon-name "zmdi-delete" :mouse-over-row? @over?]]])

(defn alarms []
  [:table.table
   [:thead
    [:tr
     [:th "Enabled"] [:th "Name"] [:th "Time"] [:th "action"]]]
   [:tbody
    (doall
     (for [[i a] (map-indexed vector @(rf/subscribe [::s/alarms]))
           :let [over? (atom false)]]
       ^{:key (str "row_" i)} [alarm-row a over?]))]])


(defn modals []
  (when-not  (empty? @(rf/subscribe [::s/modals]))
    [rc/modal-panel
     :backdrop-on-click #(rf/dispatch [::events/pop-modal])
     :child (first @(rf/subscribe [::s/modals]))]))

(defn app-component []
  [rc/v-box
   :align :center
   :justify :between
   :class "app-component container mt-4"
   :style {:display          :flex
           :background-color "rgba(255,255,255, 0.8)"}
   :children
   [[modals]
    [alarm-modal]
    [rc/h-box
     :gap "2em"
     :align :stretch
     :justify :between
     :children
     [[rc/v-box
       :size "auto"
       :children [[rc/button
                   :label "Create Alarm"
                   :on-click #(rf/dispatch [::events/push-modal [new-alarm-panel]])
                   :class "btn-outline-primary btn-block my-5"]
                  [alarms]
                  [next-alarm-panel]]]
      [rc/box
       :size "auto"
       :child [rc/line :size "3px" :color "red"]]
      [rc/v-box
       :size "auto"
       :children
       [[rc/box
         :size "auto"
         :justify :center
         :align :center
         :child [time-component (rf/subscribe [::s/time])]]
        [mysvg/new-clock  (rf/subscribe [::s/time])]
        #_(when-not (nil? (rf/subscribe [::s/time]))
           [rc/box
            :size "auto"
            :child
            [:div.media
             [clock 150
              (rf/subscribe [::s/time])]]])]]]]]])

(defn mount-components! []
  (reagent/render
   app-component
   (. js/document (querySelector "#root"))))

(defn ^:export main []
  (println "hello rendererer")
  (mount-components!)
  (rf/dispatch-sync [::events/init-state]))
