from infra.settings.config import PolarisClient
from infra.repository.catalog_repository import CatalogRepository
from use_cases.create_catalog import CreateCatalogUseCase

class CreateCatalogComposer:
    @staticmethod
    def compose(args):
        polaris_client = PolarisClient(client_id=args.client_id, client_secret=args.client_secret, host=args.host)
        management_client = polaris_client.get_management_client()
        catalog_repo = CatalogRepository(management_client)
        return CreateCatalogUseCase(catalog_repo)
