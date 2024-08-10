import pyspark
from pyspark.sql import SparkSession

POLARIS_URI = 'http://localhost:8181/api/catalog'
POLARIS_CATALOG_NAME = 'worldstrides'
POLARIS_CREDENTIALS = 'dcde51a83ad6f540:43dc23f154564d628af54bbfc7d9d786'
POLARIS_SCOPE = 'PRINCIPAL_ROLE:ALL'
AWS_REGION = 'us-east-1'

conf = (
    pyspark.SparkConf()
        .setAppName('app_name')
        # SQL Extensions
        .set('spark.sql.catalog.spark_catalog', 'org.apache.iceberg.spark.SparkSessionCatalog')
        .set('spark.sql.extensions', 'org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions')
        # Configuring Catalog
        .set('spark.sql.catalog.polaris', 'org.apache.iceberg.spark.SparkCatalog')
        .set('spark.sql.catalog.polaris.type', 'rest')
        .set('spark.sql.catalog.polaris.uri', POLARIS_URI)
        .set('spark.sql.catalog.polaris.token-refresh-enabled', 'true')
        .set('spark.sql.catalog.polaris.credential', POLARIS_CREDENTIALS)
        .set('spark.sql.catalog.polaris.warehouse', POLARIS_CATALOG_NAME)
        .set('spark.sql.catalog.polaris.scope', POLARIS_SCOPE)
        .set('spark.sql.catalog.polaris.header.X-Iceberg-Access-Delegation', 'true')
        .set('spark.sql.catalog.polaris.io-impl', 'org.apache.iceberg.io.ResolvingFileIO')
        .set('spark.sql.catalog.polaris.s3.region', AWS_REGION)
)

## Start Spark Session
spark = SparkSession.builder.config(conf=conf).getOrCreate()
print("Spark Running")

## Run a Query
spark.sql("CREATE NAMESPACE IF NOT EXISTS polaris.bronze").show()
spark.sql("CREATE TABLE IF NOT EXISTS polaris.bronze.table2 (id INT, name STRING) USING iceberg").show()
spark.sql("INSERT INTO polaris.bronze.table2 VALUES (1, 'Fabiano Pena'), (2, 'John Doe')").show()
spark.sql("SELECT * FROM polaris.bronze.table2").show()

## Stop Spark Session
spark.stop()
print("Spark Session Stopped")
