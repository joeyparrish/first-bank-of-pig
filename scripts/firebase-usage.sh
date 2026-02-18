#!/bin/bash
# Firebase Firestore usage report
# Requires: gcloud CLI, authenticated with `gcloud auth login`

# -----------------------------------------------------------------------------
# Configuration - EDIT THESE VALUES
# -----------------------------------------------------------------------------

# Choose a globally unique project ID (lowercase, numbers, hyphens only)
# This will be your project's identifier across all of Google Cloud/Firebase
PROJECT_ID="first-bank-of-pig"

# -----------------------------------------------------------------------------
# End of Configuration
# -----------------------------------------------------------------------------

echo "=== Firebase Firestore Usage Report ==="
echo "Project: $PROJECT_ID"
echo ""

# Current month boundaries
MONTH_START=$(date -u -d "$(date +%Y-%m-01)" +%Y-%m-%dT00:00:00Z)
NOW=$(date -u +%Y-%m-%dT%H:%M:%SZ)

# Previous month boundaries
PREV_MONTH_START=$(date -u -d "$(date +%Y-%m-01) -1 month" +%Y-%m-%dT00:00:00Z)
PREV_MONTH_END=$(date -u -d "$(date +%Y-%m-01) -1 second" +%Y-%m-%dT23:59:59Z)

fetch_metric() {
    local metric_type=$1
    local start=$2
    local end=$3

    gcloud monitoring read \
        --project="$PROJECT_ID" \
        "metric.type=\"firestore.googleapis.com/document/${metric_type}\"" \
        --interval-start-time="$start" \
        --interval-end-time="$end" \
        --format="value(points.value.int64Value)" \
        2>/dev/null | paste -sd+ | bc 2>/dev/null || echo "0"
}

PREV_MONTH_LABEL=$(date -u -d "$(date +%Y-%m-01) -1 month" +"%B %Y")
CUR_MONTH_LABEL=$(date +"%B %Y")

echo "--- Previous Month ---"
echo "  $PREV_MONTH_LABEL"
echo "  Reads:   $(fetch_metric read_count "$PREV_MONTH_START" "$PREV_MONTH_END")"
echo "  Writes:  $(fetch_metric write_count "$PREV_MONTH_START" "$PREV_MONTH_END")"
echo "  Deletes: $(fetch_metric delete_count "$PREV_MONTH_START" "$PREV_MONTH_END")"
echo ""

echo "--- Current Month (to date) ---"
echo "  $CUR_MONTH_LABEL"
echo "  Reads:   $(fetch_metric read_count "$MONTH_START" "$NOW")"
echo "  Writes:  $(fetch_metric write_count "$MONTH_START" "$NOW")"
echo "  Deletes: $(fetch_metric delete_count "$MONTH_START" "$NOW")"
echo ""

echo "--- Storage ---"
gcloud firestore databases describe --project="$PROJECT_ID" --format="table(name,type)" 2>/dev/null
echo ""

# Document count and storage via Cloud Monitoring
STORAGE_BYTES=$(gcloud monitoring read \
    --project="$PROJECT_ID" \
    "metric.type=\"firestore.googleapis.com/document/storage_total_bytes\"" \
    --interval-start-time="$(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%SZ)" \
    --interval-end-time="$NOW" \
    --format="value(points.value.int64Value)" \
    2>/dev/null | tail -1)

DOC_COUNT=$(gcloud monitoring read \
    --project="$PROJECT_ID" \
    "metric.type=\"firestore.googleapis.com/document/count\"" \
    --interval-start-time="$(date -u -d '1 hour ago' +%Y-%m-%dT%H:%M:%SZ)" \
    --interval-end-time="$NOW" \
    --format="value(points.value.int64Value)" \
    2>/dev/null | tail -1)

if [ -n "$STORAGE_BYTES" ] && [ "$STORAGE_BYTES" != "0" ]; then
    STORAGE_KB=$((STORAGE_BYTES / 1024))
    echo "  Documents: ${DOC_COUNT:-unknown}"
    echo "  Storage:   ${STORAGE_KB} KB (${STORAGE_BYTES} bytes)"
else
    echo "  Documents: ${DOC_COUNT:-not available (check web console)}"
    echo "  Storage:   not available (check web console)"
fi

echo ""

echo "--- Registered Families ---"
ACCESS_TOKEN=$(gcloud auth print-access-token 2>/dev/null)
FAMILY_COUNT=$(curl -s \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -H "Content-Type: application/json" \
    -d '{
        "structuredAggregationQuery": {
            "structuredQuery": {
                "from": [{"collectionId": "families"}]
            },
            "aggregations": [{"alias": "count", "count": {}}]
        }
    }' \
    "https://firestore.googleapis.com/v1/projects/$PROJECT_ID/databases/(default)/documents:runAggregationQuery" \
    2>/dev/null | grep -o '"integerValue"[: ]*"[0-9]*"' | grep -o '[0-9]*')
echo "  Families: ${FAMILY_COUNT:-0}"
echo ""

echo "Free tier: 50K reads/day, 20K writes/day, 1 GiB storage"
