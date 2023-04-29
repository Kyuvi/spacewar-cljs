(ns sutils.rf
  (:require
   [re-frame.core :as rf]
   [re-frame.db :as rfdb]
   [sutils.browser :as bru]
   ))

(defn prn-db
  ([] (bru/clog @rfdb/app-db))
  ([arg]
   (prn-db) arg))

(defn <sub
  "Alias for re-frame subscibing.
   @(re-frame.core/subscibe [:model]) -> (<sub [:model])."
  [vector]
  @(rf/subscribe vector))

(defn >evt
  "Alias for re-frame dispatch.
   #(re-frame.core/dispatch [:init]) -> (>evt [:init])."
  [vector]
  (fn [] (rf/dispatch vector)))

(defn basic-sub
  "Alias to register a simple getter subscription."
  ([kw]
   (rf/reg-sub kw (fn [db _] (kw db))))
  ([kw kpath]
   (rf/reg-sub kw (fn [db _] (get-in db kpath)))))
