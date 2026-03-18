# CLAUDE.md

This file provides project-level default instructions for Claude Code.

@docs/双端协同统一手册.md

## Startup Checklist

For every development task in this repository, read in this order:
1. The general collaboration handbook is imported above and should be treated as active project memory.
2. If the task is Java / Spring Boot related: [docs/Codex与ClaudeCode双端协同开发清单.md](D:/桌面文件/project/知识库agent/docs/Codex与ClaudeCode双端协同开发清单.md)
3. If this is a real implementation task, structure the work with the task template section in [docs/双端协同统一手册.md](D:/桌面文件/project/知识库agent/docs/双端协同统一手册.md#12-任务模板)

Before coding:
- Clarify goal and non-goal.
- Freeze file ownership and change scope.
- Freeze interface, data, config, and error semantics when relevant.
- Reuse existing code patterns before introducing new abstractions.

Before finishing:
- Summarize changed files.
- Summarize validation performed.
- State unverified items.
- State known risks.

## Repository Notes

- `docs/` is the system of record for long-lived collaboration rules.
- Do not invent a parallel workflow when the docs already define one.
- If new persistent rules are needed, update `docs/` first and then keep this file short.

## Current Project Focus

- Main stack: `Java 17`, `Spring Boot 3.x`, `MyBatis-Plus`, `MySQL`, `Redis`, `Milvus`.
- For backend tasks, follow layered boundaries and the Spring Boot checklist.
