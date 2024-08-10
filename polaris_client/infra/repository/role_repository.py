import logging
from core.interfaces import RoleRepositoryInterface
from polaris.management import (
    PolarisDefaultApi, CreateCatalogRoleRequest, CreatePrincipalRoleRequest,
    GrantCatalogRoleRequest, AddGrantRequest, CatalogRole, PrincipalRole,
    CatalogGrant, GrantPrincipalRoleRequest
)
from polaris.management.api_client import ApiException

logger = logging.getLogger(__name__)

class RoleRepository(RoleRepositoryInterface):
    def __init__(self, api_client):
        self.api = PolarisDefaultApi(api_client)

    def create_principal_role(self, name):
        role = PrincipalRole(name=name)
        try:
            self.api.create_principal_role(CreatePrincipalRoleRequest(principal_role=role))
            logger.info("Principal role %s created.", name)
        except ApiException as e:
            if e.status == 409:
                logger.info("Principal role %s already exists.", name)
            else:
                raise e
        return self.api.get_principal_role(principal_role_name=name)

    def create_catalog_role(self, catalog_name, role_name):
        role = CatalogRole(name=role_name)
        try:
            self.api.create_catalog_role(
                catalog_name=catalog_name,
                create_catalog_role_request=CreateCatalogRoleRequest(catalog_role=role)
            )
            logger.info("Catalog role %s created for catalog %s.", role_name, catalog_name)
        except ApiException as e:
            if e.status == 409:
                logger.info("Catalog role %s already exists in catalog %s.", role_name, catalog_name)
            else:
                raise e
        return self.api.get_catalog_role(catalog_name=catalog_name, catalog_role_name=role_name)

    def assign_catalog_role_to_principal_role(self, principal_role_name, catalog_name, catalog_role_name):
        catalog_role = CatalogRole(name=catalog_role_name)
        grant_request = GrantCatalogRoleRequest(catalog_role=catalog_role)
        try:
            self.api.assign_catalog_role_to_principal_role(
                principal_role_name=principal_role_name,
                catalog_name=catalog_name,
                grant_catalog_role_request=grant_request
            )
            logger.info(
                "Assigned catalog role %s in catalog %s to principal role %s.",
                catalog_role_name, catalog_name, principal_role_name
            )
        except ApiException as e:
            logger.error(
                "Failed to assign catalog role %s in catalog %s to principal role %s: %s",
                catalog_role_name, catalog_name, principal_role_name, e
            )
            if e.status == 409:
                logger.info(
                    "Catalog role %s already assigned to principal role %s.",
                    catalog_role_name, principal_role_name
                )
            else:
                raise e

    def add_grant_to_catalog_role(self, catalog_name, role_name, privileges):
        for privilege in privileges:
            try:
                grant = CatalogGrant(catalog_name=catalog_name, type='catalog', privilege=privilege)
                self.api.add_grant_to_catalog_role(
                    catalog_name=catalog_name,
                    catalog_role_name=role_name,
                    add_grant_request=AddGrantRequest(grant=grant)
                )
                logger.info(
                    "Assigned privileges %s to catalog role %s in catalog %s.",
                    privilege, role_name, catalog_name
                )
            except ApiException as e:
                logger.error(
                    "Failed to grant %s to %s in catalog %s: %s",
                    privilege, role_name, catalog_name, e
                )

    def assign_principal_role(self, principal_name, principal_role_name):
        try:
            self.api.assign_principal_role(
                principal_name=principal_name,
                grant_principal_role_request=GrantPrincipalRoleRequest(
                    principal_role=PrincipalRole(name=principal_role_name)
                )
            )
            logger.info("Assigned principal role %s to principal %s.", principal_role_name, principal_name)
        except ApiException as e:
            if e.status == 409:
                logger.info("Principal role %s already assigned to principal %s.", principal_role_name, principal_name)
            else:
                raise e
