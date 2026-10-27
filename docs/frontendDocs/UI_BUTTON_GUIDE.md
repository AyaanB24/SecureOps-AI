# SecureOps AI - UI Button & Navigation Guide

**Visual reference for finding every button and page in the application.**

---

## 🏠 Main Navigation (Top Navigation Bar)

The top navigation bar appears on every page and has links to:

```
[SecureOps Logo] | Dashboard | Projects | Pipelines | Scans | Findings | Policies | Decisions | Settings
```

---

## 📍 Page-by-Page Button Locations

### 1️⃣ Projects Page

**URL:** `http://localhost:5173/projects`

```
┌─────────────────────────────────────────────────────────────┐
│ Projects                                                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ [+ Add Project]  ← MAIN BUTTON - Top right                │
│                                                             │
│ Projects Table:                                             │
│ ┌────────────────────────────────────────────────────────┐ │
│ │ Name         │ Repository      │ Created      │ Delete │ │
│ │ my-spring... │ github.com/...  │ 2026-10-27  │   🗑️  │ │
│ │ another-app  │ github.com/...  │ 2026-10-27  │   🗑️  │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                             │
└─────────────────────────────────────────────────────────────┘

WHERE TO CREATE A PROJECT: Click [+ Add Project]
```

### 2️⃣ Pipelines Page

**URL:** `http://localhost:5173/pipelines`

```
┌─────────────────────────────────────────────────────────────┐
│ Pipelines & Scans                                           │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ Select Project: [Dropdown: my-spring-app]                 │
│                                                             │
│ PIPELINES SECTION:                                          │
│ ┌─────────────────────────────────────────────────────┐   │
│ │ Pipelines                                           │   │
│ │                                         [+ Create Pipeline]  │
│ │                       ← BUTTON LOCATION TOP RIGHT   │   │
│ │                                                     │   │
│ │ Pipelines Table:                                    │   │
│ │ ┌──────────────────────────────────────────────────┐ │   │
│ │ │ Provider │ Job Name │ Build # │ Branch │ Created │ │   │
│ │ │ JENKINS  │ my-job   │ 1       │ main   │ ...     │ │   │
│ │ └──────────────────────────────────────────────────┘ │   │
│ └─────────────────────────────────────────────────────┘   │
│                                                             │
│ SCANS SECTION:                                              │
│ ┌─────────────────────────────────────────────────────┐   │
│ │ Scans                                               │   │
│ │                             [+ Create Scan] ← BUTTON│   │
│ │                                                     │   │
│ │ Scans Table:                                        │   │
│ │ ┌──────────────────────────────────────────────────┐ │   │
│ │ │ Environment │ Status      │ Started │ Completed  │ │   │
│ │ │ DEV         │ COMPLETED   │ ...     │ ...        │ │   │
│ │ └──────────────────────────────────────────────────┘ │   │
│ └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘

WHERE TO CREATE A PIPELINE: Click [+ Create Pipeline] in Pipelines section
WHERE TO CREATE A SCAN:     Click [+ Create Scan] in Scans section
```

### 3️⃣ Scans Page

**URL:** `http://localhost:5173/scans`

```
┌─────────────────────────────────────────────────────────────┐
│ Scans                                                       │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ Select Project: [Dropdown: my-spring-app]                 │
│                                                             │
│ Scans Table:                                                │
│ ┌────────────────────────────────────────────────────────┐ │
│ │ ID   │ Status    │ Environment │ Created │ Report    │ │
│ │ 123  │ COMPLETED │ DEV         │ ...     │ [Upload]  │ │
│ │                                      ← BUTTON           │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                             │
└─────────────────────────────────────────────────────────────┘

WHERE TO UPLOAD A REPORT: Click [Upload] button next to scan
```

### 4️⃣ Findings Page

**URL:** `http://localhost:5173/findings`

```
┌─────────────────────────────────────────────────────────────┐
│ Findings                                                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ Filters:                                                    │
│ Project: [Dropdown: my-spring-app]                        │
│ Scan:    [Dropdown: Scan #123]                            │
│                                                             │
│ Findings Table:                                             │
│ ┌────────────────────────────────────────────────────────┐ │
│ │ Severity │ Title              │ Package │ Fixed Ver  │ │
│ │ CRITICAL │ SQL Injection      │ spring  │ 2.0.1      │ │
│ │          │ (click for details)                         │ │
│ │ HIGH     │ XSS Vulnerability  │ commons │ 1.5.2      │ │
│ │ MEDIUM   │ Weak Crypto        │ bcrypt  │ 0.4.1      │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                             │
└─────────────────────────────────────────────────────────────┘

WHERE TO VIEW FINDINGS: Just view the table (no buttons)
WHERE TO VIEW DETAILS:  Click on any finding row
```

### 5️⃣ Policies Page

**URL:** `http://localhost:5173/policies`

```
┌─────────────────────────────────────────────────────────────┐
│ Policies                                                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ Select Project: [Dropdown: my-spring-app]                 │
│                                                             │
│ Policies by Environment:                                    │
│                                                             │
│ DEVELOPMENT Environment:                                    │
│ ┌────────────────────────────────────────────────────────┐ │
│ │ Max Critical:   5   [Edit]  ← BUTTON                   │ │
│ │ Max High:       10  [Edit]  ← BUTTON                   │ │
│ │ Max Risk Score: 100 [Edit]  ← BUTTON                   │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                             │
│ STAGING Environment:                                        │
│ ┌────────────────────────────────────────────────────────┐ │
│ │ Max Critical:   1   [Edit]  ← BUTTON                   │ │
│ │ Max High:       5   [Edit]  ← BUTTON                   │ │
│ │ Max Risk Score: 50  [Edit]  ← BUTTON                   │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                             │
│ PRODUCTION Environment:                                     │
│ ┌────────────────────────────────────────────────────────┐ │
│ │ Max Critical:   0   [Edit]  ← BUTTON                   │ │
│ │ Max High:       1   [Edit]  ← BUTTON                   │ │
│ │ Max Risk Score: 30  [Edit]  ← BUTTON                   │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                             │
└─────────────────────────────────────────────────────────────┘

WHERE TO EDIT A POLICY: Click [Edit] button next to any threshold
```

### 6️⃣ Decisions Page

**URL:** `http://localhost:5173/decisions`

```
┌─────────────────────────────────────────────────────────────┐
│ Security Decisions                                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ Select Project: [Dropdown: my-spring-app]                 │
│                                                             │
│ Decisions Table:                                            │
│ ┌────────────────────────────────────────────────────────┐ │
│ │ Environment │ Decision    │ Risk Score │ Critical │... │ │
│ │ DEV         │ PASS ✅     │ 24         │ 0        │    │ │
│ │ STAGING     │ BLOCK ❌    │ 45         │ 2        │    │ │
│ │ PROD        │ PASS ✅     │ 15         │ 0        │    │ │
│ │ (click any row for details)                             │ │
│ └────────────────────────────────────────────────────────┘ │
│                                                             │
└─────────────────────────────────────────────────────────────┘

WHERE TO VIEW DECISIONS: Just view the table (click row for details)
```

### 7️⃣ Settings Page

**URL:** `http://localhost:5173/settings`

```
┌─────────────────────────────────────────────────────────────┐
│ Settings                                                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│ Backend Connection Status:                                  │
│ ├─ Status: ✅ Connected                                    │
│ ├─ URL: http://localhost:8081 [Copy]  ← BUTTON            │
│ ├─ Latency: 12ms                                           │
│ └─ Last Check: just now                                    │
│                                                             │
│ Application Information:                                    │
│ ├─ Version: 1.0.0                                          │
│ ├─ Tech Stack: React 18, Spring Boot, PostgreSQL          │
│ └─ Backend Port: 8081                                      │
│                                                             │
│ Data & Privacy:                                             │
│ [Privacy Policy Text...]                                   │
│                                                             │
│ Keyboard Shortcuts:                                         │
│ ├─ Ctrl+/ : Open help                                      │
│ └─ ...                                                     │
│                                                             │
└─────────────────────────────────────────────────────────────┘

WHERE TO CHECK CONNECTION: Just view this page (no buttons to click)
WHERE TO COPY API URL: Click [Copy] button
```

---

## 🎯 Quick Button Reference

| Action | Page | Button Location | Button Text |
|--------|------|-----------------|-------------|
| Register a project | Projects | Top right | + Add Project |
| Create a pipeline | Pipelines | Pipelines section, top right | + Create Pipeline |
| Create a scan | Pipelines | Scans section, top right | + Create Scan |
| Upload a report | Scans | Next to scan in table | Upload |
| Edit a policy | Policies | Next to each threshold | Edit |
| View decision details | Decisions | Click any row | (click row) |

---

## 🔍 Common Questions - Button Locations

### Q: Where do I register a project?
**A:** Go to **Projects** page → Click **[+ Add Project]** button (top right)

### Q: Where do I create a pipeline?
**A:** Go to **Pipelines** page → Click **[+ Create Pipeline]** button (top right of Pipelines section)

### Q: Where do I create a scan?
**A:** Go to **Pipelines** page → Scroll to **Scans** section → Click **[+ Create Scan]** button

### Q: Where do I upload a Trivy report?
**A:** Go to **Scans** page → Find your scan in the table → Click **[Upload]** button

### Q: Where do I edit policy thresholds?
**A:** Go to **Policies** page → Find the environment → Click **[Edit]** button next to the threshold

### Q: Where do I view my security decision?
**A:** Go to **Decisions** page → Find your scan → Click the row to see details

---

## 🧭 Navigation Flow

```
START HERE
    ↓
Projects page → [+ Add Project] → Register project
    ↓
Policies page → Review auto-created policies (optional: [Edit])
    ↓
Pipelines page → [+ Create Pipeline] → Create pipeline
    ↓
Pipelines page → Scans section → [+ Create Scan] → Create scan
    ↓
Scans page → [Upload] → Upload Trivy report
    ↓
Findings page → View vulnerabilities
    ↓
Decisions page → View PASS/BLOCK verdict
```

---

## ✨ That's It!

You now know exactly where every button is and what it does.

**Start with Projects page → [+ Add Project]**

