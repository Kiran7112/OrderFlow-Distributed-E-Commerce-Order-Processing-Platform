# OrderFlow — AWS Setup Guide (Console / UI, Step by Step)

This guide shows you **exactly** how to run the entire OrderFlow platform on AWS using the **AWS Management Console (web UI)** — no prior AWS experience needed. Every click is spelled out, with demo outputs so you know what success looks like.

> **What we're doing:** Renting one Linux server on AWS (EC2), copying the project onto it, and running all 13 containers (Kafka, PostgreSQL, Redis, 7 microservices, React frontend) with one command. You only need **Java locally** to inspect code — AWS runs everything else.

> **About the code:** The backend is a **Maven multi-module reactor** (one parent POM in `services/` builds all microservices). You don't build it by hand — the `ec2-setup.sh` script and Docker Compose handle compilation inside containers. Nothing in this guide changes because of that; just copy the whole project folder as-is.

---

## Table of Contents

1. [What you'll need](#0-what-youll-need)
2. [Create an AWS account](#1-create-an-aws-account)
3. [Create an EC2 Key Pair (your password file)](#2-create-an-ec2-key-pair)
4. [Create a Security Group (firewall rules)](#3-create-a-security-group)
5. [Launch an EC2 instance (your server)](#4-launch-an-ec2-instance)
6. [Connect to your server](#5-connect-to-your-server)
7. [Copy the project to EC2](#6-copy-the-project-to-ec2)
8. [Configure and run the project](#7-configure-and-run-the-project)
9. [Verify everything works](#8-verify-everything-works)
10. [Open the app in your browser](#9-open-the-app-in-your-browser)
11. [Stopping / starting / saving money](#10-stopping-starting-and-saving-money)
12. [Troubleshooting](#11-troubleshooting)

---

## 0. What You'll Need

| Item | Notes |
|---|---|
| A credit/debit card | AWS requires one to create an account (you'll stay within paid usage here — see cost note) |
| An email address | For the AWS account |
| The OrderFlow project folder | The one you have locally |
| ~45 minutes | First-time setup |

> 💲 **Cost note:** A `t3.large` instance costs roughly **$0.08/hour** (~$2 for a full day). **Stop or terminate the instance when done** (Section 10) so you don't keep paying. The free-tier `t2.micro` is **too small** for this stack — don't use it.

---

## 1. Create an AWS Account

> Skip this section if you already have an AWS account.

1. Go to **https://aws.amazon.com/**
2. Click the orange **"Create an AWS Account"** button (top-right).
3. Enter your **email address**, choose an **AWS account name** (e.g., `orderflow-demo`), and click **Verify email address**.
4. Enter the verification code sent to your email.
5. Create a **root user password**.
6. Fill in **contact information** (choose "Personal" account type).
7. Enter your **payment (card)** details.
8. Verify your **phone number** (SMS or call).
9. Choose the **Basic support – Free** plan.
10. Click **Complete sign up**.

✅ **Demo output:** You'll land on a page saying *"Congratulations! Your AWS account is being activated"*. Activation usually completes within a few minutes (sometimes up to 24h).

11. Click **Go to the AWS Management Console** and **Sign in** as the **Root user** with your email + password.

---

## 2. Create an EC2 Key Pair

A **key pair** is like a password file that lets you securely log into your server.

1. In the AWS Console, find the **search bar at the top** and type `EC2`, then click **EC2** under Services.

2. On the left sidebar, scroll to **Network & Security → Key Pairs**.

3. Click **Create key pair** (top-right, orange button).

4. Fill in:
   - **Name:** `orderflow-key`
   - **Key pair type:** `RSA`
   - **Private key file format:**
     - Choose **`.pem`** if you're on **Mac/Linux** or **Windows with Git Bash / OpenSSH**
     - Choose **`.ppk`** if you'll use **PuTTY** on Windows

5. Click **Create key pair**.

✅ **Demo output:** A file named `orderflow-key.pem` (or `.ppk`) **downloads automatically** to your computer. 

> ⚠️ **IMPORTANT:** Save this file somewhere safe (e.g., `C:\Users\<you>\Downloads\orderflow-key.pem`). You **cannot download it again**. If you lose it, you must create a new key pair.

6. **(Mac/Linux/Git Bash only)** Lock down the file permissions so SSH will accept it:
   ```bash
   chmod 400 ~/Downloads/orderflow-key.pem
   ```

---

## 3. Create a Security Group

A **security group** is a firewall that controls which ports are open to the internet.

1. In the EC2 left sidebar, go to **Network & Security → Security Groups**.

2. Click **Create security group**.

3. Fill in **Basic details**:
   - **Security group name:** `orderflow-sg`
   - **Description:** `OrderFlow ports`
   - **VPC:** leave the default one selected

4. Under **Inbound rules**, click **Add rule** for each row below:

   | Type | Protocol | Port range | Source | Description |
   |---|---|---|---|---|
   | SSH | TCP | 22 | My IP | SSH login |
   | Custom TCP | TCP | 3000 | Anywhere-IPv4 (0.0.0.0/0) | React frontend |
   | Custom TCP | TCP | 8080 | Anywhere-IPv4 (0.0.0.0/0) | API Gateway |
   | Custom TCP | TCP | 8081-8086 | Anywhere-IPv4 (0.0.0.0/0) | Microservices (debug, optional) |

   > For **SSH**, selecting **"My IP"** restricts login to your current internet connection (more secure). The app ports (3000, 8080) use **Anywhere** so you can open them in any browser.

5. Leave **Outbound rules** as default (all traffic allowed).

6. Click **Create security group**.

✅ **Demo output:** You'll see a green banner: *"Security group (sg-0abc123…) was created successfully"* and your 4 inbound rules listed.

---

## 4. Launch an EC2 Instance

This is your actual server.

1. In the EC2 left sidebar, click **Instances**, then **Launch instances** (top-right).

2. **Name and tags:**
   - **Name:** `orderflow-server`

3. **Application and OS Images (AMI):**
   - Click **Ubuntu**.
   - In the AMI dropdown, select a **plain Ubuntu Server LTS** image — **22.04 LTS or 24.04 LTS both work**. Pick whichever is offered and marked **"Free tier eligible"**.
   - ⚠️ **Do NOT** select any image whose name contains **"SQL Server"**, **"Windows"**, or **"Microsoft"**. Those carry a license that is **not allowed on `t3.large`** and will fail with: *"Microsoft SQL Server is not supported for the instance type 't3.large'."* OrderFlow uses **PostgreSQL inside Docker**, so you need a clean Ubuntu image with nothing pre-installed.
   - ✅ Correct choice looks like: `Ubuntu Server 24.04 LTS (HVM), SSD Volume Type` · `Free tier eligible`.

4. **Instance type:**
   - Click the dropdown and select **`t3.large`** (2 vCPU, 8 GB RAM).
   > ❗ Do **not** use `t2.micro` / `t3.micro` — the stack needs at least 8 GB RAM or services will crash.

5. **Key pair (login):**
   - Select **`orderflow-key`** (the one you created in Section 2).

6. **Network settings:**
   - Click **Edit**.
   - **Firewall (security groups):** choose **Select existing security group**.
   - Pick **`orderflow-sg`**.

7. **Configure storage:**
   - Change the size to **30 GiB**, type **gp3**.

8. Review the **Summary** panel on the right, then click **Launch instance**.

✅ **Demo output:** A green page: *"Success! Successfully initiated launch of instance (i-0abc123def…)"*.

9. Click the instance ID link, or go to **Instances**. Wait until:
   - **Instance state:** `Running`
   - **Status check:** `2/2 checks passed` (takes ~2 minutes)

10. **Copy the Public IPv4 address** — you'll need it. Example: `54.123.45.67`.

   ```
   Public IPv4 address:  54.123.45.67     [copy icon]
   ```

> Throughout this guide, replace `<EC2_IP>` with this address.

---

## 5. Connect to Your Server

You have two options. Pick one.

### Option A — Browser (easiest, no setup)

1. Select your `orderflow-server` instance and click **Connect** (top).
2. Choose the **EC2 Instance Connect** tab.
3. Click **Connect**.

✅ **Demo output:** A black terminal opens in your browser, ending with:
```
ubuntu@ip-172-31-x-x:~$
```
That prompt means you're logged into your server. 🎉

### Option B — SSH from your own computer (needed for file copy in Section 6)

Open a terminal (Git Bash on Windows, or Terminal on Mac/Linux):

```bash
ssh -i ~/Downloads/orderflow-key.pem ubuntu@<EC2_IP>
```

First time, it asks to trust the host — type `yes` and press Enter.

✅ **Demo output:**
```
Welcome to Ubuntu 22.04.4 LTS (GNU/Linux ...)
...
ubuntu@ip-172-31-x-x:~$
```

> **PuTTY users (Windows .ppk):** Open PuTTY → Host Name: `ubuntu@<EC2_IP>` → Connection → SSH → Auth → Credentials → Browse to `orderflow-key.ppk` → Open.

---

## 6. Copy the Project to EC2

Run these commands **on your LOCAL computer** (not the server terminal). Open Git Bash / Terminal in a folder *above* the project, or use the full path.

### Using scp (Mac / Linux / Windows Git Bash)

```bash
scp -i ~/Downloads/orderflow-key.pem -r \
  "C:/Users/kiranmaddireddy/Downloads/ALL_IN_ONE/Ecommerce_Order_Mangement_System" \
  ubuntu@<EC2_IP>:/home/ubuntu/orderflow
```

> This uploads the whole project into `/home/ubuntu/orderflow` on the server. It may take a couple of minutes.

✅ **Demo output:** A stream of file names with progress bars:
```
docker-compose.yml              100%   8KB  120.5KB/s   00:00
OrderServiceApplication.java    100%  412   45.1KB/s    00:00
pom.xml                         100%  3KB   210.0KB/s   00:00
...
```

> **Windows PowerShell alternative** (if not using Git Bash): same command works, just use the Windows path with backslashes or keep forward slashes inside quotes.

> **No SSH/scp?** You can also: zip the project → upload to an **S3 bucket** via the console → download on EC2 with `aws s3 cp`. But scp is simplest.

---

## 7. Configure and Run the Project

Now switch back to the **server terminal** (from Section 5).

1. Go into the project folder:
   ```bash
   cd /home/ubuntu/orderflow
   ls
   ```
   ✅ **Demo output:**
   ```
   AWS_UI_Doc.md   DEPLOYMENT.md   docker-compose.yml   ec2-setup.sh
   frontend        infrastructure  IMPLEMENTATION_PLAN.md  Makefile
   README.md       services
   ```

2. Create your environment file:
   ```bash
   cp .env.example .env
   nano .env
   ```

3. In the `nano` editor, set these three values (use your real EC2 IP):
   ```
   EC2_HOST=54.123.45.67
   DB_PASSWORD=MyStrongPassword123
   JWT_SECRET=change-this-to-a-long-random-string-at-least-32-chars
   ```
   Save and exit nano: press **Ctrl+O**, **Enter**, then **Ctrl+X**.

4. Run the setup script (installs Docker + Docker Compose, then builds & starts everything):
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
 => [order-service] exporting to image
 => [frontend] exporting to image
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

> ⏱️ The **first build takes 5–10 minutes** because Maven downloads dependencies and compiles 7 Java services + builds the React app. This is normal. Subsequent restarts are fast.

> If `docker` asks for permission, the script added you to the `docker` group. Either log out/in (`exit` then SSH again) or prefix commands with `sudo` for this session.

---

## 8. Verify Everything Works

1. Check all containers are up:
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
   kafka-init             Exited (0)                          <- expected! it ran once
   notification-service   Up (healthy)             0.0.0.0:8084->8084/tcp
   order-service          Up (healthy)             0.0.0.0:8081->8081/tcp
   payment-service        Up (healthy)             0.0.0.0:8083->8083/tcp
   postgres               Up (healthy)             0.0.0.0:5432->5432/tcp
   redis                  Up (healthy)             0.0.0.0:6379->6379/tcp
   shipping-service       Up (healthy)             0.0.0.0:8085->8085/tcp
   zookeeper              Up (healthy)             0.0.0.0:2181->2181/tcp
   ```
   > `kafka-init` showing **Exited (0)** is **correct** — it creates the Kafka topics and stops.

2. Confirm the Kafka topics were created:
   ```bash
   docker-compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list
   ```
   ✅ **Demo output:**
   ```
   dlq-events
   inventory-events
   order-events
   payment-events
   shipping-events
   ```

3. Confirm the databases exist:
   ```bash
   docker-compose exec postgres psql -U postgres -c "\l"
   ```
   ✅ **Demo output:**
   ```
        Name      |  Owner   | Encoding
   ---------------+----------+----------
    analytics_db  | postgres | UTF8
    inventory_db  | postgres | UTF8
    orders_db     | postgres | UTF8
    payments_db   | postgres | UTF8
    shipping_db   | postgres | UTF8
   ```

4. Health-check the API Gateway:
   ```bash
   curl http://localhost:8080/actuator/health
   ```
   ✅ **Demo output:**
   ```json
   {"status":"UP"}
   ```

---

## 9. Open the App in Your Browser

On **your own computer**, open a browser and go to:

```
http://<EC2_IP>:3000
```

✅ **Demo output:** The OrderFlow storefront loads with a navbar (Shop · My Orders · Operations · Inventory) and a product grid showing items like *Laptop Pro*, *Wireless Mouse*, etc., each with a stock badge ("In stock" / "Only 3 left").

### Run a full end-to-end test (watch the Saga work)

1. **Place an order:** Click **Shop → Add to Cart** on a product → **Cart** → **Proceed to Checkout** → fill the address → **Place Order**.

2. You're redirected to the **Order Tracking** page. Watch the timeline auto-advance every 3 seconds:
   ```
   ✓ Order Placed
   ✓ Confirmed (Stock Reserved)
   ✓ Shipped
   ✓ Delivered
   ```

3. **Or test via the command line** (on the server):
   ```bash
   curl -X POST http://localhost:8080/api/orders \
     -H "Content-Type: application/json" \
     -d '{
       "customerId": "99999999-9999-9999-9999-999999999999",
       "items": [
         { "productId": "22222222-2222-2222-2222-222222222222", "quantity": 1, "unitPrice": 29.99 }
       ]
     }'
   ```
   ✅ **Demo output:**
   ```json
   {
     "id": "7f3a1c2e-...",
     "customerId": "99999999-9999-9999-9999-999999999999",
     "status": "PLACED",
     "totalAmount": 29.99,
     "currency": "USD",
     "items": [ { "productId": "2222...", "quantity": 1, "unitPrice": 29.99 } ],
     "createdAt": "2026-06-18T15:30:00"
   }
   ```

4. **Watch the notification emails** (console-logged) as the saga progresses:
   ```bash
   docker-compose logs -f notification-service
   ```
   ✅ **Demo output:**
   ```
   ================================================================================
   EMAIL NOTIFICATION
   ================================================================================
   From: OrderFlow Notifications <noreply@orderflow.local>
   To: customer@example.com
   Subject: Order Confirmation - Order #7f3a1c2e-...
   --------------------------------------------------------------------------------
   Dear Valued Customer, Thank you for your order! ...
   ================================================================================
   ```
   Press **Ctrl+C** to stop following logs.

5. **View the Operations Dashboard:** open `http://<EC2_IP>:3000/ops` in your browser.
   ✅ **Demo output:** Live order pipeline counts, a revenue line chart, failure-rate cards, Kafka consumer-lag table (should trend to **0**), and a service-health grid with green dots for all 6 services.

6. **Check analytics via API:**
   ```bash
   curl http://localhost:8080/api/analytics/kafka/consumer-lag
   ```
   ✅ **Demo output:**
   ```json
   { "consumerGroup": "analytics-service-group", "totalLag": 0, "metrics": { ... } }
   ```

---

## 10. Stopping, Starting, and Saving Money

> 💲 You are billed while the instance is **running**. Stop it when you're not using it.

### Stop the containers (keep the server)
On the server:
```bash
cd /home/ubuntu/orderflow
docker-compose down          # stops containers, keeps data
docker-compose up -d         # start them again later (fast)
```

### Stop the EC2 instance (stops billing for compute)
In the AWS Console: **EC2 → Instances → select `orderflow-server` → Instance state → Stop instance**.

✅ When you want it back: **Instance state → Start instance**. 
> ⚠️ The **Public IP changes** after a stop/start. Grab the new IP and update `.env` (`EC2_HOST`), then rebuild the frontend:
> ```bash
> docker-compose up -d --build frontend
> ```

### Terminate (delete) the instance permanently
**EC2 → Instances → Instance state → Terminate instance**. This **deletes everything** and fully stops billing for it.

---

## 11. Troubleshooting

| Problem | What to check / do |
|---|---|
| **Can't SSH ("Permission denied")** | On Mac/Linux/Git Bash run `chmod 400 orderflow-key.pem`. Make sure you use user `ubuntu`. |
| **Connection timed out (SSH)** | Your IP changed — edit `orderflow-sg` → SSH rule → set Source to **My IP** again. |
| **Browser can't open :3000 or :8080** | Confirm ports 3000 & 8080 are open in `orderflow-sg` with Source `0.0.0.0/0`. Confirm instance is **Running** and containers are **Up**. |
| **A service shows "Restarting" or unhealthy** | `docker-compose logs <service-name>` to see the error. Often it just needs Kafka/Postgres to finish starting — wait 30s and re-check `docker-compose ps`. |
| **"Cannot connect to the Docker daemon"** | Log out and SSH back in (the setup added you to the `docker` group), or use `sudo docker-compose ...`. |
| **Out of memory / containers killed** | You're on too small an instance. Use `t3.large` (8 GB) or bigger. |
| **Frontend loads but no data / network errors** | `.env` `EC2_HOST` must be your **public IP**, and you must rebuild the frontend after changing it: `docker-compose up -d --build frontend`. |
| **First build seems stuck** | It's downloading Maven/npm dependencies. Give it 5–10 minutes. Watch with `docker-compose logs -f`. |
| **Want a fresh start** | `docker-compose down -v` (wipes DB + Kafka data) then `docker-compose up -d --build`. |

---

## Quick Reference Card

```bash
# On the EC2 server, in /home/ubuntu/orderflow:
docker-compose ps                      # see all containers
docker-compose logs -f order-service   # follow one service's logs
docker-compose restart payment-service # restart one service
docker-compose down                    # stop all (keep data)
docker-compose down -v                 # stop all (wipe data)
docker-compose up -d --build           # rebuild + start

# URLs (from your browser):
http://<EC2_IP>:3000        # Storefront
http://<EC2_IP>:3000/ops    # Operations Dashboard
http://<EC2_IP>:3000/inventory  # Inventory Portal
http://<EC2_IP>:8080/actuator/health  # API Gateway health
```

---

🎉 **That's it!** You now have the full OrderFlow microservices platform — Kafka, PostgreSQL, Redis, 7 Spring Boot services, and a React frontend — running live on AWS, set up entirely through the console UI.

For deeper architecture details and the failure/compensation (Saga) walkthrough, see `DEPLOYMENT.md` and `README.md`.
