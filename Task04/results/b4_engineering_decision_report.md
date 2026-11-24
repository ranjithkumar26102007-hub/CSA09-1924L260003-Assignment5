# Task B4: Enterprise Migration Engine & Final Architectural Decision Report

**Course:** CSA09 – Programming in Java  
**Student Name:** R. Ranjith Kumar  
**Register Number:** 1924L260003  
**Institution:** Saveetha School of Engineering, SIMATS  
**Environment:** Java 27, MySQL Community Server 8.4.9, MySQL Connector/J 9.1.0  

---

## 1. Enterprise Expansion Scenario Overview

The university transport infrastructure expands from a single main campus to **12 geographically distributed campuses**, scaling annual volume by $100\times$ from 8,000 passes to approximately **800,000 active bus passes**.

### Key Architectural Challenges:
1. **High Concurrency**: Ticket counters across all 12 campuses issue and query bus passes simultaneously.
2. **Data Consistency**: A student must not hold multiple conflicting passes across campuses for the same validity period.
3. **Fault Containment**: Clerical typos, invalid dates, and corrupt inputs must be rejected with detailed audit logs without disrupting continuous operations.

---

## 2. Dynamic Database Inspection via `DatabaseMetaData`

Using Java JDBC’s `DatabaseMetaData` interface, the migration engine inspected the runtime characteristics of the underlying database server without relying on hardcoded assumptions.

### Actual Inspection Output:
| Property | Value Reported by Driver | Technical Significance |
| :--- | :--- | :--- |
| **Database Product Name** | `MySQL` | Confirms enterprise relational database engine |
| **Database Product Version** | `8.4.9` | Long-Term Support (LTS) release of MySQL 8.4 |
| **JDBC Driver Name** | `MySQL Connector/J` | Official Oracle JDBC Type-4 pure Java driver |
| **JDBC Driver Version** | `mysql-connector-j-9.1.0 (Revision cf2917e)` | Latest stable Connector/J release |
| **JDBC Specification Version** | `4.2` | Supports batching, try-with-resources, and auto-close |
| **Supports Transactions?** | `true` | Full ACID transaction compliance via InnoDB engine |
| **Supports Batch Updates?** | `true` | Allows bulk transmission of statements |
| **Default Transaction Isolation** | `REPEATABLE_READ` | Prevents dirty reads and non-repeatable reads |
| **Discovered Catalog Tables** | `app_user`, `bus_pass`, `enterprise_bus_pass` | Dynamically discovered tables in `csa09_assignment5` |

---

## 3. Migration Pipeline Execution & Audit Reconciliation

The enterprise migration engine `PassMigration.java` was executed to ingest a 50,000-line legacy source file into `enterprise_bus_pass`. The pipeline applies two-tiered validation:
- **Syntactic Validation**: Column count, non-empty tokens, parseable dates, numeric amounts.
- **Business Rule Validation**: Register number alphanumeric length (9–11 chars), positive amounts, Rs 50,000 ceiling, validity date $\ge 2020$, and intra-file duplicate pass ID rejection.

### Migration Reconciliation Summary:
| Reconciliation Metric | Empirical Value | Audit Conformance |
| :--- | :--- | :---: |
| **Source File** | `migration_source.txt` (3,159,582 bytes) | Verified |
| **Total Lines Read** | **50,000 lines** | Verified |
| **Valid Records Migrated** | **49,992 records** | Verified |
| **Corrupted / Invalid Records Rejected** | **8 records** | Verified |
| **Total Migration Time** | **21,051 ms (~21.0 s)** | Throughput: 2,374 rows/s |
| **Database Table Rows After Migration** | **49,992 rows** | Verified via `SELECT count(*)` |
| **Accounting Identity Reconciled?** | **YES**: $49,992 + 8 = 50,000$ | **100% Reconciled** |

### Rejected Records Audit Log:
| Line # | Specific Rejection Reason | Injected Fault Category | Pipeline Action |
| :---: | :--- | :--- | :---: |
| **1001** | `register number is not 9 to 11 characters: INVALID_REG` | Business Rule Violation | Rejected & Logged |
| **2002** | `amount is not positive: -500` | Constraint Violation | Rejected & Logged |
| **3003** | `amount above the Rs 50,000 ceiling: 6500000` | Business Rule Violation | Rejected & Logged |
| **4004** | `expected 7 fields, found 5` | Structural / Format Flaw | Rejected & Logged |
| **5005** | `blank line` | Whitespace Anomaly | Rejected & Logged |
| **6006** | `validity date before 2020: 2018-03-31` | Stale Date Anomaly | Rejected & Logged |
| **7007** | `duplicate pass ID in file: P000001` | Entity Integrity Violation | Rejected & Logged |
| **8008** | `bad date: 31-03-2027` | Format Standard Violation | Rejected & Logged |

---

## 4. Final Architectural Evaluation: Text Files vs. Serialization vs. Database

| System Requirement | Delimited Text Files | Java Native Serialization | Relational Database (MySQL 8.4) |
| :--- | :--- | :--- | :--- |
| **Lookup Latency at 800,000 records** | $O(n)$ linear scan; ~280–400 ms per query | Must deserialize entire object graph before first lookup (> 800 ms) | **$O(\log n)$ B-Tree index lookup (< 1 ms)** on primary key |
| **Concurrent Counter Writes (12 Campuses)** | **Catastrophic**: Silent file corruption or torn lines when multiple processes append concurrently | **Catastrophic**: Rewriting file overwrites concurrent writes; last write wins | **Row-Level Locking (ACID)**: InnoDB row locks guarantee 100% concurrent write safety |
| **Failure Recovery & Rollback** | **Zero**: An interrupted append leaves a torn line; no transaction rollback | **Zero**: A failed write corrupts the whole stream; all records lost | **Full Rollback**: `ROLLBACK` completely undoes failed batches |
| **Schema Evolution (Adding Phone #)** | Requires rewriting all 800,000 lines or complex migration scripts | **Fragile**: Modifying class structure triggers `InvalidClassException` | **Trivial**: Instant `ALTER TABLE enterprise_bus_pass ADD COLUMN ...` |
| **Memory Footprint** | Fixed 8 KB buffer | Requires keeping 800,000 objects in JVM heap (**> 320 MB**) | Constant buffer pool; pages paged to/from disk transparently |
| **Cross-Language Interoperability** | Universal text format | **Zero**: Locked strictly to Java runtime | Universal client support (Python, Go, Node.js, C#, Java) |

---

## 5. Architectural Comparison: One Shared Database vs. 12 Separate Serialized Files

| Strategic Dimension | 12 Separate Serialized Files (One per Campus) | One Shared Relational Database |
| :--- | :--- | :--- |
| **Multi-Campus Duplicate Detection** | **Impossible**: Campus A file has no visibility into Campus B passes. | **Trivial**: Enforced by global unique constraint: `UNIQUE (register_no, valid_until)`. |
| **University-Wide Headcount / Revenue** | Slow and brittle: Must open and deserialize 12 distinct binary files. | **Instant**: Single aggregate query: `SELECT SUM(amount_paise) FROM enterprise_bus_pass`. |
| **Campus Network Disconnection** | Campus continues issuing locally, but sync conflicts occur later. | Handled via local caching or localized edge read replicas. |
| **Schema Migration Overhead** | Must run 12 separate file transformation scripts across campuses. | **Single Step**: One schema migration applied to the centralized database cluster. |
| **Backup and Disaster Recovery** | Highly error-prone: Keeping 12 independent binary files in lockstep. | **Enterprise Standard**: Continuous Write-Ahead Log (WAL) replication and point-in-time snapshots. |

---

## 6. Definitive Engineering Decision

### Final Verdict: **Deploy One Shared Relational Database (MySQL 8.4)**

#### Architectural Justification:
1. **Search Speed**: At 800,000 passes, a sequential text scan takes 300 ms, whereas an indexed B-Tree database lookup takes **< 1 ms**.
2. **Concurrency**: A text file or serialized file cannot support 12 ticket counters appending passes simultaneously without data corruption. MySQL’s row-level locks and ACID transaction isolation eliminate concurrency race conditions.
3. **Data Integrity**: Enforces business rules and constraints (`amount_paise > 0 AND amount_paise <= 5000000`) at the engine level, preventing corrupt entries from entering persistent storage.
4. **Audit and Reconciliation**: Reconciled all 50,000 lines with zero dropped records, proving that high-speed batching ($> 2,300\text{ rows/sec}$) can coexist with complete auditability.
