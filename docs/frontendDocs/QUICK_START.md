# SecureOps AI - Quick Start (5 Minutes)

**Goal:** Complete one full cycle from project creation to security decision.

---

## ✅ Checklist: Do These 7 Steps

### Step 1: Register a Project
```
URL: http://localhost:5173/projects
Action: Click "Add Project" button
Enter:
  - Project Name: "my-spring-app"
  - Repository URL: "https://github.com/user/my-spring-app"
  - Description: (optional)
Click: "Create"
✅ Result: Project created + 3 policies auto-created
```

### Step 2: Create a Pipeline
```
URL: http://localhost:5173/pipelines
Action: Click "Create Pipeline" button
Fill Form:
  - Provider: "JENKINS" (dropdown)
  - Job Name: "my-build-job"
  - Build Number: "1"
  - Branch: "main"
  - Commit SHA: (leave empty - optional)
Click: "Create Pipeline"
✅ Result: Pipeline registered
```

### Step 3: Create a Scan
```
URL: http://localhost:5173/pipelines (scroll down to Scans section)
OR: URL: http://localhost:5173/scans
Action: Click "Create Scan" button
Fill Form:
  - Pipeline: "my-build-job (Build #1)" (dropdown)
  - Environment: "DEV" (dropdown)
Click: "Create Scan"
✅ Result: Scan created
```

### Step 4: Upload a Trivy Report
```
Get a Trivy JSON file:
  Option A: Use sample from: Sample_Reports/trivy-report.json
  Option B: Run: trivy image --format json --output trivy-report.json myimage:latest
  Option C: Run: trivy fs --format json --output trivy-report.json ./my-code

URL: http://localhost:5173/scans
Action: Find your scan → Click "Upload Report" button
Select: Trivy JSON file from your computer
Click: "Upload"
✅ Result: Report processed, findings extracted
```

### Step 5: View Findings
```
URL: http://localhost:5173/findings
Select Project: "my-spring-app" (dropdown)
Select Scan: Your scan (dropdown)
✅ Result: See all vulnerabilities grouped by severity
```

### Step 6: View Policies
```
URL: http://localhost:5173/policies
✅ Result: See 3 policies auto-created (DEV, STAGING, PROD)
Review: Decision thresholds for each environment
Optional: Click policy to edit thresholds
```

### Step 7: View Security Decision
```
URL: http://localhost:5173/decisions
Find: Your scan's decision
See: "PASS" or "BLOCK" verdict
Click: Decision row for detailed reasoning
✅ Result: Understand why decision was PASS or BLOCK
```

---

## 📍 Page Locations

| Page | URL | Purpose |
|------|-----|---------|
| Dashboard | `http://localhost:5173/` | Overview |
| Projects | `http://localhost:5173/projects` | Create/manage projects |
| Pipelines | `http://localhost:5173/pipelines` | Create/manage pipelines & scans |
| Scans | `http://localhost:5173/scans` | Upload reports |
| Findings | `http://localhost:5173/findings` | View vulnerabilities |
| Policies | `http://localhost:5173/policies` | View/edit security thresholds |
| Decisions | `http://localhost:5173/decisions` | View PASS/BLOCK verdicts |
| Settings | `http://localhost:5173/settings` | Check backend status |

---

## 🎯 What Happens Automatically

When you **Create a Project**:
- ✅ Project is saved
- ✅ Three default policies are auto-created:
  - **DEVELOPMENT:** max 5 critical, 10 high, 100 risk score
  - **STAGING:** max 1 critical, 5 high, 50 risk score
  - **PRODUCTION:** max 0 critical, 1 high, 30 risk score

When you **Upload a Report**:
- ✅ Trivy JSON is parsed
- ✅ Vulnerabilities are extracted as Findings
- ✅ Risk score is calculated
- ✅ Security Decision (PASS/BLOCK) is generated

When **BLOCK Decision** is made:
- ❌ At least one policy threshold exceeded
- ❌ Deployment would be blocked

When **PASS Decision** is made:
- ✅ All policy thresholds satisfied
- ✅ Deployment can proceed

---

## 📊 Example Workflow

```
PROJECT: "spring-boot-api"

POLICIES (auto-created):
├─ DEV:        max 5 critical, 10 high, score 100
├─ STAGING:    max 1 critical, 5 high, score 50
└─ PROD:       max 0 critical, 1 high, score 30

PIPELINE: "jenkins-build-job" (Build #42, branch main)

SCAN: DEV environment

FINDINGS (from Trivy report):
├─ 1 CRITICAL vulnerability
├─ 2 HIGH vulnerabilities
└─ 3 MEDIUM vulnerabilities

RISK SCORE: 24

DECISION:
├─ Policy allows: 5 critical ✅, 10 high ✅, score 100 ✅
├─ Actual:       1 critical ✅, 2 high ✅, score 24 ✅
└─ Verdict:      PASS ✅ (all thresholds met)
```

---

## ❓ FAQ

**Q: Do I need Jenkins?**  
A: No. Jenkins is optional. You can manually create pipelines and upload reports for testing.

**Q: Where do I create a pipeline?**  
A: Go to **Pipelines page** → Click **"Create Pipeline"** button (top right).

**Q: Why is "Create Scan" button disabled?**  
A: You need to create a pipeline first. Go to Pipelines page → Create Pipeline.

**Q: Where do I upload the Trivy report?**  
A: Go to **Scans page** → Find your scan → Click **"Upload Report"** button.

**Q: Can I edit policies?**  
A: Yes. Go to **Policies page** → Click on a policy to edit the thresholds.

**Q: Where is the security decision?**  
A: Go to **Decisions page** → Find your scan → Click to see details.

**Q: Why does my decision show BLOCK?**  
A: Review the decision details. At least one threshold was exceeded. Go to **Findings** to see what was found.

---

## 🔗 Resources

- **Detailed Guide:** `docs/COMPLETE_WORKFLOW_GUIDE.md`
- **Setup Instructions:** `docs/SETUP.md`
- **Jenkins Integration:** `docs/JENKINS_INTEGRATION_TESTING.md`
- **Sample Trivy Report:** `Sample_Reports/trivy-report.json`

---

## ✨ You're Ready!

Open `http://localhost:5173` and start with **Step 1: Register a Project**.

**Happy securing!**

