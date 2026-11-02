# Frigus Core API + BFF on Amazon EKS

This directory deploys the Core API and BFF to one EKS cluster. The BFF is the only public service and is reached through an internet-facing Application Load Balancer (ALB). The Core API stays on a private Kubernetes `ClusterIP` and the BFF calls it inside the cluster.

## Before creating AWS resources

Choose the AWS account and Region, a domain for the BFF (for example, `api.example.com`), and an ACM certificate for that host in the same Region as the ALB. The example cluster and image paths use `sa-east-1`; change the Region consistently if you choose another one. EKS, worker nodes, NAT gateway, ECR storage, and the ALB incur AWS charges.

The cluster expects PostgreSQL, Redis, the Frigus AI API, and Supabase to be reachable at the URLs you configure. Keep PostgreSQL and Redis private or TLS protected, and restrict access to the application as appropriate. Core API startup runs Flyway migrations against the configured database, so back up the production database and review pending migrations before the first deployment.

## 1. Install tools on Windows

Install these tools and reopen PowerShell after installation:

- AWS CLI v2: [AWS CLI installation guide](https://docs.aws.amazon.com/cli/latest/userguide/getting-started-install.html)
- `kubectl`: [Amazon EKS kubectl setup](https://docs.aws.amazon.com/eks/latest/userguide/install-kubectl.html)
- `eksctl`: [official eksctl installation guide](https://eksctl.io/installation/)
- Helm: [Helm installation guide](https://helm.sh/docs/intro/install/)
- Docker Desktop: [Docker Desktop for Windows](https://docs.docker.com/desktop/setup/install/windows-install/)

Check that each command is available:

```powershell
aws --version
kubectl version --client
eksctl version
helm version
docker version
```

## 2. Sign in to AWS

Use your organization's IAM Identity Center (SSO) profile where available. Do not put AWS access keys in this repository.

```powershell
aws configure sso --profile frigus-prod
aws sso login --profile frigus-prod
$env:AWS_PROFILE = "frigus-prod"
$env:AWS_REGION = "sa-east-1"
aws sts get-caller-identity
```

Ask your AWS administrator for an identity with permissions to create EKS clusters and node groups, networking/CloudFormation resources, ECR repositories, IAM roles and policies, and the ALB controller service account. EKS cluster creation requires broad AWS permissions; use the organization's approved role and least privilege process.

## 3. Create the EKS cluster

Review `cluster.yaml` first. Change the cluster name, Region, node size, or node counts to match your account's capacity and budget. This configuration uses two private managed worker nodes and enables the cluster OIDC provider for IAM Roles for Service Accounts.

From the workspace root:

```powershell
eksctl create cluster --config-file frigus-core-api/deploy/k8s/cluster.yaml
aws eks update-kubeconfig --region $env:AWS_REGION --name frigus-prod
kubectl get nodes
```

If the cluster name or Region was changed, use those same values in the commands below.

## 4. Install the AWS Load Balancer Controller

The Ingress resource needs this controller to provision the ALB. Follow the current [AWS installation guide](https://docs.aws.amazon.com/eks/latest/userguide/lbc-helm.html). In short, download the controller IAM policy from its official release, create the IAM policy, associate the EKS OIDC provider, create the controller service account with that policy, and install the Helm chart. Example commands (use the release versions currently listed in the AWS guide):

```powershell
Invoke-WebRequest https://raw.githubusercontent.com/kubernetes-sigs/aws-load-balancer-controller/v2.14.1/docs/install/iam_policy.json -OutFile iam_policy.json
aws iam create-policy --policy-name AWSLoadBalancerControllerIAMPolicy --policy-document file://iam_policy.json
```

If that policy already exists, reuse its ARN. Then substitute your AWS account ID in the ARN:

```powershell
$AccountId = (aws sts get-caller-identity --query Account --output text).Trim()
$PolicyArn = "arn:aws:iam::${AccountId}:policy/AWSLoadBalancerControllerIAMPolicy"
eksctl create iamserviceaccount --cluster=frigus-prod --namespace=kube-system --name=aws-load-balancer-controller --attach-policy-arn=$PolicyArn --override-existing-serviceaccounts --region=$env:AWS_REGION --approve
helm repo add eks https://aws.github.io/eks-charts
helm repo update eks
helm install aws-load-balancer-controller eks/aws-load-balancer-controller --namespace kube-system --set clusterName=frigus-prod --set serviceAccount.create=false --set serviceAccount.name=aws-load-balancer-controller --version 1.14.0
kubectl get deployment -n kube-system aws-load-balancer-controller
```

Use the current chart and controller versions from AWS documentation when they differ from this example.

## 5. Create ECR repositories and push the images

Create the repositories once:

```powershell
$AccountId = (aws sts get-caller-identity --query Account --output text).Trim()
aws ecr create-repository --repository-name frigus-core-api --image-scanning-configuration scanOnPush=true --region $env:AWS_REGION
aws ecr create-repository --repository-name frigus-bff --image-scanning-configuration scanOnPush=true --region $env:AWS_REGION
```

For each release, use a unique tag (a Git commit SHA is preferable). Example using a timestamp:

```powershell
$Tag = Get-Date -Format "yyyyMMddHHmmss"
$Registry = "${AccountId}.dkr.ecr.${env:AWS_REGION}.amazonaws.com"
aws ecr get-login-password --region $env:AWS_REGION | docker login --username AWS --password-stdin $Registry
docker build -t "${Registry}/frigus-core-api:${Tag}" -f frigus-core-api/Dockerfile frigus-core-api
docker push "${Registry}/frigus-core-api:${Tag}"
docker build -t "${Registry}/frigus-bff:${Tag}" -f frigus-bff/Dockerfile frigus-bff
docker push "${Registry}/frigus-bff:${Tag}"
```

## 6. Set production values and secrets

Edit these files before applying:

1. In `kustomization.yaml`, replace `ACCOUNT_ID` and the Region in both `newName` values; set each `newTag` to the image tag pushed above. Use the same ECR account and Region as the cluster.
2. In `configmap.yaml` and `bff-configmap.yaml`, replace `https://app.seudominio.com` with the real frontend origin. Keep `WEB_ORIGINS` as a comma-separated list of trusted HTTPS origins. If you only have one frontend, use one origin.
3. In `ingress.yaml`, set the BFF API hostname and the ACM certificate ARN. The certificate must be issued in the cluster's Region and cover that hostname.
4. Copy `secrets.example.yaml` to `secrets.yaml`, fill in real production values, then apply that local file. The Core API needs PostgreSQL, Redis, JWT, and Frigus AI values; configure optional Brevo and Google values if those integrations are used. The BFF needs the Supabase URL and secret key for storage operations.

When building the separately hosted React frontend, set `VITE_API_URL` to `https://<BFF_HOST>` so its `/web` and `/mobile` requests go to this Ingress. Keep `WEB_ORIGINS` and the Core API allowed origins set to the frontend's origin.

`secrets.yaml` is excluded by `frigus-core-api/.gitignore`. Do not commit it or paste secret values into chat or source control. Kubernetes Secrets are base64 encoded in etcd; for a long-lived production environment, prefer AWS Secrets Manager with External Secrets Operator and encrypt EKS secrets at rest.

## 7. Deploy and connect DNS

From the workspace root:

```powershell
kubectl apply -f frigus-core-api/deploy/k8s/secrets.yaml
kubectl apply -k frigus-core-api/deploy/k8s
kubectl -n frigus rollout status deployment/core-api
kubectl -n frigus rollout status deployment/bff
kubectl -n frigus get ingress frigus-public
```

When the Ingress shows an ALB hostname, create a DNS alias/CNAME from the BFF hostname to that ALB. Then check `https://<BFF_HOST>/health`. The Core API is intentionally not exposed to the public internet.

Useful diagnostics:

```powershell
kubectl -n frigus get pods,services,ingress
kubectl -n frigus logs deployment/core-api
kubectl -n frigus logs deployment/bff
kubectl -n kube-system logs deployment/aws-load-balancer-controller
```

For a new image release, push both images with a new tag, update the two `newTag` values in `kustomization.yaml`, and run `kubectl apply -k frigus-core-api/deploy/k8s` again.
