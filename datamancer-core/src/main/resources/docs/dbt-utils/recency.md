---
title: "recency"
sidebar_label: "recency"
id: "recency"
description: "Asserts that a timestamp column in the reference model contains data that is at least as recent as the defined date interval."
category: "Generic Tests"
---

Asserts that a timestamp column in the reference model contains data that is at least as recent as the defined date interval.

**Usage:**

```yaml
version: 2

models:
  - name: model_name
    tests:
      - dbt_utils.recency:
          arguments:
            datepart: day
            field: created_at
            interval: 1
```
This test supports the `group_by_columns` parameter; see [Grouping in tests](#grouping-in-tests) for details.
