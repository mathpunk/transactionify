(ns reference-client.user-client
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [aero.core :as aero]
            [clojure.java.io :as io]))

(def config
  (aero/read-config (io/resource "config.edn")))

(def base-url (get-in config [:api :base-url]))
(def api-key (get-in config [:api :api-key]))

(defn- auth-header []
  {"Authorization" (str "APIKey " api-key)})

(defn check-auth
  "Checks if the configured API key is valid without creating any resources.
  Returns true if authenticated, false otherwise.

  Implementation: Makes a GET request to balance endpoint with a fake account ID.
  - 403 Forbidden = invalid credentials
  - 404 Not Found = valid credentials (account doesn't exist, but auth worked)"
  []
  (let [fake-account-id "00000000-0000-0000-0000-000000000000"
        response (http/get (str base-url "/api/v1/accounts/" fake-account-id "/balance")
                          {:headers (auth-header)
                           :throw-exceptions false})]
    (not= 403 (:status response))))

;; Live Testing (2026-01-04):
;; Call: (check-auth) with valid API key configured
;; Response: true (underlying HTTP response: 404 "Account not found")
;;
;; Call: (check-auth) with invalid API key "wrong-key-12345"
;; Response: false (underlying HTTP response: 403 "Forbidden")
;;
;; Notes:
;; - This provides a lightweight way to validate credentials before making other API calls
;; - No resources are created during this check
;; - HEAD requests not supported by the API, so we use GET with fake account ID instead

(defn create-account
  "Creates a new account with the specified currency (USD, EUR, or GBP).

  Example:
    (create-account \"USD\")"
  [currency]
  (http/post (str base-url "/api/v1/accounts")
             {:headers (merge (auth-header) {"Content-Type" "application/json"})
              :body (json/generate-string {:currency currency})
              :as :json}))

;; Live Testing (2026-01-03):
;; Call: (create-account "USD")
;; Response:
;; {:status 200,
;;  :headers {...},
;;  :body {:id "80071d79-baec-7590-9275-c80cc49b4b18",
;;         :currency "USD"}}

(defn create-payment
  "Creates a payment for the specified account.

  Example:
    (create-payment \"80071d79-baec-7590-9275-c80cc49b4b18\" \"100.50\" \"USD\")"
  [account-id amount currency]
  (http/post (str base-url "/api/v1/accounts/" account-id "/payments")
             {:headers (merge (auth-header) {"Content-Type" "application/json"})
              :body (json/generate-string {:amount {:value amount :currency currency}})
              :as :json}))

;; Live Testing (2026-01-03):
;; Call: (create-payment "80071d79-baec-7590-9275-c80cc49b4b18" "100.50" "USD")
;; Response:
;; {:status 200,
;;  :headers {...},
;;  :body {:id "06709707-7c8f-70cd-9080-0944e66f87f3",
;;         :type "payment",
;;         :amount {:value "100.50", :currency "USD"},
;;         :status "pending",
;;         :timestamp "2026-01-03T23:34:58.589321Z"}}

(defn get-balance
  "Retrieves the current balance for the specified account.

  Example:
    (get-balance \"80071d79-baec-7590-9275-c80cc49b4b18\")"
  [account-id]
  (http/get (str base-url "/api/v1/accounts/" account-id "/balance")
            {:headers (auth-header)
             :as :json}))

;; Live Testing (2026-01-03):
;; Call: (get-balance "80071d79-baec-7590-9275-c80cc49b4b18")
;; Response:
;; {:status 200,
;;  :headers {...},
;;  :body {:balance {:value "0.00", :currency "USD"},
;;         :date "2026-01-03T23:47:03.421997Z"}}

(defn list-transactions
  "Lists transactions for the specified account. Supports pagination.

  Examples:
    (list-transactions \"80071d79-baec-7590-9275-c80cc49b4b18\")
    (list-transactions \"80071d79-baec-7590-9275-c80cc49b4b18\" {:limit 3})
    (list-transactions \"80071d79-baec-7590-9275-c80cc49b4b18\" {:limit 3 :cursor \"eyJQS...\"})"
  ([account-id]
   (list-transactions account-id {}))
  ([account-id {:keys [limit cursor]}]
   (let [query-params (cond-> {}
                        limit (assoc :limit limit)
                        cursor (assoc :cursor cursor))]
     (http/get (str base-url "/api/v1/accounts/" account-id "/transactions")
               {:headers (auth-header)
                :query-params query-params
                :as :json}))))

;; Live Testing (2026-01-03):
;;
;; Test 1: Basic call (no pagination parameters)
;; Call: (list-transactions "80071d79-baec-7590-9275-c80cc49b4b18")
;; Response:
;; {:status 200,
;;  :headers {...},
;;  :body {:transactions [{:id "06709707-7c8f-70cd-9080-0944e66f87f3",
;;                         :type "payment",
;;                         :amount {:value "100.50", :currency "USD"},
;;                         :timestamp "2026-01-03T23:34:58.589321Z"}],
;;         :has_more false}}
;;
;; Test 2: Pagination with limit (account with 11 transactions)
;; Call: (list-transactions "80071d79-baec-7590-9275-c80cc49b4b18" {:limit 3})
;; Response:
;; {:status 200,
;;  :headers {...},
;;  :body {:transactions [{:id "...", :type "payment", :amount {...}, :timestamp "..."}
;;                        {:id "...", :type "payment", :amount {...}, :timestamp "..."}
;;                        {:id "...", :type "payment", :amount {...}, :timestamp "..."}],
;;         :next_cursor "eyJQSI6ICJBQ0NPVU5UIzgwMDcxZDc5LWJhZWMtNzU5MC05Mjc1LWM4MGNjNDliNGIxOCIsICJTSyI6ICJUUkFOU0FDVElPTiMwMTkzOTQxZC04Zjk5LTdmYzktOWUyNS0yOGVmMDQ1NDhlNGQifQ==",
;;         :has_more true}}
;;
;; Test 3: Using cursor to get next page
;; Call: (list-transactions "80071d79-baec-7590-9275-c80cc49b4b18"
;;                          {:limit 3
;;                           :cursor "eyJQSI6ICJBQ0NPVU5UIzgwMDcxZDc5LWJhZWMtNzU5MC05Mjc1LWM4MGNjNDliNGIxOCIsICJTSyI6ICJUUkFOU0FDVElPTiMwMTkzOTQxZC04Zjk5LTdmYzktOWUyNS0yOGVmMDQ1NDhlNGQifQ=="})
;; Response:
;; {:status 200,
;;  :headers {...},
;;  :body {:transactions [{:id "...", :type "payment", :amount {...}, :timestamp "..."}
;;                        {:id "...", :type "payment", :amount {...}, :timestamp "..."}
;;                        {:id "...", :type "payment", :amount {...}, :timestamp "..."}],
;;         :next_cursor "eyJQSI6ICJBQ0NPVU5UIzgwMDcxZDc5LWJhZWMtNzU5MC05Mjc1LWM4MGNjNDliNGIxOCIsICJTSyI6ICJUUkFOU0FDVElPTiMwMTkzOTQxZC1iNTY4LTdmYWItOWJiYS03ZWYwYzA3ZDc4NTEifQ==",
;;         :has_more true}}
;;
;; Notes:
;; - Cursor is opaque base64-encoded token (contains DynamoDB PK/SK for pagination)
;; - has_more indicates whether more results are available
;; - next_cursor only present when has_more is true
;; - Transactions ordered by timestamp (newest first)
