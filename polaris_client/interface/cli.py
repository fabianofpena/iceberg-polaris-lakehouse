import argparse
from core.entities import Privileges

def parse_args():
    parser = argparse.ArgumentParser(description="Polaris API Client")
    parser.add_argument('--client_id', required=True, help='Client ID for Polaris API')
    parser.add_argument('--client_secret', required=True, help='Client Secret for Polaris API')
    parser.add_argument('--host', required=True, help='Host URL for Polaris API')
    parser.add_argument('--catalog_name', required=True, help='Name of the catalog')
    parser.add_argument('--s3_location', required=True, help='S3 location for the catalog')
    parser.add_argument('--role_arn', required=True, help='Role ARN for the catalog')
    parser.add_argument('--principal_name', required=True, help='Name of the principal')
    parser.add_argument('--principal_role_name', required=True, help='Name of the principal role')
    parser.add_argument('--catalog_role_name', required=True, help='Name of the catalog role')
    parser.add_argument(
        '--role_type', required=True, choices=['admin', 'reader', 'read_only'],
        help='Type of role to assign'
    )
    return parser.parse_args()

def get_privileges(role_type):
    if role_type == 'admin':
        return Privileges.ADMIN
    if role_type == 'reader':
        return Privileges.READER
    if role_type == 'read_only':
        return Privileges.READ_ONLY
    raise ValueError("Invalid role type specified")
