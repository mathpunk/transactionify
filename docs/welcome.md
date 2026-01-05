---
title: Welcome
category:
  uri: getting-started
content:
  excerpt: Welcome to Transactionify API
---

<Callout icon="fa-exclamation" theme="warning">
  **Release Blocked:** The following issues must be addressed before releasing this API to external users:

  1. **OpenAPI Specification Discrepancies** - Response schemas for 2 endpoints do not match actual API behavior
  2. **Failing Unit Tests** - 6 tests fail due to pagination function rename (breaking change)
  3. **Payment Settlement Behavior** - Unclear how/when payments settle and balances update
  4. **Amount Validation Gap** - API accepts non-numeric strings like "not-a-number" for payment amounts
  5. **Rate Limiting Unspecified** - No documentation on rate limits; testing shows >5000 concurrent requests succeed

  See ISSUES.md in repository for full details, reproduction steps, and recommended actions.
</Callout>

<Cards>

  <Card title="Quickstart" href="quickstart" icon="fa-duotone fa-rocket-launch">Learn to make your first successful API call.</Card>

  <Card title="API Reference" href="reference" icon="fa-duotone fa-code-simple">Complete API reference with endpoint details and schemas.</Card>

  <Card title="Error Catalog" href="error-catalog" icon="fa-duotone fa-exclamation-triangle">Learn about common error codes and messages you might receive.</Card>

</Cards>
