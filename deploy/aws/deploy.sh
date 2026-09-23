#!/usr/bin/env bash

set -Eeuo pipefail

ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"

AWS_REGION="${AWS_REGION:-us-east-1}"
EKS_CLUSTER_NAME="${EKS_CLUSTER_NAME:-frigus-cluster}"
ECR_REPOSITORY="${ECR_REPOSITORY:-frigus-core-api}"
KUBE_NAMESPACE="${KUBE_NAMESPACE:-frigus}"
DEFAULT_IMAGE_TAG="$(git -C "$ROOT_DIR" rev-parse --short HEAD)"
if [[ -n "$(git -C "$ROOT_DIR" status --porcelain)" ]]; then
  DEFAULT_IMAGE_TAG="${DEFAULT_IMAGE_TAG}-dirty-$(date -u +%Y%m%d%H%M%S)"
fi
IMAGE_TAG="${IMAGE_TAG:-$DEFAULT_IMAGE_TAG}"

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Erro: comando obrigatório não encontrado: $1" >&2
    exit 1
  }
}

for command_name in aws docker kubectl git; do
  require_command "$command_name"
done

if [[ ! -f "$ROOT_DIR/.env" ]]; then
  echo "Erro: crie o arquivo .env local antes do deploy." >&2
  exit 1
fi

echo "Validando credenciais AWS..."
AWS_ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
ECR_REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
IMAGE_URI="${ECR_REGISTRY}/${ECR_REPOSITORY}:${IMAGE_TAG}"

echo "Atualizando kubeconfig para o cluster ${EKS_CLUSTER_NAME}..."
aws eks update-kubeconfig \
  --region "$AWS_REGION" \
  --name "$EKS_CLUSTER_NAME" >/dev/null

echo "Construindo ${IMAGE_URI}..."
docker build -t "$IMAGE_URI" "$ROOT_DIR"

echo "Autenticando no ECR..."
aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "$ECR_REGISTRY" >/dev/null

echo "Enviando imagem para o ECR..."
docker push "$IMAGE_URI"

echo "Aplicando namespace e configuração..."
kubectl apply -f "$ROOT_DIR/deploy/k8s/namespace.yaml"
kubectl -n "$KUBE_NAMESPACE" apply -f "$ROOT_DIR/deploy/k8s/configmap.yaml"

echo "Atualizando Secret do Kubernetes a partir do .env..."
secret_arguments=()
for secret_key in DATABASE_URL DATABASE_USERNAME DATABASE_PASSWORD REDIS_URL JWT_SECRET JWT_EXPIRATION; do
  secret_value="$(awk -F= -v key="$secret_key" '$1 == key { sub(/^[^=]*=/, ""); print; exit }' "$ROOT_DIR/.env")"
  if [[ -z "$secret_value" ]]; then
    echo "Erro: variável obrigatória ausente no .env: $secret_key" >&2
    exit 1
  fi
  secret_arguments+=("--from-literal=${secret_key}=${secret_value}")
done

kubectl -n "$KUBE_NAMESPACE" create secret generic core-api-secrets \
  "${secret_arguments[@]}" \
  --dry-run=client \
  --output=yaml \
  | kubectl apply -f -

echo "Aplicando Deployment e Load Balancer..."
kubectl -n "$KUBE_NAMESPACE" apply -f "$ROOT_DIR/deploy/k8s/deployment.yaml"
kubectl -n "$KUBE_NAMESPACE" apply -f "$ROOT_DIR/deploy/k8s/service.yaml"
kubectl -n "$KUBE_NAMESPACE" set image deployment/core-api \
  "core-api=${IMAGE_URI}"

echo "Aguardando a API ficar saudável..."
kubectl -n "$KUBE_NAMESPACE" rollout status deployment/core-api --timeout=180s

echo "Deploy concluído. Serviço atual:"
kubectl -n "$KUBE_NAMESPACE" get service core-api -o wide
