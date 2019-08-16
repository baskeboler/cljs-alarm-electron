(ns alarm-clock.core
  (:require ["electron"
             :as electron
             :refer [app crashReporter BrowserWindow Tray]]))

(defonce main-window (atom nil))

(defn init-browser! []
  (println "Starting up your clock!")
  (reset! main-window ^js (BrowserWindow.
                           (clj->js {:width 800
                                     ;; :icon "resources/public/images/icon.ico"
                                     :height 600
                                     :webPreferences
                                     {:nodeIntegration true
                                      :autoHideMenu true}})))
  (.loadURL ^js @main-window (str "file://" js/__dirname "/public/index.html"))
  (.on ^js @main-window "closed" #(reset! main-window nil))
  (let [tray (Tray. (str "file://" js/__dirname "/public/images/icon.ico"))]
    (. tray (setTooltip "Alarm app"))))
    

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


