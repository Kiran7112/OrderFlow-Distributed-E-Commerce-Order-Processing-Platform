.PHONY: help build build-module build-docker deploy-ec2 up down logs logs-service ps clean test

help:
	@echo "OrderFlow - Microservices E-Commerce Platform"
	@echo ""
	@echo "Available commands:"
	@echo "  make build          - Build ALL services via the Maven reactor"
	@echo "  make build-module   - Build one module (e.g., make build-module MODULE=order-service)"
	@echo "  make build-docker   - Build Docker images"
	@echo "  make up             - Start all services with Docker Compose"
	@echo "  make down           - Stop all services"
	@echo "  make logs           - View logs from all services"
	@echo "  make logs-service   - View logs from specific service (e.g., make logs-service SERVICE=order-service)"
	@echo "  make deploy-ec2     - Deploy to EC2 (requires EC2_HOST and SSH access)"
	@echo "  make clean          - Remove all built artifacts and stop containers"
	@echo "  make ps             - Show running containers"
	@echo ""

build:
	@echo "Building all services via the Maven reactor (one command)..."
	cd services && mvn clean package -DskipTests

build-module:
	@echo "Building a single module: $(MODULE)"
	cd services && mvn -pl $(MODULE) -am clean package -DskipTests

build-docker:
	@echo "Building Docker images..."
	docker-compose build

up:
	@echo "Starting OrderFlow services..."
	docker-compose up -d
	@echo "Waiting for services to start..."
	@sleep 5
	docker-compose ps

down:
	@echo "Stopping OrderFlow services..."
	docker-compose down

logs:
	docker-compose logs -f

logs-service:
	docker-compose logs -f $(SERVICE)

ps:
	docker-compose ps

clean:
	@echo "Cleaning up..."
	docker-compose down -v
	find services -name target -type d -exec rm -rf {} +
	rm -f .env

deploy-ec2:
	@echo "Deploying to EC2..."
	@if [ -z "$(EC2_HOST)" ]; then \
		echo "Error: EC2_HOST not set. Usage: make deploy-ec2 EC2_HOST=your-ec2-ip"; \
		exit 1; \
	fi
	@echo "Copying files to EC2..."
	scp -r docker-compose.yml .env.example ec2-setup.sh infrastructure/ services/ frontend/ ubuntu@$(EC2_HOST):/home/ubuntu/orderflow/
	@echo "Running setup script on EC2..."
	ssh ubuntu@$(EC2_HOST) 'cd /home/ubuntu/orderflow && bash ec2-setup.sh'
	@echo "Deployment complete!"

test:
	@echo "Running all tests via the Maven reactor..."
	cd services && mvn test

.DEFAULT_GOAL := help
