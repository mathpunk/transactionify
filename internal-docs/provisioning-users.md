# Provisioning New Users

This guide will help you provision a new user. First, let's clarify which users we're talking about.

Transactionify is for creating accounts, posting payments to them, and checking the balances of those accounts. The users of those accounts, we'll call **customer users**. Those IDs are managed by consumers of the Transactionify API, and they are created via API gateway through the `/accounts` endpoint. See [openapi.yaml](../openapi.yaml), [the Postman collection](../Transactionify_API.postman_collection.json), or the [public documentation](https://transactionify-th.readme.io/reference/createaccount) for more on customer users.

The other sort of user is a developer who is consuming the Transactionify API. These users need API keys in order to access Transactionify. But we do **not** expose the functionality for creating these client users through an API gateway. Instead, that functionality is secured behind an AWS Lambda, and invoked via command line, the AWS Console, or automation scripts.

In this guide, we'll assume you're creating a new client user from the command line. The procedure should be very similar for automated scripts, provided the host machine has the necessary prerequisites.

## Prerequisites

- **AWS CLI** installed and configured
- **AWS credentials** configured via `aws configure` (access key ID and secret access key)
- **Region set to us-east-1** (our production region)
  - Set via `aws configure`, or
  - Set via environment variable: `export AWS_DEFAULT_REGION=us-east-1`

NOTE: Your credentials must have `lambda:InvokeFunction` permission for the `transactionify-provisioning` function. How this permission is granted depends on how we handle IAM setup -- it may be attached directly to your IAM User, or you may need to assume an IAM Role. **[Investigate: Who should engineers contact for IAM permissions -- DevOps? A specific engineering manager?]**


## Procedure

To invoke the provisioning function, you'll need to reference the function name (`transactionify-provisioning`), provide a payload (possibly empty), and specify the filename where the response should be written (e.g., `output.json`).

**Basic invocation (auto-generate user_id):**

```bash
aws lambda invoke \
    --function-name transactionify-provisioning \
    --payload '{}' \
    output.json
```

**With a specific user_id:**

```bash
aws lambda invoke \
    --function-name transactionify-provisioning \
    --payload '{"user_id": "019a4757-c049-7ea8-a110-2ea110c5a6f7"}' \
    output.json
```

You can supply an empty dictionary as a payload (`'{}'`), and a user_id for the client user will be automatically generated. You can also provide your own; however, the only acceptable format for a user_id is **UUIDv7** (which includes a timestamp for sortability). If you supply a user_id in a different format (such as UUIDv4), the Lambda will reject it with a 400 error.

After running the command, check the response file:

```bash
cat output.json
```

**Successful response:**
```json
{
  "statusCode": 200,
  "body": "{\"api_key\": \"019a4757-c049-7ea8-a110-2ea110c5a6f6\", \"user_id\": \"019a4757-c049-7ea8-a110-2ea110c5a6f7\", \"message\": \"API key successfully created\"}"
}
```

The `api_key` in the response is what you'll provide to the client for authentication. They'll use it in the `Authorization` header like this:

```bash
Authorization: APIKey 019a4757-c049-7ea8-a110-2ea110c5a6f6
```

**Error response (invalid user_id format):**
```json
{
  "statusCode": 400,
  "body": "{\"error\": \"Invalid user_id format\", \"message\": \"Invalid user_id format. Must be UUIDv7: 550e8400-e29b-41d4-a716-446655440000\"}"
}
```

## Behind the Scenes

When you invoke a Lambda function with `aws lambda invoke`, your JSON payload is converted into an `event` parameter that gets passed to the handler function inside the Lambda. The handler function is defined at [`src/python/transactionify/handlers/provisioning/main.py`](../src/python/transactionify/handlers/provisioning/main.py).

Inside the Lambda:
1. Your payload `{"user_id": "..."}` becomes the `event` dictionary
2. The handler extracts the `user_id` from `event.get('user_id', '')`
3. If no user_id is provided (or it's empty), a new UUIDv7 is generated
4. A new API key (also UUIDv7) is created and stored in DynamoDB
5. The response is written to your specified output file

This is why the provisioning functionality isn't a REST endpoint—it's an internal-only Lambda function that requires AWS credentials to invoke, adding an extra layer of security for client user creation. 
