#!/bin/bash
# infrastructure/superset/purge-all-reports.sh

echo "⚠️  SKIPPING PURGE to preserve Slice IDs for frontend compatibility."
echo "   The provisioning script has been updated to modify existing charts in-place."
echo "   This ensures 'sliceId' references in the React app remain valid."
exit 0
