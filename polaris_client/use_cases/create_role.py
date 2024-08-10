from core.entities import PrincipalRole, CatalogRole
from core.interfaces import RoleRepositoryInterface

class CreateRoleUseCase:
    def __init__(self, role_repo: RoleRepositoryInterface):
        self.role_repo = role_repo

    def create_principal_role(self, name):
        role = PrincipalRole(name)
        return self.role_repo.create_principal_role(role.name)

    def create_catalog_role(self, catalog_name, role_name):
        role = CatalogRole(role_name)
        return self.role_repo.create_catalog_role(catalog_name, role.name)
