from infra.settings.config import PolarisClient
from infra.repository.principal_repository import PrincipalRepository
from use_cases.create_principal import CreatePrincipalUseCase

class CreatePrincipalComposer:
    @staticmethod
    def compose(args):
        polaris_client = PolarisClient(client_id=args.client_id, client_secret=args.client_secret, host=args.host)
        management_client = polaris_client.get_management_client()
        principal_repo = PrincipalRepository(management_client)
        return CreatePrincipalUseCase(principal_repo)
