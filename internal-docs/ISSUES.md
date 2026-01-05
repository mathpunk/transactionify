# Potential Issues & Discussion Points

This document tracks issues discovered during technical documentation development that require team discussion or architectural decisions.

## 1. Test Suite Out of Sync with Deployed Code

**Status:** Needs Attention
**Severity:** Low - Production works correctly; tests need maintenance
**Discovered:** 2026-01-03 during unit test validation

### Issue Description

Commit `a0d9b5e` ("Added pagination for transactions") renamed the internal function `query_by_pk` to `query_by_pk_paginated`. The deployed API works correctly, but 6 tests still reference the old function name.

### API Change is Additive

The pagination update **adds** information to responses without requiring changes from existing clients:

- **Before:** `{"transactions": [...]}`
- **After:** `{"transactions": [...], "has_more": false, "next_cursor": null}`

Clients accessing `response["transactions"]` continue to work unchanged. The new fields (`has_more`, `next_cursor`) are available for clients who want pagination support.

### Failing Tests

```
FAILED: services/test_transaction.py::TestListTransactions::test_list_transactions_success
FAILED: services/test_transaction.py::TestListTransactions::test_list_transactions_empty
FAILED: services/test_transaction.py::TestListTransactions::test_list_transactions_uses_account_currency
FAILED: services/test_transaction.py::TestListTransactions::test_list_transactions_query_parameters
FAILED: handlers/api/rest/transaction/list/test_main.py::TestListTransactionsHandler::test_handler_success
FAILED: handlers/api/rest/transaction/list/test_main.py::TestListTransactionsHandler::test_handler_empty_list
```

### Resolution

Update or remove the stale tests to match the current implementation.

---

## 2. Settlement Not Implemented

**Status:** Needs Engineering Input
**Severity:** Medium - Feature incomplete; API works as coded
**Discovered:** 2026-01-03 during live API testing

### Issue Description

Payments are recorded but never settle. Balance remains $0.00 regardless of payment activity.

### Evidence

1. **Live testing:** Created $881.50 in payments over 48+ hours. Balance: still $0.00.

2. **Code analysis:**
   - `payment.py`: Creates transaction with `status: 'pending'`, does NOT update BALANCE record
   - `balance.py`: Reads stored BALANCE record, does NOT compute from transactions
   - No settlement service exists in this repository

3. **OpenAPI spec:** Payment status enum only allows `"pending"` — no other states defined.

### Conclusion

The balance is a static record initialized to $0.00 at account creation. No code path exists to update it. This is either:
- **Intentional:** Demo/test API where settlement is out of scope
- **Incomplete:** Settlement service was planned but not built
- **External:** Settlement happens via a separate system we don't have visibility into

### Question for Engineering

Is there a settlement mechanism outside this repository, or is this feature intentionally unimplemented?

---

## 3. Amount Validation Required for Payment Endpoint

**Status:** Needs Fix
**Severity:** High - Financial Data Integrity Risk
**Discovered:** 2026-01-04 during error discovery testing

### Issue Description

The payment endpoint accepts `amount.value` as a string—a good design choice to avoid floating-point precision issues with currency. However, the endpoint does not validate that the string represents a valid monetary amount. Non-numeric values like `"not-a-number"` are accepted and stored.

### Why This Matters

Financial APIs cannot rely on clients to "just send valid data." Backend validation is essential because:

1. **Data integrity:** Invalid amounts corrupt the transaction record
2. **Fail-fast principle:** Reject bad input at the boundary, not downstream
3. **Defense in depth:** Clients make mistakes; the API should catch them

### Observed Behavior

**Request:**
```json
POST /api/v1/accounts/{account_id}/payments
{"amount": {"value": "not-a-number", "currency": "USD"}}
```

**Response:** `200 OK` — payment accepted and stored

### Other Fields Validate Correctly

| Field | Invalid Input | Response |
|-------|---------------|----------|
| `amount.value` (missing) | `{}` | 400 - Missing required field |
| `currency` (missing) | `{"value": "100"}` | 400 - Missing required field |
| `currency` (invalid) | `"CAD"` | 400 - Invalid currency |
| **`amount.value` (non-numeric)** | `"not-a-number"` | **200 OK** |

The validation gap is inconsistent with how other fields are handled.

### Recommended Fix

Add validation to reject non-numeric `amount.value` with:
- `400 Bad Request`
- Message: `"Invalid amount format. Expected numeric string (e.g., '100.50')"`

### Related Files

- `src/python/transactionify/handlers/api/rest/payment/create/main.py`
- `src/python/transactionify/services/payment.py`

---

## 4. Rate Limiting: Documentation Needed

**Status:** Needs Product/Engineering Input
**Severity:** Low - No immediate impact; guidance needed for documentation
**Discovered:** 2026-01-04 during rate limiting investigation

### Issue Description

Load testing up to 7500 concurrent requests did not trigger any rate limiting (429) responses. The API handled all requests successfully. This isn't a problem—but we need guidance on what to tell API consumers about expected throughput and limits.

### Test Results

| Concurrent Requests | Result | Duration | Throughput |
|---------------------|--------|----------|------------|
| 100                 | ✓ All successful | 2.4s | ~42 req/sec |
| 1000                | ✓ All successful | 13.3s | ~75 req/sec |
| 5000                | ✓ All successful | 150s | ~33 req/sec |
| 7500                | ✓ All successful | 162s | ~46 req/sec |

No 429 responses observed at any level tested.

### Questions for Product/Engineering

1. **Are rate limits configured?** If yes, what are they? If no, are they planned?
2. **What should we document?** Should we tell users "no limits" or "generous limits apply"?
3. **Do any customers need high throughput?** If typical usage is well below what we tested, this may not warrant documentation at all.

### Recommendation

For now, advise clients to implement reasonable backoff for production use, and note that limits may be added in future versions.

## 5. OpenAPI Spec Should Be Auto-Generated

**Status:** Recommendation
**Severity:** Low - Process improvement
**Discovered:** 2026-01-04

### Background

During documentation development, we initially believed the OpenAPI spec had drifted from the live API. However, live REPL verification on 2026-01-05 confirmed **the spec is accurate**:

- Create Account: Spec says `{id, balance}` → API returns `{id, balance}` ✓
- Create Payment: Spec says `{id, type, amount, status}` → API returns exactly that ✓

The earlier analysis was in error.

### Recommendation

The spec is currently hand-maintained. To prevent future drift, consider implementing auto-generation through FastAPI, Swagger, Connexion, or similar tooling.
