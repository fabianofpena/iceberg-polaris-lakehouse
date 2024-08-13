import logging
from polaris.catalog.api.iceberg_o_auth2_api import IcebergOAuth2API
from polaris.catalog.api_client import ApiClient as CatalogApiClient
from polaris.catalog.api_client import Configuration as CatalogApiClientConfiguration
from polaris.management import ApiClient as ManagementApiClient
from polaris.management import Configuration as ManagementApiClientConfiguration

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

class PolarisClient:
    def __init__(self, client_id, client_secret, host):
        self.client_id = client_id
        self.client_secret = client_secret
        self.host = host
        self.client = CatalogApiClient(CatalogApiClientConfiguration(username=client_id,
                                                                     password=client_secret,
                                                                     host=host))
        self.token = self._get_token()

    def _get_token(self):
        oauth_api = IcebergOAuth2API(self.client)
        try:
            token = oauth_api.get_token(scope='PRINCIPAL_ROLE:ALL',
                                        client_id=self.client_id,
                                        client_secret=self.client_secret,
                                        grant_type='client_credentials',
                                        _headers={'realm': 'default-realm'})
            return token
        except Exception as e:
            logger.error("Failed to get token: %s", e)
            raise

    def get_catalog_client(self):
        return CatalogApiClient(CatalogApiClientConfiguration(access_token=self.token.access_token,
                                                              host=self.host))

    def get_management_client(self):
        return ManagementApiClient(ManagementApiClientConfiguration(access_token=self.token.access_token,
                                                                    host=self.host.replace('/catalog', '/management/v1')))
