# Session Analytics Context

**Purpose**: Provides guidance for analyzing Claude Code sessions to generate meaningful retrospectives
**Owner**: Retrospective Skill
**Usage**: Auto-loaded when conducting session retrospectives

---

## Data Sources for Session Analysis

### 1. Git History Analysis
**What to Examine**:
- Commits made during the session (timestamps, messages, changed files)
- Diffs showing actual code changes and their scope
- Branch activity and merge patterns
- File modification frequency and complexity

**Key Metrics**:
- Number of files modified/created/deleted
- Lines of code added/removed
- Commit frequency and granularity
- Commit message quality and clarity

**Commands for Analysis**:
```bash
# Get commits from session time range
git log --since="YYYY-MM-DD HH:MM" --until="YYYY-MM-DD HH:MM" --oneline

# Detailed diff for session
git diff <start-commit>...<end-commit> --stat

# Files changed during session
git diff <start-commit>...<end-commit> --name-only
```

### 2. Claude Logs Analysis
**What to Examine**:
- `.claude/logs/` directory for conversation transcripts
- Tool usage patterns (which tools were called, frequency, success rates)
- Error messages and retry patterns
- Decision-making rationale in responses

**Key Indicators**:
- Repeated tool calls suggesting exploration or confusion
- Error recovery patterns
- Context switches and task transitions
- Clarification requests and user interactions

### 3. Project Files Analysis
**What to Examine**:
- Test coverage changes (new tests added, coverage percentages)
- Code quality indicators (complexity, duplication, adherence to standards)
- Documentation updates (README, inline comments, API docs)
- Build and compilation status

**Key Metrics**:
- Test-to-production code ratio
- Compilation success/failure
- Adherence to project coding standards
- Documentation completeness

### 4. User Feedback
**What to Gather**:
- Session goals and whether they were achieved
- User satisfaction with outcomes
- Pain points or friction during the session
- Specific examples of what worked well or poorly

**Gathering Methods**:
- Direct prompting: "What were your goals for this session?"
- Targeted questions: "Which parts of this session were most/least effective?"
- Outcome validation: "Did the implementation meet your expectations?"

### 5. Sub-agent Interaction Analysis
**What to Examine** (when applicable):
- Which sub-agents were invoked during the session
- Task handoffs between Claude and sub-agents
- Sub-agent success rates and output quality
- Communication clarity in agent instructions

**Feedback Collection**:
- Invoke sub-agents with retrospective prompts
- Ask about instruction clarity, tool availability, context sufficiency
- Gather suggestions for improved coordination

---

## Analysis Framework

### Success Indicators
**Code Quality**:
- Compilation succeeds without errors
- Tests pass with appropriate coverage
- Code follows project standards and patterns
- Security considerations properly addressed

**Workflow Efficiency**:
- Minimal rework or backtracking
- Efficient tool usage (right tool for the task)
- Clear progression toward stated goals
- Effective user-Claude communication

**Learning & Adaptation**:
- Applying lessons from earlier in session
- Recognizing and correcting mistakes
- Adapting approach based on feedback
- Discovering and using existing patterns

### Problem Indicators
**Code Quality Issues**:
- Compilation failures or test failures
- Deviations from project architecture/style
- Security vulnerabilities introduced
- Missing or inadequate documentation

**Workflow Inefficiencies**:
- Repeated failed attempts at same task
- Excessive tool calls without progress
- Misunderstanding requirements (multiple clarifications)
- Creating new patterns when existing ones should be used

**Communication Gaps**:
- Ambiguous instructions leading to wrong implementations
- User frustration or confusion
- Missing context causing incorrect assumptions
- Inadequate status updates or progress visibility

---

## Quantitative Metrics to Track

### Session Scope Metrics
- **Duration**: Total time from session start to completion
- **Task Count**: Number of distinct tasks/subtasks completed
- **File Impact**: Files created, modified, deleted
- **Code Volume**: Lines added, removed, net change

### Quality Metrics
- **Compilation Rate**: % of time code compiled successfully
- **Test Coverage**: Coverage percentage change during session
- **Rework Rate**: % of changes that required revision
- **Standard Compliance**: Adherence to project coding standards

### Efficiency Metrics
- **Tool Success Rate**: % of tool calls that succeeded on first attempt
- **Context Switches**: Number of major topic/task transitions
- **Clarification Rate**: User questions per task completed
- **Completion Rate**: % of stated goals fully achieved

### User Experience Metrics
- **Satisfaction**: User-reported satisfaction (if gathered)
- **Friction Points**: Number of reported pain points
- **Value Delivered**: User assessment of outcome usefulness
- **Would Repeat**: User willingness to use approach again

---

## Qualitative Analysis Areas

### Pattern Recognition
- **Successful Approaches**: What techniques led to good outcomes?
- **Problematic Patterns**: What approaches caused issues?
- **Reusable Solutions**: What can be extracted for future use?
- **Context-Specific Learnings**: What only applies to this project/task type?

### Communication Effectiveness
- **Instruction Clarity**: Were instructions clear and actionable?
- **Context Sufficiency**: Was enough context provided upfront?
- **Feedback Loops**: How well did iterative feedback work?
- **User Engagement**: Appropriate level of user involvement?

### Technical Excellence
- **Architecture Alignment**: Proper use of established patterns?
- **Code Quality**: Maintainable, readable, well-structured code?
- **Testing Rigor**: Appropriate test coverage and quality?
- **Security Awareness**: Proper handling of security considerations?

---

## Retrospective Output Guidelines

### Structure Recommendations
1. **Executive Summary**: High-level overview of session outcomes
2. **Quantitative Metrics**: Data-driven assessment of performance
3. **Qualitative Insights**: Pattern analysis and learnings
4. **Action Items**: Specific, prioritized improvements for future sessions

### Actionability Standards
- Every recommendation should be **specific** (not vague)
- Include **evidence** from session data to support claims
- Provide **implementation guidance** for improvements
- **Prioritize** based on impact and feasibility

### Audience Considerations
- **Users**: Want to know if goals were met, what to improve
- **Future Claude sessions**: Need actionable patterns to replicate or avoid
- **Marketplace consumers**: Need to understand value and use cases
- **Plugin developers**: May extend or integrate with other tools

---

This context provides a comprehensive framework for analyzing Claude Code sessions systematically and generating valuable retrospective insights.
