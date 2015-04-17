(set-env!
 :source-paths   #{"cljs"}
 :resource-paths #{"html" "js"}
 :dependencies '[[adzerk/boot-cljs "0.0-2814-4" :scope "test"]
                 [adzerk/boot-cljs-repl "0.1.9" :scope "test"]
                 [adzerk/boot-reload "0.2.4" :scope "test"]
                 [pandeiro/boot-http "0.3.0" :scope "test"]

                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.omcljs/om "0.8.8" :exclusions [cljsjs/react]]
                 [cljsjs/react "0.12.2-8"]])

(require
 '[adzerk.boot-cljs :refer [cljs]]
 '[adzerk.boot-cljs-repl :refer [cljs-repl start-repl]]
 '[adzerk.boot-reload :refer [reload]]
 '[pandeiro.http :refer [serve]])

(deftask contrast-reload
  []
  (reload :on-jsload 'contrast.page-triggers/reload-code))

;; =============================================================================
;; Example commands. They'll put a file-copy-deployable website in /target.
;; =============================================================================

;; boot serve -d target/ watch speak contrast-reload cljs-repl cljs -sO :none
(deftask dev
  []
  (comp (serve "-d" "target/")
        (watch)
        ;; (speak)
        (contrast-reload)
        (cljs-repl)
        (cljs :optimizations :none :source-map true)))

;; boot serve -d target/ watch speak cljs -sO :advanced
(deftask production
  []
  (comp (serve "-d" "target/")
        (watch)
        (speak)
        (cljs :optimizations :advanced :source-map true)))
