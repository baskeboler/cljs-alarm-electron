(ns alarm-clock.macros
  (:require [pl.danieljanus.tagsoup :as ts]
            [clojure.string :as str]
            [com.rpl.specter :as sp]))
(defn parse-style-rule [rule]
  (let [[n v] (str/split rule #":")]
    {(keyword n) v}))

(defn parse-style-string [styles]
  (if (and
       styles
       (count (str/trim styles)))
    (let [pairs (str/split styles #";")]
      (->>
       (mapv (comp parse-style-rule str/trim) pairs)
       (apply merge)))
    nil))

(defn fix-attrs [attrs]
  ;; (println attrs)
  (update attrs :style parse-style-string))

(defn fix-styles [elems]
  (when elems

    (loop [[t attrs children] (first elems)
           rest (rest elems)
           result []]
      (println (str [t attrs]))
      (let [fixed [t (fix-attrs attrs)]])
      [t (fix-attrs attrs) (when children
                             (mapv fix-styles children))])))
(def TREE-VALUES
  (sp/recursive-path [] p
                     (sp/if-path vector?
                                 [sp/ALL p]
                                 sp/STAY)))
(defmacro svg-hiccup [fname]
  (let [f (slurp fname)]
    (->> (ts/parse-string f)
         (sp/transform [TREE-VALUES map? #(not= nil (:style %))] fix-attrs))))
;; fix-styles)))

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
