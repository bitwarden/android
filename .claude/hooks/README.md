# Claude Code Session Logging Hooks

Comprehensive logging hooks that capture all Claude Code session activity for retrospective analysis.

## Overview

These hooks automatically log all session activity to `.claude/skills/retrospecting/logs/` in two formats:

- **NDJSON** (`.ndjson`): Compact, machine-readable logs for Claude/subagent processing
- **Markdown** (`.md`): Human-readable logs for manual review

## Log Formats

### NDJSON Format (Machine-Readable)

Newline-delimited JSON with compact event codes for space efficiency. Each line is a JSON object:

```json
{"e":"start","sid":"session-123","t":"2025-01-15T10:00:00","cwd":"/path"}
{"t":"2025-01-15T10:00:05","e":"up","d":{"prompt":"user message"}}
{"t":"2025-01-15T10:00:10","e":"tu","d":{"tool_name":"Read","tool_input":{...}}}
{"t":"2025-01-15T10:00:15","e":"cr","d":{"response":"claude response"}}
{"t":"2025-01-15T10:01:00","e":"end"}
```

**Event Codes:**
- `start`: Session start
- `up`: User prompt
- `cr`: Claude response
- `tu`: Tool use
- `ss`: Subagent stop
- `end`: Session end

**Fields:**
- `e`: Event type (code)
- `t`: Timestamp (ISO 8601)
- `d`: Event data
- `sid`: Session ID (start event only)
- `cwd`: Working directory (start event only)

### Markdown Format (Human-Readable)

Chronological session log with formatted sections for easy review:

```markdown
# Claude Code Session Log

**Session ID**: `session-123`
**Started**: 2025-01-15 10:00:00
**Working Directory**: `/path/to/project`

---

## [10:00:05] UserPrompt

**User**:
\```
user message here
\```

---

## [10:00:10] ToolUse

**Tool**: `Read`
...
```

## Installed Hooks

### SessionStart
- **Trigger**: Session begins or resumes
- **Action**: Initialize empty log files with session metadata
- **Logs**: Session start event with ID, timestamp, working directory

### UserPromptSubmit
- **Trigger**: User submits a prompt
- **Action**: Log user's message
- **Logs**: Full user prompt text

### PostToolUse
- **Trigger**: After each tool completes successfully
- **Action**: Log tool name, inputs, and outputs
- **Logs**: All tool uses including:
  - File operations (Read, Write, Edit)
  - Shell commands (Bash)
  - Web operations (WebFetch, WebSearch)
  - **Subagent invocations (Task tool)** - special handling with subagent type and prompt

### Stop
- **Trigger**: Claude finishes responding
- **Action**: Parse transcript and log Claude's response
- **Logs**: Claude's complete response text

### SubagentStop
- **Trigger**: Subagent finishes executing
- **Action**: Log subagent completion
- **Logs**: Subagent completion event

### SessionEnd
- **Trigger**: Session ends/cleanup
- **Action**: Finalize logs and add end timestamp
- **Logs**: Session end event

## Log Files

Logs are stored in `.claude/skills/retrospeccting/logs/` with filename format:
```
YYYY-MM-DD_HH-MM-SS_<session-id>.ndjson
YYYY-MM-DD_HH-MM-SS_<session-id>.md
```

Example:
```
.claude/logs/
├── 2025-01-15_10-00-00_session-abc123.ndjson
├── 2025-01-15_10-00-00_session-abc123.md
├── 2025-01-15_14-30-00_session-def456.ndjson
└── 2025-01-15_14-30-00_session-def456.md
```

## Usage with Retrospective Skill

The retrospective skill (`/.claude/skills/retrospective/`) uses these logs for session analysis:

```
# User triggers retrospective
User: "Run a retrospective on this session"

# Retrospective skill reads logs
- Parses NDJSON logs for quantitative analysis
- References Markdown logs for qualitative review
- Generates comprehensive retrospective report
```

## Processing NDJSON Logs

To parse NDJSON logs in Python:

```python
import json

def read_ndjson_log(log_path):
    events = []
    with open(log_path, 'r') as f:
        for line in f:
            events.append(json.loads(line.strip()))
    return events

# Example: Count tool uses
def count_tool_uses(events):
    return sum(1 for e in events if e.get('e') == 'tu')
```

To parse in bash:

```bash
# Count user prompts
grep -c '"e":"up"' session.ndjson

# Extract all tool names
grep '"e":"tu"' session.ndjson | jq -r '.d.tool_name'

# Find subagent invocations
grep '"tool_name":"Task"' session.ndjson | jq '.d.tool_input'
```

## Maintenance

### Cleaning Old Logs

Logs accumulate over time. Clean up old logs periodically:

```bash
# Remove logs older than 30 days
find .claude/logs -name "*.ndjson" -mtime +30 -delete
find .claude/logs -name "*.md" -mtime +30 -delete
```

### Disabling Logging

To temporarily disable logging, remove execute permissions:

```bash
chmod -x .claude/hooks/SessionStart
chmod -x .claude/hooks/UserPromptSubmit
chmod -x .claude/hooks/PostToolUse
chmod -x .claude/hooks/Stop
chmod -x .claude/hooks/SessionEnd
chmod -x .claude/hooks/SubagentStop
```

To re-enable:

```bash
chmod +x .claude/hooks/*
```

## Error Handling

All hooks use fail-safe error handling:
- Errors are logged to stderr but **never block** session activity
- If logging fails, the session continues normally
- Best-effort approach ensures reliability

## Implementation Details

- **Language**: Python 3
- **Dependencies**: Standard library only (json, os, datetime, pathlib)
- **Shared Utility**: `logging_utils.py` provides SessionLogger class
- **Format**: NDJSON for efficiency, Markdown for readability
- **Safety**: All hooks exit(0) even on errors to avoid blocking

## Security Considerations

- Logs contain full session transcripts including prompts and responses
- Logs are stored locally in `.claude/skills/retrospecting/logs/`
- Add `.claude/skills/retrospecting/logs/` to `.gitignore` if sensitive data is involved
- Consider log rotation/cleanup for long-running projects
