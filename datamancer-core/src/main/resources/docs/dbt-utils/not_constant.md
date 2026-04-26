---
title: "not_constant"
sidebar_label: "not_constant"
id: "not_constant"
description: "Asserts that a column does not have the same value in all rows."
category: "Generic Tests"
---

Asserts that a column does not have the same value in all rows.

**Usage:**

```yaml
version: 2

models:
  - name: model_name
    columns:
      - name: column_name
        tests:
          - dbt_utils.not_constant
```

This test supports the `group_by_columns` parameter; see [Grouping in tests](#grouping-in-tests) for details.
