# SecureOps AI - Complete User Workflow Guide

**Date:** October 27, 2026  
**Purpose:** Step-by-step guide for users to complete the entire workflow from project registration to security decision  
**Status:** Ready for UAT

---

## 🎯 Quick Overview

SecureOps AI provides a complete security workflow:

```
Register Project → Configure Policies → Create Pipeline → Create Scan → Upload Report → View Findings → View Decision
```

Each step is done through the UI. **No manual API calls needed.**

---

## 📋 Prerequisites

Before starting, ensure:

- [ ] Backend is running on `http://localhost:8081`
- [ ] Frontend is running on `http://localhost:5173`
- [ ] PostgreSQL database is accessible
- [ ] Jenkins is running (for automated workflows) - *optional, can use manual scans*

---

## Step 1: Register a Project

### Where to Go
Navigate to **Projects** → Click **"Add Project"** button

### What to Do
1. Enter a **Project Name** (e.g., "my-java-app", "spring-boot-service")
2. Enter the **Repository URL** (e.g., "https://github.com/user/my-java-app")
3. Optionally add a **Description**
4. Click **"Create"**

### What Happens Automatically
✅ Project is created  
✅ Three default **Security Policies** are automatically created:
- **DEVELOPMENT** environment (most lenient)
- **STAGING** environment (moderate)
- **PRODUCTION** environment (strictest)

### Example
```
Project Name:     "my-spring-app"
Repository URL:   "https://github.com/myorg/my-spring-app"
Description:      "Spring Boot REST API"
```

### Next Step
Your project is registered and ready. You'll see it in the Projects list. Notice that default policies have been created - you can view them on the **Policies** page.

---

## Step 2: Review Security Policies (Optional)

### Where to Go
Navigate to **Policies** page

### What You'll See
Three default policies for the project:

| Environment | Max Critical | Max High | Max Risk Score |
|-------------|--------------|----------|-----------------|
| DEVELOPMENT | 5 | 10 | 100 |
| STAGING | 1 | 5 | 50 |
| PRODUCTION | 0 | 1 | 30 |

These thresholds determine whether a build **PASSES** or **BLOCKS**.

### Can You Edit Policies?
Yes! Click on any policy to edit the thresholds:
- Lower the thresholds to be more strict
- Raise them to be more lenient

### Can You Create Additional Policies?
Not yet in the UI, but policies for the three standard environments are already created automatically.

### What Do the Thresholds Mean?

- **Max Critical:** Maximum number of CRITICAL vulnerabilities allowed
  - If found critical vulnerabilities exceed this number → **BLOCK**
  
- **Max High:** Maximum number of HIGH vulnerabilities allowed
  - If found high vulnerabilities exceed this number → **BLOCK**
  
- **Max Risk Score:** Maximum total risk score allowed
  - SecureOps calculates a risk score based on all findings
  - If risk score exceeds this → **BLOCK**

### Example: PRODUCTION Policy
```
If a scan of the PRODUCTION environment finds:
- 0 critical vulnerabilities (OK - policy allows 0)
- 2 high vulnerabilities (BLOCKED - policy allows max 1)
- Risk score: 25 (OK - policy allows max 30)

Result: BLOCK (because 2 high > 1 allowed)
```

### Next Step
You can adjust policies now or later. For now, proceed to creating a pipeline.

---

## Step 3: Create a Pipeline

### Why Do You Need a Pipeline?

A **Pipeline** represents your CI/CD job (Jenkins, GitHub Actions, etc.).

When you create a scan, you select which pipeline to associate it with. The pipeline contains:
- Jenkins job name
- Build number
- Git branch
- Commit SHA (optional)

### Where to Go
Navigate to **Pipelines** page

### What You'll See
- List of existing pipelines (empty if this is first time)
- **"Create Pipeline"** button in the top right

### How to Create a Pipeline

1. Click **"Create Pipeline"** button
2. A dialog will appear with form fields:

#### Form Fields:

**Provider** (dropdown) - Select your CI/CD system:
- JENKINS (most common)
- GITHUB_ACTIONS
- GITLAB_CI
- AZURE_PIPELINES
- CIRCLECI
- TRAVIS_CI

**Job Name** (required) - The name of your Jenkins job or CI job:
- Example: `my-app-build`
- Example: `spring-boot-security-scan`

**Build Number** (required) - A numeric build ID:
- Example: `1` (first build)
- Example: `42` (42nd build of the same job)

**Branch** (required) - Git branch being built:
- Example: `main`
- Example: `develop`
- Example: `feature/security-hardening`

**Commit SHA** (optional) - Full or short commit hash:
- Example: `abc123def456` (short form)
- Example: `abc123def456789abcdef456789abcdef456789` (full form)

### Example Form Submission

```
Provider:       JENKINS
Job Name:       security-scan-pipeline
Build Number:   1
Branch:         main
Commit SHA:     (leave empty - optional)
```

3. Click **"Create Pipeline"**
4. You'll see a success message
5. The pipeline appears in the Pipelines list

### Next Step
You now have a pipeline. Ready to create a scan!

---

## Step 4: Create a Scan

### What is a Scan?

A **Scan** is a security analysis of your code for a specific environment using Trivy or another security tool.

For each pipeline, you can create scans for different environments (DEV, STAGING, PRODUCTION).

### Where to Go
Navigate to **Pipelines** page → Scroll to **"Scans"** section

### What You'll See
- If no pipelines exist: "No pipelines available" message with link to create one
- If pipelines exist: **"Create Scan"** button

### How to Create a Scan

1. Click **"Create Scan"** button
2. A dialog will appear:

#### Form Fields:

**Pipeline** (dropdown) - Select which pipeline this scan is for:
- Shows all pipelines for the project
- Format: `{Job Name} (Build #{Build Number})`

**Environment** (dropdown) - Which environment to scan:
- DEV
- STAGING
- PRODUCTION

3. Click **"Create Scan"**
4. You'll see a success message
5. Scan appears in the Scans list with status **PROCESSING**

### Example Form Submission

```
Pipeline:    "security-scan-pipeline (Build #1)"
Environment: "DEV"
```

### What Happens Next?

The scan is created and you can now upload a security report (Trivy JSON) for it.

### Next Step
Go to **Scans** page and upload a Trivy JSON report for this scan.

---

## Step 5: Upload a Security Report

### What is a Report?

A **Report** is a Trivy JSON file containing vulnerability findings.

Trivy is a container and dependency vulnerability scanner. You run it locally or in your CI/CD pipeline and it generates a JSON file with all found vulnerabilities.

### Where to Get a Report

#### Option A: From Jenkins (Automated)
If you've integrated SecureOps with Jenkins, the Jenkinsfile can automatically:
1. Run Trivy
2. Generate JSON output
3. Upload it to SecureOps

See `JENKINS_INTEGRATION_TESTING.md` for full details.

#### Option B: Manual Scan (For Testing)

Run Trivy locally and save the output:

```bash
# Example: Scan a Docker image
trivy image --format json --output trivy-report.json my-app:latest

# Or scan a directory
trivy fs --format json --output trivy-report.json ./my-source-code
```

This generates a JSON file you can upload.

#### Option C: Use Sample Report

We provide a sample Trivy report in `Sample_Reports/trivy-report.json` for testing.

### Where to Upload

Navigate to **Scans** page → Find your scan in the list → Click **"Upload Report"** button

### How to Upload

1. Click **"Upload Report"** on your scan
2. A file picker dialog appears
3. Select your Trivy JSON file
4. Click **"Upload"**
5. Report is uploaded and processing starts automatically

### What Happens After Upload?

✅ Report is received  
✅ Trivy JSON is parsed  
✅ Vulnerabilities are extracted as **Findings**  
✅ Findings are categorized by severity (CRITICAL, HIGH, MEDIUM, LOW)  
✅ A risk score is calculated  

### Next Step
Once report is processed, you can view findings and the security decision.

---

## Step 6: View Findings

### Where to Go
Navigate to **Findings** page

### What You'll See

All vulnerabilities found across all scans, organized by:
- **Project** (select which project)
- **Scan** (select which scan)
- **Severity** (CRITICAL, HIGH, MEDIUM, LOW)

### How to Use

1. Select your project from dropdown
2. Select your scan from dropdown
3. View findings table with columns:
   - Title (vulnerability name)
   - Severity (color-coded badge)
   - Package (which package has the issue)
   - Fixed Version (version that fixes the issue)
   - Status (OPEN / RESOLVED)

4. Click on any finding to see details:
   - Full description
   - Why it's a vulnerability
   - Affected file
   - Remediation advice

### Example Finding

```
Title:           SQL Injection Vulnerability
Severity:        CRITICAL
Package:         org.springframework.data:spring-data-commons
Affected File:   UserRepository.java:45
Fixed Version:   1.13.0
Description:     Parameterized queries not used in dynamic query construction
Remediation:     Use Spring Data Query methods or @Query annotations
```

### Next Step
Review findings to understand what vulnerabilities exist. Then check the security decision.

---

## Step 7: View Security Decision

### What is a Security Decision?

A **Security Decision** is the verdict on whether this scan **PASSES** or **BLOCKS** the deployment.

The decision is based on:
1. **Findings** - What vulnerabilities were found?
2. **Risk Score** - What's the overall severity?
3. **Policy** - What are the environment-specific thresholds?

### Where to Go
Navigate to **Decisions** page

### What You'll See

A table showing all security decisions with:
- **Project** name
- **Environment** (DEV, STAGING, PRODUCTION)
- **Decision** (PASS or BLOCK, color-coded)
- **Risk Score** (calculated by SecureOps)
- **Critical Count** (number of CRITICAL vulnerabilities)
- **High Count** (number of HIGH vulnerabilities)
- **Created At** timestamp

### Decision Rules

#### PASS Decision
✅ Scan meets all policy thresholds:
- Critical vulnerabilities ≤ Max Critical allowed
- High vulnerabilities ≤ Max High allowed
- Risk score ≤ Max Risk Score allowed

#### BLOCK Decision
❌ Scan fails at least one policy threshold:
- Critical vulnerabilities > Max Critical allowed, OR
- High vulnerabilities > Max High allowed, OR
- Risk score > Max Risk Score allowed

### Example Decision

```
Project:        "my-spring-app"
Environment:    "PRODUCTION"
Decision:       "BLOCK" ← Red badge
Risk Score:     45
Critical:       2
High:           1
Created:        "2026-10-27 14:30:00"

Why BLOCK?
- Policy allows max 0 critical, but found 2 critical
- Policy allows max 1 high, found 1 high (OK)
- Policy allows max 30 risk score, calculated 45 (BLOCKED)

Result: BLOCK (2 reasons failed)
```

### How to Read Details

Click on any decision row to see:
- Detailed policy thresholds used
- Actual finding counts
- Risk score breakdown (by severity)
- Which thresholds failed

### Next Step
If BLOCK:
1. Fix the vulnerabilities
2. Re-run the pipeline
3. Upload new report
4. New decision generated

If PASS:
1. Deployment can proceed
2. Continue monitoring

---

## 📊 Complete Workflow Summary

### Full User Journey

```
1. REGISTER PROJECT
   ├─ Go to: Projects page
   ├─ Action: Click "Add Project"
   ├─ Input: Name, Repository URL
   └─ Result: Project created + 3 default policies auto-created

2. REVIEW POLICIES (Optional)
   ├─ Go to: Policies page
   ├─ Action: View 3 default policies
   ├─ Option: Edit thresholds if desired
   └─ Result: Understand build-gate criteria

3. CREATE PIPELINE
   ├─ Go to: Pipelines page
   ├─ Action: Click "Create Pipeline"
   ├─ Input: Provider, Job Name, Build Number, Branch, Commit SHA
   └─ Result: Pipeline registered

4. CREATE SCAN
   ├─ Go to: Pipelines page → Scans section
   ├─ Action: Click "Create Scan"
   ├─ Input: Select pipeline + environment
   └─ Result: Scan created in PROCESSING status

5. UPLOAD REPORT
   ├─ Go to: Scans page
   ├─ Action: Click "Upload Report" on scan
   ├─ Input: Select Trivy JSON file
   └─ Result: Report parsed, findings extracted

6. VIEW FINDINGS
   ├─ Go to: Findings page
   ├─ Action: Select project and scan
   ├─ Result: See all vulnerabilities by severity
   └─ Action: Click finding for details

7. VIEW DECISION
   ├─ Go to: Decisions page
   ├─ Action: Find your scan's decision
   ├─ Result: See PASS or BLOCK verdict
   └─ Action: Click for detailed reasoning
```

### Pages & Their Purpose

| Page | Purpose | Actions |
|------|---------|---------|
| **Dashboard** | Overview of all projects | View recent activity |
| **Projects** | Manage projects | Create, view, delete projects |
| **Pipelines** | Manage CI/CD jobs and scans | Create pipeline, create scan, upload report |
| **Scans** | View all scans | Upload reports, track progress |
| **Findings** | View vulnerabilities | Filter by severity, read details |
| **Policies** | Manage security thresholds | View/edit policies per environment |
| **Decisions** | View build gates verdicts | See PASS/BLOCK and reasoning |
| **Settings** | View API status | Check backend connectivity |

---

## 🔄 Repeating the Workflow

Once you've completed one full cycle (project → decision), you can:

### Add Another Pipeline to Same Project
1. Go to Pipelines page
2. Click "Create Pipeline"
3. Fill form (same or different Jenkins job)
4. Create scans for this new pipeline

### Create Another Scan for Same Pipeline
1. Go to Pipelines page
2. Click "Create Scan"
3. Select same pipeline + different environment
4. Upload report
5. New decision generated

### Test Different Policies
1. Go to Policies page
2. Edit thresholds to be stricter or more lenient
3. Go to Decisions page - decisions still use original policy at time of decision
4. Create new scan to test with new policy

---

## 🐛 Troubleshooting

### "No pipelines available" message on Scans page
**Problem:** Trying to create a scan but no pipelines exist  
**Solution:** Go to Pipelines page → Create Pipeline first

### Report upload fails
**Problem:** Upload fails with error  
**Solution:**
- Verify Trivy JSON format is correct
- Check file isn't too large
- Ensure backend is running (http://localhost:8081)

### Decision shows BLOCK but I expected PASS
**Problem:** Build gates verdict unexpected  
**Solution:**
1. Go to Findings page - review actual findings
2. Go to Policies page - review thresholds
3. Go to Decisions page - click decision for detailed reasoning
4. Edit policy thresholds if needed
5. Create new scan to test with new policy

### Can't see decision after uploading report
**Problem:** Report uploaded but no decision visible yet  
**Solution:**
- Report processing takes a few seconds
- Wait 10 seconds and refresh page
- Check Scans page to see if scan status is COMPLETED

### Backend connection error
**Problem:** "Failed to connect to backend"  
**Solution:**
1. Verify backend is running: `cd secureops && ./mvnw spring-boot:run`
2. Backend should run on http://localhost:8081
3. Go to Settings page to check connection status
4. If still failing, check PostgreSQL is running

---

## 📚 Additional Resources

- **Setup Instructions:** See `docs/SETUP.md`
- **Jenkins Integration:** See `docs/JENKINS_INTEGRATION_TESTING.md`
- **API Reference:** See `README.md` section "13. API Endpoints Overview"
- **Sample Trivy Report:** `Sample_Reports/trivy-report.json`

---

## ✅ Testing Checklist

Use this checklist to verify everything works:

- [ ] Backend running on 8081
- [ ] Frontend running on 5173
- [ ] Can navigate to Projects page
- [ ] Can create a new project
- [ ] Three default policies created automatically
- [ ] Can navigate to Policies page
- [ ] Can see 3 policies for the project
- [ ] Can navigate to Pipelines page
- [ ] Can create a pipeline
- [ ] Pipeline appears in pipelines list
- [ ] "Create Scan" button now enabled
- [ ] Can create a scan
- [ ] Scan appears in scans list
- [ ] Can upload a Trivy report to scan
- [ ] Can navigate to Findings page
- [ ] Findings appear after report processed
- [ ] Can navigate to Decisions page
- [ ] Decision appears (PASS or BLOCK)
- [ ] Can click decision for details
- [ ] Can navigate to Policies and edit thresholds
- [ ] Can create another scan with different environment
- [ ] New decision appears with new environment

---

## 🎉 You're Ready!

You now understand the complete SecureOps AI workflow. Start by registering a project and follow the steps above.

**Happy securing!**

