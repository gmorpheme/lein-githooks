(ns leiningen.githooks-test
  (:require [clojure.test :refer :all]
            [leiningen.githooks :as g]))

(deftest accepts-hooks-as-ours
  (is (re-find g/cookie g/hook-template)))
