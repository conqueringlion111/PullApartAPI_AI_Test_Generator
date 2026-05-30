# PullApart API Automation Framework 🔨

**AI-Enhanced REST API Test Automation | Java • Rest Assured • TestNG • Anthropic Claude API**

A production-grade REST API automation framework for [Pull-A-Part](https://www.pullapart.com) — a national
used auto parts retailer — enhanced with AI-powered test generation, LLM response validation,
chatbot testing, and an autonomous AI agent that analyzes test failures using the Anthropic Claude API.

---

## Highlights

- **25 automated API tests** across 4 business domains — Location, Search, Used Cars, Parts Pricing
- **AI-powered test generator** that produces Rest Assured test classes from plain English endpoint descriptions
- **Autonomous AI agent** that runs the full test suite, detects failures, sends them to Claude for root cause analysis, and generates an HTML report
- **Real defect detected** — the AI agent identified a Birmingham, AL location returning an empty `locationName` field from the API, flagged for human review, and the test was correctly disabled pending an API data fix
- **Data-driven tests** using TestNG `@DataProvider` for scalable regression coverage
- **POJO-based deserialization** for structured, type-safe response validation

---

## Tech Stack

| Tool                  | Version  | Purpose                              |
|-----------------------|----------|--------------------------------------|
| Java                  | 17 (LTS) | Core language                        |
| Rest Assured          | 5.5.7    | REST API test execution              |
| TestNG                | 7.11.0   | Test runner and suite management     |
| Anthropic Java SDK    | 2.32.0   | Claude API integration (AI modules)  |
| Jackson               | 2.18.3   | JSON parsing and POJO deserialization|
| dotenv-java           | 3.2.0    | Secure API key loading from .env     |
| SLF4J + Logback       | 2.0.18   | Logging                              |
| Maven                 | 3.x      | Build and dependency management      |
| IntelliJ IDEA         | Community| IDE                                  |

---

## Project Structure

```
pullapartMmlAPI/
├── pom.xml
├── .env                                        ← Anthropic API key (never committed)
├── .gitignore
├── README.md
├── src/
│   ├── main/java/com/synlee/qforge/
│   │   ├── agent/                              ← AI Agent (4 classes)
│   │   │   ├── QForgeAgent.java               ← Main entry point
│   │   │   ├── TestRunnerAgent.java           ← Runs Maven, parses failures
│   │   │   ├── FailureAnalyzerService.java    ← Sends failures to Claude
│   │   │   └── AgentReportService.java        ← Generates HTML report
│   │   ├── chatbot/
│   │   │   └── ChatbotService.java            ← Multi-turn Claude chatbot
│   │   ├── client/
│   │   │   └── AnthropicClientProvider.java   ← Shared Anthropic API client
│   │   ├── generator/
│   │   │   └── TestGeneratorService.java      ← AI test code generator
│   │   └── validator/
│   │       └── LlmValidatorService.java       ← LLM response validator
│   └── test/java/com/synlee/pullapart/api/
│       ├── tests/                              ← PullApart API regression tests
│       │   ├── LocationTest.java              ← /location endpoint tests
│       │   ├── SearchTest.java                ← /Vehicle/Search endpoint tests
│       │   ├── UsedCarsTest.java              ← /usedCars endpoint tests
│       │   └── PartsPricingTest.java          ← Parts pricing endpoint tests
│       └── qforge/                            ← AI module tests
│           ├── generator/
│           │   └── TestGeneratorTest.java     ← AI test generator tests
│           ├── validator/
│           │   └── LlmValidatorTest.java      ← LLM response validator tests
│           └── chatbot/
│               └── ChatbotTest.java           ← Chatbot test suite
```

---

## Test Coverage

### PullApart API Regression Tests

| Test Class         | Endpoint              | Tests | Coverage                                      |
|--------------------|-----------------------|-------|-----------------------------------------------|
| `LocationTest`     | `GET /location`       | 5     | Enterprise locations, data provider, POJO deserialization |
| `SearchTest`       | `GET /Vehicle/Search` | 6     | Advanced search, URI formatter, POJO, data provider |
| `UsedCarsTest`     | `GET /usedCars`       | 2*    | Used car location search by zipcode           |
| `PartsPricingTest` | `GET /partsPrice`     | 1     | Generic and premium alternator pricing        |

> *UsedCarsTest Birmingham tests disabled — API returning empty `locationName` field. Defect logged for API data fix.

### AI Module Tests

| Module                  | Tests | What It Validates                                      |
|-------------------------|-------|--------------------------------------------------------|
| AI Test Generator       | 1     | Generates Rest Assured test class for PullApart endpoint |
| LLM Response Validator  | 6     | Response quality, keywords, length, system prompt, edge cases |
| Chatbot Test Suite      | 6     | Single-turn, multi-turn memory, persona, reset, edge cases |

---

## AI Modules

### AI Test Generator
Sends a plain English description of a REST API endpoint to Claude and receives a
complete, ready-to-run Rest Assured + TestNG test class in return.

```
"GET /usedCars?zipcode=30074 — returns a list of used car locations near the zipcode.
Each location has locationName, address, city, state, and distance fields.
Returns 200 OK on success."
```
→ Claude generates a full Java test class with happy path, field validation, and negative tests.

### LLM Response Validator
Sends prompts to Claude and validates response quality against rules:
response time, minimum/maximum length, required keywords, system prompt adherence, and edge cases.

### Chatbot Test Suite
Tests a Claude-backed QA automation chatbot across 6 scenarios including
multi-turn conversation memory, persona adherence, conversation reset, and edge case handling.

### AI Agent
The most advanced module — runs the full Maven test suite autonomously,
parses `testng-results.xml` for failures, sends each failure to Claude for
root cause analysis and fix suggestions, and generates a professional HTML report.

**Real-world result:** The agent detected that `UsedCarsTest.searchUsedCars` was failing
because the Birmingham, AL location returned `"locationName": ""` from the Pull-A-Part API —
an actual data defect, correctly flagged for human review.

---

## Getting Started

### Prerequisites
- Java 17 ([Adoptium Temurin](https://adoptium.net))
- Maven 3.x
- IntelliJ IDEA Community Edition
- Anthropic API key — sign up at [console.anthropic.com](https://console.anthropic.com)

### Setup

**1. Clone the repository**
```bash
git clone https://github.com/conqueringlion111/pullapart-api-automation.git
cd pullapart-api-automation
```

**2. Create your `.env` file in the project root**
```
ANTHROPIC_API_KEY=your_api_key_here
```
> ⚠️ Never commit this file. It is already listed in `.gitignore`.

**3. Install dependencies**
```bash
mvn clean install -DskipTests
```

**4. Run the full test suite**
```bash
mvn test
```

**5. Run the AI Agent**
```bash
mvn exec:java -Dexec.mainClass="com.synlee.qforge.agent.QForgeAgent"
```

Then open the HTML report:
```
target/qforge-agent-report.html
```

---

## Running Specific Modules

```bash
# PullApart regression tests only
mvn test -Dgroups=regression

# Smoke tests only
mvn test -Dgroups=smoke

# AI Test Generator only
mvn test -Dtest=TestGeneratorTest

# LLM Validator only
mvn test -Dtest=LlmValidatorTest

# Chatbot tests only
mvn test -Dtest=ChatbotTest
```

---

## Test Results

```
Tests run: 25, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## Author

**Syn H. Lee**
Senior Test Automation Engineer | UI, API, Mobile Automation
- GitHub: [conqueringlion111](https://github.com/conqueringlion111)
- Email: synhlee@gmail.com
- Location: Lawrenceville, GA

---

## Related Project

**[QForge](https://github.com/conqueringlion111/qforge)** — The standalone AI-powered QA toolkit
that the AI modules in this framework are based on.

---

## License

This project is open source and available under the [MIT License](LICENSE).