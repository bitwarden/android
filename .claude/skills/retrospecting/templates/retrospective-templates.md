# Retrospective Report Templates

**Purpose**: Standardized templates for comprehensive session retrospectives
**Owner**: Retrospective Skill
**Storage**: All reports generated in `.claude/retro/` directory

---

## Template 1: Comprehensive Session Retrospective

```markdown
# Session Retrospective - [Project Name] [Session Date]

**Session ID**: [Unique identifier]
**Duration**: [Start time] - [End time] ([Total duration])
**Scope**: [Brief description of session goals]
**Outcome**: [SUCCESS/PARTIAL/NEEDS_FOLLOW_UP]

---

## Executive Summary

### Key Achievements
- [Major accomplishments from the session]
- [Quantitative metrics: lines of code, files created, tests added]
- [Quality outcomes: test coverage %, standard compliance]

### Success Metrics
| Metric | Target | Achieved | Status |
|--------|---------|----------|--------|
| Task Completion Rate | 90% | X% | ✅/⚠️/❌ |
| Compilation Success | 100% | X% | ✅/⚠️/❌ |
| Test Coverage | 80% | X% | ✅/⚠️/❌ |
| Code Quality Score | 8/10 | X/10 | ✅/⚠️/❌ |

---

## What Went Well

### Successful Workflow Patterns
1. **[Pattern Name]**: [Description]
   - **Evidence**: [Specific examples from session with file:line references]
   - **Impact**: [Quantified benefit - time saved, quality improved, etc.]
   - **Replicability**: [How to repeat this success in future sessions]

2. **[Pattern Name]**: [Description]
   - **Evidence**: [Specific examples from session]
   - **Impact**: [Quantified benefit]
   - **Replicability**: [How to repeat success]

### Quality Achievements
- **Architecture Compliance**: [Specific examples of well-implemented patterns]
- **Test Coverage**: [Coverage metrics and thoroughness of tests]
- **Security Standards**: [Security best practices followed]
- **Performance**: [Performance optimizations or considerations]

### Communication & Collaboration
- **Effective Interactions**: [What communication patterns worked well]
- **Clear Requirements**: [Examples of well-defined tasks]
- **Useful Feedback**: [Valuable user input that improved outcomes]

---

## Pain Points & Challenges

### Workflow Friction Areas
1. **[Issue Category]**: [Description]
   - **Root Cause**: [Analysis of underlying cause]
   - **Impact**: [Quantified impact - time lost, quality affected, etc.]
   - **Frequency**: [How often this occurred]
   - **Prevention Strategy**: [How to avoid in future sessions]

2. **[Issue Category]**: [Description]
   - **Root Cause**: [Analysis of underlying cause]
   - **Impact**: [Quantified impact on workflow]
   - **Prevention Strategy**: [How to avoid in future]

### Technical Challenges
- **Compilation Issues**: [Number of failures, causes, resolution]
- **Test Failures**: [Test-related problems and how they were resolved]
- **Tool Limitations**: [Constraints or issues with available tools]
- **Integration Problems**: [Difficulties combining components or changes]

### Communication Gaps
- **Unclear Requirements**: [Cases where instructions were ambiguous]
- **Missing Context**: [Information that should have been provided upfront]
- **Assumption Mismatches**: [Differences between expected and actual outcomes]

---

## Improvement Recommendations

### Immediate Actions (Next Session)
1. **[High Priority Item]**: [Specific action with expected impact]
2. **[High Priority Item]**: [Specific action with expected impact]
3. **[High Priority Item]**: [Specific action with expected impact]

### Short-term Enhancements (1-2 weeks)
1. **[Enhancement Area]**: [Description and implementation approach]
2. **[Enhancement Area]**: [Description and implementation approach]

### Long-term Vision (1-3 months)
1. **[Strategic Improvement]**: [Long-term enhancement with roadmap]
2. **[Strategic Improvement]**: [Long-term enhancement with roadmap]

---

## Workflow Optimization Analysis

### Cycle Time Analysis
- **Average Task Duration**: [Time from start to completion]
- **Rework Frequency**: [% of tasks requiring significant revision]
- **Bottlenecks**: [Where workflow got stuck or slowed]
- **Efficiency Wins**: [What made progress faster]

### Quality Progression
- **Code Quality Trend**: [Quality improvement over session duration]
- **Test Coverage Trend**: [Coverage changes over time]
- **Standard Compliance**: [Adherence to project guidelines]
- **Learning Evidence**: [Signs of improving effectiveness during session]

### Tool & Resource Usage
- **Tool Effectiveness**: [Which tools were most/least effective]
- **Context Sufficiency**: [Was enough context available]
- **Documentation Quality**: [Usefulness of available documentation]

---

## Patterns for Future Reference

### Successful Patterns to Replicate
1. **[Pattern Name]**: [Description and replication instructions]
2. **[Pattern Name]**: [Description and replication instructions]

### Anti-Patterns to Avoid
1. **[Anti-Pattern Name]**: [Description and prevention strategy]
2. **[Anti-Pattern Name]**: [Description and prevention strategy]

### Context-Specific Learnings
- **Project Type**: [Insights specific to this type of project]
- **Task Complexity**: [Approaches that worked for this complexity level]
- **Tech Stack**: [Technology-specific patterns discovered]

---

## Sub-agent Feedback (if applicable)

### [Sub-agent Name] Feedback
- **Strengths**: [What worked well in this sub-agent's tasks]
- **Challenges**: [What caused difficulty or confusion]
- **Suggestions**: [Recommendations for improved coordination]

### [Sub-agent Name] Feedback
- **Strengths**: [What worked well]
- **Challenges**: [What caused difficulty]
- **Suggestions**: [Recommendations for improvement]

---

## User Experience Summary

### Most Valuable Aspects
- [What the user found most helpful about this session]
- [Specific features or approaches that delivered value]

### Friction Points
- [What caused user frustration or confusion]
- [Process inefficiencies from user perspective]

### Desired Improvements
- [User suggestions for enhancement]
- [Features or capabilities user wishes were available]

---

## Next Session Preparation

### Workflow Optimizations to Implement
- [ ] [Specific workflow change based on learnings]
- [ ] [Process improvement to apply next session]
- [ ] [Tool usage optimization to implement]

### Context Enhancements
- [ ] [Additional context to provide upfront]
- [ ] [Documentation updates needed]
- [ ] [Reference materials to prepare]

### Success Criteria
- [ ] [Define clear goals for next session]
- [ ] [Establish metrics to track]
- [ ] [Identify resources needed]

---

**Report Generated**: [Date/Time]
**Generated By**: Retrospective Skill
**Next Review**: [Scheduled follow-up date]
```

---

## Template 2: Quick Retrospective

```markdown
# Quick Retrospective - [Session Topic]

**Date**: [Date]
**Duration**: [Duration]
**Scope**: [One-line description of what was accomplished]

## Highlights ✅
- [Top success with specific example]
- [Key achievement with measurable outcome]
- [Quality win or best practice followed]

## Challenges ⚠️
- [Main technical challenge and resolution]
- [Process friction point]
- [Communication or clarity issue]

## Key Learnings 💡
- [Important pattern discovered]
- [Approach to replicate in future]
- [Anti-pattern to avoid next time]

## Action Items 🚀
- [ ] [Immediate improvement to implement]
- [ ] [Follow-up task for next session]
- [ ] [Documentation or context update needed]

**Quick Assessment**: [1-2 sentence overall evaluation of session effectiveness]
```

---

## Template Usage Guidelines

### When to Use Each Template

**Comprehensive Template**:
- End of major feature implementations
- Completion of multi-day or multi-task sessions
- When significant learnings emerged
- User requests detailed analysis
- Sessions with noteworthy successes or challenges

**Quick Template**:
- Regular session wrap-ups
- Single-task completions
- Brief work sessions (< 1 hour)
- Routine maintenance or updates
- User requests lightweight summary

### Customization Guidelines

**Project-Specific Adaptations**:
- Add sections relevant to specific project types (e.g., mobile, web, backend)
- Include project-specific quality metrics
- Adapt success criteria to match project goals
- Reference project-specific documentation and standards

**Metric Customization**:
- Adjust target percentages based on project maturity
- Add domain-specific metrics (e.g., accessibility scores, performance benchmarks)
- Include team or organization KPIs
- Track custom success indicators

**Context Adaptation**:
- Include relevant architectural patterns for the project
- Reference project coding standards
- Note security requirements specific to domain
- Consider team composition and expertise levels

### Storage and Organization

```
.claude/retro/
├── session-retrospectives/
│   ├── YYYY-MM-DD-brief-description.md
│   └── YYYY-MM-DD-another-session.md
└── patterns-library/
    ├── successful-patterns.md
    └── anti-patterns-to-avoid.md
```

**Naming Convention**: `YYYY-MM-DD-brief-description.md`
- Use ISO date format for chronological sorting
- Keep description concise (3-5 words)
- Use hyphens for readability

**Pattern Library Maintenance**:
- Extract reusable patterns from retrospectives
- Update patterns as new evidence emerges
- Organize by project type, technology, or problem domain
- Reference specific retrospectives as evidence

---

These templates provide structured frameworks for capturing session insights and generating actionable recommendations for continuous improvement.
