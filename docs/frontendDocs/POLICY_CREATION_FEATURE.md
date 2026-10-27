# Policy Creation Feature - User Guide

**Date:** October 27, 2026  
**Status:** ✅ IMPLEMENTED  
**Build:** ✅ SUCCESS (0 TypeScript errors)

---

## ✅ What Was Fixed

The Policies page now has a **[+ Create Policy]** button allowing users to create new security policies when:
- They select a project from the dropdown
- That project has fewer than 3 policies (not all environments have policies yet)

---

## 📍 WHERE TO CREATE A POLICY

### Go to the Policies Page
```
URL: http://localhost:5173/policies
```

### Select Your Project
```
Dropdown: Choose your project
```

### Click the Create Button
```
Button: [+ Create Policy]  ← Appears top right next to "Project Policies" heading
```

### Fill the Form
```
1. Environment (dropdown)
   - Choose from DEVELOPMENT, STAGING, or PRODUCTION
   - Only shows environments that don't already have policies

2. Max Critical Findings (number input)
   - Default: 5
   - 0 = zero tolerance (no CRITICAL vulnerabilities allowed)

3. Max High Findings (number input)
   - Default: 10
   - Maximum number of HIGH severity vulnerabilities allowed

4. Max Risk Score (number input)
   - Default: 100
   - Maximum cumulative risk score allowed
```

### Save
```
Click: [Create Policy] button
Confirm: Confirm creation dialog appears
Result: ✅ Policy created, appears in policy list
```

---

## 📋 Step-by-Step Example

### Scenario
You have a project with only DEVELOPMENT policy, and you want to add STAGING and PRODUCTION policies.

### Steps

**1. Open Policies page**
```
Go to: http://localhost:5173/policies
```

**2. Select your project**
```
From dropdown: Select your project
Current policies shown: DEVELOPMENT only
```

**3. Click Create Policy**
```
Button: [+ Create Policy]
Location: Top right of "Project Policies" section
```

**4. Create STAGING policy**
```
Environment:        STAGING (from dropdown)
Max Critical:       1
Max High:           5
Max Risk Score:     50
Click:              "Create Policy"
Confirm:            "Yes, create"
Result:             ✅ STAGING policy created
```

**5. Create PRODUCTION policy**
```
Click:              [+ Create Policy] (button reappears)
Environment:        PRODUCTION
Max Critical:       0
Max High:           1
Max Risk Score:     30
Click:              "Create Policy"
Confirm:            "Yes, create"
Result:             ✅ PRODUCTION policy created
```

**6. Verify**
```
Now you have 3 policies visible:
├─ DEVELOPMENT (5 critical, 10 high, 100 risk score)
├─ STAGING (1 critical, 5 high, 50 risk score)
└─ PRODUCTION (0 critical, 1 high, 30 risk score)

[+ Create Policy] button disappears (all 3 environments have policies)
```

---

## 🎯 When Does the Create Button Appear?

The **[+ Create Policy]** button appears when:
- ✅ You've selected a project
- ✅ That project has fewer than 3 policies
- ✅ There are still environments without policies

The button **disappears** when:
- ❌ All 3 environments (DEV, STAGING, PROD) have policies
- ❌ No project is selected

---

## 🔍 Troubleshooting

### Q: I don't see the [+ Create Policy] button
**A:** Check if:
1. You've selected a project from the dropdown
2. Your project already has 3 policies (all environments configured)
3. If all 3 environments have policies, button won't appear (you can only edit existing ones)

### Q: The environment dropdown only shows some options
**A:** The dropdown filters out environments that already have policies for your project.
If you see "No available environments", all 3 are already created. Just edit existing ones.

### Q: I get an error "Policy for X already exists"
**A:** You tried to create a policy for an environment that already has one.
- Solution: Edit the existing policy instead (click [Edit] button on the policy card)

### Q: Backend connection failed
**A:** Make sure backend is running:
```bash
cd secureops
./mvnw spring-boot:run
# Should be running on http://localhost:8081
```

---

## 📊 Policy Defaults

When creating a new policy, suggested thresholds:

### DEVELOPMENT (Most Lenient)
```
Max Critical:   5
Max High:       10
Max Risk Score: 100
Reason: Allow developers to experiment and learn
```

### STAGING (Moderate)
```
Max Critical:   1
Max High:       5
Max Risk Score: 50
Reason: Catch critical issues before production
```

### PRODUCTION (Most Strict)
```
Max Critical:   0
Max High:       1
Max Risk Score: 30
Reason: Near-zero tolerance for live environment
```

---

## ✨ What Happens After Creating a Policy?

1. ✅ Policy is saved to database
2. ✅ Appears immediately in your policy list
3. ✅ Can now be used for scanning
4. ✅ Future scans use these thresholds
5. ✅ You can edit the thresholds anytime

---

## 🔄 Can I Edit a Policy After Creating It?

**Yes!** Click the **[Edit]** button on any policy card to:
- Adjust max critical count
- Adjust max high count
- Adjust max risk score
- A confirmation dialog appears before saving

---

## ✅ Verification Checklist

- [ ] I can see the Policies page at http://localhost:5173/policies
- [ ] I can select a project from the dropdown
- [ ] If project has < 3 policies, I see [+ Create Policy] button
- [ ] Clicking button opens a create dialog
- [ ] I can select an environment from dropdown
- [ ] I can enter thresholds (numbers)
- [ ] Clicking "Create Policy" button creates the policy
- [ ] New policy appears in the policy list
- [ ] I can click [Edit] to modify any policy
- [ ] Success message appears after creating

---

## 📚 Related Documentation

- **Complete Workflow:** [`docs/COMPLETE_WORKFLOW_GUIDE.md`](./docs/COMPLETE_WORKFLOW_GUIDE.md) - Step 2
- **Button Locations:** [`UI_BUTTON_GUIDE.md`](./UI_BUTTON_GUIDE.md) - Policies Page
- **Quick Start:** [`QUICK_START.md`](./QUICK_START.md) - Step 2

---

## 🎉 That's It!

You can now create security policies through the UI without needing to call APIs manually.

**Next Steps:**
1. Create policies for your project's environments
2. Create a pipeline
3. Create a scan
4. Upload a Trivy report
5. View findings and decision

**Happy securing!** 🔒

