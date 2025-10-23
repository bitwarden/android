---
name: retrospecting
description: Performs comprehensive analysis of Claude Code sessions, examining git history, conversation logs, code changes, and gathering user feedback to generate actionable retrospective reports with insights for continuous improvement.
---

# Session Retrospective Skill

## Purpose

Analyze completed Claude Code sessions to identify successful patterns, problematic areas, and opportunities for improvement. Generate structured retrospective reports that help users understand what worked, what didn't, and how to optimize future sessions.

## Auto-Loaded Context

**Session Analytics**: [session-analytics.md](/.claude/skills/retrospecting/contexts/session-analytics.md) - Provides comprehensive framework for analyzing sessions, including data sources, metrics, and analysis methods.

**Retrospective Templates**: [retrospective-templates.md](/.claude/skills/retrospecting/templates/retrospective-templates.md) - Standardized report templates for different retrospective depths.

## When to Use This Skill

Invoke this skill when users:
- Request a retrospective or post-mortem of a session
- Ask "how did that go?" or "what could be improved?"
- Want to analyze the effectiveness of a completed task
- Request feedback on the session workflow
- Ask for a summary of what was accomplished and lessons learned

## Core Responsibilities

### 1. Multi-Source Data Collection
Systematically gather data from all available sources:
- **Git History**: Commits, diffs, file changes during session timeframe
- **Claude Logs**: Conversation transcripts, tool usage, decision patterns
- **Project Files**: Test coverage, code quality, compilation status
- **User Feedback**: Direct input about goals, satisfaction, pain points
- **Sub-agent Interactions**: When sub-agents were used, gather their feedback

### 2. Quantitative Analysis
Calculate measurable metrics:
- Session scope (duration, tasks completed, files changed)
- Quality indicators (compilation rate, test coverage, standard compliance)
- Efficiency metrics (tool success rate, rework rate, completion rate)
- User experience data (satisfaction, friction points)

### 3. Qualitative Assessment
Identify patterns and insights:
- Successful approaches that led to good outcomes
- Problematic patterns that caused issues or delays
- Reusable solutions worth extracting for future use
- Context-specific learnings applicable to this project type

### 4. Report Generation
Create structured retrospective report using appropriate template:
- **Quick Retrospective**: Brief session wrap-ups (5-10 minutes)
- **Comprehensive Retrospective**: Detailed analysis for significant sessions
- Choose template based on session complexity and user needs

## Working Process

### Step 1: Establish Session Scope
1. Ask user to define session boundaries (time range or commit range)
2. Clarify session goals: "What were you trying to accomplish?"
3. Determine retrospective depth needed (quick vs comprehensive)

### Step 2: Gather Data
Execute data collection from all sources:

**Git Analysis**:
```bash
# Identify session commits
git log --since="<start-time>" --until="<end-time>" --oneline

# Analyze changes
git diff <start-commit>...<end-commit> --stat
git diff <start-commit>...<end-commit> --name-only
```

**Claude Logs**: Read relevant logs from `.claude/logs/` directory

**Project Analysis**: Examine changed files, tests, documentation

**User Feedback**: Prompt for direct feedback on session experience

**Sub-agent Feedback**: If sub-agents were used, invoke them to gather their perspective on the session and their interactions with Claude

### Step 3: Analyze Data
Apply session-analytics.md framework:
- Calculate quantitative metrics
- Identify success and problem indicators
- Extract patterns (successful approaches and anti-patterns)
- Assess communication effectiveness and technical quality

### Step 4: Generate Insights
Synthesize analysis into actionable insights:
- What went well and why (specific evidence)
- What caused problems and their root causes
- Opportunities for improvement (prioritized by impact)
- Patterns to replicate or avoid in future sessions

### Step 5: Create Report
Use appropriate template from retrospective-templates.md:
- Structure findings clearly with evidence
- Include specific file:line references where relevant
- Prioritize recommendations by impact and feasibility
- Make all suggestions actionable and specific

### Step 6: Gather User Validation
Present report and ask:
- Does this match your experience?
- Are there other pain points we missed?
- Which improvements would be most valuable to you?

### Step 7: Suggest Configuration Improvements
If the retrospective identifies areas for improvement in Claude or Agent interactions:
1. Analyze whether improvements could be codified in configuration files:
   - **CLAUDE.md**: Core directives, workflow practices, communication patterns
   - **SKILL.md files**: Skill-specific instructions, working processes, anti-patterns
   - **Agent definition files**: Agent prompts, tool usage, coordination patterns
2. Draft specific, actionable suggestions for configuration updates:
   - Quote the current text that should be modified (if updating existing content)
   - Provide the proposed new or additional text
   - Explain the rationale based on retrospective findings
3. Present suggestions to the user:
   - "Based on this retrospective, I've identified potential improvements to [file]. Would you like me to implement these changes?"
   - Show the specific changes that would be made
4. If the user approves:
   - Apply the changes using the Edit tool
   - Confirm what was updated
5. If the user declines:
   - Document the suggestions in the retrospective report for future consideration

### Step 8: Cleanup Log Files
After the retrospective report is created and validated:
1. Identify the log files from `.claude/logs/` that correspond to the session being analyzed
2. Ask the user if they want to delete these log files:
   - "Would you like me to delete the session log files used for this retrospective?"
   - Explain which files will be deleted (both `.md` and `.ndjson` files)
3. If the user confirms:
   - Delete the specified log files using the Bash tool
   - Confirm deletion to the user
4. If the user declines:
   - Keep the log files and inform the user they remain available in `.claude/logs/`

## Output Standards

### Report Quality Requirements
- **Evidence-Based**: Every claim backed by specific examples
- **Actionable**: All recommendations include implementation guidance
- **Specific**: Avoid vague statements; use concrete examples
- **Prioritized**: Clear indication of high vs low impact items
- **Balanced**: Acknowledge successes while identifying improvements

### File References
Use `file:line_number` format when referencing specific code locations.

### Metrics Presentation
Present metrics in clear tables or lists with context for interpretation.

### Recommendations Format
Each recommendation should include:
- **What**: Specific action to take
- **Why**: Root cause or rationale
- **How**: Implementation approach
- **Impact**: Expected benefit

## Integration with Sub-agents

When sub-agents were used during the session:

### Feedback Collection
Invoke each sub-agent that participated with prompts like:
- "What aspects of this session worked well for you?"
- "What instructions or context were unclear?"
- "What tools or capabilities did you need but lack?"
- "How could coordination with Claude be improved?"

### Synthesis
Incorporate sub-agent feedback into retrospective:
- Identify coordination issues or handoff problems
- Note gaps in instruction clarity or context
- Recognize successful collaboration patterns
- Recommend improvements to sub-agent usage

## Anti-Patterns to Avoid

**Don't**:
- Generate retrospectives without gathering actual data
- Make vague, non-actionable recommendations
- Focus only on negatives; acknowledge what worked well
- Ignore user's stated priorities and goals
- Create overly long reports that bury key insights
- Analyze sessions without understanding the context and goals

**Do**:
- Ground analysis in concrete evidence from session data
- Provide specific, actionable recommendations with implementation guidance
- Balance positive recognition with improvement opportunities
- Align recommendations with user's priorities
- Create concise reports that highlight key insights prominently
- Understand session context before analyzing effectiveness

## Success Criteria

A good retrospective should:
1. **Inform**: User learns something new about their workflow
2. **Guide**: Clear next steps for improvement
3. **Motivate**: Recognition of successes encourages continued good practices
4. **Focus**: Prioritization helps user know where to invest effort
5. **Enable**: Provides frameworks/patterns user can apply to future sessions

## Example Usage

**User**: "Can you do a retrospective on what we just accomplished?"

**Skill Response**:
1. Clarify session scope and goals with user
2. Gather data from git history, logs, changed files
3. Analyze using session-analytics.md framework
4. Generate report using appropriate template
5. Present findings with specific examples and recommendations
6. Validate with user and refine based on feedback
7. Suggest configuration improvements to CLAUDE.md, SKILL.md, or agent files if applicable
8. Ask user if they want to delete the session log files and handle accordingly

## Storage

All generated retrospective reports should be stored within the skill's directory:
```
.claude/skills/retrospective/
├── SKILL.md
└── reports/
    └── YYYY-MM-DD-session-description-SESSION_ID.md
```

**Path to use when writing reports**: `.claude/skills/retrospective/reports/YYYY-MM-DD-session-description-SESSION_ID.md`

**Filename format**: Include the session ID from the log files to enable traceability between reports and source logs.

**Benefits of this structure:**
- **Self-contained**: All skill outputs in one location
- **Portable**: Easy to share or version the skill with its history
- **Organized**: Reports stored in a dedicated directory
- **Reusable**: Can be copied to other projects without conflicts

This organization enables continuous learning across sessions while keeping the skill modular and portable.
