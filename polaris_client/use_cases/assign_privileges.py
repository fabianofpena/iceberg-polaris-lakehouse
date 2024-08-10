from core.interfaces import RoleRepositoryInterface

class AssignPrivilegesUseCase:
    def __init__(self, role_repo: RoleRepositoryInterface):
        self.role_repo = role_repo

    def assign_privileges(self, catalog_name, role_name, privileges):
        return self.role_repo.add_grant_to_catalog_role(catalog_name, role_name, privileges)

    def assign_catalog_role_to_principal_role(self, principal_role_name, catalog_name, catalog_role_name):
        return self.role_repo.assign_catalog_role_to_principal_role(principal_role_name, catalog_name, catalog_role_name)
