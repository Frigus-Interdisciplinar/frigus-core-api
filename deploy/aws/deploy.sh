#!/usr/bin/env bash

set -Eeuo pipefail

CORE_API_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
WORKSPACE_DIR="$(cd -- "$CORE_API_DIR/.." && pwd)"
BFF_DIR="${BFF_DIR:-$WORKSPACE_DIR/bff}"
K8S_DIR="$CORE_API_DIR/deploy/k8s"

AWS_REGION="${AWS_REGION:-us-east-1}"
EKS_CLUSTER_NAME="${EKS_CLUSTER_NAME:-frigus-prod}"
KUBE_NAMESPACE="${KUBE_NAMESPACE:-frigus}"
CORE_API_REPOSITORY="${CORE_API_REPOSITORY:-frigus-core-api}"
BFF_REPOSITORY="${BFF_REPOSITORY:-frigus-bff}"
CORE_API_SECRETS_FILE="${CORE_API_SECRETS_FILE:-$CORE_API_DIR/.env.production}"
BFF_SECRETS_FILE="${BFF_SECRETS_FILE:-$BFF_DIR/.env.production}"

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Erro: comando obrigatório não encontrado: $1" >&2
    exit 1
  }
}

for command_name in aws docker kubectl envsubst git; do
  require_command "$command_name"
done

for required_variable in API_HOST ALB_CERTIFICATE_ARN FRONTEND_ORIGINS APP_FRONTEND_URL SECURITY_TRUSTED_PROXY_CIDRS; do
  if [[ -z "${!required_variable:-}" ]]; then
    echo "Erro: defina a variável de ambiente $required_variable." >&2
    exit 1
  fi
done

if [[ ! -d "$BFF_DIR" || ! -f "$BFF_DIR/Dockerfile" ]]; then
  echo "Erro: BFF não encontrado em $BFF_DIR. Defina BFF_DIR se estiver em outro caminho." >&2
  exit 1
fi

for secrets_file in "$CORE_API_SECRETS_FILE" "$BFF_SECRETS_FILE"; do
  if [[ ! -f "$secrets_file" ]]; then
    echo "Erro: arquivo de secrets não encontrado: $secrets_file" >&2
    echo "Crie-o a partir dos arquivos .env.production.example correspondentes." >&2
    exit 1
  fi
done

validate_secret_file() {
  local file="$1"
  shift
  local key
  for key in "$@"; do
    if ! awk -F= -v key="$key" '$1 == key && length(substr($0, index($0, "=") + 1)) > 0 { found=1 } END { exit !found }' "$file"; then
      echo "Erro: $key ausente ou vazio em $file" >&2
      exit 1
    fi
  done
}

validate_secret_file "$CORE_API_SECRETS_FILE" \
  DATABASE_URL DATABASE_USERNAME DATABASE_PASSWORD REDIS_URL JWT_SECRET \
  FRIGUS_AI_BASE_URL FRIGUS_AI_API_KEY AD_CLICK_REPORT_SECRET
validate_secret_file "$BFF_SECRETS_FILE" SUPABASE_URL SUPABASE_SECRET_KEY

if [[ "$API_HOST" == *"/"* || "$API_HOST" == *":"* || "$API_HOST" == *" "* ]]; then
  echo "Erro: API_HOST deve conter somente o hostname, por exemplo api.example.com." >&2
  exit 1
fi

if [[ "$ALB_CERTIFICATE_ARN" != arn:aws:acm:"$AWS_REGION":*:certificate/* ]]; then
  echo "Erro: ALB_CERTIFICATE_ARN precisa ser um ARN ACM da região $AWS_REGION." >&2
  exit 1
fi

image_tag_for() {
  local repo="$1"
  local tag
  tag="${RELEASE_ID}-$(git -C "$repo" rev-parse --short HEAD)"
  if [[ -n "$(git -C "$repo" status --porcelain)" ]]; then
    tag="${tag}-dirty"
  fi
  printf '%s' "$tag"
}

RELEASE_ID="${RELEASE_ID:-$(date -u +%Y%m%d%H%M%S)}"
CORE_API_TAG="${CORE_API_IMAGE_TAG:-$(image_tag_for "$CORE_API_DIR")}"
BFF_TAG="${BFF_IMAGE_TAG:-$(image_tag_for "$BFF_DIR")}"

echo "Validando conta AWS e cluster EKS..."
AWS_ACCOUNT_ID="$(aws sts get-caller-identity --query Account --output text)"
aws eks describe-cluster --region "$AWS_REGION" --name "$EKS_CLUSTER_NAME" >/dev/null
ECR_REGISTRY="${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"

aws eks update-kubeconfig --region "$AWS_REGION" --name "$EKS_CLUSTER_NAME" >/dev/null
if ! kubectl -n kube-system get deployment aws-load-balancer-controller >/dev/null 2>&1; then
  echo "Erro: AWS Load Balancer Controller não está instalado no cluster $EKS_CLUSTER_NAME." >&2
  echo "Instale-o conforme o guia em deploy/k8s/README.md antes do primeiro deploy." >&2
  exit 1
fi

ensure_ecr_repository() {
  local repository="$1"
  if ! aws ecr describe-repositories --region "$AWS_REGION" --repository-names "$repository" >/dev/null 2>&1; then
    aws ecr create-repository \
      --region "$AWS_REGION" \
      --repository-name "$repository" \
      --image-tag-mutability IMMUTABLE \
      --image-scanning-configuration scanOnPush=true >/dev/null
    echo "Criado ECR: $repository"
  fi
}

ensure_ecr_repository "$CORE_API_REPOSITORY"
ensure_ecr_repository "$BFF_REPOSITORY"

echo "Autenticando no ECR..."
aws ecr get-login-password --region "$AWS_REGION" \
  | docker login --username AWS --password-stdin "$ECR_REGISTRY" >/dev/null

CORE_API_IMAGE="$ECR_REGISTRY/$CORE_API_REPOSITORY:$CORE_API_TAG"
BFF_IMAGE="$ECR_REGISTRY/$BFF_REPOSITORY:$BFF_TAG"

echo "Construindo e enviando Core API: $CORE_API_IMAGE"
docker build --platform linux/amd64 --pull -t "$CORE_API_IMAGE" "$CORE_API_DIR"
docker push "$CORE_API_IMAGE"

echo "Construindo e enviando BFF: $BFF_IMAGE"
docker build --platform linux/amd64 --pull -t "$BFF_IMAGE" "$BFF_DIR"
docker push "$BFF_IMAGE"

export AWS_REGION API_HOST ALB_CERTIFICATE_ARN FRONTEND_ORIGINS APP_FRONTEND_URL SECURITY_TRUSTED_PROXY_CIDRS

echo "Aplicando namespace e configurações..."
kubectl apply -f "$K8S_DIR/namespace.yaml"
envsubst '${AWS_REGION} ${API_HOST} ${FRONTEND_ORIGINS} ${APP_FRONTEND_URL} ${SECURITY_TRUSTED_PROXY_CIDRS}' \
  < "$K8S_DIR/configmap.yaml" | kubectl -n "$KUBE_NAMESPACE" apply -f -
envsubst '${FRONTEND_ORIGINS}' \
  < "$K8S_DIR/bff-configmap.yaml" | kubectl -n "$KUBE_NAMESPACE" apply -f -

echo "Atualizando secrets Kubernetes a partir dos arquivos locais..."
kubectl -n "$KUBE_NAMESPACE" create secret generic core-api-secrets \
  --from-env-file="$CORE_API_SECRETS_FILE" \
  --dry-run=client --output=yaml | kubectl apply -f -
kubectl -n "$KUBE_NAMESPACE" create secret generic bff-secrets \
  --from-env-file="$BFF_SECRETS_FILE" \
  --dry-run=client --output=yaml | kubectl apply -f -

echo "Aplicando services, deployments, PDBs e Ingress..."
kubectl apply -f "$K8S_DIR/deployment.yaml"
kubectl apply -f "$K8S_DIR/service.yaml"
kubectl apply -f "$K8S_DIR/bff-deployment.yaml"
kubectl apply -f "$K8S_DIR/bff-service.yaml"
kubectl apply -f "$K8S_DIR/pdb.yaml"
envsubst '${API_HOST} ${ALB_CERTIFICATE_ARN}' \
  < "$K8S_DIR/ingress.yaml" | kubectl -n "$KUBE_NAMESPACE" apply -f -

kubectl -n "$KUBE_NAMESPACE" set image deployment/core-api "core-api=$CORE_API_IMAGE"
kubectl -n "$KUBE_NAMESPACE" set image deployment/bff "bff=$BFF_IMAGE"

echo "Aguardando Core API e BFF ficarem saudáveis..."
kubectl -n "$KUBE_NAMESPACE" rollout status deployment/core-api --timeout=300s
kubectl -n "$KUBE_NAMESPACE" rollout status deployment/bff --timeout=180s

echo "Deploy concluído. Recursos publicados:"
kubectl -n "$KUBE_NAMESPACE" get deployments,pods,services,ingress
