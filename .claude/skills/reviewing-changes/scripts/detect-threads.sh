#!/bin/bash
# Detects existing PR comment threads with full context for intelligent duplicate detection
# Usage: ./scripts/detect-threads.sh <pr-number>
#
# Returns JSON with thread details including:
#   - location (file:line)
#   - severity (extracted from emoji)
#   - issue_summary (first line)
#   - body_preview (first 200 chars for similarity matching)
#   - full_body (complete comment for detailed analysis)
#   - resolved status (includes both general comments and inline review threads)
#   - timestamp
#
# Output format:
#   {"total_threads": N, "threads": [...]} or {"total_threads": 0}
#
# Note: Retrieves both general PR comments AND inline code review threads (including resolved ones)

set -euo pipefail

PR_NUMBER=$1

if [ -z "$PR_NUMBER" ]; then
  echo '{"error": "PR number required"}' >&2
  exit 1
fi

# Fetch repository info from current git remote
REPO_INFO=$(gh repo view --json owner,name --jq '.owner.login + "/" + .name' 2>/dev/null || echo "bitwarden/android")
REPO_OWNER=$(echo "$REPO_INFO" | cut -d'/' -f1)
REPO_NAME=$(echo "$REPO_INFO" | cut -d'/' -f2)

# Fetch general PR comments
COMMENTS_JSON=$(gh pr view "$PR_NUMBER" --json comments 2>/dev/null || echo '{"comments":[]}')

# Fetch inline review threads via GraphQL (includes resolved threads)
REVIEW_THREADS_JSON=$(gh api graphql --raw-field query="
{
  repository(owner:\"${REPO_OWNER}\", name:\"${REPO_NAME}\") {
    pullRequest(number:${PR_NUMBER}) {
      reviewThreads(first:100) {
        edges {
          node {
            isResolved
            comments(first:10) {
              edges {
                node {
                  id
                  author {
                    login
                  }
                  createdAt
                  body
                  path
                  line
                }
              }
            }
          }
        }
      }
    }
  }
}
" 2>/dev/null | jq '.data.repository.pullRequest.reviewThreads.edges // []' || echo '[]')

# Transform review threads into comment format
REVIEW_COMMENTS=$(echo "$REVIEW_THREADS_JSON" | jq '[
  .[] |
  .node.comments.edges[0].node as $comment |
  {
    path: $comment.path,
    line: $comment.line,
    body: $comment.body,
    createdAt: $comment.createdAt,
    author: {
      login: $comment.author.login
    },
    isResolved: .node.isResolved
  }
]' 2>/dev/null || echo '[]')

# Merge both comment sources
MERGED_COMMENTS=$(echo "$COMMENTS_JSON" | jq --argjson review_comments "$REVIEW_COMMENTS" '{
  comments: (.comments + $review_comments)
}')

# Parse and enhance thread data
echo "$MERGED_COMMENTS" | jq '{
  total_threads: (.comments | length),
  threads: [
    .comments[] | {
      location: (
        if .path then "\(.path):\(.line // "unknown")"
        else "general"
        end
      ),
      severity: (
        .body |
        if test("❌") then "CRITICAL"
        elif test("⚠️") then "IMPORTANT"
        elif test("♻️") then "TECHNICAL_DEBT"
        elif test("🎨") then "IMPROVEMENT"
        elif test("💭") then "QUESTION"
        else "UNKNOWN"
        end
      ),
      issue_summary: (
        .body | split("\n")[0] |
        gsub("^[❌⚠️♻️🎨💭]\\s*\\*\\*[A-Z_]+\\*\\*:\\s*"; "") |
        .[0:100]
      ),
      body_preview: (.body | .[0:200]),
      full_body: .body,
      resolved: (.isResolved // false),
      created_at: .createdAt,
      author: .author.login,
      path: .path,
      line: .line
    }
  ]
}' > "/tmp/pr_threads_${PR_NUMBER}.json"

# Output the result
cat "/tmp/pr_threads_${PR_NUMBER}.json"
