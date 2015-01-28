(ns ^{:doc "Hooks for auto-installation"}
  lein-githooks.plugin
  (:require [robert.hooke :as hk]
            [leiningen.githooks :as gh]
            [leiningen.core.main :refer (info abort)]))

(defn hooks
  "Add leiningen hooks to allow the githooks to be installed when
  leiningen is run."
  []
  (hk/add-hook #'leiningen.core.main/resolve-and-apply #'gh/auto-install))

