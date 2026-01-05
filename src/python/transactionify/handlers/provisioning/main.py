import json
from typing import Dict, Any
from transactionify.services.api_key import register_new_api_key
from transactionify.tools.generators.uuid import generate_uuidv7


def handler(event: Dict[str, Any], context: Any) -> Dict[str, Any]:
    """
    Provisioning Lambda handler for registering new users.

    This function generates a new API key (UUIDv7) for a user and stores it in DynamoDB.
    It is NOT exposed as an API endpoint and is meant to be invoked manually
    (e.g., via AWS CLI, Console, or automation scripts).

    Event format:
    {
        "user_id": "019a4757-c049-7ea8-a110-2ea110c5a6f7"  # Optional, will be generated if not provided
    }

    Args:
        event: Lambda event containing optional user_id
        context: Lambda context object

    Returns:
        Response with generated API key and user_id

    Example invocation:
        aws lambda invoke --function-name transactionify-provisioning \\
            --payload '{"user_id": "019a4757-c049-7ea8-a110-2ea110c5a6f7"}' \\
            output.json
    """
    print(f"Provisioning event: {json.dumps(event)}")

    # Extract or generate user_id
    user_id = event.get('user_id', '')

    if not user_id:
        # Generate new user_id (UUIDv7)
        user_id = generate_uuidv7()
        print(f"Generated new user_id: {user_id}")

    # Register new API key using the service (validates user_id)
    try:
        api_key = register_new_api_key(user_id)
        print(f"Successfully created API key for user: {user_id}")

        return {
            'statusCode': 200,
            'body': json.dumps({
                'api_key': api_key,
                'user_id': user_id,
                'message': 'API key successfully created'
            })
        }

    except ValueError as e:
        # Validation error from register_new_api_key
        error_msg = str(e)
        print(f"Validation error: {error_msg}")
        return {
            'statusCode': 400,
            'body': json.dumps({
                'error': 'Invalid user_id format',
                'message': error_msg
            })
        }

    except Exception as e:
        error_msg = f"Failed to create API key: {str(e)}"
        print(error_msg)
        return {
            'statusCode': 500,
            'body': json.dumps({
                'error': 'Internal server error',
                'message': error_msg
            })
        }
