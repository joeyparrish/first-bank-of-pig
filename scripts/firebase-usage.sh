#!/bin/bash
# Firebase Firestore usage report
# Requires: gcloud CLI, authenticated with `gcloud auth login`

# -----------------------------------------------------------------------------
# Configuration
# -----------------------------------------------------------------------------
# Edit scripts/config.sh, or override values in scripts/config.local.sh
# (which is not revision controlled).

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "$SCRIPT_DIR/config.sh"

echo "=== Firebase Firestore Usage Report ==="
echo "Project: $PROJECT_ID"
echo ""

ACCESS_TOKEN=$(gcloud auth print-access-token 2>/dev/null)
if [ -z "$ACCESS_TOKEN" ]; then
    echo "Error: Could not get access token. Run 'gcloud auth login' first."
    exit 1
fi

MONITORING_API="https://monitoring.googleapis.com/v3/projects/$PROJECT_ID/timeSeries"

# Current month boundaries
MONTH_START=$(date -u -d "$(date +%Y-%m-01)" +%Y-%m-%dT00:00:00Z)
NOW=$(date -u +%Y-%m-%dT%H:%M:%SZ)

# Previous month boundaries
PREV_MONTH_START=$(date -u -d "$(date +%Y-%m-01) -1 month" +%Y-%m-%dT00:00:00Z)
PREV_MONTH_END=$(date -u -d "$(date +%Y-%m-01)" +%Y-%m-%dT00:00:00Z)

# Fetch a DELTA metric (read/write/delete counts) summed over a time range.
# Uses ALIGN_SUM to sum within each time series, and REDUCE_SUM to combine
# across series (e.g., different operation types).
fetch_delta_metric() {
    local metric_type=$1
    local start=$2
    local end=$3
    local alignment_seconds=$4

    local response
    response=$(curl -s -H "Authorization: Bearer $ACCESS_TOKEN" \
        "${MONITORING_API}?filter=metric.type%3D%22firestore.googleapis.com%2Fdocument%2F${metric_type}%22&interval.startTime=${start}&interval.endTime=${end}&aggregation.alignmentPeriod=${alignment_seconds}s&aggregation.perSeriesAligner=ALIGN_SUM&aggregation.crossSeriesReducer=REDUCE_SUM" \
        2>/dev/null)

    # Extract int64Value from response; default to 0 if no data
    local value
    value=$(echo "$response" | grep -o '"int64Value"[: ]*"[0-9]*"' | grep -o '[0-9]*' | paste -sd+ | bc 2>/dev/null)
    echo "${value:-0}"
}

# Fetch latest GAUGE metric value (e.g., storage bytes).
# Returns the most recent doubleValue or int64Value, rounded to integer.
fetch_gauge_metric() {
    local metric_type=$1

    local start end
    start=$(date -u -d '2 days ago' +%Y-%m-%dT%H:%M:%SZ)
    end=$NOW

    local response
    response=$(curl -s -H "Authorization: Bearer $ACCESS_TOKEN" \
        "${MONITORING_API}?filter=metric.type%3D%22firestore.googleapis.com%2F${metric_type}%22&interval.startTime=${start}&interval.endTime=${end}&aggregation.alignmentPeriod=86400s&aggregation.perSeriesAligner=ALIGN_MEAN&aggregation.crossSeriesReducer=REDUCE_SUM" \
        2>/dev/null)

    # Try doubleValue first (storage metrics), then int64Value
    local value
    value=$(echo "$response" | grep -o '"doubleValue"[: ]*[0-9.]*' | grep -o '[0-9.]*' | head -1)
    if [ -z "$value" ]; then
        value=$(echo "$response" | grep -o '"int64Value"[: ]*"[0-9]*"' | grep -o '[0-9]*' | head -1)
    fi
    # Round to integer
    if [ -n "$value" ]; then
        printf "%.0f" "$value" 2>/dev/null
    fi
}

PREV_MONTH_LABEL=$(date -u -d "$(date +%Y-%m-01) -1 month" +"%B %Y")
CUR_MONTH_LABEL=$(date +"%B %Y")

# Calculate alignment periods (total seconds in each range)
PREV_ALIGNMENT=$(( ($(date -d "$PREV_MONTH_END" +%s) - $(date -d "$PREV_MONTH_START" +%s)) ))
CUR_ALIGNMENT=$(( ($(date -d "$NOW" +%s) - $(date -d "$MONTH_START" +%s)) ))

echo "--- Previous Month ---"
echo "  $PREV_MONTH_LABEL"
echo "  Reads:   $(fetch_delta_metric read_count "$PREV_MONTH_START" "$PREV_MONTH_END" "$PREV_ALIGNMENT")"
echo "  Writes:  $(fetch_delta_metric write_count "$PREV_MONTH_START" "$PREV_MONTH_END" "$PREV_ALIGNMENT")"
echo "  Deletes: $(fetch_delta_metric delete_count "$PREV_MONTH_START" "$PREV_MONTH_END" "$PREV_ALIGNMENT")"
echo ""

echo "--- Current Month (to date) ---"
echo "  $CUR_MONTH_LABEL"
echo "  Reads:   $(fetch_delta_metric read_count "$MONTH_START" "$NOW" "$CUR_ALIGNMENT")"
echo "  Writes:  $(fetch_delta_metric write_count "$MONTH_START" "$NOW" "$CUR_ALIGNMENT")"
echo "  Deletes: $(fetch_delta_metric delete_count "$MONTH_START" "$NOW" "$CUR_ALIGNMENT")"
echo ""

echo "--- Storage ---"
STORAGE_BYTES=$(fetch_gauge_metric "storage/data_and_index_storage_bytes")

if [ -n "$STORAGE_BYTES" ] && [ "$STORAGE_BYTES" != "0" ]; then
    STORAGE_KB=$((STORAGE_BYTES / 1024))
    echo "  Storage: ${STORAGE_KB} KB (${STORAGE_BYTES} bytes)"
else
    echo "  Storage: not available (check web console)"
fi

echo ""

echo "--- Registered Families ---"
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
