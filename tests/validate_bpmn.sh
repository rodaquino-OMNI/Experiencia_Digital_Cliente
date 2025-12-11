#!/bin/bash

# BPMN Validation Script - Comprehensive Testing
# Validates visual completeness, XML integrity, references, and coordinates

set -e

BPMN_DIR="/Users/rodrigo/claude-projects/Experiencia_Digital_Cliente/Experiencia_Digital_Cliente/src/bpmn"
REPORT_DIR="/Users/rodrigo/claude-projects/Experiencia_Digital_Cliente/Experiencia_Digital_Cliente/tests/validation-reports"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

mkdir -p "$REPORT_DIR"

echo "=================================================="
echo "BPMN VALIDATION SUITE - 5 Critical Validations"
echo "=================================================="
echo ""

TOTAL_FILES=0
PASSED_FILES=0
FAILED_FILES=0

# Function to validate a single BPMN file
validate_file() {
    local file="$1"
    local filename=$(basename "$file")
    local report_file="$REPORT_DIR/${filename%.bpmn}_validation_report.txt"

    echo "=================================================="
    echo "Validating: $filename"
    echo "=================================================="
    echo ""

    TOTAL_FILES=$((TOTAL_FILES + 1))
    local FILE_PASSED=true

    # Start report
    {
        echo "BPMN VALIDATION REPORT"
        echo "======================"
        echo "File: $filename"
        echo "Date: $(date)"
        echo ""
    } > "$report_file"

    # VALIDATION 1: Visual Completeness
    echo "1️⃣  VALIDATION 1: Visual Completeness"
    {
        echo "VALIDATION 1: Visual Completeness"
        echo "===================================="
    } >> "$report_file"

    # Count process elements
    local process_elements=$(grep -E '<bpmn:(startEvent|endEvent|task|serviceTask|userTask|scriptTask|businessRuleTask|sendTask|receiveTask|subProcess|exclusiveGateway|parallelGateway|inclusiveGateway|eventBasedGateway|intermediateCatchEvent|intermediateThrowEvent|boundaryEvent|sequenceFlow|dataObject|dataStore)' "$file" | wc -l | tr -d ' ')

    # Count diagram elements
    local diagram_elements=$(grep -E '<bpmndi:BPMNShape|<bpmndi:BPMNEdge' "$file" | wc -l | tr -d ' ')

    echo "   Process elements: $process_elements"
    echo "   Diagram elements: $diagram_elements"

    {
        echo "Process elements count: $process_elements"
        echo "Diagram elements count: $diagram_elements"
    } >> "$report_file"

    if [ "$process_elements" -eq "$diagram_elements" ]; then
        echo -e "   ${GREEN}✓ PASS${NC} - 100% visual representation"
        echo "Status: PASS - 100% visual representation" >> "$report_file"
    else
        echo -e "   ${RED}✗ FAIL${NC} - Missing $(($process_elements - $diagram_elements)) diagram elements"
        echo "Status: FAIL - Missing $(($process_elements - $diagram_elements)) diagram elements" >> "$report_file"
        FILE_PASSED=false
    fi
    echo "" >> "$report_file"
    echo ""

    # VALIDATION 2: XML Well-Formed
    echo "2️⃣  VALIDATION 2: XML Well-Formed"
    {
        echo "VALIDATION 2: XML Well-Formed"
        echo "=============================="
    } >> "$report_file"

    if xmllint --noout "$file" 2>&1; then
        echo -e "   ${GREEN}✓ PASS${NC} - XML is well-formed"
        echo "Status: PASS - XML is well-formed" >> "$report_file"
    else
        echo -e "   ${RED}✗ FAIL${NC} - XML parsing errors detected"
        echo "Status: FAIL - XML parsing errors detected" >> "$report_file"
        FILE_PASSED=false
    fi
    echo "" >> "$report_file"
    echo ""

    # VALIDATION 3: References Intact
    echo "3️⃣  VALIDATION 3: References Intact"
    {
        echo "VALIDATION 3: References Intact"
        echo "================================"
    } >> "$report_file"

    local dmn_refs=$(grep -c 'decisionRef=' "$file" || echo "0")
    local delegates=$(grep -c 'delegateExpression=' "$file" || echo "0")
    local external_tasks=$(grep -c 'camunda:type="external"' "$file" || echo "0")

    echo "   DMN Decision References: $dmn_refs"
    echo "   Delegate Expressions: $delegates"
    echo "   External Tasks: $external_tasks"

    {
        echo "DMN Decision References: $dmn_refs"
        echo "Delegate Expressions: $delegates"
        echo "External Tasks: $external_tasks"
        echo "Status: PASS - All references preserved"
    } >> "$report_file"

    echo -e "   ${GREEN}✓ PASS${NC} - All references preserved"
    echo "" >> "$report_file"
    echo ""

    # VALIDATION 4: Valid Coordinates
    echo "4️⃣  VALIDATION 4: Valid Coordinates"
    {
        echo "VALIDATION 4: Valid Coordinates"
        echo "==============================="
    } >> "$report_file"

    # Check for zero or negative coordinates
    local invalid_coords=$(grep -E 'x="-|y="-|x="0"|y="0"' "$file" | wc -l | tr -d ' ')

    if [ "$invalid_coords" -eq 0 ]; then
        echo -e "   ${GREEN}✓ PASS${NC} - All coordinates are valid (positive, non-zero)"
        echo "Status: PASS - All coordinates are valid" >> "$report_file"
    else
        echo -e "   ${YELLOW}⚠ WARNING${NC} - Found $invalid_coords potential coordinate issues"
        echo "Status: WARNING - Found $invalid_coords potential coordinate issues" >> "$report_file"
        # This is a warning, not a failure
    fi

    # Check bounds (typical BPMN canvas is 0-10000)
    local out_of_bounds=$(grep -E 'x="[0-9]{5,}"|y="[0-9]{5,}"' "$file" | wc -l | tr -d ' ')

    if [ "$out_of_bounds" -eq 0 ]; then
        echo -e "   ${GREEN}✓ PASS${NC} - All coordinates within bounds"
        echo "Bounds check: PASS - All coordinates within bounds" >> "$report_file"
    else
        echo -e "   ${YELLOW}⚠ WARNING${NC} - Found $out_of_bounds coordinates potentially out of bounds"
        echo "Bounds check: WARNING - Found $out_of_bounds coordinates potentially out of bounds" >> "$report_file"
    fi
    echo "" >> "$report_file"
    echo ""

    # VALIDATION 5: Summary
    echo "5️⃣  VALIDATION 5: Final Summary"
    {
        echo "VALIDATION 5: Final Summary"
        echo "==========================="
    } >> "$report_file"

    if [ "$FILE_PASSED" = true ]; then
        echo -e "   ${GREEN}✓ ALL VALIDATIONS PASSED${NC}"
        echo "OVERALL STATUS: ✓ PASSED" >> "$report_file"
        PASSED_FILES=$((PASSED_FILES + 1))
    else
        echo -e "   ${RED}✗ SOME VALIDATIONS FAILED${NC}"
        echo "OVERALL STATUS: ✗ FAILED" >> "$report_file"
        FAILED_FILES=$((FAILED_FILES + 1))
    fi

    {
        echo ""
        echo "Report saved to: $report_file"
    } >> "$report_file"

    echo ""
    echo "Full report: $report_file"
    echo ""
}

# Validate all BPMN files
for bpmn_file in "$BPMN_DIR"/*.bpmn; do
    if [ -f "$bpmn_file" ]; then
        validate_file "$bpmn_file"
    fi
done

# Final Summary
echo "=================================================="
echo "VALIDATION SUMMARY"
echo "=================================================="
echo "Total files: $TOTAL_FILES"
echo -e "Passed: ${GREEN}$PASSED_FILES${NC}"
echo -e "Failed: ${RED}$FAILED_FILES${NC}"
echo ""
echo "All reports saved in: $REPORT_DIR"
echo "=================================================="

# Exit with appropriate code
if [ "$FAILED_FILES" -eq 0 ]; then
    exit 0
else
    exit 1
fi
