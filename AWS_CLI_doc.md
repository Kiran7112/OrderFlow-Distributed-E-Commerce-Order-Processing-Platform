# OrderFlow — AWS Setup Guide (AWS CLI, Step by Step)

This guide does the **exact same thing** as `AWS_UI_Doc.md` — provisions one EC2 server and runs the full OrderFlow stack — but **entirely from the command line** using the **AWS CLI**. Every command is given with its demo output so you know what success looks like.

> **Why CLI?** It's scriptable, repeatable, and faster once set up. If you prefer clicking in the browser, use `AWS_UI_Doc.md` instead.

> **What we're doing:** Renting one Linux server on AWS (EC2), copying the project onto it, and running all 13 containers (Kafka, PostgreSQL, Redis, 7 microservices, React frontend) with one command. You only need **Java locally** to inspect code — AWS runs everything else.

> **About the code:** The backend is a **Maven multi-module reactor** (one parent POM in `services/` builds all microservices). Compilation happens inside Docker during `ec2-setup.sh`; you don't build it manually. Copy the whole project folder as-is — nothing in this guide changes because of the modular structure.

---

## Table of Contents

1. [What you'll need](#0-what-youll-need)
2. [Install the AWS CLI](#1-install-the-aws-cli)
3. [Create an access key & configure the CLI](#2-create-an-access-key--configure-the-cli)
4. [Set reusable variables](#3-set-reusable-shell-variables)
5. [Create a key pair](#4-create-a-key-pair)
6. [Create a security group + rules](#5-create-a-security-group--rules)
7. [Find the Ubuntu AMI](#6-find-the-ubuntu-22.04-ami)
8. [Launch the EC2 instance](#7-launch-the-ec2-instance)
9. [Get the public IP](#8-get-the-public-ip)
10. [Copy the project to EC2](#9-copy-the-project-to-ec2)
11. [Configure and run the project](#10-configure-and-run-the-project)
12. [Verify everything works](#11-verify-everything-works)
13. [Open the app](#12-open-the-app-in-your-browser)
14. [One-shot script](#13-one-shot-provisioning-script)
15. [Cleanup (stop paying)](#14-cleanup--stop-paying)
16. [Troubleshooting](#15-troubleshooting)

---

## 0. What You'll Need

| Item | Notes |
|---|---|
| An AWS account | If you don't have one, create it via `AWS_UI_Doc.md` Section 1 |
| A terminal | Git Bash on Windows, or Terminal on Mac/Linux |
| Card on file with AWS | Required by AWS; usage here costs ~$2/day if you forget to stop it |

> 💲 **Cost note:** `t3.large` ≈ **$0.08/hour**. Run the **cleanup** in Section 14 when done. Don't use `t2.micro` — too small (needs 8 GB RAM).

All commands below use region **`us-east-1`**. Change it everywhere if you prefer another region.

---

## 1. Install the AWS CLI

### Windows
Download and run the installer: **https://awscli.amazonaws.com/AWSCLIV2.msi**

### Mac
```bash
curl "https://awscli.amazonaws.com/AWSCLIV2.pkg" -o "AWSCLIV2.pkg"
sudo installer -pkg AWSCLIV2.pkg -target /
```

### Linux
```bash
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip && sudo ./aws/install
```

**Verify the install:**
```bash
aws --version
```
✅ **Demo output:**
```
aws-cli/2.15.30 Python/3.11.8 Windows/10 exe/AMD64
```

---

## 2. Create an Access Key & Configure the CLI

The CLI needs credentials. Create an access key in the console **once**:

1. Console → search **IAM** → **Users** → your user (or create one with `AdministratorAccess`) → **Security credentials** tab → **Create access key** → choose **Command Line Interface (CLI)** → copy the **Access key ID** and **Secret access key**.

   > For a quick demo you can use the root account's access key, but best practice is an IAM user with `AmazonEC2FullAccess`.

2. Configure the CLI:
   ```bash
   aws configure
   ```
   Fill in the prompts:
   ```
   AWS Access Key ID [None]: AKIAIOSFODNN7EXAMPLE
   AWS Secret Access Key [None]: wJalrXUtnFEMI/K7MDENG/bPxRfiCYEXAMPLEKEY
   Default region name [None]: us-east-1
   Default output format [None]: json
   ```

3. Test that it works:
   ```bash
   aws sts get-caller-identity
   ```
   ✅ **Demo output:**
   ```json
   {
     "UserId": "AIDAEXAMPLE...",
     "Account": "123456789012",
     "Arn": "arn:aws:iam::123456789012:user/orderflow-admin"
   }
   ```

---

## 3. Set Reusable Shell Variables

Setting these once keeps the rest of the commands clean. Run in **Git Bash / Mac / Linux**:

```bash
export AWS_REGION="us-east-1"
export KEY_NAME="orderflow-key"
export SG_NAME="orderflow-sg"
export INSTANCE_NAME="orderflow-server"
export INSTANCE_TYPE="t3.large"
export PROJECT_DIR="C:/Users/kiranmaddireddy/Downloads/ALL_IN_ONE/Ecommerce_Order_Mangement_System"
```

> **Windows PowerShell** users: replace `export X="Y"` with `$X="Y"` and use `$X` instead of `$X` in later commands (PowerShell syntax differs). Git Bash is recommended so the commands below work as-is.

---

## 4. Create a Key Pair

This creates the private key file used to SSH into the server.

```bash
aws ec2 create-key-pair \
  --region "$AWS_REGION" \
  --key-name "$KEY_NAME" \
  --query 'KeyMaterial' \
  --output text > "$HOME/$KEY_NAME.pem"

chmod 400 "$HOME/$KEY_NAME.pem"
```

✅ **Demo output:** No console output, but a file `~/orderflow-key.pem` now exists. Verify:
```bash
aws ec2 describe-key-pairs --key-names "$KEY_NAME" --region "$AWS_REGION" \
  --query 'KeyPairs[0].KeyName' --output text
```
```
orderflow-key
```

> ⚠️ Keep `~/orderflow-key.pem` safe — it's your only way in. AWS does not store the private key.

---

## 5. Create a Security Group + Rules

A security group is the firewall. Create it, then open the ports.

```bash
# Create the security group, capture its ID
export SG_ID=$(aws ec2 create-security-group \
  --region "$AWS_REGION" \
  --group-name "$SG_NAME" \
  --description "OrderFlow ports" \
  --query 'GroupId' --output text)

echo "Security Group ID: $SG_ID"
```
✅ **Demo output:**
```
Security Group ID: sg-0a1b2c3d4e5f6a7b8
```

Find your own public IP (so SSH is restricted to you):
```bash
export MY_IP=$(curl -s https://checkip.amazonaws.com)
echo "My IP: $MY_IP"
```
✅ **Demo output:**
```
My IP: 203.0.113.42
```

Now add the inbound rules:
```bash
# SSH (port 22) — only from your IP
aws ec2 authorize-security-group-ingress --region "$AWS_REGION" \
  --group-id "$SG_ID" --protocol tcp --port 22 --cidr "${MY_IP}/32"

# React frontend (3000) — open to anyone
aws ec2 authorize-security-group-ingress --region "$AWS_REGION" \
  --group-id "$SG_ID" --protocol tcp --port 3000 --cidr 0.0.0.0/0

# API Gateway (8080) — open to anyone
aws ec2 authorize-security-group-ingress --region "$AWS_REGION" \
  --group-id "$SG_ID" --protocol tcp --port 8080 --cidr 0.0.0.0/0

# Microservices (8081-8086) — optional, for debugging
aws ec2 authorize-security-group-ingress --region "$AWS_REGION" \
  --group-id "$SG_ID" --protocol tcp --port 8081-8086 --cidr 0.0.0.0/0
```
✅ **Demo output** (each command returns):
```json
{
    "Return": true,
    "SecurityGroupRules": [ { "SecurityGroupRuleId": "sgr-0abc...", "IsEgress": false, "FromPort": 22, ... } ]
}
```

---

## 6. Find the Ubuntu 22.04 AMI

AMI IDs differ per region. Look up the latest official Canonical Ubuntu 22.04 image:

```bash
export AMI_ID=$(aws ec2 describe-images \
  --region "$AWS_REGION" \
  --owners 099720109477 \
  --filters "Name=name,Values=ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*" \
            "Name=state,Values=available" \
  --query 'sort_by(Images, &CreationDate)[-1].ImageId' \
  --output text)

echo "AMI ID: $AMI_ID"
```
✅ **Demo output:**
```
AMI ID: ami-0c7217cdde317cfec
```
> `099720109477` is Canonical's official AWS account ID — this guarantees a trusted Ubuntu image.
> This filter pulls **Ubuntu 22.04 (jammy)**. To use **24.04 (noble)** instead, change the
> `Values=` pattern to `ubuntu/images/hvm-ssd/ubuntu-noble-24.04-amd64-server-*`. Both work
> equally well — the setup script installs Docker correctly on either. Never use a
> "SQL Server"/"Windows" image (license isn't allowed on `t3.large`).

---

## 7. Launch the EC2 Instance

```bash
export INSTANCE_ID=$(aws ec2 run-instances \
  --region "$AWS_REGION" \
  --image-id "$AMI_ID" \
  --instance-type "$INSTANCE_TYPE" \
  --key-name "$KEY_NAME" \
  --security-group-ids "$SG_ID" \
  --block-device-mappings '[{"DeviceName":"/dev/sda1","Ebs":{"VolumeSize":30,"VolumeType":"gp3"}}]' \
  --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=$INSTANCE_NAME}]" \
  --query 'Instances[0].InstanceId' --output text)

echo "Instance ID: $INSTANCE_ID"
```
✅ **Demo output:**
```
Instance ID: i-0123456789abcdef0
```

Wait until it's running and passes status checks:
```bash
echo "Waiting for instance to be running..."
aws ec2 wait instance-running --region "$AWS_REGION" --instance-ids "$INSTANCE_ID"
echo "Instance is running. Waiting for status checks (this takes ~2 min)..."
aws ec2 wait instance-status-ok --region "$AWS_REGION" --instance-ids "$INSTANCE_ID"
echo "Ready!"
```
✅ **Demo output:**
```
Waiting for instance to be running...
Instance is running. Waiting for status checks (this takes ~2 min)...
Ready!
```

---

## 8. Get the Public IP

```bash
export EC2_IP=$(aws ec2 describe-instances \
  --region "$AWS_REGION" \
  --instance-ids "$INSTANCE_ID" \
  --query 'Reservations[0].Instances[0].PublicIpAddress' \
  --output text)

echo "EC2 Public IP: $EC2_IP"
```
✅ **Demo output:**
```
EC2 Public IP: 54.123.45.67
```

---

## 9. Copy the Project to EC2

```bash
scp -i "$HOME/$KEY_NAME.pem" -r \
  "$PROJECT_DIR" \
  ubuntu@"$EC2_IP":/home/ubuntu/orderflow
```
> If asked to trust the host the first time, type `yes`.

✅ **Demo output:**
```
The authenticity of host '54.123.45.67' can't be established...
Are you sure you want to continue connecting (yes/no)? yes
docker-compose.yml              100%   8KB  120.5KB/s   00:00
OrderServiceApplication.java    100%  412   45.1KB/s    00:00
pom.xml                         100%  3KB   210.0KB/s   00:00
...
```

---

## 10. Configure and Run the Project

SSH into the server:
```bash
ssh -i "$HOME/$KEY_NAME.pem" ubuntu@"$EC2_IP"
```
✅ **Demo output:**
```
Welcome to Ubuntu 22.04.4 LTS (GNU/Linux ...)
ubuntu@ip-172-31-x-x:~$
```

Now **on the server**, configure and launch:
```bash
cd /home/ubuntu/orderflow
cp .env.example .env
nano .env
```

Set these three values (use your real public IP from Section 8):
```
EC2_HOST=54.123.45.67
DB_PASSWORD=MyStrongPassword123
JWT_SECRET=change-this-to-a-long-random-string-at-least-32-chars
```
Save & exit nano: **Ctrl+O**, **Enter**, **Ctrl+X**.

Run the setup script (installs Docker + Compose, builds & starts all 13 containers):
```bash
bash ec2-setup.sh
```
✅ **Demo output (abbreviated):**
```
=========================================
OrderFlow EC2 Setup Script
=========================================
Updating system packages...
Installing Docker...
Installing Docker Compose...
...
Starting Docker containers...
[+] Building 320.5s (84/84) FINISHED
[+] Running 13/13
 ✔ Container zookeeper             Started
 ✔ Container kafka                 Started
 ✔ Container kafka-init            Started
 ✔ Container postgres              Started
 ✔ Container redis                 Started
 ✔ Container api-gateway           Started
 ✔ Container order-service         Started
 ✔ Container inventory-service     Started
 ✔ Container payment-service       Started
 ✔ Container shipping-service      Started
 ✔ Container notification-service  Started
 ✔ Container analytics-service     Started
 ✔ Container frontend              Started
=========================================
Setup Complete!
=========================================
```
> ⏱️ First build takes **5–10 minutes** (Maven + npm downloads). Normal.
> If Docker complains about permissions, run `exit`, SSH back in, and re-run — the script added you to the `docker` group.

---

## 11. Verify Everything Works

On the server:
```bash
docker-compose ps
```
✅ **Demo output:**
```
NAME                   STATUS                   PORTS
analytics-service      Up (healthy)             0.0.0.0:8086->8086/tcp
api-gateway            Up (healthy)             0.0.0.0:8080->8080/tcp
frontend               Up (healthy)             0.0.0.0:3000->80/tcp
inventory-service      Up (healthy)             0.0.0.0:8082->8082/tcp
kafka                  Up (healthy)             0.0.0.0:9092->9092/tcp
kafka-init             Exited (0)                          <- expected!
notification-service   Up (healthy)             0.0.0.0:8084->8084/tcp
order-service          Up (healthy)             0.0.0.0:8081->8081/tcp
payment-service        Up (healthy)             0.0.0.0:8083->8083/tcp
postgres               Up (healthy)             0.0.0.0:5432->5432/tcp
redis                  Up (healthy)             0.0.0.0:6379->6379/tcp
shipping-service       Up (healthy)             0.0.0.0:8085->8085/tcp
zookeeper              Up (healthy)             0.0.0.0:2181->2181/tcp
```

Check topics, databases, and gateway health:
```bash
docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
docker-compose exec postgres psql -U postgres -c "\l"
curl http://localhost:8080/actuator/health
```
✅ **Demo output:**
```
dlq-events
inventory-events
order-events
payment-events
shipping-events

     Name      |  Owner   | Encoding
---------------+----------+----------
 analytics_db  | postgres | UTF8
 inventory_db  | postgres | UTF8
 orders_db     | postgres | UTF8
 payments_db   | postgres | UTF8
 shipping_db   | postgres | UTF8

{"status":"UP"}
```

---

## 12. Open the App in Your Browser

On your own computer, visit:
```
http://<EC2_IP>:3000
```
✅ **Demo output:** The OrderFlow storefront with the product grid and stock badges.

**Place a test order via API** (from your computer or the server):
```bash
curl -X POST http://<EC2_IP>:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "99999999-9999-9999-9999-999999999999",
    "items": [ { "productId": "22222222-2222-2222-2222-222222222222", "quantity": 1, "unitPrice": 29.99 } ]
  }'
```
✅ **Demo output:**
```json
{ "id": "7f3a1c2e-...", "status": "PLACED", "totalAmount": 29.99, "currency": "USD", ... }
```

Then open `http://<EC2_IP>:3000/track/<that-id>` and watch the timeline advance `PLACED → CONFIRMED → SHIPPED → DELIVERED`. The Operations Dashboard is at `http://<EC2_IP>:3000/ops`.

Confirm Kafka consumers are caught up:
```bash
curl http://<EC2_IP>:8080/api/analytics/kafka/consumer-lag
```
✅ **Demo output:**
```json
{ "consumerGroup": "analytics-service-group", "totalLag": 0, "metrics": { ... } }
```

---

## 13. One-Shot Provisioning Script

Want to do Sections 4–9 in one go? Save this as `aws-provision.sh` **on your local machine** and run `bash aws-provision.sh`. (Assumes `aws configure` is already done and `$PROJECT_DIR` is correct.)

```bash
#!/bin/bash
set -e

AWS_REGION="us-east-1"
KEY_NAME="orderflow-key"
SG_NAME="orderflow-sg"
INSTANCE_NAME="orderflow-server"
INSTANCE_TYPE="t3.large"
PROJECT_DIR="C:/Users/kiranmaddireddy/Downloads/ALL_IN_ONE/Ecommerce_Order_Mangement_System"

echo "==> Creating key pair..."
aws ec2 create-key-pair --region "$AWS_REGION" --key-name "$KEY_NAME" \
  --query 'KeyMaterial' --output text > "$HOME/$KEY_NAME.pem"
chmod 400 "$HOME/$KEY_NAME.pem"

echo "==> Creating security group..."
SG_ID=$(aws ec2 create-security-group --region "$AWS_REGION" \
  --group-name "$SG_NAME" --description "OrderFlow ports" \
  --query 'GroupId' --output text)

MY_IP=$(curl -s https://checkip.amazonaws.com)
aws ec2 authorize-security-group-ingress --region "$AWS_REGION" --group-id "$SG_ID" --protocol tcp --port 22 --cidr "${MY_IP}/32"
aws ec2 authorize-security-group-ingress --region "$AWS_REGION" --group-id "$SG_ID" --protocol tcp --port 3000 --cidr 0.0.0.0/0
aws ec2 authorize-security-group-ingress --region "$AWS_REGION" --group-id "$SG_ID" --protocol tcp --port 8080 --cidr 0.0.0.0/0
aws ec2 authorize-security-group-ingress --region "$AWS_REGION" --group-id "$SG_ID" --protocol tcp --port 8081-8086 --cidr 0.0.0.0/0

echo "==> Finding Ubuntu 22.04 AMI..."
AMI_ID=$(aws ec2 describe-images --region "$AWS_REGION" --owners 099720109477 \
  --filters "Name=name,Values=ubuntu/images/hvm-ssd/ubuntu-jammy-22.04-amd64-server-*" "Name=state,Values=available" \
  --query 'sort_by(Images, &CreationDate)[-1].ImageId' --output text)

echo "==> Launching instance..."
INSTANCE_ID=$(aws ec2 run-instances --region "$AWS_REGION" \
  --image-id "$AMI_ID" --instance-type "$INSTANCE_TYPE" --key-name "$KEY_NAME" \
  --security-group-ids "$SG_ID" \
  --block-device-mappings '[{"DeviceName":"/dev/sda1","Ebs":{"VolumeSize":30,"VolumeType":"gp3"}}]' \
  --tag-specifications "ResourceType=instance,Tags=[{Key=Name,Value=$INSTANCE_NAME}]" \
  --query 'Instances[0].InstanceId' --output text)

aws ec2 wait instance-status-ok --region "$AWS_REGION" --instance-ids "$INSTANCE_ID"

EC2_IP=$(aws ec2 describe-instances --region "$AWS_REGION" --instance-ids "$INSTANCE_ID" \
  --query 'Reservations[0].Instances[0].PublicIpAddress' --output text)

echo "==> Copying project to EC2..."
scp -o StrictHostKeyChecking=no -i "$HOME/$KEY_NAME.pem" -r "$PROJECT_DIR" ubuntu@"$EC2_IP":/home/ubuntu/orderflow

echo "============================================"
echo "Instance ID : $INSTANCE_ID"
echo "Public IP   : $EC2_IP"
echo "Next steps  :"
echo "  ssh -i ~/$KEY_NAME.pem ubuntu@$EC2_IP"
echo "  cd /home/ubuntu/orderflow && cp .env.example .env"
echo "  nano .env   # set EC2_HOST=$EC2_IP, DB_PASSWORD, JWT_SECRET"
echo "  bash ec2-setup.sh"
echo "============================================"
```
✅ **Demo output (tail):**
```
============================================
Instance ID : i-0123456789abcdef0
Public IP   : 54.123.45.67
Next steps  :
  ssh -i ~/orderflow-key.pem ubuntu@54.123.45.67
  cd /home/ubuntu/orderflow && cp .env.example .env
  nano .env   # set EC2_HOST=54.123.45.67, DB_PASSWORD, JWT_SECRET
  bash ec2-setup.sh
============================================
```

---

## 14. Cleanup — Stop Paying

> 💲 You're billed while the instance is **running**.

**Stop containers but keep the server** (on the server):
```bash
cd /home/ubuntu/orderflow && docker-compose down
```

**Stop the EC2 instance** (compute billing pauses):
```bash
aws ec2 stop-instances --region "$AWS_REGION" --instance-ids "$INSTANCE_ID"
```
✅ **Demo output:**
```json
{ "StoppingInstances": [ { "InstanceId": "i-0123...", "CurrentState": { "Name": "stopping" } } ] }
```
> ⚠️ After a stop/start the **public IP changes**. Get the new one (Section 8), update `.env`, and run `docker-compose up -d --build frontend`.

**Start it again later:**
```bash
aws ec2 start-instances --region "$AWS_REGION" --instance-ids "$INSTANCE_ID"
```

**Terminate (delete) everything permanently:**
```bash
aws ec2 terminate-instances --region "$AWS_REGION" --instance-ids "$INSTANCE_ID"
aws ec2 wait instance-terminated --region "$AWS_REGION" --instance-ids "$INSTANCE_ID"

# Then remove the security group and key pair
aws ec2 delete-security-group --region "$AWS_REGION" --group-id "$SG_ID"
aws ec2 delete-key-pair --region "$AWS_REGION" --key-name "$KEY_NAME"
rm -f "$HOME/$KEY_NAME.pem"
```
✅ **Demo output:**
```json
{ "TerminatingInstances": [ { "InstanceId": "i-0123...", "CurrentState": { "Name": "shutting-down" } } ] }
```

---

## 15. Troubleshooting

| Problem | Fix |
|---|---|
| `Unable to locate credentials` | Run `aws configure` again (Section 2). |
| `An error occurred (UnauthorizedOperation)` | Your IAM user lacks EC2 permissions. Attach `AmazonEC2FullAccess`. |
| `InvalidKeyPair.Duplicate` | The key name already exists. Use a new `KEY_NAME` or delete the old one: `aws ec2 delete-key-pair --key-name orderflow-key`. |
| `InvalidGroup.Duplicate` | Security group name exists. Reuse its ID: `aws ec2 describe-security-groups --group-names orderflow-sg --query 'SecurityGroups[0].GroupId' --output text`. |
| SSH `Permission denied (publickey)` | `chmod 400 ~/orderflow-key.pem`; ensure you connect as user `ubuntu`. |
| SSH `Connection timed out` | Your IP changed. Re-add the SSH rule with your new IP, or check the instance is `running`. |
| Browser can't reach :3000 / :8080 | Confirm SG rules for 3000 & 8080 (`0.0.0.0/0`) and that containers are `Up`. |
| `permission denied … /var/run/docker.sock` | Session predates the `docker` group change. Run `newgrp docker` or log out/in; or use `sudo docker-compose ...`. |
| `the attribute version is obsolete` warning | Harmless Compose v2 notice; already removed from `docker-compose.yml`. |
| Service unhealthy / restarting | `docker-compose logs <service>`; usually just needs Kafka/Postgres to finish booting — wait 30s. |
| Out of memory | Use `t3.large` or larger (needs 8 GB RAM). |
| Frontend shows no data | `.env` `EC2_HOST` must be the public IP; rebuild: `docker-compose up -d --build frontend`. |

---

## Quick Reference Card

```bash
# Provision (local machine, after `aws configure`):
bash aws-provision.sh                         # Sections 4-9 in one shot

# On the EC2 server (/home/ubuntu/orderflow):
docker-compose ps                             # container status
docker-compose logs -f order-service          # follow logs
docker-compose up -d --build                  # rebuild + start
docker-compose down                           # stop (keep data)

# Lifecycle (local machine):
aws ec2 stop-instances      --instance-ids "$INSTANCE_ID"
aws ec2 start-instances     --instance-ids "$INSTANCE_ID"
aws ec2 terminate-instances --instance-ids "$INSTANCE_ID"

# URLs:
http://<EC2_IP>:3000        # Storefront
http://<EC2_IP>:3000/ops    # Operations Dashboard
http://<EC2_IP>:8080/actuator/health  # API Gateway health
```

---

🎉 **Done!** You provisioned an EC2 server and deployed the entire OrderFlow microservices stack from the command line. For the console/click-through version see `AWS_UI_Doc.md`; for architecture and the Saga walkthrough see `DEPLOYMENT.md` and `README.md`.
