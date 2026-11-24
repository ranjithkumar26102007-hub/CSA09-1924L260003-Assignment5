# CSA09 – Programming in Java
## Assignment 5 (CO5): Data Persistence Engineering (Files and Databases)

### Student Information
- **Student Name:** R. Ranjith Kumar
- **Register Number:** 1924L260003
- **Course Code:** CSA09 – Programming in Java
- **Institution:** Saveetha School of Engineering, SIMATS
- **Environment:**
  - **Java Version:** Java 27 (`javac 27` / `java version "27" 2026-09-15`)
  - **Database:** MySQL Community Server 8.4 (`8.4.9 for Win64 on x86_64`)
  - **JDBC Driver:** MySQL Connector/J 9.1.0 (`lib/mysql-connector-j-9.1.0.jar`)
  - **Version Control:** Git 2.56.0

---

### Assignment Overview
This repository contains the deliverables for **Assignment 5 – CO5: Data Persistence Engineering (Files and Databases)**, structured according to the SIMATS Engineering standards. The assignment is organized into two major divisions:

1. **Part A: Persistence Design & Algorithmic Pseudocode (A1 to A5)**
   - **Task A1:** File Format Design — Delimited text vs. binary format via `DataOutputStream`, crash/fault recovery, and comparison with `RandomAccessFile`.
   - **Task A2:** Buffered I/O — System-call reduction analysis, buffered redesign, buffer boundary conditions, and comparison with `Files.readAllLines()`.
   - **Task A3:** Data-Access Design Pattern — Generic `Dao<T, K>` interface, `StudentDao` implementation, SQL-to-object mapping, and separation of concerns.
   - **Task A4:** Serialization Architecture — Transient state design, deep object graphs, schema evolution, and comparison with CSV/JSON.
   - **Task A5:** Correctness and Safety — Transactional ACID semantics (`commit`/`rollback`), check-then-act races, and SQL injection defense via `PreparedStatement`.

2. **Part B: Implementation & Empirical Investigation (Task01 to Task04)**
   - **Task01:** Bus-Pass Text-File Store — Implementation of file persistence with error isolation, damaged record skipping, and linear scan vs. in-memory indexing benchmarks.
   - **Task02:** Adversarial Input Analysis — Demonstrating SQL injection breaches on raw string concatenation and verified prevention with parameterized queries, plus malformed record boundary testing.
   - **Task03:** JDBC Performance Benchmarking — Quantitative evaluation of `Statement`, `PreparedStatement`, and JDBC batch processing across scale (1K, 10K, 100K rows), including mid-batch transaction rollback verification.
   - **Task04:** Final Engineering Decision & Migration — Enterprise migration engine with validation pipelines, `DatabaseMetaData` schema inspection, and architecture recommendation for multi-campus scaling.

---

### Directory Structure
```
CSA09-1924L260003-Assignment5/
├── PartA/
│   ├── A1/                  # Task A1 Design & SIMATS Pseudocode
│   ├── A2/                  # Task A2 Buffered I/O Analysis
│   ├── A3/                  # Task A3 Generic DAO Pattern
│   ├── A4/                  # Task A4 Serialization Design
│   └── A5/                  # Task A5 Transactions & Security
├── Task01/
│   ├── src/                 # Java source files (BusPass, PassStore, Benchmarks)
│   ├── input/               # Input sample data & corrupted test files
│   ├── screenshots/         # Verified terminal execution screenshots
│   └── results/             # Benchmark timing data and test logs
├── Task02/
│   ├── src/                 # LoginDemo & Adversarial validation code
│   ├── sql/                 # Schema creation & test user seeds
│   ├── screenshots/         # SQL injection test execution screenshots
│   └── results/             # Adversarial report tables
├── Task03/
│   ├── src/                 # PassLoader, Batch benchmarks & Transaction tests
│   ├── sql/                 # Bus pass table DDL and constraints
│   ├── screenshots/         # Execution & rollback verification screenshots
│   └── results/             # CSV performance metrics and comparisons
├── Task04/
│   ├── src/                 # PassMigration & Metadata inspection engine
│   ├── sql/                 # Schema migration DDL
│   ├── input/               # Multi-record migration source data
│   ├── screenshots/         # Migration audit execution screenshots
│   └── results/             # Migration reconciliation audit logs
├── lib/
│   └── mysql-connector-j-9.1.0.jar  # MySQL JDBC Connector
└── README.md                # Project documentation and student metadata
```

---

### Compilation & Execution Notes
All Java code is compiled and executed using Java 27 with the MySQL Connector/J library:

```bash
# Example compilation (Task 03)
javac -cp ".;lib/mysql-connector-j-9.1.0.jar" -d bin Task03/src/*.java

# Example execution
java -cp "bin;lib/mysql-connector-j-9.1.0.jar" PassLoader
```
