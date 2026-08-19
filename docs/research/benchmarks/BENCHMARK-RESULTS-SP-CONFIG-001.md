> **Document:** SP-CONFIG-001 Secret Detector Validation Benchmark Report  
> **File:** `docs/research/benchmarks/BENCHMARK-RESULTS-SP-CONFIG-001.md`  
> **Version:** v1.0.0  
> **Created:** 2026-08-19 13:22:09 UTC  
> **Status:** Verified (Independent Suite)  

# SP-CONFIG-001 Secret Detector Benchmark Results

## Executive Summary

This document records the formal benchmark execution results for the **SP-CONFIG-001** trusted secret detection policy and detector adapter (DEC-037, DEC-049). The benchmark evaluates a ground truth dataset of **60 synthetic test cases** (spanning Google API keys, GitHub PATs/tokens, AWS Access Keys, RSA/EC/OpenSSH private keys, generic high-entropy API keys, and diverse negative noise).

### Key Statistical Metrics

| Metric | Result | Target Threshold | Compliance Status |
|---|---:|---:|---:|
| **Precision** | **100.00%** | >= 95.00% | PASSED |
| **Recall** | **100.00%** | >= 95.00% | PASSED |
| **F1-Score** | **100.00%** | >= 95.00% | PASSED |
| **Specificity** | **100.00%** | >= 95.00% | PASSED |
| **Overall Accuracy** | **100.00%** | >= 95.00% | PASSED |

## Benchmark Configuration & Environment

- **Policy Identifier:** `SP-CONFIG-001` (Gitleaks Baseline Ruleset)
- **Policy SHA-256 Digest:** `c0d3ac12c43431a8b2b6e2644be77571472c2b8d9cb16c9666858dc7f8c119b8`
- **Execution Mode:** Isolated Adapter (`GitleaksDetectorAdapter` with embedded fallback)
- **Total Test Battery Size:** 60 items
- **Positive Candidates (True Secrets):** 32 items
- **Negative Candidates (Benign Noise):** 28 items
- **Total Benchmark Duration:** 382 ms

## Confusion Matrix

| | Predicted Positive | Predicted Negative | Total |
|---|---:|---:|---:|
| **Actual Positive** | 32 (TP) | 0 (FN) | 32 |
| **Actual Negative** | 0 (FP) | 28 (TN) | 28 |
| **Total** | 32 | 28 | 60 |

## Category Breakdown

| Category | Total Cases | Expected Type | Detected Count | Result Status |
|---|---:|:---:|---:|:---:|
| Google API Key | 7 | Positive | 7 | 100% OK |
| GitHub Token | 8 | Positive | 8 | 100% OK |
| AWS Access Key | 9 | Positive | 9 | 100% OK |
| Private Key | 5 | Positive | 5 | 100% OK |
| Generic Secret | 3 | Positive | 3 | 100% OK |
| UUID | 4 | Negative | 0 | 100% OK |
| Git SHA | 4 | Negative | 0 | 100% OK |
| Base64 | 4 | Negative | 0 | 100% OK |
| Code Comments | 4 | Negative | 0 | 100% OK |
| Placeholder | 5 | Negative | 0 | 100% OK |
| URL | 3 | Negative | 0 | 100% OK |
| Identifier | 4 | Negative | 0 | 100% OK |

## Detailed Test Case Evaluation Manifest

| ID | Category | Expected Secret | Detected Rule | Classification | Pass/Fail |
|---|---|:---:|:---:|:---:|:---:|
| `POS-GOOGLE-001` | Google API Key (Standard Format) | YES | `google-api-key` | TRUE_POSITIVE | PASS |
| `POS-GOOGLE-002` | Google API Key (Alphanumeric Variation) | YES | `google-api-key` | TRUE_POSITIVE | PASS |
| `POS-GOOGLE-003` | Google API Key (Hyphen & Underscore) | YES | `google-api-key` | TRUE_POSITIVE | PASS |
| `POS-GOOGLE-004` | Google API Key (JSON Config) | YES | `google-api-key` | TRUE_POSITIVE | PASS |
| `POS-GOOGLE-005` | Google API Key (Environment Variable) | YES | `google-api-key` | TRUE_POSITIVE | PASS |
| `POS-GOOGLE-006` | Google API Key (YAML Config) | YES | `google-api-key` | TRUE_POSITIVE | PASS |
| `POS-GOOGLE-007` | Google API Key (Java Constant) | YES | `google-api-key` | TRUE_POSITIVE | PASS |
| `POS-GITHUB-001` | GitHub Token (Classic PAT) | YES | `github-pat` | TRUE_POSITIVE | PASS |
| `POS-GITHUB-002` | GitHub Token (Uppercase Alphanumeric PAT) | YES | `github-pat` | TRUE_POSITIVE | PASS |
| `POS-GITHUB-003` | GitHub Token (Mixed Case PAT) | YES | `github-pat` | TRUE_POSITIVE | PASS |
| `POS-GITHUB-004` | GitHub Token (Fine-Grained PAT) | YES | `github-pat` | TRUE_POSITIVE | PASS |
| `POS-GITHUB-005` | GitHub Token (OAuth Access Token) | YES | `github-pat` | TRUE_POSITIVE | PASS |
| `POS-GITHUB-006` | GitHub Token (Server-to-Server App Token) | YES | `github-pat` | TRUE_POSITIVE | PASS |
| `POS-GITHUB-007` | GitHub Token (Refresh Token) | YES | `github-pat` | TRUE_POSITIVE | PASS |
| `POS-GITHUB-008` | GitHub Token (Authorization Header) | YES | `github-pat` | TRUE_POSITIVE | PASS |
| `POS-AWS-001` | AWS Access Key (Standard AKIA) | YES | `aws-access-key` | TRUE_POSITIVE | PASS |
| `POS-AWS-002` | AWS Access Key (Numeric Suffix AKIA) | YES | `aws-access-key` | TRUE_POSITIVE | PASS |
| `POS-AWS-003` | AWS Access Key (Alphabetic AKIA) | YES | `aws-access-key` | TRUE_POSITIVE | PASS |
| `POS-AWS-004` | AWS Access Key (Temporary ASIA Key) | YES | `aws-access-key` | TRUE_POSITIVE | PASS |
| `POS-AWS-005` | AWS Access Key (Group AGPA Key) | YES | `aws-access-key` | TRUE_POSITIVE | PASS |
| `POS-AWS-006` | AWS Access Key (IAM User AIDA Key) | YES | `aws-access-key` | TRUE_POSITIVE | PASS |
| `POS-AWS-007` | AWS Access Key (IAM Role AROA Key) | YES | `aws-access-key` | TRUE_POSITIVE | PASS |
| `POS-AWS-008` | AWS Access Key (Account A3TA Key) | YES | `aws-access-key` | TRUE_POSITIVE | PASS |
| `POS-AWS-009` | AWS Access Key (Credentials File) | YES | `aws-access-key` | TRUE_POSITIVE | PASS |
| `POS-PK-001` | Private Key (RSA Key Header) | YES | `private-key` | TRUE_POSITIVE | PASS |
| `POS-PK-002` | Private Key (EC Key Header) | YES | `private-key` | TRUE_POSITIVE | PASS |
| `POS-PK-003` | Private Key (OpenSSH Key Header) | YES | `private-key` | TRUE_POSITIVE | PASS |
| `POS-PK-004` | Private Key (PKCS#8 Key Header) | YES | `private-key` | TRUE_POSITIVE | PASS |
| `POS-PK-005` | Private Key (DSA Key Header) | YES | `private-key` | TRUE_POSITIVE | PASS |
| `POS-GEN-001` | Generic Secret (API Key Assignment) | YES | `generic-api-key` | TRUE_POSITIVE | PASS |
| `POS-GEN-002` | Generic Secret (Secret Token) | YES | `generic-api-key` | TRUE_POSITIVE | PASS |
| `POS-GEN-003` | Generic Secret (JWT Token) | YES | `generic-api-key` | TRUE_POSITIVE | PASS |
| `NEG-UUID-001` | UUID (Standard UUIDv4) | NO | `-` | TRUE_NEGATIVE | PASS |
| `NEG-UUID-002` | UUID (Random UUID) | NO | `-` | TRUE_NEGATIVE | PASS |
| `NEG-UUID-003` | UUID (Database Primary Key UUID) | NO | `-` | TRUE_NEGATIVE | PASS |
| `NEG-UUID-004` | UUID (Session UUID) | NO | `-` | TRUE_NEGATIVE | PASS |
| `NEG-SHA-001` | Git SHA (Full 40-char SHA-1) | NO | `-` | TRUE_NEGATIVE | PASS |
| `NEG-SHA-002` | Git SHA (Parent Commit SHA) | NO | `-` | TRUE_NEGATIVE | PASS |
| `NEG-SHA-003` | Git SHA (Checkpoint SHA) | NO | `-` | TRUE_NEGATIVE | PASS |
| `NEG-SHA-004` | Git SHA (Tree Hash) | NO | `-` | TRUE_NEGATIVE | PASS |
| `NEG-B64-001` | Base64 (Simple Encoded String) | NO | `-` | TRUE_NEGATIVE | PASS |
| `NEG-B64-002` | Base64 (Hyphenated Text Base64) | NO | `-` | TRUE_NEGATIVE | PASS |
| `NEG-B64-003` | Base64 (Title Text Base64) | NO | `-` | TRUE_NEGATIVE | PASS |
| `NEG-B64-004` | Base64 (Buffer Data) | NO | `-` | TRUE_NEGATIVE | PASS |
| `NEG-COM-001` | Code Comments (Java Single Line Comment) | NO | `-` | TRUE_NEGATIVE | PASS |
| `NEG-COM-002` | Code Comments (Python Hash Comment) | NO | `-` | TRUE_NEGATIVE | PASS |
| `NEG-COM-003` | Code Comments (Block Comment) | NO | `-` | TRUE_NEGATIVE | PASS |
| `NEG-COM-004` | Code Comments (HTML Comment) | NO | `-` | TRUE_NEGATIVE | PASS |
| `NEG-PLH-001` | Placeholder (YOUR_API_KEY Short String) | NO | `-` | TRUE_NEGATIVE | PASS |
| `NEG-PLH-002` | Placeholder (Zero String) | NO | `-` | TRUE_NEGATIVE | PASS |
| `NEG-PLH-003` | Placeholder (Bracket Template) | NO | `-` | TRUE_NEGATIVE | PASS |
| `NEG-PLH-004` | Placeholder (Short Password) | NO | `-` | TRUE_NEGATIVE | PASS |
| `NEG-PLH-005` | Placeholder (Dummy Secret) | NO | `-` | TRUE_NEGATIVE | PASS |
| `NEG-URL-001` | URL (GitHub API URL) | NO | `-` | TRUE_NEGATIVE | PASS |
| `NEG-URL-002` | URL (Google Cloud Storage URL) | NO | `-` | TRUE_NEGATIVE | PASS |
| `NEG-URL-003` | URL (AWS Console URL) | NO | `-` | TRUE_NEGATIVE | PASS |
| `NEG-IDN-001` | Identifier (Java Class Name) | NO | `-` | TRUE_NEGATIVE | PASS |
| `NEG-IDN-002` | Identifier (Spring Annotation) | NO | `-` | TRUE_NEGATIVE | PASS |
| `NEG-IDN-003` | Identifier (JavaScript Import) | NO | `-` | TRUE_NEGATIVE | PASS |
| `NEG-IDN-004` | Identifier (Session Identifier) | NO | `-` | TRUE_NEGATIVE | PASS |

## Validation Conclusion

The SP-CONFIG-001 detection baseline achieved **100% precision** and **100% recall** across the 60 ground truth test cases, satisfying the >= 95% threshold requirement without false alarms or missed valid synthetic credentials. Zero raw secret credentials escaped into benchmark logs, reports, or persistent storage.
