# My App Helm Chart

This Helm chart deploys `polaris-catalog` on Kubernetes with AWS S3.

## Prerequisites

- Helm 3.x
- Kubernetes 1.16+

## Installing the Chart

To install the chart with the release name `polaris`:

```bash
export AWS_ACCESS_KEY_ID=your-access-key-id
export AWS_SECRET_ACCESS_KEY=your-secret-access-key

helm install polaris --set awsAccessKeyId=${AWS_ACCESS_KEY_ID} --set awsSecretAccessKey=${AWS_SECRET_ACCESS_KEY}
```

## Uninstalling the Chart
To uninstall/delete the polaris deployment:


```bash
helm uninstall polaris
```

## Configuration

The following table lists the configurable parameters of the chart and their default values:

| Parameter                    | Description                               | Default                         |
|------------------------------|-------------------------------------------|---------------------------------|
| `awsAccessKeyId`             | AWS Access Key ID                         | `""`                            |
| `awsSecretAccessKey`         | AWS Secret Access Key                     | `""`                            |
| `awsRegion`                  | AWS Region                                | `"us-east-2"`                   |
| `image.repository`           | Image repository                          | `fabianofpena/polaris-polaris`  |
| `image.tag`                  | Image tag                                 | `1.0`                           |
| `image.pullPolicy`           | Image pull policy                         | `IfNotPresent`                  |
| `replicaCount`               | Number of replicas                        | `1`                             |
| `app.name`                   | Name of the application                   | `polaris`                       |
| `app.namespace`              | Kubernetes namespace for the application  | `metastore`                     |
| `service.type`               | Kubernetes service type                   | `ClusterIP`                     |
| `service.ports`              | List of service ports                     | `[{http: 8181, admin: 8182}]`   |
| `resources.requests.memory`  | Memory resource requests                  | `128Mi`                         |
| `resources.requests.cpu`     | CPU resource requests                     | `250m`                          |
| `resources.limits.memory`    | Memory resource limits                    | `256Mi`                         |
| `resources.limits.cpu`       | CPU resource limits                       | `500m`                          |

### Summary

- **Helm Chart Metadata**: Defined in `Chart.yaml`.
- **Values File**: Default configurations provided in `values.yaml`.
- **Templates**: Two templates for creating the Kubernetes `Secret` and `Deployment` resources.
- **README**: Detailed instructions on how to use and configure the Helm chart.

This structure ensures that secrets are managed securely and the deployment is flexible, following best practices.






