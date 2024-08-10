import logging
from core.interfaces import PrincipalRepositoryInterface
from polaris.management import PolarisDefaultApi, CreatePrincipalRequest, Principal
from polaris.management.api_client import ApiException

logger = logging.getLogger(__name__)

class PrincipalRepository(PrincipalRepositoryInterface):
    def __init__(self, api_client):
        self.api = PolarisDefaultApi(api_client)

    def create_principal(self, name):
        try:
            principal = self.api.get_principal(principal_name=name)
            logger.info("Principal %s already exists.", name)
        except ApiException:
            principal = Principal(name=name, type="SERVICE")
            principal_result = self.api.create_principal(CreatePrincipalRequest(principal=principal))
            logger.info("Principal %s created.", name)
            logger.info("Principal credentials - client_id: %s and client_secret: %s",
                        principal_result.credentials.client_id,
                        principal_result.credentials.client_secret)
        return self.api.get_principal(principal_name=name)
