#
# Copyright (c) 2024 Snowflake Computing Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

import json
import os
import sys
from json import JSONDecodeError

from cli.constants import Arguments, CLIENT_ID_ENV, CLIENT_SECRET_ENV
from cli.options.option_tree import Argument
from cli.options.parser import Parser
from polaris.management import ApiClient, Configuration
from polaris.management import PolarisDefaultApi


class PolarisCli:
    """
    Implements a basic Command-Line Interface (CLI) for interacting with a Polaris service. The CLI can be used to
    manage entities like catalogs, principals, and grants within Polaris and can perform most operations that are
    available in the Python client API.

    Example usage:
    * ./polaris --client-id ${id} --client-secret ${secret} --host ${hostname} principals create example_user
    * ./polaris --client-id ${id} --client-secret ${secret} --host ${hostname} principal-roles create example_role
    * ./polaris --client-id ${id} --client-secret ${secret} --host ${hostname} catalog-roles list
    """

    # Can be enabled if the client is able to authenticate directly without first fetching a token
    DIRECT_AUTHENTICATION_ENABLED = False

    @staticmethod
    def execute(args=None):
        options = Parser.parse(args)
        client_builder = PolarisCli._get_client_builder(options)
        with client_builder() as api_client:
            try:
                from cli.command import Command
                admin_api = PolarisDefaultApi(api_client)
                command = Command.from_options(options)
                command.execute(admin_api)
            except Exception as e:
                PolarisCli._try_print_exception(e)
                sys.exit(1)

    @staticmethod
    def _try_print_exception(e):
        try:
            error = json.loads(e.body)['error']
            sys.stderr.write(f'Exception when communicating with the Polaris server.'
                             f' {error["type"]}: {error["message"]}{os.linesep}')
        except JSONDecodeError as _:
            sys.stderr.write(f'Exception when communicating with the Polaris server.'
                             f' {e.status}: {e.reason}{os.linesep}')
        except Exception as _:
            sys.stderr.write(f'Exception when communicating with the Polaris server.'
                             f' {e}{os.linesep}')

    @staticmethod
    def _get_token(api_client: ApiClient, catalog_url, client_id, client_secret) -> str:
        response = api_client.call_api(
            'POST',
            f'{catalog_url}/oauth/tokens',
            header_params={'Content-Type': 'application/x-www-form-urlencoded'},
            post_params={
                'grant_type': 'client_credentials',
                'client_id': client_id,
                'client_secret': client_secret,
                'scope': 'PRINCIPAL_ROLE:ALL'
            }
        ).response.data
        if 'access_token' not in json.loads(response):
            raise Exception('Failed to get access token')
        return json.loads(response)['access_token']

    @staticmethod
    def _get_client_builder(options):

        # Validate
        has_access_token = options.access_token is not None
        has_client_secret = options.client_id is not None and options.client_secret is not None
        if has_access_token and has_client_secret:
            raise Exception(f'Please provide credentials via either {Argument.to_flag_name(Arguments.CLIENT_ID)} &'
                            f' {Argument.to_flag_name(Arguments.CLIENT_SECRET)} or'
                            f' {Argument.to_flag_name(Arguments.ACCESS_TOKEN)}, but not both')

        # Authenticate accordingly
        polaris_management_url = f'http://{options.host}:{options.port}/api/management/v1'
        polaris_catalog_url = f'http://{options.host}:{options.port}/api/catalog/v1'
        builder = None
        if has_access_token:
            builder = lambda: ApiClient(
                Configuration(host=polaris_management_url, access_token=options.access_token),
            )
        elif has_client_secret:
            builder = lambda: ApiClient(
                Configuration(host=polaris_management_url, username=options.client_id, password=options.client_secret),
            )
        elif os.getenv('CLIENT_ID') and os.getenv('CLIENT_SECRET'):
            builder = lambda: ApiClient(
                Configuration(
                    host=polaris_management_url,
                    username=os.getenv(CLIENT_ID_ENV),
                    password=os.getenv(CLIENT_SECRET_ENV)
                )
            )
        else:
            raise Exception(f'Please provide credentials via either {Argument.to_flag_name(Arguments.CLIENT_ID)} &'
                            f' {Argument.to_flag_name(Arguments.CLIENT_SECRET)} or'
                            f' {Argument.to_flag_name(Arguments.ACCESS_TOKEN)}.'
                            f' Alternatively, you may set the environment variables {CLIENT_ID_ENV} &'
                            f' {CLIENT_SECRET_ENV}.')

        if not has_access_token and not PolarisCli.DIRECT_AUTHENTICATION_ENABLED:
            token = PolarisCli._get_token(builder(), polaris_catalog_url, options.client_id, options.client_secret)
            builder = lambda: ApiClient(
                Configuration(host=polaris_management_url, access_token=token),
            )
        return builder



if __name__ == '__main__':
    PolarisCli.execute()
