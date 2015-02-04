(ns contrast.dev
  (:require [contrast.page-triggers :as page-triggers]
            [figwheel.client :as figwheel :include-macros true]
            [cljs.core.async :refer [put!]]
            [weasel.repl :as weasel]))

(enable-console-print!)

(figwheel/watch-and-reload
  :websocket-url "ws://localhost:3449/figwheel-ws"
  :jsload-callback page-triggers/render)

(weasel/connect "ws://localhost:9001" :verbose true :print #{:repl :console})

(page-triggers/render)
