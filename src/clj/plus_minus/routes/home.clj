(ns plus-minus.routes.home
  (:require
    [plus-minus.layout :as layout]
    [clojure.java.io :as io]
    [plus-minus.middleware :as middleware]
    [ring.util.http-response :as response]))

(defn home-page [request]
  (layout/render request "home.html"))

(defn privacy-policy [request]
  (layout/render request "privacy-policy.html"))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/app" {:get home-page}]
   ["/privacy-policy" {:get privacy-policy}]
   ["/docs" {:get (fn [_]
                    (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                        (response/header "Content-Type" "text/plain; charset=utf-8")))}]])

