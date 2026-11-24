# Task B2: Adversarial Input Analysis, SQL Injection & Data Integrity Report

**Course:** CSA09 – Programming in Java  
**Student Name:** R. Ranjith Kumar  
**Register Number:** 1924L260003  
**Institution:** Saveetha School of Engineering, SIMATS  
**Environment:** Java 27, MySQL Community Server 8.4.9, Connector/J 9.1.0  

---

## 1. Local Database Environment Configuration

In strict accordance with assignment safety rules, a dedicated local test database was provisioned on MySQL 8.4:
- **Database Schema**: `csa09_assignment5`
- **Table**: `app_user` (created via `Task02/sql/b2_login.sql`)
- **Seeded Credentials**:
  - `admin` (Role: `ADMIN`, Password: `admin123`)
  - `vignesh` (Role: `STUDENT`, Password: `pass123`)
  - `ranjith` (Role: `STUDENT`, Password: `secret456`)

Credential management was decoupled via `DbConfig.java`, ensuring database passwords are never committed to version control.

---

## 2. SQL Injection Evaluation: Statement vs. PreparedStatement

The verification driver `LoginDemo.java` evaluated 8 adversarial test vectors against both the raw string concatenation implementation (`Statement`) and the parameterized query implementation (`PreparedStatement`).

### Actual Execution Console Results:
```text
================================================================================
  CSA09 Assignment 5 - Task B2: SQL Injection Adversarial Test Suite
  Student: R. Ranjith Kumar | Reg No: 1924L260003
================================================================================
Connected to Database: jdbc:mysql://localhost:3306/csa09_assignment5?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC

Executing 8 Adversarial Test Vectors across Unsafe and Safe Handlers:

ID   | Intent / Attack Vector          | Unsafe (Statement)   | Safe (Prepared)    | Verdict           
--------------------------------------------------------------------------------------------------
L1   | Genuine valid student login     | vignesh/STUDENT      | vignesh/STUDENT    | both correct      
L2   | Right user, wrong password      | rejected             | rejected           | both reject       
L3   | Non-existent user account       | rejected             | rejected           | both reject       
L4   | Comment-out password check      | vignesh/STUDENT      | rejected           | UNSAFE BREACHED   
L5   | Tautology payload (' OR '1'='1) | admin/ADMIN          | rejected           | UNSAFE BREACHED   
L6   | Admin bypass with arbitrary pass| admin/ADMIN          | rejected           | UNSAFE BREACHED   
L7   | Apostrophe bypass (' OR ''=')  | rejected             | rejected           | safe defended     
L8   | Inline comment bypass           | admin/ADMIN          | rejected           | UNSAFE BREACHED   
--------------------------------------------------------------------------------------------------
SUMMARY: 4 out of 5 injection vectors successfully bypassed Unsafe Statement.
         PreparedStatement rejected 100% of malicious payloads (0 breaches).
================================================================================
```

### Deep Analysis of Critical Breaches:

#### 1. Attack L6: Administrative Account Takeover (`admin' -- `)
- **Vulnerable Code**:
  ```java
  String sql = "SELECT username, role FROM app_user WHERE username = '" + user + "' AND password = '" + pass + "'";
  ```
- **Attacker Input**: `user = "admin' -- "` and `pass = "invalid_pass"`
- **Assembled SQL**:
  ```sql
  SELECT username, role FROM app_user WHERE username = 'admin' -- ' AND password = 'invalid_pass'
  ```
- **Database Engine Execution**:
  The MySQL lexer interprets `-- ` as the start of an end-of-line comment. The entire password condition `' AND password = 'invalid_pass'` is truncated from the Abstract Syntax Tree (AST). The query returns the `admin` row with role `ADMIN`. The attacker gains full administrative access without knowing the administrator's password.

#### 2. Attack L5: Tautology Authentication Bypass (`' OR '1'='1`)
- **Attacker Input**: `user = "' OR '1'='1"` and `pass = "' OR '1'='1"`
- **Assembled SQL**:
  ```sql
  SELECT username, role FROM app_user WHERE username = '' OR '1'='1' AND password = '' OR '1'='1'
  ```
- **Database Engine Execution**:
  Because `'1'='1'` evaluates to TRUE for every row in the table, the database returns the first row encountered in table scan order (which is `admin`), instantly compromising the system.

### Why `PreparedStatement` Completely Immunizes the System:
In `PreparedStatement`:
```sql
SELECT username, role FROM app_user WHERE username = ? AND password = ?
```
The database compiles the query structure into an immutable execution plan **before** receiving any data. When `ps.setString(1, "admin' -- ")` is called, the string is sent as a pure data parameter across the binary protocol. The MySQL parser never re-interprets the text. It searches for an account whose exact username is the literal 10-character string `admin' -- `. Because no such account exists, 0 rows are returned and the login is rejected.

---

## 3. Adversarial Bus-Pass File Data Analysis

Using `AdversarialDataTest.java`, 8 corrupted and hostile file inputs were evaluated against the delimited file store.

### Empirical Test Matrix:
| Case ID | Adversarial Input Vector | Observed Store Behaviour | Architectural Design Insight |
| :---: | :--- | :--- | :--- |
| **C1** | Wrong field count (5 instead of 7) | `Skipped: line 2 (expected 7, got 5)` | **Format Resynchronization**: The newline character survives, allowing the reader to skip only the corrupted row without corrupting subsequent rows. |
| **C2** | Non-numeric amount (`FIVELAKH`) | `Skipped: line 3 (non-numeric amount)` | **Per-Field Type Enforcement**: Numeric parse failures (`NumberFormatException`) are trapped locally, isolating error blast radius to exactly one record. |
| **C3** | Empty line / trailing whitespace | `Skipped silently (not a record)` | **Defensive Filtering**: Blank lines are checked via `line.isBlank()` and skipped cleanly without generating bogus diagnostic warnings. |
| **C4** | Non-ISO date format (`31-03-2027`) | `Skipped: line 5 (malformed date)` | **Strict Lexical Validation**: Prevents ambiguous date interpretations (e.g. DD-MM-YYYY vs MM-DD-YYYY). |
| **C5** | Pipe delimiter inside name (`Name\|With`) | `Skipped: line 6 (expected 7, got 9)` | **Delimiter Injection Vulnerability**: The pipe character splits the single name field into 3 fields, breaking the 7-field structure. |
| **C6** | Valid pass after 5 consecutive corruptions | `Loaded cleanly: P2006` | **Error Containment**: A cluster of corrupt lines does not cascade or abort downstream valid records. |
| **C7** | Torn write / EOF cut mid-line | `Skipped: line 8 (expected 7, got 3)` | **Torn Write Containment**: A truncated line from a sudden power outage is discarded cleanly; all preceding records remain intact. |
| **C8** | Missing storage file | `NoSuchFileException thrown` | **Explicit State Signaling**: A missing file throws a hard exception, preventing an absent file from being misread as "0 students hold bus passes". |

---

## 4. Architectural Comparison: Delimiter Injection vs. SQL Injection

There is a profound architectural parallel between delimiter injection in text files and SQL injection in databases:
1. **The Shared Vulnerability Root**: In both paradigms, **untrusted user data is placed directly into a channel where a parser expects syntactic control structure**.
   - In SQL: An unescaped quote (`'`) tricks the SQL parser into closing a string literal and opening executable code.
   - In Delimited Text: An unescaped pipe (`|`) tricks the CSV splitter into creating a new record column.
2. **The Defense**:
   - For SQL: Parameterized queries (`PreparedStatement`) separate the code AST from data buffers.
   - For File Storage: Data fields must either be strictly validated to reject delimiters (`validate(!name.contains("|"))`), quote-escaped, or migrated to typed binary formats or relational databases.
