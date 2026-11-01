# Deploy da Core API e do BFF na AWS (EKS)

O deploy publica os dois containers no ECR e atualiza os deployments no EKS. O BFF fica atrás de um Application Load Balancer (ALB) público; a Core API fica em um `ClusterIP` privado. O script de release é `core-api/deploy/aws/deploy.sh`.

## Infraestrutura e dependências

Antes do primeiro deploy, tenha:

- Conta AWS e permissões para EKS, IAM, ECR e Elastic Load Balancing.
- EKS na região escolhida, com nós capazes de baixar imagens do ECR. O exemplo em `cluster.yaml` cria `frigus-prod` em `sa-east-1` com dois nós `t3.medium`; revise capacidade e custo antes de criá-lo. Se mudar nome ou região, atualize `cluster.yaml` e `prod.env` para manter os valores iguais.
- AWS Load Balancer Controller instalado no cluster. Siga o [guia oficial da AWS](https://docs.aws.amazon.com/eks/latest/userguide/lbc-helm.html) e confirme que `deployment/aws-load-balancer-controller` está disponível em `kube-system`.
- Domínio para o BFF e certificado ACM emitido na mesma região do ALB.
- PostgreSQL, Redis, serviço Frigus AI e Supabase acessíveis a partir dos pods. Use TLS e restrinja os bancos à rede necessária.
- `aws`, `docker`, `kubectl`, `envsubst` e `git` instalados.

O cluster não é criado pelo script de release. Se ainda não existir, revise `cluster.yaml`, autentique com o perfil AWS correto e crie-o uma vez:

```bash
aws sso login --profile frigus-prod
export AWS_REGION=sa-east-1
eksctl create cluster --config-file core-api/deploy/k8s/cluster.yaml
```

O cluster de exemplo usa nós privados. A configuração do VPC precisa permitir que os nós obtenham imagens do ECR e alcancem as dependências externas, por NAT Gateway ou pelos VPC endpoints apropriados. A AWS lista os endpoints necessários para ECR em seu [guia de clusters privados](https://docs.aws.amazon.com/eks/latest/userguide/private-clusters.html).

## Preparar configurações

Na raiz do workspace, faça cópias locais dos exemplos:

```bash
cp -n core-api/.env.production.example core-api/.env.production
cp -n bff/.env.production.example bff/.env.production
cp -n core-api/deploy/aws/prod.env.example core-api/deploy/aws/prod.env
```

Preencha os arquivos `.env.production` com as URLs e credenciais reais. O arquivo da Core API requer PostgreSQL, Redis, JWT, Frigus AI e `AD_CLICK_REPORT_SECRET`; o do BFF requer URL e chave secreta do Supabase. Os dois arquivos são ignorados pelo Git.

Preencha `core-api/deploy/aws/prod.env` com:

- `AWS_PROFILE` do perfil SSO autenticado (o exemplo usa `frigus-prod`). Faça `aws sso login --profile frigus-prod` antes do deploy.
- `AWS_REGION` e `EKS_CLUSTER_NAME` iguais aos do cluster.
- `API_HOST` como hostname do BFF, sem `https://`.
- `ALB_CERTIFICATE_ARN` do certificado ACM para esse hostname, na mesma região.
- `FRONTEND_ORIGINS` com as origens web completas, separadas por vírgula.
- `APP_FRONTEND_URL` com a URL pública do frontend.
- `SECURITY_TRUSTED_PROXY_CIDRS` com os CIDRs reais das subnets do ALB. Esse valor permite à Core API interpretar `X-Forwarded-For` confiando apenas no ALB.

O script cria os repositórios ECR `frigus-core-api` e `frigus-bff` se estiverem ausentes. Ele constrói imagens `linux/amd64`, compatíveis com o node group `t3.medium`, e gera tags de release únicas; alterações locais não commitadas também entram na imagem e são marcadas como `dirty` na tag.

## Publicar

No Bash, da raiz do workspace:

```bash
set -a
source core-api/deploy/aws/prod.env
set +a
./core-api/deploy/aws/deploy.sh
```

O script verifica os arquivos e as chaves obrigatórias, identifica a conta AWS, confirma o cluster e o Load Balancer Controller, constrói e envia as duas imagens, atualiza os Secrets e ConfigMaps Kubernetes, aplica os manifests e aguarda os dois rollouts. Pode-se apontar para outro diretório do BFF com `BFF_DIR=/caminho/para/bff`.

Depois do primeiro deploy, aguarde o ALB aparecer e crie no DNS um alias para o hostname indicado em:

```bash
kubectl -n frigus get ingress frigus-public
```

Confira `https://<API_HOST>/health`. O endpoint de health verifica o processo do BFF; confira também os rollouts e logs dos dois serviços:

```bash
kubectl -n frigus get deployments,pods,services,ingress
kubectl -n frigus logs deployment/core-api
kubectl -n frigus logs deployment/bff
```

As migrações Flyway rodam no startup da Core API. Antes de usar um banco de produção, faça backup e revise as migrations pendentes. O exemplo mantém os valores secretos em Secrets do Kubernetes; para requisitos mais rigorosos, migre-os para AWS Secrets Manager com External Secrets Operator e configure criptografia em repouso no EKS.

## Novas versões

Repita a etapa **Publicar**. Cada execução cria tags novas para os dois containers, aplica as imagens juntas e aguarda os dois rollouts. Para fixar tags manualmente, defina `CORE_API_IMAGE_TAG` e `BFF_IMAGE_TAG` antes de executar o script.
