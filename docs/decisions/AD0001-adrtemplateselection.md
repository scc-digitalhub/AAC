---
adr_id: "0001"
comments:
    - author: "gianpaolo-tndigit"
      comment: "1"
      date: "2026-07-22 14:24:31"
status: decided
title: ADRTemplateSelection
---

## <a name="question"></a> Question

Which Architectural Decision Record (ADR) template specification should be used for the AAC project documentation?

## <a name="options"></a> Options

1. <a name="option-1"></a> MADR (Markdown Architectural Decision Records - Minimal/Full Template)
2. <a name="option-2"></a> Nygard ADR Template (Original 2011 5-section format)
3. <a name="option-3"></a> Y-Statement Template (Structured single-sentence format)

## <a name="criteria"></a> Criteria

The template must support tradeoff analysis (options with pros/cons), be lightweight, version-controlled in Git, and natively integrated with the ADG CLI tool. Official references: MADR bare template (https://github.com/adr/madr/blob/develop/template/adr-template.md), ADR Templates Overview (https://adr.github.io/adr-templates/), and Nygard 2011 specification (https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions.html).

## <a name="outcome"></a> Outcome
We decided for [Option 1](#option-1) because: MADR provides a lightweight, Markdown-native structure with YAML metadata that integrates seamlessly with Git and the ADG CLI tool.

## <a name="comments"></a> Comments
<a name="comment-1"></a>1. (2026-07-22 14:24:31) : marked decision as decided
