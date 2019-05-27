(ns plus-minus.routes.services.auth
  (:require [plus-minus.db.core :as db]
            [plus-minus.validation :as validate]
            [plus-minus.utils :as utils]
            [ring.util.http-response :as response]
            [buddy.hashers :as hashers]
            [clojure.tools.logging :as log]
            [clojure.pprint :refer [pprint]]))

(defn- handle-reg-exc [e]
  (let [duplicate? (->> (utils/ex-chain e)
                        (map ex-message)
                        (filter #(.startsWith % "ERROR: duplicate key"))
                        seq)]
    (if duplicate?
      (response/precondition-failed
       {:result  :error
        :message "user with the selected ID already exists"})
      (do
        (log/error e)
        (response/internal-server-error
         {:result  :error
          :message "server error occured while adding the user"})))))

(defn register! [{session :session :as req} user]
  (pprint req)
  (if-let [errors (validate/registration-errors user)]
    (response/precondition-failed {:result  :error
                                   :message "precondition failed"
                                   :validation errors})
    (try
      (db/create-user! (-> user
                           (dissoc :pass-confirm)
                           (update :pass hashers/encrypt)))
      (-> {:result :ok}
          (response/ok)
          (assoc :session (assoc session :identity (:id user))))
      (catch Exception e (handle-reg-exc e)))))

#_(str "Basic " (.encodeToString (java.util.Base64/getEncoder) (.getBytes "user:pass")))

(defn decode-auth [encoded]
  (let [auth (second (.split encoded " "))]
    (-> (.decode (java.util.Base64/getDecoder) auth)
        (String. (java.nio.charset.Charset/forName "UTF-8"))
        (.split ":"))))

(defn- authenticate [[id pass]]
  (when-let [user (db/get-user {:id id})]
    (when (hashers/check pass (:pass user))
      id)))

(defn login! [{:keys [session] :as req} auth]
  (pprint req)
  (if-let [id (authenticate (decode-auth auth))]
    (-> {:result :ok}
        (response/ok)
        (assoc :session (assoc session :identity id)))
    (response/unauthorized {:result :unauthorized
                            :message "login failure"})))


(defn logout! [req]
  (pprint req)
  (-> {:result :ok}
      (response/ok)
      (assoc :session nil)))

(defn delete-account! [identity]
  (db/delete-user! {:id identity})
  (-> {:result :ok}
      (response/ok)
      (assoc :session nil)))
