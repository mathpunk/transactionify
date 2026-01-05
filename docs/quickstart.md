---
title: Quickstart
category:
  uri: getting-started
content:
  excerpt: Learn to make your first successful API call
---

# Quickstart

This guide shows you how to make your first successful API call to the Transactionify API.

## Prerequisites

- **API Key**: Contact your administrator to obtain an API key
- **Base URL**: `https://gj7edrv1il.execute-api.us-east-1.amazonaws.com`

## Authenticating

If you send a simple GET request to the API endpoint without credentials, like so:

```bash
curl https://gj7edrv1il.execute-api.us-east-1.amazonaws.com/api/v1/accounts/00000000-0000-0000-0000-000000000000/balance
```

You'll receive a `403 Forbidden` error:

```json
{
  "message": "Forbidden"
}
```

To verify that you've set up your API key correctly, include it in the Authorization header:

```bash
curl -H "Authorization: APIKey YOUR_API_KEY_HERE" \
  https://gj7edrv1il.execute-api.us-east-1.amazonaws.com/api/v1/accounts/00000000-0000-0000-0000-000000000000/balance
```

You'll receive a `404 Not Found` error instead:

```json
{
  "message": "Account not found"
}
```

This confirms your API key is valid! The `404` means the account doesn't exist, but authentication succeeded.

## Creating Your First Account

To create your first customer account, send a POST request with your desired currency (USD, EUR, or GBP):

```bash
curl -X POST https://gj7edrv1il.execute-api.us-east-1.amazonaws.com/api/v1/accounts \
  -H "Authorization: APIKey YOUR_API_KEY_HERE" \
  -H "Content-Type: application/json" \
  -d '{"currency": "USD"}'
```

**Successful Response:**

```json
{
  "id": "019a4757-c049-7ea8-a110-2ea110c5a6f8",
  "currency": "USD"
}
```

Congratulations! You've created your first account. Save the `id` value—you'll need it to create payments, check balances, and list transactions.

## Next Steps

Now that you have an account, you can:

- **Create payments**: [createPayment](https://transactionify-th.readme.io/reference/createpayment)
- **Check balances**: [getBalance](https://transactionify-th.readme.io/reference/getbalance)
- **List transactions**: [listTransactions](https://transactionify-th.readme.io/reference/listtransactions)
- **Handle errors**: See the [Error Catalog](https://transactionify-th.readme.io/docs/error-catalog) for common error codes and solutions

