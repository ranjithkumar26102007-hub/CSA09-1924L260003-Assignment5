# Task B3: JDBC Performance, Transactions and Alternatives Report

**Course:** CSA09 – Programming in Java  
**Student Name:** R. Ranjith Kumar  
**Register Number:** 1924L260003  
**Institution:** Saveetha School of Engineering, SIMATS  
**Environment:** Java 27, MySQL Community Server 8.4.9, MySQL Connector/J 9.1.0  

---

## 1. Schema Definition & 10-Row Baseline Verification

The relational table `bus_pass` was deployed in the dedicated `csa09_assignment5` schema via `Task03/sql/b3_bus_pass.sql`. The core implementation in `PassLoader.java` fulfills `TODO 1`, `TODO 2`, and `TODO 3`.

### 10 Records Insertion Verification:
| Approach | Time (ms) | Rows Inserted | SELECT Verification Status |
| :--- | :---: | :---: | :--- |
| **Statement (TODO 1)** | 90 ms | 10 | **PASS** (10 rows present) |
| **PreparedStatement (TODO 2)** | 271 ms | 10 | **PASS** (10 rows present) |
| **PreparedStatement Batch (TODO 3)** | 31 ms | 10 | **PASS** (10 rows present) |

### Schema Structure from `ResultSetMetaData`:
The schema constraints and column types were extracted dynamically at runtime:

| # | Column Name | Relational SQL Type | Display Size | Nullable? | Mapped Java Class |
| :---: | :--- | :--- | :---: | :---: | :--- |
| 1 | `pass_id` | `VARCHAR` | 16 | **NO** | `java.lang.String` |
| 2 | `register_no` | `VARCHAR` | 16 | **NO** | `java.lang.String` |
| 3 | `name` | `VARCHAR` | 80 | **NO** | `java.lang.String` |
| 4 | `route_no` | `VARCHAR` | 8 | **NO** | `java.lang.String` |
| 5 | `boarding_stop`| `VARCHAR` | 60 | **NO** | `java.lang.String` |
| 6 | `valid_until` | `DATE` | 10 | **NO** | `java.sql.Date` |
| 7 | `amount_paise` | `BIGINT` | 19 | **NO** | `java.lang.Long` |

---

## 2. Transaction Rollback & Failure Investigation

Using `TransactionRollbackTest.java`, an experiment was conducted where 1,000 passes were loaded into the database, with **Record 500 containing an invalid negative amount (`amount_paise = -1`)** that deliberately violates the MySQL relational constraint:
$$\text{CONSTRAINT } chk\_amount \text{ CHECK } (amount\_paise \ge 0)$$

### Actual Empirical Rollback Test Results:
| Execution Mode | Batch Size | Triggered Exception | Remaining Rows in Table | Architectural Consequence |
| :--- | :---: | :---: | :---: | :--- |
| **Auto-Commit Mode** | 100 | `BatchUpdateException` | **499 rows** | **Corrupt Partial State**: Batches 1 to 4 and first 99 rows of batch 5 committed permanently. Retrying the import fails due to duplicate primary key collisions. |
| **Single Transaction** | 100 | `BatchUpdateException` | **0 rows** | **Clean Atomic Rollback**: `conn.rollback()` reverted all uncommitted work. Zero orphan rows remain. System can be retried safely. |

### Technical Interpretation:
1. **The Inherent Danger of Auto-Commit**: In auto-commit mode, the database commits each chunk immediately. When the 500th row triggers a constraint violation, all 499 preceding rows remain locked into the database. The system is left in an inconsistent half-imported state.
2. **The Atomic Guarantee of Transactions**: In single-transaction mode (`conn.setAutoCommit(false)`), no changes become permanent until `conn.commit()` is explicitly reached. When `BatchUpdateException` is thrown, the catch block executes `conn.rollback()`, ensuring absolute **Atomicity** (All or Nothing).

---

## 3. Empirical JDBC Insertion Scalability Measurements

Using `LoaderBenchmark.java`, insertion times were benchmarked across **1,000**, **10,000**, and **100,000** records.

### Actual Execution Benchmarking Results:
| Total Records | Approach | Measured Time (ms) | Throughput (Rows/s) | Relative Performance |
| :---: | :--- | :---: | :---: | :---: |
| **1,000** | Batch (1000, 1 Transaction) | 1,410 ms | 709.2 rows/s | **Baseline (Fastest)** |
| **1,000** | Batch (1000, Auto-Commit) | 4,931 ms | 202.8 rows/s | $3.5\times$ slower |
| **1,000** | PreparedStatement per row | 4,116 ms | 243.0 rows/s | $2.9\times$ slower |
| **1,000** | Statement per row | 4,262 ms | 234.6 rows/s | $3.0\times$ slower |
| **10,000** | Batch (1000, 1 Transaction) | 4,852 ms | 2,061.0 rows/s | **Baseline (Fastest)** |
| **10,000** | Batch (1000, Auto-Commit) | 42,462 ms | 235.5 rows/s | $8.8\times$ slower |
| **10,000** | PreparedStatement per row | 41,988 ms | 238.2 rows/s | $8.7\times$ slower |
| **10,000** | Statement per row | 40,472 ms | 247.1 rows/s | $8.3\times$ slower |
| **100,000** | Batch (1000, 1 Transaction) | **34,519 ms** | **2,897.0 rows/s** | **Baseline (Fastest)** |
| **100,000** | Batch (1000, Auto-Commit) | 425,291 ms | 235.1 rows/s | $12.3\times$ slower |
| **100,000** | PreparedStatement per row | 400,840 ms | 249.5 rows/s | $11.6\times$ slower |
| **100,000** | Statement per row | *Not Run (Extrapolated)* | ~240 rows/s | $> 12\times$ slower |

*(Data preserved in `Task03/results/B3_insert_times.csv`)*

### Architectural Insights That Contradict Textbook Assumptions:
1. **PreparedStatement Row-by-Row is NOT Faster than Statement**:
   - Textbook theory claims PreparedStatement is substantially faster because the query is parsed once.
   - However, our empirical data shows that at 10,000 records, Statement took 40.4s while PreparedStatement took 41.9s.
   - **Root Cause**: In row-by-row execution, the dominant bottleneck is the **network round-trip latency** (TCP socket transmission + OS context switch + physical disk fsync) per query. This network/disk overhead (~4 ms per row) completely swamps the fractional microsecond difference in query parsing. PreparedStatement must be chosen for **security** (Task B2), not single-row speed.
2. **Batching with One Transaction is the Game Changer**:
   - At 100,000 records, row-by-row insertion requires over **400 seconds** (~6.7 minutes).
   - In contrast, batched PreparedStatement inside a single transaction completed in **34.5 seconds** (2,897 rows/sec), achieving a **$12.3\times$ speedup**.
   - **Why?** It transmits 1,000 rows per network buffer and commits the write-ahead redo log to disk only once upon final completion.

---

## 4. Architectural Recommendations

### Scenario 1: Nightly Import of 100,000 Bus Passes
- **Recommended Method**: **`PreparedStatement` Batching (batch size = 1,000) inside One Single Transaction**.
- **Justification**:
  1. *Throughput*: Takes 34.5 seconds compared to ~7 minutes for row-by-row processing.
  2. *Transactional Integrity*: If an invalid record is encountered at row 95,000, `conn.rollback()` guarantees zero orphan rows are left in the database.
  3. *Zero Heap Spikes*: Batch size of 1,000 bounds memory overhead to < 2 MB, preventing heap overflow.

### Scenario 2: Issuing One Bus Pass at a Counter
- **Recommended Method**: **Single `PreparedStatement` with Auto-Commit**.
- **Justification**:
  1. *Immediate Visibility*: A student standing at the ticket counter requires their pass to be committed immediately so other campus systems (library, dining hall) see the record.
  2. *Batching Inapplicability*: When issuing one pass at a time, batching has nothing to batch; holding records in memory introduces risk of loss during a power glitch.

---

## 5. Architectural Evaluation: Batching vs. Multi-Row INSERT vs. `LOAD DATA INFILE`

| Evaluation Criterion | `PreparedStatement` Batching | Multi-Row `INSERT` (`VALUES (...), (...)`) | MySQL `LOAD DATA INFILE` |
| :--- | :--- | :--- | :--- |
| **Throughput (100K Rows)**| Very High (~34.5 s, 2,897 rows/s) | High (~30–35 s) | **Extreme Speed (< 2 s)** |
| **SQL Injection Security** | **Guaranteed**: Values bound as binary parameters | **Secure**: Uses `?` placeholders | N/A (bypasses SQL parser entirely) |
| **Application Validation**| **Complete**: Full Java domain validation, business rules, custom checks | **Complete**: Java validates before building batch | **None**: Raw text loaded directly by server; invalid data becomes warnings |
| **Detailed Error Reporting**| **High**: `BatchUpdateException.getUpdateCounts()` pinpoints failing row | **Low**: A single error fails the entire multi-row statement | **Poor**: Writes warnings to server log; hard to programmatically catch |
| **Packet Size Constraints**| **Immune**: Driver chunks execution across buffers | **Hazard**: Exceeds MySQL `max_allowed_packet` if batch is too large | **Immune**: Streams directly from disk |
| **Database Portability** | **Universal**: Standard JDBC contract supported on Oracle, PostgreSQL, DB2 | Moderate (Syntax varies across database engines) | **Zero**: Proprietary to MySQL server |
| **File Location Constraint**| File resides on application client machine | File resides on application client machine | File must be physically accessible to MySQL server daemon |

### Conclusion:
While `LOAD DATA INFILE` is raw-speed fastest, it strips away Java application validation, business rule verification, and fine-grained error reporting. **`PreparedStatement` Batching** represents the gold standard enterprise balance of high throughput, transactional safety, and business-layer data integrity.
