from core.entities import Catalog
from core.interfaces import CatalogRepositoryInterface

class CreateCatalogUseCase:
    def __init__(self, catalog_repo: CatalogRepositoryInterface):
        self.catalog_repo = catalog_repo

    def execute(self, name, s3_location, role_arn):
        catalog = Catalog(name, s3_location, role_arn)
        return self.catalog_repo.create_catalog(catalog.name, catalog.s3_location, catalog.role_arn)
