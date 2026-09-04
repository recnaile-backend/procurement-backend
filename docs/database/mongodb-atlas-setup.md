# MongoDB Atlas Setup

This foundation uses **MongoDB Atlas** instead of a local MongoDB Docker container.

## Development

1. Create a MongoDB Atlas cluster.
2. Create a dedicated database user.
3. Add your development IP address to the Atlas Network Access list.
4. Copy the SRV connection string.
5. Put it in your local environment as `MONGODB_URI`.

Example:

```text
mongodb+srv://<username>:<password>@<cluster>.mongodb.net/<database>?retryWrites=true&w=majority
```

Do not commit the real connection string.

## Staging / Production

Use a separate Atlas project/cluster (or separately isolated databases according to your
organization's policy) for each environment. Prefer private connectivity from AWS (VPC
Peering or AWS PrivateLink) and restrict Atlas Network Access to the required AWS network.

Secrets should be stored in AWS Secrets Manager and synchronized into Kubernetes with
External Secrets Operator. No database password belongs in GitHub, Dockerfiles, Helm values,
or committed `.env` files.

## Important foundation scope

There are currently no business microservices in this foundation ZIP, so no application
component currently opens a MongoDB connection. `MONGODB_URI` is the standardized environment
contract for the business services that will be added later.
