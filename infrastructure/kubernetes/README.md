# Kubernetes deployment

1. Create the namespace and config:
   `kubectl apply -f namespace.yaml -f configmap.yaml`

2. Create the Atlas secret from a secure shell/CI secret store:
   `kubectl -n procurement create secret generic mongodb-atlas-secret --from-literal=MONGODB_BASE_URI="mongodb+srv://<db_username>:<db_password>@recnaile.x5w2qyy.mongodb.net"`

3. Build and push the gateway and 12 service images using your registry, then update the `image:` values in `services.yaml`.

4. Deploy:
   `kubectl apply -k .`

5. Configure your ingress/DNS host in `ingress.yaml`.

Never commit the real Atlas password to Git or Kubernetes manifests. Rotate the credential if it has been exposed outside the intended secret store.
