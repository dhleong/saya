(ns saya.modules.layout.core-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [clojure.zip :as zip]
            [saya.modules.layout.core :as layout]
            [saya.modules.layout.components :as components]))

(def ^:private content (atom ["for the honor"]))

(def ^:private sample-layout
  [:vertical
   [:horizontal {:height 3}
    [:edit {:content ["of grayskull"]}]]
   [:horizontal
    [:edit {:file "grayskull.json"
            :focus :initial}]
    [:edit {:content content}]]])

(def ^:private complicated-layout
  [:horizontal
   [:vertical
    [:horizontal {:height 3}
     [:edit {:content ["of grayskull"]}]]
    [:horizontal
     [:edit {:file "grayskull.json"
             :focus :initial}]]]
   [:vertical
    [:edit {:file "honor.json"}]]])

(defn- evaluated-layout
  ([] (evaluated-layout sample-layout))
  ([static-layout]
   (layout/evaluate
    {:layout/id 0
     :layout/component (constantly static-layout)
     :layout/state-atom (atom nil)})))

(deftest navigation-test
  (testing "Find zipper leaf"
    (let [layout (evaluated-layout)
          grayskull-key [0 :vertical 1 :horizontal 0 {:file "grayskull.json"}]
          zip (layout/zipper-at-key
               layout
               grayskull-key)]
      (is (= grayskull-key
             (layout/zipper-key zip)))))

  (testing "Find content node to right of grayskull"
    (let [layout (evaluated-layout)
          grayskull-key [0 :vertical 1 :horizontal 0 {:file "grayskull.json"}]
          zip (layout/zipper-at-key
               layout
               grayskull-key)]
      (is (= [0 :vertical 1 :horizontal 1 :ref]
             (-> zip
                 (layout/navigate-right)
                 (layout/zipper-key))))))

  (testing "Find content node to right in grandparent :horizontal"
    (let [layout (evaluated-layout
                  complicated-layout)
          grayskull-key [0 :horizontal 0 :vertical 1 :horizontal 0 {:file "grayskull.json"}]
          zip (layout/zipper-at-key
               layout
               grayskull-key)]
      (is (= [0 :horizontal 1 :vertical 0 {:file "honor.json"}]
             (-> zip
                 (layout/navigate-right)
                 (layout/zipper-key))))))

  (testing "Gracefully handle no navigation destination"
    (let [layout (evaluated-layout)
          grayskull-key [0 :vertical 1 :horizontal 0 {:file "grayskull.json"}]
          zip (layout/zipper-at-key
               layout
               grayskull-key)]
      (is (nil?
           (-> zip
               (layout/navigate-left)))))))

(deftest find-sibling-test
  (testing "Find sibling in shared parent"
    (let [layout (evaluated-layout)
          grayskull-key [0 :vertical 1 :horizontal 0 {:file "grayskull.json"}]
          zip (layout/zipper-at-key
               layout
               grayskull-key)]
      (is (= [0 :vertical 1 :horizontal 1 :ref]
             (-> zip
                 (layout/find-sibling-in-ancestors :horizontal zip/right)
                 (layout/zipper-key))))))

  (testing "Find sibling in grandparent"
    (let [layout (evaluated-layout complicated-layout)
          grayskull-key [0 :horizontal 0 :vertical 1 :horizontal 0 {:file "grayskull.json"}]
          zip (layout/zipper-at-key
               layout
               grayskull-key)]
      (is (= [components/vertical
              nil
              [components/edit-file-view
               {:key [0 :horizontal 1 :vertical 0
                      {:file "honor.json"}]}
               "honor.json"]]
             (-> zip
                 (layout/find-sibling-in-ancestors :horizontal zip/right)
                 (zip/node)))))))
