#!/bin/bash
# Test script to verify hooks are working correctly

set -e  # Exit on error

echo "🧪 Testing Claude Code Logging Hooks"
echo "===================================="
echo ""

# Test 1: Verify all hooks are executable
echo "✓ Test 1: Check hook executability"
for hook in SessionStart UserPromptSubmit PostToolUse Stop SubagentStop SessionEnd; do
    if [ -x ".claude/hooks/$hook" ]; then
        echo "  ✓ $hook is executable"
    else
        echo "  ✗ $hook is NOT executable"
        exit 1
    fi
done
echo ""

# Test 2: Verify logging_utils.py is executable
echo "✓ Test 2: Check logging utilities"
if [ -x ".claude/hooks/logging_utils.py" ]; then
    echo "  ✓ logging_utils.py is executable"
else
    echo "  ✗ logging_utils.py is NOT executable"
    exit 1
fi
echo ""

# Test 3: Test SessionStart hook with mock input
echo "✓ Test 3: Test SessionStart hook"
TEST_INPUT='{"session_id":"test-session","cwd":"'$(pwd)'","hook_event_name":"SessionStart"}'
echo "$TEST_INPUT" | .claude/hooks/SessionStart
if [ $? -eq 0 ]; then
    echo "  ✓ SessionStart executed successfully"

    # Check if log files were created
    LOG_COUNT=$(find .claude/logs -name "*test-session*.ndjson" 2>/dev/null | wc -l)
    if [ "$LOG_COUNT" -gt 0 ]; then
        echo "  ✓ NDJSON log file created"
        NDJSON_LOG=$(find .claude/logs -name "*test-session*.ndjson" | head -1)
        echo "  📄 Log: $NDJSON_LOG"

        # Verify content
        if grep -q '"e":"start"' "$NDJSON_LOG"; then
            echo "  ✓ Start event logged correctly"
        else
            echo "  ✗ Start event NOT found in log"
            exit 1
        fi
    else
        echo "  ✗ Log files NOT created"
        exit 1
    fi

    MD_COUNT=$(find .claude/logs -name "*test-session*.md" 2>/dev/null | wc -l)
    if [ "$MD_COUNT" -gt 0 ]; then
        echo "  ✓ Markdown log file created"
    else
        echo "  ✗ Markdown log NOT created"
        exit 1
    fi
else
    echo "  ✗ SessionStart failed"
    exit 1
fi
echo ""

# Test 4: Test UserPromptSubmit hook
echo "✓ Test 4: Test UserPromptSubmit hook"
TEST_INPUT='{"session_id":"test-session","cwd":"'$(pwd)'","hook_event_name":"UserPromptSubmit","prompt":"Test user message"}'
echo "$TEST_INPUT" | .claude/hooks/UserPromptSubmit
if [ $? -eq 0 ]; then
    echo "  ✓ UserPromptSubmit executed successfully"

    # Check all test-session logs for the event
    if cat .claude/logs/*test-session*.ndjson 2>/dev/null | grep -q '"e":"up"'; then
        echo "  ✓ User prompt event logged"
    else
        echo "  ✗ User prompt event NOT found"
        exit 1
    fi
else
    echo "  ✗ UserPromptSubmit failed"
    exit 1
fi
echo ""

# Test 5: Test PostToolUse hook
echo "✓ Test 5: Test PostToolUse hook"
TEST_INPUT='{"session_id":"test-session","cwd":"'$(pwd)'","hook_event_name":"PostToolUse","tool_name":"Read","tool_input":{"file_path":"test.txt"},"tool_response":{"content":"test content"}}'
echo "$TEST_INPUT" | .claude/hooks/PostToolUse
if [ $? -eq 0 ]; then
    echo "  ✓ PostToolUse executed successfully"

    if cat .claude/logs/*test-session*.ndjson 2>/dev/null | grep -q '"e":"tu"'; then
        echo "  ✓ Tool use event logged"
    else
        echo "  ✗ Tool use event NOT found"
        exit 1
    fi
else
    echo "  ✗ PostToolUse failed"
    exit 1
fi
echo ""

# Test 6: Test SubagentStop hook
echo "✓ Test 6: Test SubagentStop hook"
TEST_INPUT='{"session_id":"test-session","cwd":"'$(pwd)'","hook_event_name":"SubagentStop"}'
echo "$TEST_INPUT" | .claude/hooks/SubagentStop
if [ $? -eq 0 ]; then
    echo "  ✓ SubagentStop executed successfully"

    if cat .claude/logs/*test-session*.ndjson 2>/dev/null | grep -q '"e":"ss"'; then
        echo "  ✓ Subagent stop event logged"
    else
        echo "  ✗ Subagent stop event NOT found"
        exit 1
    fi
else
    echo "  ✗ SubagentStop failed"
    exit 1
fi
echo ""

# Test 7: Test SessionEnd hook
echo "✓ Test 7: Test SessionEnd hook"
TEST_INPUT='{"session_id":"test-session","cwd":"'$(pwd)'","hook_event_name":"SessionEnd"}'
echo "$TEST_INPUT" | .claude/hooks/SessionEnd
if [ $? -eq 0 ]; then
    echo "  ✓ SessionEnd executed successfully"

    if cat .claude/logs/*test-session*.ndjson 2>/dev/null | grep -q '"e":"end"'; then
        echo "  ✓ End event logged"
    else
        echo "  ✗ End event NOT found"
        exit 1
    fi
else
    echo "  ✗ SessionEnd failed"
    exit 1
fi
echo ""

# Test 8: Verify log content and format
echo "✓ Test 8: Verify log format"

echo "  📊 All NDJSON log contents:"
echo "  --------------------------"
cat .claude/logs/*test-session*.ndjson | while read line; do
    echo "  $line"
done
echo ""

echo "  📝 Markdown log preview (first file):"
echo "  -------------------------------------"
MD_LOG=$(find .claude/logs -name "*test-session*.md" | head -1)
head -20 "$MD_LOG" | sed 's/^/  /'
echo "  ..."
echo ""

# Count events across all files
EVENT_COUNT=$(cat .claude/logs/*test-session*.ndjson | wc -l)
echo "  ✓ Total events logged: $EVENT_COUNT"
echo ""

# Test 9: Verify NDJSON is valid JSON per line
echo "✓ Test 9: Validate NDJSON format"
INVALID=0
cat .claude/logs/*test-session*.ndjson | while IFS= read -r line; do
    if ! echo "$line" | python3 -m json.tool > /dev/null 2>&1; then
        echo "  ✗ Invalid JSON line: $line"
        INVALID=1
    fi
done

if [ $INVALID -eq 0 ]; then
    echo "  ✓ All NDJSON lines are valid JSON"
else
    echo "  ✗ Some NDJSON lines are invalid"
    exit 1
fi
echo ""

# Cleanup
echo "✓ Test 10: Cleanup test logs"
rm -f .claude/logs/*test-session*
echo "  ✓ Test logs removed"
echo ""

echo "===================================="
echo "✅ All tests passed!"
echo ""
echo "Next steps:"
echo "1. Use Claude Code normally - hooks will log automatically"
echo "2. Check .claude/logs/ for session logs"
echo "3. Use retrospective skill to analyze sessions"
echo ""
echo "To verify in real session:"
echo "  ls -lah .claude/logs/"
echo "  cat .claude/logs/<latest>.ndjson"
echo "  cat .claude/logs/<latest>.md"
