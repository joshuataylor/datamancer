---
title: "not_null_proportion"
sidebar_label: "not_null_proportion"
id: "not_null_proportion"
description: "Asserts that the proportion of non-null values present in a column is between a specified range [`at_least`, `at_most`] where `at_most` is an optional argument (default: `1.0`)."
category: "Generic Tests"
---

Asserts that the proportion of non-null values present in a column is between a specified range [`at_least`, `at_most`] where `at_most` is an optional argument (default: `1.0`).

**Usage:**

```yaml
version: 2

models:
  - name: my_model
    columns:
      - name: id
        tests:
          - dbt_utils.not_null_proportion:
              arguments:
                at_least: 0.95
```

This test supports the `group_by_columns` parameter; see [Grouping in tests](#grouping-in-tests) for details.
