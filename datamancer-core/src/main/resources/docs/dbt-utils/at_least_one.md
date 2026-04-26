---
title: "at_least_one"
sidebar_label: "at_least_one"
id: "at_least_one"
description: "Asserts that a column has at least one value that is not `null`."
category: "Generic Tests"
---

Asserts that a column has at least one value that is not `null`.

**Usage:**

```yaml
version: 2

models:
  - name: model_name
    columns:
      - name: col_name
        tests:
          - dbt_utils.at_least_one
```

This test supports the `group_by_columns` parameter; see [Grouping in tests](#grouping-in-tests) for details.
