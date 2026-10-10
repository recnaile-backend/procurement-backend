.PHONY: help local-up local-down local-logs kafka-topics build lint detekt \
        verify-no-rabbitmq verify-no-business-logic gateway-image helm-lint \
        terraform-fmt terraform-validate

help:
	@echo "Procurement & Inventory Platform — common commands"
	@echo ""
	@echo "  make local-up                  Start local dev infrastructure (docker compose)"
	@echo "  make local-down                Stop and remove local dev infrastructure"
	@echo "  make local-logs                Tail logs from local dev infrastructure"
	@echo "  make kafka-topics              Bootstrap local Kafka topics"
	@echo "  make build                     Compile shared libs + gateway"
	@echo "  make lint                      Run ktlint"
	@echo "  make detekt                    Run detekt static analysis"
	@echo "  make gateway-image             Build the API Gateway Docker image locally"
	@echo "  make helm-lint                 Lint the reusable service Helm chart"
	@echo "  make terraform-fmt             Format all Terraform files"
	@echo "  make terraform-validate        Validate all Terraform environments"
	@echo "  make verify-no-rabbitmq        Guard: fail if RabbitMQ references exist"
	@echo "  make verify-no-business-logic  Guard: fail if the 12 service folders contain code"

local-up:
	docker compose up -d

local-down:
	docker compose down -v

local-logs:
	docker compose logs -f

kafka-topics:
	bash infrastructure/kafka/local-topics-init.sh

build:
	./gradlew assemble --no-daemon

lint:
	./gradlew ktlintCheck --no-daemon

detekt:
	./gradlew detekt --no-daemon

gateway-image:
	docker build -f gateway/api-gateway/Dockerfile -t api-gateway:local .

helm-lint:
	helm lint infrastructure/helm/procurement-service-chart

terraform-fmt:
	terraform fmt -recursive infrastructure/terraform

terraform-validate:
	@for env in dev staging production; do \
		echo "== $$env =="; \
		(cd infrastructure/terraform/environments/$$env && terraform init -backend=false -input=false >/dev/null && terraform validate); \
	done

verify-no-rabbitmq:
	bash scripts/verify-no-rabbitmq.sh

verify-no-business-logic:
	bash scripts/verify-no-business-logic-in-services.sh
