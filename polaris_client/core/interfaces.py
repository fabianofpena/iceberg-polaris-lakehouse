from abc import ABC, abstractmethod

class PrincipalRepositoryInterface(ABC):
    @abstractmethod
    def create_principal(self, name):
        pass

class RoleRepositoryInterface(ABC):
    @abstractmethod
    def create_principal_role(self, name):
        pass

    @abstractmethod
    def create_catalog_role(self, catalog_name, role_name):
        pass

    @abstractmethod
    def assign_catalog_role_to_principal_role(self, principal_role_name, catalog_name, catalog_role_name):
        pass

    @abstractmethod
    def add_grant_to_catalog_role(self, catalog_name, role_name, privileges):
        pass

class CatalogRepositoryInterface(ABC):
    @abstractmethod
    def create_catalog(self, name, s3_location, role_arn):
        pass
