(ns alarm-clock.core
  (:require ["electron"
             :as electron
             :refer [app Menu crashReporter BrowserWindow Tray Notification]]))

(defonce main-window (atom nil))
(defonce tray-icon (atom nil))

(defn- notify-launch []
  (let [n (Notification. (clj->js {:title "App Started" :body "The application has started successfully"}))]
    (set! (. n -onclick) (fn [] (println "the notification has been clicked")))
    n))

(defn init-browser! []
  (println "Starting up your clock!")
  (reset! main-window ^js (BrowserWindow.
                           (clj->js {:width 800
                                     :icon "resources/icons/256x256.png"
                                     :height 600
                                     :autoHideMenuBar true
                                     :webPreferences
                                     {:nodeIntegration true}})))
  ;; (. ^js Menu (setApplicationMenu nil))
  (.removeMenu ^js @main-window)
  (.loadURL ^js @main-window (str "file://" js/__dirname "/public/index.html"))
  (.on ^js @main-window "closed" #(reset! main-window nil))
  (notify-launch)
  (let [tray ^js (Tray. (str "resources/icons/32x32.png"))]
    (. ^js tray (setToolTip "Alarm app"))
    (.on ^js tray "click" (fn [evt bounds pos] (println "tray icon clicked!")))
    (reset! tray-icon tray)))
    

(defn ^:export main []
  (.start ^js crashReporter
          (clj->js
           {:companyName "ggsoft"
            :productName "alarm-clock"
            :submitURL "https://example.com/submit-endpoint"
            :autoSubmit false}))
  (.on ^js app "window-all-closed" #(when-not (= js/process.platform "darwin")
                                      (println "Exiting.")
                                      (.quit ^js app)))
  (.on ^js app "ready" init-browser!))


