class Principal:
    """
    Represents a principal entity, which can be a service or other types.
    
    Attributes:
        name (str): The name of the principal.
        type (str): The type of the principal, default is 'SERVICE'.
    """
    def __init__(self, name, principal_type="SERVICE"):
        self.name = name
        self.type = principal_type

class PrincipalRole:
    """
    Represents a role that can be assigned to a principal.
    
    Attributes:
        name (str): The name of the principal role.
    """
    def __init__(self, name):
        self.name = name

class CatalogRole:
    """
    Represents a role within a catalog.
    
    Attributes:
        name (str): The name of the catalog role.
    """
    def __init__(self, name):
        self.name = name

class Catalog:
    """
    Represents a catalog with associated properties.
    
    Attributes:
        name (str): The name of the catalog.
        s3_location (str): The S3 location associated with the catalog.
        role_arn (str): The ARN of the role associated with the catalog.
    """
    def __init__(self, name, s3_location, role_arn):
        self.name = name
        self.s3_location = s3_location
        self.role_arn = role_arn

class Privileges:
    """
    Defines various privilege levels for roles.
    
    Constants:
        ADMIN (list): Privileges for admin roles.
        READER (list): Privileges for reader roles.
        READ_ONLY (list): Privileges for read-only roles.
    """
    ADMIN = ['CATALOG_MANAGE_CONTENT']
    READER = [
        'TABLE_LIST', 'TABLE_READ_PROPERTIES', 'TABLE_READ_DATA',
        'VIEW_LIST', 'VIEW_READ_PROPERTIES',
        'NAMESPACE_READ_PROPERTIES', 'NAMESPACE_LIST'
    ]
    READ_ONLY = ['TABLE_LIST', 'TABLE_READ_DATA']
