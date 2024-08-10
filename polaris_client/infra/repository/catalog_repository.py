import logging
from core.interfaces import CatalogRepositoryInterface
from polaris.management import PolarisDefaultApi, CreateCatalogRequest, Catalog, AwsStorageConfigInfo

logger = logging.getLogger(__name__)

class CatalogRepository(CatalogRepositoryInterface):
    def __init__(self, api_client):
        self.api = PolarisDefaultApi(api_client)

    def create_catalog(self, name, s3_location, role_arn):
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
