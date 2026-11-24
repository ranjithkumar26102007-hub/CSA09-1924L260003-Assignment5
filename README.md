# CSA09 – Programming in Java
## Assignment 5 (CO5): Data Persistence Engineering (Files and Databases)

### Student Information
- **Student Name:** R. Ranjith Kumar
- **Register Number:** 1924L260003
- **Course Code:** CSA09 – Programming in Java
- **Institution:** Saveetha School of Engineering, SIMATS
- **System Environment:**
  - **Operating System:** Windows 10/11 x86_64
  - **Java Development Kit:** Java 27 (`javac 27`, `java version "27" 2026-09-15`)
  - **Database Server:** MySQL Community Server 8.4.9 (`MySQL84` service)
  - **JDBC Driver:** MySQL Connector/J 9.1.0 (`lib/mysql-connector-j-9.1.0.jar`)
  - **Version Control:** Git 2.56.0

---

## Deliverables Summary

### Part A: Persistence Design & Algorithmic Pseudocode (COMPLETED)
All five Part A design tasks adhere strictly to the SIMATS Engineering Standard for Algorithmic Pseudocode:

| Task ID | Task Description | Status | Deliverable Report Link |
| :---: | :--- | :---: | :--- |
| **A1** | File Format Design (Delimited text vs binary `DataOutputStream`, crash recovery, `RandomAccessFile` comparison) | **Completed** | [PartA/A1/A1.md](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/PartA/A1/A1.md) |
| **A2** | Buffered I/O Analysis (50 MB math derivation, system-call reduction, buffered redesign, `Files.readAllLines`) | **Completed** | [PartA/A2/A2.md](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/PartA/A2/A2.md) |
| **A3** | Data-Access Design Pattern (JDBC lifecycle, generic `Dao<T, K>` interface, `StudentDao`, ORM mapping, UML) | **Completed** | [PartA/A3/A3.md](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/PartA/A3/A3.md) |
| **A4** | Serialization Architecture (`Hostel` $\rightarrow$ `Room` $\rightarrow$ `Resident`, transient fields, `serialVersionUID`, JSON) | **Completed** | [PartA/A4/A4.md](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/PartA/A4/A4.md) |
| **A5** | Correctness & Safety (₹500 wallet transfer transaction, `SELECT ... FOR UPDATE`, SQL injection AST defense) | **Completed** | [PartA/A5/A5.md](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/PartA/A5/A5.md) |

---

### Part B: Implementation & Empirical Investigation (COMPLETED)

| Task | Title | Key Implementation Details | Artifacts & Reports | Execution Screenshots |
| :---: | :--- | :--- | :--- | :--- |
| **B1** | **File Store & Benchmarks** | • Completed `PassStore.find()` (`TODO 1`)<br>• Tested normal, missing, and damaged lines<br>• Verified expected output: `P1003` found, `P1002` skipped<br>• Measured 1 MB, 10 MB, 50 MB buffering benchmarks<br>• Measured 1K–100K pass search scalability and `HashMap` crossover | • [BusPass.java](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/Task01/src/BusPass.java)<br>• [PassStore.java](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/Task01/src/PassStore.java)<br>• [IoBenchmark.java](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/Task01/src/IoBenchmark.java)<br>• [SearchBenchmark.java](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/Task01/src/SearchBenchmark.java)<br>• [b1_results.md](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/Task01/results/b1_results.md) | • [PassStoreDemo Verification](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/Task01/screenshots/01-PassStoreDemo-Verification.png)<br>• [I/O Buffering Benchmark](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/Task01/screenshots/02-IoBenchmark-Results.png)<br>• [Search Scalability Benchmark](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/Task01/screenshots/03-SearchBenchmark-Results.png) |
| **B2** | **Adversarial Input & Security** | • Provisioned local test database `csa09_assignment5`<br>• Evaluated 8 attack vectors across `Statement` vs `PreparedStatement`<br>• Demonstrated `admin' -- ` breach on unsafe statement vs 100% rejection by `PreparedStatement`<br>• Evaluated 8 corrupted/hostile file inputs in `AdversarialDataTest` | • [b2_login.sql](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/Task02/sql/b2_login.sql)<br>• [LoginDemo.java](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/Task02/src/LoginDemo.java)<br>• [AdversarialDataTest.java](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/Task02/src/AdversarialDataTest.java)<br>• [b2_adversarial_report.md](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/Task02/results/b2_adversarial_report.md) | • [SQL Injection Breach](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/Task02/screenshots/01-LoginDemo-SqlInjection-Breach.png)<br>• [Adversarial Data Test](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/Task02/screenshots/02-AdversarialDataTest-Results.png) |
| **B3** | **JDBC Performance & Transactions** | • Implemented `PassLoader.java` (`TODO 1` Statement, `TODO 2` PreparedStatement, `TODO 3` Batch inside transaction)<br>• Extracted runtime column schema via `ResultSetMetaData`<br>• Verified transaction rollback on row 500 failure (`chk_amount` check constraint): 499 rows in auto-commit vs 0 rows in transaction<br>• Measured 1K, 10K, 100K insertion scalability | • [b3_bus_pass.sql](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/Task03/sql/b3_bus_pass.sql)<br>• [PassLoader.java](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/Task03/src/PassLoader.java)<br>• [TransactionRollbackTest.java](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/Task03/src/TransactionRollbackTest.java)<br>• [LoaderBenchmark.java](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/Task03/src/LoaderBenchmark.java)<br>• [B3_insert_times.csv](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/Task03/results/B3_insert_times.csv)<br>• [b3_performance_report.md](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/Task03/results/b3_performance_report.md) | • [PassLoader & MetaData](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/Task03/screenshots/01-PassLoader-Verification-Metadata.png)<br>• [Transaction Rollback Failure](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/Task03/screenshots/02-TransactionRollback-Failure.png)<br>• [JDBC Insertion Scalability](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/Task03/screenshots/03-LoaderBenchmark-Performance.png) |
| **B4** | **Enterprise Migration & Decision** | • Inspected MySQL engine features using `DatabaseMetaData`<br>• Migrated 50,000 legacy records with 8 injected errors<br>• Reconciled: $49,992\text{ migrated} + 8\text{ rejected} = 50,000\text{ lines read}$<br>• Formulated architectural decision for 12 campuses & 800,000 passes | • [b4_migration_schema.sql](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/Task04/sql/b4_migration_schema.sql)<br>• [PassMigration.java](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/Task04/src/PassMigration.java)<br>• [b4_engineering_decision_report.md](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/Task04/results/b4_engineering_decision_report.md) | • [Migration & Metadata Audit](file:///C:/Users/User/Downloads/CSA09-1924L260003-Assignment5/Task04/screenshots/01-PassMigration-Execution-Metadata.png) |

---

## Directory Layout
```
CSA09-1924L260003-Assignment5/
├── PartA/
│   ├── A1/A1.md
│   ├── A2/A2.md
│   ├── A3/A3.md
│   ├── A4/A4.md
│   └── A5/A5.md
├── Task01/
│   ├── src/ (BusPass.java, PassStore.java, PassStoreDemo.java, IoBenchmark.java, SearchBenchmark.java)
│   ├── input/ (b1_demo.txt, log_1MB.txt, passes_1000.txt, ...)
│   ├── results/ (b1_results.md)
│   └── screenshots/ (01-PassStoreDemo-Verification.png, 02-IoBenchmark-Results.png, 03-SearchBenchmark-Results.png)
├── Task02/
│   ├── sql/ (b2_login.sql)
│   ├── src/ (DbConfig.java, LoginDemo.java, AdversarialDataTest.java)
│   ├── input/ (adversarial_passes.txt)
│   ├── results/ (b2_adversarial_report.md)
│   └── screenshots/ (01-LoginDemo-SqlInjection-Breach.png, 02-AdversarialDataTest-Results.png)
├── Task03/
│   ├── sql/ (b3_bus_pass.sql)
│   ├── src/ (DbConfig.java, PassLoader.java, TransactionRollbackTest.java, LoaderBenchmark.java)
│   ├── results/ (b3_performance_report.md, B3_insert_times.csv)
│   └── screenshots/ (01-PassLoader-Verification-Metadata.png, 02-TransactionRollback-Failure.png, 03-LoaderBenchmark-Performance.png)
├── Task04/
│   ├── sql/ (b4_migration_schema.sql)
│   ├── src/ (DbConfig.java, PassMigration.java)
│   ├── input/ (migration_source.txt)
│   ├── results/ (b4_engineering_decision_report.md)
│   └── screenshots/ (01-PassMigration-Execution-Metadata.png)
├── lib/
│   └── mysql-connector-j-9.1.0.jar
├── .gitignore
└── README.md
```

---

## Compilation & Execution Instructions

All programs can be compiled and executed directly from the project root using Java 27:

```bash
# 1. Compile all task modules
javac -d Task01/bin Task01/src/*.java
javac -cp ".;lib/mysql-connector-j-9.1.0.jar" -d Task02/bin Task02/src/*.java
javac -cp ".;lib/mysql-connector-j-9.1.0.jar" -d Task03/bin Task03/src/*.java
javac -cp ".;lib/mysql-connector-j-9.1.0.jar" -d Task04/bin Task04/src/*.java

# 2. Run Task B1 (File Store Verification & Benchmarks)
java -cp Task01/bin PassStoreDemo
java -cp Task01/bin IoBenchmark
java -cp Task01/bin SearchBenchmark

# 3. Run Task B2 (Adversarial Security & SQL Injection Tests)
java -cp "Task02/bin;lib/mysql-connector-j-9.1.0.jar" LoginDemo
java -cp Task02/bin AdversarialDataTest

# 4. Run Task B3 (JDBC Performance, Transactions & Rollback)
java -cp "Task03/bin;lib/mysql-connector-j-9.1.0.jar" PassLoader
java -cp "Task03/bin;lib/mysql-connector-j-9.1.0.jar" TransactionRollbackTest
java -cp "Task03/bin;lib/mysql-connector-j-9.1.0.jar" LoaderBenchmark

# 5. Run Task B4 (Enterprise Migration Engine)
java -cp "Task04/bin;lib/mysql-connector-j-9.1.0.jar" PassMigration
```
