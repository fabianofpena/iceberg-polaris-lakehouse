from core.interfaces import PrincipalRepositoryInterface

class CreatePrincipalUseCase:
    def __init__(self, principal_repo: PrincipalRepositoryInterface):
        self.principal_repo = principal_repo

    def execute(self, name):
        return self.principal_repo.create_principal(name)
