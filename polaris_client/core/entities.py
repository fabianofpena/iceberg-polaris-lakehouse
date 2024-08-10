class Principal:
    def __init__(self, name, principal_type="SERVICE"):
        self.name = name
        self.type = principal_type

class PrincipalRole:
    def __init__(self, name):
        self.name = name

class CatalogRole:
    def __init__(self, name):
        self.name = name

class Catalog:
    def __init__(self, name, s3_location, role_arn):
        self.name = name
        self.s3_location = s3_location
        self.role_arn = role_arn

class Privileges:
    ADMIN = ['CATALOG_MANAGE_CONTENT']
    READER = [
        'TABLE_LIST', 'TABLE_READ_PROPERTIES', 'TABLE_READ_DATA',
        'VIEW_LIST', 'VIEW_READ_PROPERTIES',
        'NAMESPACE_READ_PROPERTIES', 'NAMESPACE_LIST'
    ]
    READ_ONLY = ['TABLE_LIST', 'TABLE_READ_DATA']
