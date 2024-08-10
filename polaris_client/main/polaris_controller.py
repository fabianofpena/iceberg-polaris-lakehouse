from main.composer.create_catalog_composer import CreateCatalogComposer
from main.composer.create_principal_composer import CreatePrincipalComposer
from main.composer.create_role_composer import CreateRoleComposer
from main.composer.assign_privileges_composer import AssignPrivilegesComposer
from interface.cli import get_privileges

class PolarisController:
    def __init__(self, args):
        self.args = args
        self.create_catalog_use_case = CreateCatalogComposer.compose(args)
        self.create_principal_use_case = CreatePrincipalComposer.compose(args)
        self.create_role_use_case = CreateRoleComposer.compose(args)
        self.assign_privileges_use_case = AssignPrivilegesComposer.compose(args)

    def execute(self):
        # Create or get catalog
        self.create_catalog_use_case.execute(self.args.catalog_name, self.args.s3_location, self.args.role_arn)

        # Create or get principal
        self.create_principal_use_case.execute(self.args.principal_name)

        # Create or get principal role
        self.create_role_use_case.create_principal_role(self.args.principal_role_name)

        # Assign principal role to principal
        self.create_role_use_case.role_repo.assign_principal_role(self.args.principal_name, self.args.principal_role_name)

        # Create or get catalog role
        self.create_role_use_case.create_catalog_role(self.args.catalog_name, self.args.catalog_role_name)

        # Assign privileges to catalog role
        privileges = get_privileges(self.args.role_type)
        self.assign_privileges_use_case.assign_privileges(self.args.catalog_name, self.args.catalog_role_name, privileges)

        # Assign catalog role to principal role
        self.assign_privileges_use_case.assign_catalog_role_to_principal_role(
            self.args.principal_role_name, self.args.catalog_name, self.args.catalog_role_name
        )
