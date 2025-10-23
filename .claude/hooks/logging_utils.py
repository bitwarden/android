#!/usr/bin/env python3
"""
Shared logging utilities for Claude Code session logging.

This module provides functions for writing session logs in both JSON and Markdown formats.
All hooks use these utilities to ensure consistent logging across the session.
"""

import json
import os
from datetime import datetime
from pathlib import Path
from typing import Any, Dict, Optional


class SessionLogger:
    """Handles logging for Claude Code sessions."""

    def __init__(self, session_id: str, cwd: str):
        """
        Initialize session logger.

        Args:
            session_id: Unique session identifier
            cwd: Current working directory
        """
        self.session_id = session_id
        self.cwd = cwd
        self.logs_dir = Path(cwd) / ".claude" / "skills" / "retrospecting" / "logs"
        self.logs_dir.mkdir(parents=True, exist_ok=True)

        # Sanitize session_id for filename
        safe_session_id = session_id.replace("/", "_").replace(":", "-")

        # Look for existing log files for this session
        existing_ndjson = list(self.logs_dir.glob(f"*_{safe_session_id}.ndjson"))
        existing_md = list(self.logs_dir.glob(f"*_{safe_session_id}.md"))

        if existing_ndjson and existing_md:
            # Reuse existing log files
            self.ndjson_log_path = existing_ndjson[0]
            self.md_log_path = existing_md[0]
        else:
            # Create new log files with timestamp
            timestamp = datetime.now().strftime("%Y-%m-%d_%H-%M-%S")
            self.ndjson_log_path = self.logs_dir / f"{timestamp}_{safe_session_id}.ndjson"
            self.md_log_path = self.logs_dir / f"{timestamp}_{safe_session_id}.md"

    def initialize_logs(self) -> None:
        """Initialize empty log files for the session."""
        # Initialize NDJSON log with session start event (compact, append-friendly)
        start_event = {"e":"start","sid":self.session_id,"t":datetime.now().isoformat(),"cwd":self.cwd}
        with open(self.ndjson_log_path, "w") as f:
            f.write(json.dumps(start_event, separators=(',', ':')) + '\n')

        # Initialize Markdown log with header
        with open(self.md_log_path, "w") as f:
            f.write(f"# Claude Code Session Log\n\n")
            f.write(f"**Session ID**: `{self.session_id}`\n")
            f.write(f"**Started**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
            f.write(f"**Working Directory**: `{self.cwd}`\n\n")
            f.write("---\n\n")

    def append_event(self, event_type: str, event_data: Dict[str, Any]) -> None:
        """
        Append an event to both JSON and Markdown logs.

        Args:
            event_type: Type of event (UserPrompt, ClaudeResponse, ToolUse, etc.)
            event_data: Event-specific data
        """
        timestamp = datetime.now().isoformat()

        # Append to JSON log
        self._append_json_event(event_type, event_data, timestamp)

        # Append to Markdown log
        self._append_markdown_event(event_type, event_data, timestamp)

    def _append_json_event(self, event_type: str, event_data: Dict[str, Any], timestamp: str) -> None:
        """Append event to NDJSON log file (newline-delimited, compact, efficient)."""
        try:
            # Map event types to short codes for space efficiency
            event_codes = {"UserPrompt":"up","ClaudeResponse":"cr","ToolUse":"tu","SubagentStop":"ss","SessionEnd":"end"}
            event_code = event_codes.get(event_type, event_type)

            # Create compact event record
            event_record = {"t":timestamp,"e":event_code,"d":event_data}

            # Append as single line (NDJSON format)
            with open(self.ndjson_log_path, "a") as f:
                f.write(json.dumps(event_record, separators=(',', ':')) + '\n')
        except Exception as e:
            # Best effort - log errors but don't fail
            pass

    def _append_markdown_event(self, event_type: str, event_data: Dict[str, Any], timestamp: str) -> None:
        """Append event to Markdown log file."""
        with open(self.md_log_path, "a") as f:
            time_str = datetime.fromisoformat(timestamp).strftime("%H:%M:%S")
            f.write(f"## [{time_str}] {event_type}\n\n")

            if event_type == "UserPrompt":
                f.write(f"**User**:\n```\n{event_data.get('prompt', '')}\n```\n\n")

            elif event_type == "ClaudeResponse":
                response = event_data.get('response', '')
                f.write(f"**Claude**:\n{response}\n\n")

            elif event_type == "ToolUse":
                tool_name = event_data.get('tool_name', 'Unknown')
                f.write(f"**Tool**: `{tool_name}`\n\n")

                if tool_name == "Task":
                    # Special handling for subagent invocations
                    tool_input = event_data.get('tool_input', {})
                    f.write(f"**Subagent Type**: `{tool_input.get('subagent_type', 'N/A')}`\n")
                    f.write(f"**Description**: {tool_input.get('description', 'N/A')}\n")
                    f.write(f"**Prompt**:\n```\n{tool_input.get('prompt', 'N/A')}\n```\n\n")

                    tool_response = event_data.get('tool_response', {})
                    if tool_response:
                        f.write(f"**Response**:\n```\n{json.dumps(tool_response, indent=2)}\n```\n\n")
                else:
                    # Regular tool use
                    tool_input = event_data.get('tool_input', {})
                    f.write(f"**Input**:\n```json\n{json.dumps(tool_input, indent=2)}\n```\n\n")

                    tool_response = event_data.get('tool_response', {})
                    if tool_response:
                        # Truncate large responses
                        response_str = json.dumps(tool_response, indent=2)
                        if len(response_str) > 1000:
                            response_str = response_str[:1000] + "\n... (truncated)"
                        f.write(f"**Response**:\n```json\n{response_str}\n```\n\n")

            elif event_type == "SubagentStop":
                f.write(f"**Subagent completed**\n\n")

            elif event_type == "SessionEnd":
                f.write(f"**Session ended**\n")
                f.write(f"**Duration**: {event_data.get('duration', 'N/A')}\n\n")

            f.write("---\n\n")

    def finalize_logs(self) -> None:
        """Finalize logs at session end."""
        # Append end event to NDJSON log
        try:
            end_event = {"t":datetime.now().isoformat(),"e":"end"}
            with open(self.ndjson_log_path, "a") as f:
                f.write(json.dumps(end_event, separators=(',', ':')) + '\n')
        except Exception:
            pass  # Best effort

        # Add footer to Markdown log
        with open(self.md_log_path, "a") as f:
            f.write(f"\n---\n\n")
            f.write(f"**Session ended**: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")

    def read_transcript(self, transcript_path: str) -> Optional[Dict[str, Any]]:
        """
        Read and parse the conversation transcript.

        Args:
            transcript_path: Path to transcript JSON or JSONL file

        Returns:
            Parsed transcript data or None if file doesn't exist
        """
        try:
            with open(transcript_path, "r") as f:
                # Try regular JSON first
                try:
                    f.seek(0)
                    return json.load(f)
                except json.JSONDecodeError:
                    # If that fails, try JSONL format (newline-delimited JSON)
                    f.seek(0)
                    lines = [line.strip() for line in f if line.strip()]
                    if not lines:
                        return None

                    # Parse the last line which contains the most recent state
                    last_entry = json.loads(lines[-1])
                    return last_entry
        except (FileNotFoundError, json.JSONDecodeError):
            return None


def get_logger(hook_input: Dict[str, Any]) -> SessionLogger:
    """
    Get a SessionLogger instance from hook input.

    Args:
        hook_input: Hook input JSON data

    Returns:
        Configured SessionLogger instance
    """
    session_id = hook_input.get("session_id", "unknown")
    cwd = hook_input.get("cwd", os.getcwd())
    return SessionLogger(session_id, cwd)
