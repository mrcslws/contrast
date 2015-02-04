(ns contrast.page-triggers
  (:require [cljs.core.async :refer [put! chan mult]]))

(defonce ^:private renders-in (chan))
(defonce renders (mult renders-in))

(defn render []
  (put! renders-in :render))
