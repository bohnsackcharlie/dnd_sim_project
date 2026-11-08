# Project Instructions

## General Guidelines
- Prioritize clean, readable, maintainable code.
- Follow existing patterns and conventions in the codebase.
- Avoid unnecessary abstractions and overengineering.
- Prefer simple solutions over clever ones.

## Development Workflow
- Inspect relevant code before making changes.
- Explain your approach before substantial changes.
- Make targeted changes rather than refactoring unrelated code.
- Don't introduce new dependencies without explaining why.
- Run relevant tests after making changes.

## Code Quality
- Use descriptive variable and method names.
- Handle errors explicitly.
- Write unit tests for new functionality.
- Consider edge cases when implementing new features.

## Communication
- Explain important implementation decisions.
- Let me know which files you modify.
- Explain any tradeoffs or assumptions you make.


## Local Qwen Model Instructions

These instructions are intended to optimize performance when running
a smaller local model with limited hardware resources.

## Qwen-Specific Instructions

- Work efficiently. Avoid extensive reasoning and exploration.
- Inspect only the files directly relevant to the requested change.
- When implementing a feature, inspect at most 2 existing
  implementations as examples unless more are necessary.
- Do not read the entire repository.
- Do not generate or rewrite entire files unnecessarily.
- Prefer small, targeted edits.
- Do not spend time explaining your implementation before
  making straightforward changes.
- Verify that every calculated variable is actually used.
- Compare the final implementation against every requirement
  in the original request.
- Verify method signatures before using unfamiliar APIs.
- Run relevant tests or compilation checks after implementation.
- If something is unclear, identify the specific uncertainty
  rather than exploring unrelated code.