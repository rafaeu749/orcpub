(ns orcpub.email
  (:require [hiccup.core :as hiccup]
            [postal.core :as postal]
            [environ.core :as environ]
            [clojure.pprint :as pprint]
            [orcpub.route-map :as routes]
            [cuerdas.core :as str]))

(defn verification-email-html [first-and-last-name username verification-url]
  [:div
   (str "Dear OrcPub Patron,")
   [:br]
   [:br]
   "Your OrcPub account is almost ready, we just need you to verify your email address going the following URL to confirm that you are authorized to use this email address:"
   [:br]
   [:br]
   [:a {:href verification-url} verification-url]
   [:br]
   [:br]
   "Sincerely,"
   [:br]
   [:br]
   "The OrcPub Team"])

(defn verification-email [first-and-last-name username verification-url]
  [{:type "text/html"
    :content (hiccup/html (verification-email-html first-and-last-name username verification-url))}])

(defn email-cfg []
  {:user (environ/env :email-access-key)
   :pass (environ/env :email-secret-key)
   :host (environ/env :email-server-url)
   :port (Integer/parseInt (or (environ/env :email-server-port) "587"))
   :ssl (or (str/to-bool (environ/env :email-ssl)) nil)
   :tls (or (str/to-bool (environ/env :email-tls)) nil)
   })

(defn send-verification-email [base-url {:keys [email username first-and-last-name]} verification-key]
  (postal/send-message (email-cfg)
                       {:from "OrcPub Team <no-reply@orcpub.com>"
                        :to email
                        :subject "OrcPub Email Verification"
                        :body (verification-email
                               first-and-last-name
                               username
                               (str base-url (routes/path-for routes/verify-route) "?key=" verification-key))}))

(defn reset-password-email-html [first-and-last-name reset-url]
  [:div
   (str "Dear OrcPub Patron")
   [:br]
   [:br]
   "We received a request to reset your password, to do so please go to the following URL to complete the reset."
   [:br]
   [:br]
   [:a {:href reset-url} reset-url]
   [:br]
   [:br]
   "If you did NOT request a reset, please do no click on the link."
   [:br]
   [:br]
   "Sincerely,"
   [:br]
   [:br]
   "The OrcPub Team"])

(defn reset-password-email [first-and-last-name reset-url]
  [{:type "text/html"
    :content (hiccup/html (reset-password-email-html first-and-last-name reset-url))}])

(defn send-reset-email [base-url {:keys [email username first-and-last-name]} reset-key]
  (postal/send-message (email-cfg)
                       {:from "OrcPub Team <no-reply@orcpub.com>"
                        :to email
                        :subject "OrcPub Password Reset"
                        :body (reset-password-email
                                first-and-last-name
                                (str base-url (routes/path-for routes/reset-password-page-route) "?key=" reset-key))}))

(defn send-error-email [context exception]
  (if (not-empty (environ/env :email-errors-to))
    (postal/send-message (email-cfg)
                         {:from (str "OrcPub Errors <" (environ/env :email-errors-to) ">")
                          :to (str (environ/env :email-errors-to))
                          :subject "Exception"
                          :body [{:type "text/plain"
                                  :content (let [writer (java.io.StringWriter.)]
                                             (do (clojure.pprint/pprint (:request context) writer)
                                                 (clojure.pprint/pprint (or (ex-data exception) exception) writer)
                                                 (str writer)))}]})))

