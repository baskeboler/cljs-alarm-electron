(ns alarm-clock.core
  (:require ["electron"
             :as electron
             :refer [app Menu MenuItem crashReporter globalShortcut BrowserWindow Tray Notification]]))

(defonce main-window (atom nil))
(defonce tray-icon (atom nil))


(defn display-os-notification [title body]
  (let [n ^js (Notification. (clj->js
                              {:title title
                               :body body}))]
    (set! (. n -onclick) (fn []
                           (println "notification clicked")
                           (. n (close))))
    (.show n)
    n))
                          
(defn- notify-launch []
  (display-os-notification "App Started" "The app has started"))

(defn toggle-hide-window []
  (if (.isVisible @main-window)
    (.hide ^js @main-window)
    (.show ^js @main-window)))

(defn build-tray-menu []
  (println "building tray menu")
  (let [menu ^js (Menu.)]
    (. menu (append ^js (MenuItem. (clj->js {:label "Hide/Show"
                                             :click (fn []
                                                      (println "hide clicked")
                                                      (toggle-hide-window))}))))
    (. menu (append ^js (MenuItem. (clj->js {:label "Exit" :click (fn []
                                                                    (println "Exit called from tray")
                                                                    (.exit app))}))))
    menu))

(defn init-browser! []
  (println "Starting up your clock!")
  (reset! main-window ^js (BrowserWindow.
                           (clj->js {:width 800
                                     :icon "resources/icons/256x256.png"
                                     :height 600
                                     :type :splash
                                     :frame false
                                     :transparent true
                                     :autoHideMenuBar true
                                     :webPreferences
                                     {:nodeIntegration true}})))
  ;; (. ^js Menu (setApplicationMenu nil))
  (.removeMenu ^js @main-window)
  (. globalShortcut (register "CommandOrControl+A" toggle-hide-window))
  (.loadURL ^js @main-window (str "file://" js/__dirname "/public/index.html"))
  (.on ^js @main-window "closed" #(reset! main-window nil))
  (let [tray ^js (Tray. (str "resources/icons/32x32.png"))]
    (. ^js tray (setToolTip "Alarm app"))
    (. ^js tray (setContextMenu (build-tray-menu)))
    (.on ^js tray "click" (fn [evt bounds pos]
                            (println "tray icon clicked!")))
    (reset! tray-icon tray))
  (.on ^js @main-window "ready" #(notify-launch)))
    

(defn ^:export main []
  (.start ^js crashReporter
          (clj->js
           {:companyName "ggsoft"
            :productName "alarm-clock"
            :submitURL "https://example.com/submit-endpoint"
            :autoSubmit false}))
  (set! (. app -applicationMenu) nil)
  (.on ^js app "window-all-closed" #(when-not (= js/process.platform "darwin")
                                      (println "Exiting.")
                                      (.quit ^js app)))
  (.on ^js app "ready" init-browser!))


