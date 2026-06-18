#!/bin/bash

# OrderFlow EC2 Setup Script
# Run this on your EC2 instance to set up Docker and deploy the application

set -e

echo "========================================="
echo "OrderFlow EC2 Setup Script"
echo "========================================="

# Update system packages
echo "Updating system packages..."
sudo apt-get update
sudo apt-get upgrade -y

# Install Docker Engine + Compose v2 plugin from Docker's official repository.
# Works on Ubuntu 22.04 (jammy) AND 24.04 (noble).
echo "Installing Docker Engine + Compose plugin..."
sudo apt-get install -y ca-certificates curl gnupg
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
  sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

sudo systemctl start docker
sudo systemctl enable docker

# Compatibility shim: make the legacy `docker-compose` command run Compose v2
# so every command in the docs (which use `docker-compose`) keeps working.
if ! command -v docker-compose >/dev/null 2>&1; then
  echo '#!/bin/bash' | sudo tee /usr/local/bin/docker-compose > /dev/null
  echo 'docker compose "$@"' | sudo tee -a /usr/local/bin/docker-compose > /dev/null
  sudo chmod +x /usr/local/bin/docker-compose
fi

# Add ubuntu user to docker group (to avoid sudo for docker commands)
echo "Configuring docker group..."
sudo usermod -aG docker ubuntu

# Install Git
echo "Installing Git..."
sudo apt-get install -y git

# Install Java 17 (in case you need it for other purposes)
echo "Installing Java 17..."
sudo apt-get install -y openjdk-17-jdk

# Create application directory
echo "Creating application directory..."
mkdir -p /home/ubuntu/orderflow
cd /home/ubuntu/orderflow

# Clone the repository (update URL if needed)
# echo "Cloning repository..."
# git clone <YOUR_REPO_URL> .

# Copy environment template
echo "Setting up environment..."
if [ ! -f .env ]; then
  cp .env.example .env
  echo "⚠️  Please edit .env with your EC2 IP and secrets!"
  echo "EC2_HOST should be your EC2 instance's public IP or domain"
fi

# Create required directories
echo "Creating infrastructure directories..."
mkdir -p infrastructure/postgres/init
mkdir -p services/{api-gateway,order-service,inventory-service,payment-service,notification-service,shipping-service,analytics-service}
mkdir -p frontend

# Build and start services (Compose v2 syntax)
echo "Starting Docker containers..."
sudo docker compose up -d --build

echo "========================================="
echo "Setup Complete!"
echo "========================================="
echo ""
echo "Services are starting. Check status with:"
echo "  docker-compose ps"
echo ""
echo "View logs with:"
echo "  docker-compose logs -f [service-name]"
echo ""
echo "Access the application at:"
echo "  Frontend: http://$EC2_HOST:3000"
echo "  API Gateway: http://$EC2_HOST:8080"
echo ""
echo "Stop all services with:"
echo "  docker-compose down"
echo ""
