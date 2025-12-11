# Camunda Modeler Visual Rendering Test Guide

## 📋 Purpose
Validate that all 11 BPMN files render correctly in Camunda Modeler after visual element generation.

---

## ✅ Pre-Test Checklist

**Files Verified:**
- [x] 11 production BPMN files present in `/src/bpmn/`
- [x] All files pass XML validation (xmllint)
- [x] All files have complete visual diagrams (616+ elements)
- [x] Duplicate files removed (SUB-001_FIXED deleted)
- [x] Backup files preserved in `/src/bpmn/backup/`

---

## 🔧 Test Environment

**Required Software:**
- Camunda Modeler 5.0.0+ (Download: https://camunda.com/download/modeler/)
- Java 17+ (for process execution testing)
- Camunda Platform 7.20.0 (optional, for runtime validation)

---

## 📊 Test Execution Plan

### **TEST 1: Visual Rendering Validation**

#### Objective
Verify all BPMN elements display correctly with proper visual representation.

#### Steps

1. **Launch Camunda Modeler**
   ```bash
   # macOS
   open -a "Camunda Modeler"
   
   # Linux
   camunda-modeler
   
   # Windows
   camunda-modeler.exe
   ```

2. **Open Each BPMN File**
   - File → Open → Navigate to `/src/bpmn/`
   - Open files in this order:

   **Priority 1 (Most Complex):**
   - [ ] PROC-ORC-001_Orquestracao_Cuidado_Experiencia.bpmn
   - [ ] SUB-001_Onboarding_Inteligente.bpmn
   - [ ] SUB-002_Motor_Proativo.bpmn

   **Priority 2 (Medium Complexity):**
   - [ ] SUB-008_Gestao_Cronicos.bpmn
   - [ ] SUB-007_Navegacao_Cuidado.bpmn
   - [ ] SUB-009_Gestao_Reclamacoes.bpmn
   - [ ] SUB-010_Follow_Up_Feedback.bpmn

   **Priority 3 (Lower Complexity):**
   - [ ] SUB-003_Recepcao_Classificacao.bpmn
   - [ ] SUB-006_Autorizacao_Inteligente.bpmn
   - [ ] SUB-004_Self_Service.bpmn
   - [ ] SUB-005_Agentes_IA.bpmn

3. **Visual Inspection Checklist**

   For EACH file, verify:

   **Elements Visibility:**
   - [ ] All Start Events visible (circles)
   - [ ] All End Events visible (circles with thick border)
   - [ ] All Tasks visible (rounded rectangles)
   - [ ] All Gateways visible (diamonds)
   - [ ] All Sequence Flows visible (arrows)
   - [ ] All Subprocesses visible (large containers)
   - [ ] All Boundary Events visible (attached to tasks)

   **Layout Quality:**
   - [ ] No overlapping elements
   - [ ] Proper spacing between elements
   - [ ] Readable labels
   - [ ] Logical left-to-right flow
   - [ ] Subprocesses properly expanded

   **Technical Validation:**
   - [ ] No red error markers
   - [ ] No yellow warning markers
   - [ ] Properties panel loads correctly
   - [ ] Can select and inspect all elements

4. **Export Test**
   - File → Export as → SVG
   - Verify SVG renders all elements
   - File → Export as → PNG
   - Verify PNG image quality

---

### **TEST 2: Business Logic Validation**

#### Objective
Confirm all Camunda-specific attributes are intact and recognized.

#### Steps

1. **DMN References Check**
   
   Open these files and inspect Business Rule Tasks:
   - SUB-001: Task_EstratificacaoRisco, Task_DeteccaoCPT
   - SUB-003: Task_ClassificarUrgencia, Task_DefinirRoteamento
   - SUB-004: Task_IdentificarFluxo
   - SUB-006: Task_RegrasAutorizacao, Task_ProtocoloClinico
   - SUB-008: Task_EstratificarRisco
   - SUB-009: Task_DefinirPrioridade
   - SUB-010: Task_ClassificarNPS
   - SUB-002: Task_IdentificarGatilhos

   **For each Business Rule Task:**
   - [ ] Select task in diagram
   - [ ] Open Properties Panel
   - [ ] Verify "Decision Ref" field is populated
   - [ ] Verify "Map Decision Result" = "singleEntry"
   - [ ] Verify "Result Variable" is defined

2. **Delegate Expressions Check**
   
   Inspect 5 random Service Tasks across different files:
   
   **For each Service Task:**
   - [ ] Select task in diagram
   - [ ] Open Properties Panel → Implementation
   - [ ] Verify "Delegate Expression" field shows `${serviceName.method}`
   - [ ] Verify no red errors on delegation

3. **External Tasks Check**
   
   Inspect these External Tasks:
   - SUB-001: Task_EnviarBoasVindas, Task_AnalisarNLP
   - SUB-002: Task_ContatoImediato
   - SUB-005: Task_ProcessarConversacao
   - SUB-009: Task_ClassificarReclamacao

   **For each External Task:**
   - [ ] Select task in diagram
   - [ ] Open Properties Panel → Implementation
   - [ ] Verify "Type" = "External"
   - [ ] Verify "Topic" field is populated

---

### **TEST 3: Process Validation**

#### Objective
Use Camunda Modeler's built-in validation.

#### Steps

1. **For EACH file:**
   - Click diagram background (deselect all)
   - Look for validation warnings/errors at bottom panel
   - Expected result: ✅ "No problems found"

2. **Check Properties Panel:**
   - Select process (click background)
   - Properties Panel → General
   - [ ] Process ID is defined
   - [ ] Process Name is defined
   - [ ] Executable = true
   - [ ] Version Tag is set
   - [ ] History Time To Live = 365

---

### **TEST 4: BPMN 2.0 Compliance**

#### Objective
Verify standard BPMN 2.0 compliance.

#### Steps

1. **For Complex Files (PROC-ORC-001, SUB-001, SUB-002):**
   
   - [ ] Event Subprocesses display correctly
   - [ ] Multi-Instance markers visible on subprocesses
   - [ ] Boundary events attached to correct tasks
   - [ ] Non-interrupting boundaries show dashed border
   - [ ] Interrupting boundaries show solid border

2. **Gateway Validation:**
   
   - [ ] Exclusive Gateways show X marker
   - [ ] Parallel Gateways show + marker
   - [ ] Inclusive Gateways show O marker
   - [ ] Event-Based Gateways show pentagon marker

3. **Flow Validation:**
   
   - [ ] Default flows show diagonal marker at source
   - [ ] Conditional flows show diamond marker at source
   - [ ] Message flows (if any) show dashed lines
   - [ ] Sequence flows show solid arrows

---

## 📊 Test Results Documentation

### Template for Each File

```markdown
## File: [FILENAME.bpmn]

**Date Tested:** [DATE]
**Modeler Version:** [VERSION]
**Tester:** [NAME]

### Visual Rendering: [✅ PASS / ❌ FAIL]
- Elements Visible: [Yes/No]
- Layout Quality: [Good/Fair/Poor]
- No Overlaps: [Yes/No]
- Notes: [Any observations]

### Business Logic: [✅ PASS / ❌ FAIL]
- DMN References: [Count checked, all valid]
- Delegates: [Count checked, all valid]
- External Tasks: [Count checked, all valid]
- Notes: [Any observations]

### Validation: [✅ PASS / ❌ FAIL]
- Modeler Validation: [No problems / X problems]
- BPMN 2.0 Compliance: [Yes/No]
- Notes: [Any issues]

### Screenshots
- [Attach/Link to screenshot if issues found]

### Overall Grade: [A+/A/B/C/F]
```

---

## 🚀 Quick Test Script

For rapid validation of all files:

```bash
#!/bin/bash
# Quick visual check script

BPMN_DIR="/Users/rodrigo/claude-projects/Experiencia_Digital_Cliente/Experiencia_Digital_Cliente/src/bpmn"

echo "=== BPMN FILES QUICK CHECK ==="
for file in "$BPMN_DIR"/*.bpmn; do
  filename=$(basename "$file")
  
  # Skip backup files
  if [[ $filename == *.backup ]]; then
    continue
  fi
  
  echo ""
  echo "File: $filename"
  echo "  Size: $(wc -c < "$file") bytes"
  echo "  Visual Elements: $(grep -c 'bpmndi:BPMN' "$file") total"
  echo "  XML Valid: $(xmllint --noout "$file" 2>&1 && echo '✅ YES' || echo '❌ NO')"
  echo "  DMN Refs: $(grep -c 'decisionRef=' "$file")"
  echo "  Delegates: $(grep -c 'delegateExpression=' "$file")"
  echo "  External: $(grep -c 'type="external"' "$file")"
done

echo ""
echo "=== SUMMARY ==="
echo "Total BPMN files: $(ls "$BPMN_DIR"/*.bpmn 2>/dev/null | grep -v backup | wc -l)"
echo "Ready for Camunda Modeler testing!"
```

---

## ⚠️ Common Issues & Solutions

### Issue 1: File Won't Open
**Symptom:** Modeler shows error when opening file  
**Cause:** XML validation failure  
**Solution:** Run `xmllint --noout filename.bpmn` to identify error

### Issue 2: Missing Elements
**Symptom:** Some elements not visible in diagram  
**Cause:** Missing BPMNShape or BPMNEdge  
**Solution:** Check if element ID has corresponding `bpmnElement` in diagram section

### Issue 3: Overlapping Elements
**Symptom:** Elements on top of each other  
**Cause:** Incorrect coordinates  
**Solution:** Use Modeler's auto-layout: Right-click → Align Elements

### Issue 4: DMN Reference Error
**Symptom:** Red marker on Business Rule Task  
**Cause:** Referenced DMN file doesn't exist yet  
**Solution:** Expected - DMN files will be created in next phase

### Issue 5: Delegate Expression Warning
**Symptom:** Yellow marker on Service Task  
**Cause:** Delegate bean not implemented yet  
**Solution:** Expected - Service beans will be implemented in next phase

---

## ✅ Success Criteria

**TEST PASSES IF:**
- ✅ All 11 files open without errors
- ✅ All elements visible with correct shapes
- ✅ No overlapping elements
- ✅ All DMN references present (even if files don't exist yet)
- ✅ All delegate expressions present
- ✅ All external task topics present
- ✅ Modeler shows no XML validation errors

**Expected Warnings (OK to have):**
- ⚠️ DMN files not found (will be created later)
- ⚠️ Delegate beans not found (will be implemented later)

---

## 📝 Next Steps After Testing

**If All Tests Pass:**
1. ✅ Mark visual rendering as production-ready
2. ✅ Proceed to DEV environment deployment testing
3. ✅ Begin DMN file creation phase
4. ✅ Begin delegate bean implementation

**If Issues Found:**
1. Document specific issues with screenshots
2. Create bug report with file name, element ID, and issue description
3. Fix issues before proceeding to DEV deployment
4. Re-run tests on fixed files

---

## 📞 Support

**For Technical Issues:**
- Check: `/docs/FINAL_VALIDATION_REPORT.md`
- Check: `/docs/HIVE_MIND_COMPLETION_REPORT.md`
- GitHub Issues: [Create issue if BPMN errors found]

**For Camunda Modeler Help:**
- Docs: https://docs.camunda.io/docs/components/modeler/desktop-modeler/
- Forum: https://forum.camunda.io/

---

**Test Guide Version:** 1.0  
**Created:** 2025-12-11  
**Last Updated:** 2025-12-11
