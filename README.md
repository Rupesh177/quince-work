# Quince Test Automation Framework

[![Java](https://img.shields.io/badge/Java-21-blue.svg)](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html)
[![Maven](https://img.shields.io/badge/Maven-3.8+-blue.svg)](https://maven.apache.org/)
[![Selenium](https://img.shields.io/badge/Selenium-4.15-green.svg)](https://www.selenium.dev/)
[![TestNG](https://img.shields.io/badge/TestNG-7.8-blue.svg)](https://testng.org/)
[![License](https://img.shields.io/badge/License-MIT-green.svg)](LICENSE)

**Quince Test Automation Suite** is a production-grade, AI-powered test automation framework designed for comprehensive testing of e-commerce A/B experiments. It integrates enterprise-level testing patterns with modern machine learning to optimize test execution, predict variant impacts.

## 🎯 Key Features

- ✅ **Multi-Signal Variant Detection** - Detects active experiment variants through 5 different signals
- 🤖 **AI-Powered Auto-Healing** - Healenium + LLM-based locator recovery
- 📊 **Flaky Test Detector & Analyzer** - Quarantine flaky tests
- 🔄 **Parallel Execution** - Thread-safe multi-threaded test runs
- 📋 **Comprehensive Reporting** - Allure reports 
- 📡 **Real-Time Monitoring** - Grafana dashboards + InfluxDB metrics

## 📋 Table of Contents

- [Technology Stack](#technology-stack)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Installation & Setup & Run](#installation--setup--run)
---

## 🛠 Technology Stack

### Core Testing Framework

| Component | Technology | Version | Purpose                                      |
|-----------|-----------|---------|----------------------------------------------|
| **Language** | Java | 21 (LTS) | Modern features: records, restAssured        |
| **Build Tool** | Maven | 3.8+ | Multi-module project management              |
| **Test Runner** | TestNG | 7.8+ | Parallel execution, grouping, data providers |
| **UI Automation** | Selenium | 4.15+ | WebDriver for Chrome, Firefox, Edge          |
| **Browser Healing** | Healenium | 3.5+ | ML-based auto-healing for broken locators    |
| **API Testing** | RestAssured | 5.3+ | HTTP client for API validation               |
| **Database** | JDBC/HikariCP | Latest | Connection pooling for DB tests              |
| **Data Generation** | Java Faker | 1.0+ | Realistic test data generation               |

### AI/ML Components

| Component | Technology     | Version | Purpose |
|-----------|----------------|---------|---------|
| **LLM Integration** | OpenAI         | Latest | Locator healing, root cause analysis |
| **Feature Flags** | Optimizely SDK | 4.1+ | Feature flag management |
| **Experiment Detection** | Custom         | 1.0 | Multi-signal variant detection |

### Infrastructure & Reporting

| Component | Technology | Version | Purpose |
|-----------|-----------|---------|---------|
| **Containerization** | Docker | 20.10+ | Service orchestration |
| **Container Orchestration** | Docker Compose | v2+ | Multi-container management |
| **CI/CD** | Jenkins | 2.350+ | Declarative pipeline orchestration |
| **Test Reporting** | Allure | 2.25+ | Beautiful test reports with history |
| **Metrics Database** | InfluxDB | 2.7+ | Time-series metrics storage |
| **Dashboards** | Grafana | Latest | Test metrics visualization |
| **Structured Logging** | Log4j2 | 2.21+ | JSON logging with structured data |
| **Frontend Demo** | React 18 + Vite | Latest | Product page for A/B testing |

## Installation & Setup & Run

### Step 1. Clone from GitHub
git clone https://github.com/quince-automation/quince-test-automation-framework.git

cd quince-test-automation-framework


### Step 2. Create .env file in project root
OPTIMIZELY_SDK_KEY=<your-actual-sdk-key>

export OPTIMIZELY_SDK_KEY=<your-sdk-key>

### Step 3. Verify Prerequisites
java -version          # Should show Java 21

mvn -version           # Should show Maven 3.8+

docker --version       # Should show Docker 20.10+

docker-compose version # Should show v2+


### Step 3. Create .env file in project root

mvn clean compile

### Run unit tests (optional)
mvn test -DskipIntegration=true

### Full build
mvn clean install

### Step 4. Setup React Product 
[See Framework README](https://github.com/Rupesh177/reactProj/tree/main/README.md)

### Step 5. Run tests
mvn test

