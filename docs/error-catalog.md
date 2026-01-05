---
title: Error Catalog
category:
  uri: api-guide
content:
  excerpt: Learn about errors, error messages, known issues, and common questions you might encounter. 
---

# Error Catalog

This guide documents common errors and sources of confusion you may have when using the Transactionify API.

## Common Error Responses

| Status Code | Error Type | Message | When This Occurs |
|-------------|------------|---------|------------------|
| **400** | Bad Request | "Missing required field: currency" | Creating an account without specifying currency |
| **400** | Bad Request | "Invalid currency. Allowed values: USD, EUR, GBP" | Attempting to use unsupported currency (e.g., JPY, CAD) |
| **400** | Bad Request | "Missing required field: amount" | Creating a payment without specifying amount |
| **400** | Bad Request | "Invalid pagination cursor" | Using an invalid or expired cursor for transaction pagination |
| **403** | Forbidden | "Forbidden" | Invalid or missing API key in Authorization header |
| **404** | Not Found | "Account not found" | Attempting operations on non-existent account ID |
| **500** | Internal Server Error | "An error occurred while processing your request" | Unexpected server-side error |

## Authentication Errors

### 403 Forbidden

**Cause:** Invalid or missing API key

**Example Request:**
```bash
curl -X POST https://gj7edrv1il.execute-api.us-east-1.amazonaws.com/api/v1/accounts \
  -H "Authorization: APIKey wrong-key-12345" \
  -H "Content-Type: application/json" \
  -d '{"currency": "USD"}'
```

**Response:**
```json
{
  "message": "Forbidden"
}
```

**How to Fix:** Verify your API key is correct and properly formatted in the Authorization header as `APIKey <your-key>`.

## Validation Errors

### Missing Required Fields

**Status:** 400 Bad Request

The API validates that all required fields are present in requests.

**Common Examples:**
- Creating account without `currency`: `"Missing required field: currency"`
- Creating payment without `amount`: `"Missing required field: amount"`

### Invalid Currency

**Status:** 400 Bad Request
**Message:** `"Invalid currency. Allowed values: USD, EUR, GBP"`

**Cause:** Attempting to use a currency not supported by the API.

**Supported Currencies:**
- USD (US Dollar)
- EUR (Euro)
- GBP (British Pound)

**Note:** The API rejects all other currency codes, including valid ISO currencies like JPY or CAD.

### Account Not Found

**Status:** 404 Not Found
**Message:** `"Account not found"`

**Cause:** Attempting to access an account that doesn't exist or using an invalid account ID format.

**Common Scenarios:**
- Creating a payment for a non-existent account
- Checking the balance for an invalid account ID
- Listing transactions for a deleted or never-created account

## Known Issues

⚠️ **Amount Validation Gap:** The API currently accepts non-numeric strings for payment amounts. This is reasonable for passing currency values like `"105.25"` but doesn't make much sense for, e.g., `"not-a-number"`). Clients should validate amount values are numeric before sending requests. In the future, the API may do its own validation, and start rejecting non-numeric data.


## Common Questions

### Why is my balance still $0.00 after creating payments?

Payments are created with status `"pending"`. Balance updates are processed separately and may not reflect recent payment activity immediately.


## Getting Help

If you encounter errors not listed here or need clarification:
1. Check the [API Reference](https://transactionify-th.readme.io/reference) for endpoint-specific requirements.
2. Contact the API support team with your request details and error response.
