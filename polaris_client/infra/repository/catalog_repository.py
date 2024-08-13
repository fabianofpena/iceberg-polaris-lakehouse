import logging
from core.interfaces import CatalogRepositoryInterface
from polaris.management import PolarisDefaultApi, CreateCatalogRequest, Catalog, AwsStorageConfigInfo

logger = logging.getLogger(__name__)

class CatalogRepository(CatalogRepositoryInterface):
    """
    Repository for managing catalog operations through Polaris API.
    """
    def __init__(self, api_client):
        """
        Initialize the repository with an API client.
        
        Args:
            api_client (ApiClient): The API client for interacting with Polaris.
        """
        self.api = PolarisDefaultApi(api_client)

    def create_catalog(self, name, s3_location, role_arn):
        """
        Create or retrieve a catalog based on the provided parameters.
        
        Args:
            name (str): The name of the catalog.
            s3_location (str): The S3 location associated with the catalog.
            role_arn (str): The ARN of the role for the catalog.
        
        Returns:
            Catalog: The created or retrieved catalog object.
        """
        try:
            catalog = self.api.get_catalog(catalog_name=name)
            logger.info("Catalog %s already exists.", name)
        except Exception:
            storage_conf = AwsStorageConfigInfo(storage_type="S3",
                                                allowed_locations=[s3_location],
                                                role_arn=role_arn)
            catalog = Catalog(name=name, type='INTERNAL', properties={"default-base-location": s3_location},
                              storage_config_info=storage_conf)
            catalog.storage_config_info = storage_conf
            self.api.create_catalog(create_catalog_request=CreateCatalogRequest(catalog=catalog))
            logger.info("Catalog %s created.", name)
        return self.api.get_catalog(catalog_name=name)
