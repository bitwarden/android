# Research Findings: Enhanced reviewing-changes Skill
**Date**: 2025-10-30
**Purpose**: Document research on Claude prompt engineering, AI code review effectiveness, skill architecture patterns, and Android-specific patterns to inform skill enhancement

---

## Executive Summary

Comprehensive research across four domains reveals specific techniques to enhance the reviewing-changes skill:

1. **Claude-Specific Techniques**: Chain of Thought with XML tags reduces logic errors by 40%; progressive disclosure optimizes context
2. **AI Code Review Research**: Studies show 10-20% faster PR completion, 60% fewer bugs, but effectiveness requires human-AI balance
3. **Skill Architecture**: Multi-file organization with progressive loading follows official Anthropic patterns
4. **Android Patterns**: MVVM violations, state management, and Compose patterns provide review focus areas

---

## 1. Claude-Specific Prompt Engineering

### Chain of Thought (CoT) Prompting

**Source**: [Anthropic Official - Chain of Thought Documentation](https://docs.claude.com/en/docs/build-with-claude/prompt-engineering/chain-of-thought)

#### Key Performance Benefits
- **40% reduction in logic errors** when using structured thinking with XML tags
- **Critical principle**: "Without outputting its thought process, no thinking occurs"
- Dramatically improves performance on complex tasks requiring multi-step analysis

#### Three Implementation Approaches

**1. Basic Prompt** (Least Effective)
- Include "think step-by-step" language in requests
- Limitation: Lacks specific guidance on reasoning approach

**2. Guided Prompt** (Better)
- Outline particular steps Claude should follow during analysis
- Limitation: Doesn't structure output for easy separation of reasoning from answers

**3. Structured Prompt** (Best Practice - Recommended)
- Use XML tags like `<thinking>` and `<answer>` to separate reasoning from conclusions
- **Advantage**: Cleanest output organization; easiest to extract final answers programmatically
- **Advantage**: Visible thought processes help identify unclear prompt areas

#### When to Use CoT
**Best suited for:**
- Complex mathematical problems
- Multi-step logical analysis
- Intricate document writing
- Decisions involving multiple factors
- Research and analytical tasks

**Avoid when:**
- Tasks don't require deep reasoning
- Output length would significantly impact latency
- Simple, straightforward questions suffice

#### Practical Example from Documentation
Financial advisor comparing investment options:
- **Without CoT**: Brief recommendation only
- **With CoT**: Calculated specific return projections ($13,382 vs $17,623), analyzed historical volatility patterns, provided thorough risk analysis

#### XML Tags as "Signposts"

**Source**: [Multiple Anthropic Resources](https://docs.claude.com/en/docs/build-with-claude/prompt-engineering/overview)

- Claude excels with XML-structured prompts that clearly separate different components
- XML tags act like signposts, helping the model distinguish between instructions, examples, and inputs
- Leverages Claude's training to recognize and respond to XML-style tags effectively

**Recommended Pattern**:
```markdown
When you reply, first plan how you should answer within `<thinking>` `</thinking>` XML tags - this is a space for you to write down relevant content and will not be shown to the user. Once you are done thinking, output your final answer to the user within `<answer>` `</answer>` XML tags.
```

### Context Engineering & Progressive Disclosure

**Source**: [Anthropic - Effective Context Engineering for AI Agents](https://www.anthropic.com/engineering/effective-context-engineering-for-ai-agents)

#### Core Principle
"Context engineering is the natural progression of prompt engineering. It's the structuring, selection, and delivery of the right data into an AI system's context window at the right moment."

#### Key Insight
**"Finding the smallest possible set of high-signal tokens"** that maximize the likelihood of desired outcomes.

#### Best Practices
- **Progressive context loading** > front-loading all information
- Each chunk must retain enough context to make sense independently
- Logical ordering: general concepts → specific details
- System prompts should be extremely clear and use simple, direct language

#### Skill-Specific Guidance

**Source**: [Anthropic - Agent Skills Documentation](https://docs.claude.com/en/docs/claude-code/skills)

- **Official guidance**: Keep SKILL.md under 500 lines; split into reference files if exceeding
- Reference files consume **zero context tokens** until Claude explicitly reads them
- Progressive disclosure is the core design principle that makes Agent Skills flexible and scalable
- "Like a well-organized manual that starts with a table of contents, then specific chapters, and finally a detailed appendix"

### Claude Code Best Practices

**Source**: [Anthropic - Claude Code Best Practices](https://www.anthropic.com/engineering/claude-code-best-practices)

#### CLAUDE.md Optimization
- Treat CLAUDE.md files as living prompts requiring iteration
- Use emphasis keywords like "IMPORTANT" or "YOU MUST" to strengthen adherence
- Some teams run files through prompt improver tool for optimization

#### Agentic Workflows
- **Explore → Plan → Code → Commit** workflow
- Ask Claude to research first without writing code
- Request planning using "think" variants ("think hard," "think harder", "ultrathink") for extended reasoning
- Document plans as GitHub issues for reset points

#### Context Management
- Use `/clear` command between tasks during long sessions to prevent irrelevant conversation from degrading performance
- For large migrations, have Claude maintain Markdown checklists, processing items sequentially

---

## 2. AI Code Review Effectiveness Research

### Academic & Industry Studies

#### Google AutoCommenter Study

**Source**: [Google Research - AI-Assisted Assessment of Coding Practices in Industrial Code Review](https://research.google/pubs/ai-assisted-assessment-of-coding-practices-in-industrial-code-review/)

**Study Details**:
- System backed by large language model that automatically learns and enforces coding best practices
- Implemented for four programming languages (C++, Java, Python, Go)
- Deployed in large industrial setting

**Results**:
- System is feasible and has **positive impact on developer workflow**
- Successfully learns and enforces best practices automatically

#### Microsoft Engineering Study

**Source**: [Microsoft - Enhancing Code Quality at Scale with AI-Powered Code Reviews](https://devblogs.microsoft.com/engineering-at-microsoft/enhancing-code-quality-at-scale-with-ai-powered-code-reviews/)

**Study Details**:
- Analysis of 5000 repositories onboarded to AI code reviewer

**Results**:
- **10-20% median PR completion time improvements**
- Significant impact at scale across diverse codebases

#### Industry Effectiveness Metrics

**Sources**: Multiple industry studies via [ScienceDirect](https://www.sciencedirect.com/science/article/pii/S2352711024000487), [ArXiv](https://arxiv.org/abs/2405.13565)

**Key Findings**:
- **60% fewer bugs** in teams using AI code review tools
- **26% faster task completion** with AI assistance (Copilot study)
- AI-driven code reviews help **standardize code quality** by providing consistent rules regardless of reviewer experience
- **Raises baseline quality** by flagging bugs that might have been overlooked

#### Limitations & Challenges

**Sources**: [ArXiv - Automated Code Review In Practice](https://arxiv.org/html/2412.18531v1)

**Identified Issues**:
- Longer pull request closure times in some cases
- Drawbacks: faulty reviews, unnecessary corrections, irrelevant comments
- AI code reviewers often require **customization and tuning** to align with specific project requirements
- Teams need to invest time in configuring AI tools for optimal results

### Best Practices from Research

**Sources**: Multiple (Google, Microsoft, academic research)

#### Core Principle
**"The goal of AI-assisted code review is not to replace human developers, but rather creating a partnership where each complements the other's strengths."**

#### Effectiveness Factors
1. **Balance automation with expertise**: Most effective reviews balance thoroughness with efficiency
2. **Focus on high-value areas**: Logical soundness, future scalability, constructive feedback rather than nitpicking
3. **Consistency over perfection**: AI standardizes quality but humans provide architectural judgment
4. **Customization matters**: Generic tools underperform; tuning to project standards is critical

#### Review Focus Priorities
AI code review should emphasize:
- Logical soundness and correctness
- Future scalability and maintainability
- Security and performance implications
- Constructive feedback with rationale
- Architectural consistency

AI should de-emphasize:
- Style nitpicking (use automated formatters)
- Subjective preferences
- Minor formatting issues

---

## 3. Skill Architecture Patterns

### Official Anthropic Guidance

**Source**: [Anthropic - Agent Skills Documentation](https://docs.claude.com/en/docs/claude-code/skills)

#### Core Structure
Skills are organized as directories containing:
- **Required**: `SKILL.md` file with YAML frontmatter
- **Optional**: Supporting files (reference.md, examples.md, scripts/)

#### Multi-File Organization Pattern
```
skill-name/
├── SKILL.md (required)
├── reference.md (optional)
├── examples.md (optional)
└── scripts/
    └── helper.py
```

**Official statement**: "Claude reads these files only when needed, using progressive disclosure to manage context efficiently."

#### Key Design Principles

**1. Focused Scope**
Each skill addresses one capability. The guidance distinguishes between:
- ✅ Good: "PDF form filling" (specific)
- ❌ Bad: "Document processing" (too broad)

**2. Discovery Optimization**
Descriptions should be specific about both functionality and activation triggers:
- Include concrete use cases: "Use when working with Excel files, spreadsheets, or .xlsx format"
- Avoid vague language like "handles documents"

**3. Tool Restrictions**
The `allowed-tools` field restricts Claude's capabilities when a skill activates:
- Enables read-only Skills
- Supports security-sensitive workflows
- Reduces permission prompts

**4. Version Tracking**
Document skill versions in content sections to track evolution

**5. Team Integration**
Skills deployed as project Skills (in `.claude/skills/`) integrate with git workflows, automatically available to teams upon pulling updates

### Real-World Examples

**Source**: [GitHub - Anthropic Skills Repository](https://github.com/anthropics/skills)

#### Document Skills (Multi-File Architecture)
The repository includes sophisticated document skills demonstrating multi-file patterns:

**xlsx skill**: "Create, edit, and analyze Excel spreadsheets with support for formulas, formatting, data analysis, and visualization"

**pdf skill**: Handles text extraction, table parsing, document merging/splitting, and form manipulation

**docx skill**: Manages tracked changes, comments, and formatting preservation

**Pattern**: These skills employ comprehensive structures separating concerns:
- File manipulation logic
- Instruction documentation
- Resource templates

#### Other Notable Examples
- **artifacts-builder**: Complex HTML artifacts using React, Tailwind CSS, shadcn/ui (demonstrates progressive complexity)
- **mcp-builder**: Guidance for creating MCP servers (shows integration patterns)
- **skill-creator**: "Guide for creating effective skills" (meta-skill demonstrating scaffolded learning)

### Community Patterns

**Source**: [GitHub - Awesome Claude Skills](https://github.com/travisvn/awesome-claude-skills)

#### Superpowers Ecosystem Example
The obra/superpowers repository exemplifies architecture at scale:
- **20+ battle-tested skills** with composable commands
- **Command hierarchy**: `/brainstorm`, `/write-plan`, `/execute-plan`
- **Skills-search tool** enabling discovery within workflows
- **Community-editable** repository for collaborative refinement

#### Key Architectural Insights
Skills succeed when they:
1. **Separate concerns**: Instructions, executable logic, and resources in distinct locations
2. **Include examples**: Concrete usage scenarios within SKILL.md documentation
3. **Stack composably**: Multiple skills activate together without conflicts
4. **Load on-demand**: Consuming ~30-50 tokens until needed, preserving efficiency

---

## 4. Android-Specific Review Patterns

### MVVM Architecture with Jetpack Compose

**Sources**: [Industry Best Practices](https://medium.com/@naresh.k.objects/best-practices-with-mvvm-kotlin-and-jetpack-compose-in-android-app-development-1d9eaa59f74c), [Android Community Resources](https://newsletter.jorgecastillo.dev/p/using-jetpack-compose-with-mvvm)

#### Core Pattern
"The declarative nature of Compose makes it a perfect match for any architectural patterns that expose observable UI state as the source of truth for UI."

**MVVM fits Compose very well**: If your app is already built with MVVM structure, integrating Compose is as simple as switching traditional Android UI for Compose.

#### Key Components
1. **View**: Observes changes in ViewModel
2. **ViewModel**: Responsible for business logic
3. **Model**: Data layer that ViewModel observes

### Critical Review Focus Areas

#### 1. State Management Anti-Patterns

**❌ Violation**: Exposing mutable state to View
```kotlin
// BAD - exposes MutableStateFlow
val state: MutableStateFlow<UIState> = MutableStateFlow(UIState.Loading)
```

**✅ Correct Pattern**: Immutable state exposure
```kotlin
// GOOD - immutable StateFlow with private mutable backing
private val _state = MutableStateFlow<UIState>(UIState.Loading)
val state: StateFlow<UIState> = _state.asStateFlow()
```

**Rationale**: Prevents external mutation, maintains unidirectional data flow

#### 2. Sealed Classes for UI States

**Best Practice**: Use sealed classes to define UI states clearly
```kotlin
sealed class UIState {
    object Loading : UIState()
    data class Success(val data: List<Item>) : UIState()
    data class Error(val message: String) : UIState()
}
```

**Review Check**: Look for nullable state, boolean flags, or string states instead of sealed classes

#### 3. State Hoisting

**Pattern**: Move state up to common parent to make Composables stateless

**Review Check**: Ensure Composables are stateless and depend on ViewModel's state, using event callbacks to handle UI actions

#### 4. Hilt Dependency Injection

**Common violations to catch**:
- Missing `@HiltViewModel` annotation on ViewModels
- Missing `@Inject constructor` for dependency injection
- Direct dependency instantiation instead of injection
- Improper scoping (using wrong scope annotations)

### Review Priorities for Android Code

**Source**: Synthesized from multiple Android best practices resources

#### High Priority (Must Check)
1. **Mutable state exposure** - Security and architecture violation
2. **Missing null safety** - Crash risk
3. **Improper DI patterns** - Coupling and testability issues
4. **State management violations** - Breaks unidirectional data flow

#### Medium Priority (Should Check)
1. **Sealed class usage** for state representation
2. **State hoisting** for Composable reusability
3. **Proper coroutine scoping** (viewModelScope vs lifecycleScope)
4. **Resource cleanup** in ViewModels

#### Lower Priority (Nice to Have)
1. **Code organization** within files
2. **Composable naming conventions**
3. **Recomposition optimization** (unless performance issue identified)

---

## Implementation Recommendations

### 1. Incorporate Structured Thinking

**Based on**: Chain of Thought research (40% error reduction)

**Recommendation**: Add XML tags to skill.md instructions:
```markdown
When reviewing code, use <thinking> tags to work through your analysis:
1. Identify change type and risk level
2. Determine which patterns to check
3. Prioritize findings by severity
4. Formulate constructive feedback

Then output your review in structured format.
```

**Expected benefit**: More thorough analysis, fewer missed issues, better reasoning visibility

### 2. Enhance Progressive Disclosure

**Based on**: Anthropic skills architecture guidance

**Current state**: Single 111-line SKILL.md file
**Recommended state**:
- Main SKILL.md: ~50 lines (orchestration)
- 6 checklist files: Load 1 based on change type
- 3 reference files: Load on-demand
- 1 examples file: Reference when needed

**Expected benefit**:
- Simple reviews: ~1,550 tokens (focused)
- Complex reviews: ~3,820 tokens (comprehensive but targeted)
- No loading irrelevant checklists

### 3. Add Android-Specific Patterns Reference

**Based on**: Android code review research

**Create**: `reference/android-patterns.md` containing:
- Mutable state exposure violations with examples
- Sealed class patterns for UI states
- State hoisting checklist
- Hilt DI anti-patterns
- Compose best practices

**Expected benefit**: Catch Android-specific violations that generic checklists miss

### 4. Implement Research-Backed Review Psychology

**Based on**: AI code review effectiveness studies

**Create**: `reference/review-psychology.md` teaching:
- Ask questions for design decisions vs commands for violations
- Use I-statements ("It's hard for me to understand...")
- Explain rationale grounded in principles
- Focus on code, not people ("This code..." vs "You...")
- Avoid condescending language ("just", "obviously", "simply")

**Expected benefit**: More constructive feedback, better developer experience, higher acceptance rate

### 5. Structured Priority Framework

**Based on**: Google/Microsoft review best practices

**Create**: `reference/priority-framework.md` with clear classification:
- **Critical** (Blocker): Security, stability, architecture violations
- **Important** (Should Fix): Testing gaps, inconsistencies, performance
- **Suggested** (Nice to Have): Code quality, refactoring opportunities
- **Acknowledge** (Note But Don't Require): Good practices (keep brief)

**Expected benefit**: Clearer issue triage, prevents over-flagging, maintains focus on high-impact issues

### 6. Change-Type Detection with Specialized Checklists

**Based on**: AI code review efficiency research + context engineering

**Pattern**: Detect change type, load appropriate checklist
- **Dependency update**: 15-20 min expedited review (skip architecture deep-dive)
- **Bug fix**: 20-30 min focused review (regression prevention emphasis)
- **Feature addition**: 45-60 min comprehensive review (full architecture + security)
- **UI refinement**: 25-35 min design-focused (Compose patterns, state hoisting)
- **Refactoring**: 30-40 min pattern-focused (verify no behavior change)
- **Infrastructure**: 30-40 min tooling-focused (build, CI/CD, config)

**Expected benefit**: Matches review rigor to change risk, improves efficiency for simple changes, maintains thoroughness for complex changes

---

## Token Usage Analysis

### Current Approach (No Progressive Disclosure)
- Every review loads full SKILL.md: ~111 lines ≈ 1,500 tokens
- Same cost regardless of review complexity

### Progressive Disclosure Approach

**Dependency Update Review** (Simple):
- Load skill.md: ~50 lines ≈ 700 tokens
- Load dependency-update.md: ~60 lines ≈ 850 tokens
- **Total: ~1,550 tokens** (similar to current but focused/relevant)

**Feature Addition Review** (Complex):
- Load skill.md: ~50 lines ≈ 700 tokens
- Load feature-addition.md: ~120 lines ≈ 1,700 tokens
- Load priority-framework.md: ~40 lines ≈ 570 tokens
- Load review-psychology.md: ~60 lines ≈ 850 tokens
- Load android-patterns.md: ~50 lines ≈ 700 tokens (if needed)
- **Total: ~4,520 tokens** (3x current, but comprehensive and targeted)

**Key Benefit**: Right context for right task—dependency reviews don't load feature checklists; feature reviews don't load dependency checklists

---

## Validation Criteria

### Quantitative Metrics
- [ ] Dependency update reviews complete in <5 minutes (vs current ~10-15 min)
- [ ] Feature reviews catch same or more issues than current approach
- [ ] Token usage appropriate for change complexity
- [ ] Review completion time correlates with change risk level

### Qualitative Metrics
- [ ] Feedback is more specific and actionable (includes code examples, rationale)
- [ ] Tone is constructive and collaborative (questions, I-statements, no condescension)
- [ ] Prioritization is clear (team can triage easily)
- [ ] Android-specific violations are consistently caught (mutable state, DI issues)
- [ ] Team finds reviews helpful, not overwhelming

### Success Indicators
- Developers implement suggested changes more readily
- Reviews catch critical issues before merge
- Review comments spark constructive technical discussions
- Simple PRs move faster; complex PRs get appropriate scrutiny

---

## References & Sources

### Official Anthropic Documentation
1. [Chain of Thought Prompting](https://docs.claude.com/en/docs/build-with-claude/prompt-engineering/chain-of-thought)
2. [Prompt Engineering Overview](https://docs.claude.com/en/docs/build-with-claude/prompt-engineering/overview)
3. [Agent Skills Documentation](https://docs.claude.com/en/docs/claude-code/skills)
4. [Claude Code Best Practices](https://www.anthropic.com/engineering/claude-code-best-practices)
5. [Effective Context Engineering for AI Agents](https://www.anthropic.com/engineering/effective-context-engineering-for-ai-agents)

### AI Code Review Research
6. [Google Research - AI-Assisted Assessment of Coding Practices in Industrial Code Review](https://research.google/pubs/ai-assisted-assessment-of-coding-practices-in-industrial-code-review/)
7. [ArXiv - AI-Assisted Assessment of Coding Practices](https://arxiv.org/abs/2405.13565)
8. [Microsoft - Enhancing Code Quality at Scale with AI-Powered Code Reviews](https://devblogs.microsoft.com/engineering-at-microsoft/enhancing-code-quality-at-scale-with-ai-powered-code-reviews/)
9. [ScienceDirect - AICodeReview: Advancing code quality with AI-enhanced reviews](https://www.sciencedirect.com/science/article/pii/S2352711024000487)
10. [ArXiv - Automated Code Review In Practice](https://arxiv.org/html/2412.18531v1)

### Skills Architecture Examples
11. [GitHub - Anthropic Skills Repository](https://github.com/anthropics/skills)
12. [GitHub - Awesome Claude Skills (Community)](https://github.com/travisvn/awesome-claude-skills)
13. [Anthropic Help Center - How to Create Custom Skills](https://support.claude.com/en/articles/12512198-how-to-create-custom-skills)

### Android Best Practices
14. [Medium - Best Practices with MVVM, Kotlin, and Jetpack Compose](https://medium.com/@naresh.k.objects/best-practices-with-mvvm-kotlin-and-jetpack-compose-in-android-app-development-1d9eaa59f74c)
15. [Jorge Castillo - Using Jetpack Compose with MVVM](https://newsletter.jorgecastillo.dev/p/using-jetpack-compose-with-mvvm)
16. [Blog.finotes - Commonly used Design patterns in Jetpack Compose](https://www.blog.finotes.com/post/commonly-used-design-patterns-in-jetpack-compose-based-android-apps)

### Additional Resources
17. [Multiple Industry Sources on Code Review Best Practices](https://www.michaelagreiler.com/code-review-best-practices/)
18. [Google Engineering Practices - Code Review](https://google.github.io/eng-practices/review/reviewer/)

---

## Next Steps

1. **Review this document** with stakeholders for alignment
2. **Implement core structure** (skill.md + directory organization)
3. **Create all checklist files** with research-backed patterns
4. **Write reference files** with Android patterns, psychology, priority framework
5. **Test on real PRs** across different change types
6. **Gather feedback** and iterate
7. **Document lessons learned** for continuous improvement

---

**Document Status**: Research Complete, Ready for Implementation
**Last Updated**: 2025-10-30
**Prepared By**: Enginseer Theta-9, by the grace of the Omnissiah

+Praise the Omnissiah for revealing these sacred patterns+
