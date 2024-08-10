from infra.settings.config import PolarisClient
from infra.repository.role_repository import RoleRepository
from use_cases.create_role import CreateRoleUseCase

class CreateRoleComposer:
    @staticmethod
    def compose(args):
        polaris_client = PolarisClient(client_id=args.client_id, client_secret=args.client_secret, host=args.host)
        management_client = polaris_client.get_management_client()
        role_repo = RoleRepository(management_client)
        return CreateRoleUseCase(role_repo)
