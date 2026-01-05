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

- **API Key**: You'll need an API key. Your customer service representative at LoanPro can help you get one if you don't have one already. 
- **Any program that can send HTTP requests**: In these examples, we'll be using the command line tool `curl`, but you can use any HTTP client for your favorite language.

## Verifying your setup

If you're new to using APIs, it can be useful to verify that you're using the tool you've chosen to connect with our API successfully. We'll start by trying to get the balance of a non-existent account:

```bash
curl https://gj7edrv1il.execute-api.us-east-1.amazonaws.com/api/v1/accounts/0000/balance
```

There's no user with an ID of `0000`, but that's okay for testing the connection. This command will return `{"message":"Unauthorized"}` (also known as HTTP status 401), which shows that the server is not letting you get any data. And that's expected! We haven't included your authentication credentials. If you get something like `Could not resolve host`, or another error, then check the URL you're using, or read the documentation for the HTTP client you've chosen. 

## Testing Authentication

Let's try and get information for a fake account again, but this time, we'll include the secret API key in the Authorization header:

```bash
curl -H "Authorization: APIKey YOUR_API_KEY_HERE" \
  https://gj7edrv1il.execute-api.us-east-1.amazonaws.com/api/v1/accounts/0000/balance
```
which will yield 

```json
{
  "message": "Account not found"
}
```
We knew there wouldn't be an account, because it's a fake number. But this time, the API is willing to tell us, because we've proven we're allowed access to that data (or maybe we should say, the lack of that data). 


## Creating Your First Account

That was all just preamble to doing some real work. Let's create your first customer account. Now we'll send a POST request. You must include your desired currency; Transactionify can handle US dollars (USD), euro (EUR), or British pounds (GBP) only: 

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

Congratulations! You've created your first account. 

IMPORTANT: You are responsible for holding onto the `id` value. You'll need it to create payments, check balances, and list transactions.


## Next Steps

Now that you have created a user's account, you can:

- [**Create payments**](https://transactionify-th.readme.io/reference/createpayment) for the user. 
- [**Check the balance**](https://transactionify-th.readme.io/reference/getbalance) of the user. 
- [**List transactions**](https://transactionify-th.readme.io/reference/listtransactions) for the user. 

It's possible you'll encounter errors while using Transactionify. See the [Error Catalog](https://transactionify-th.readme.io/docs/error-catalog) for common failure modes, errors, and solutions. 

