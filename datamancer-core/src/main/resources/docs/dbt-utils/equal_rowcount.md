---
title: "equal_rowcount"
sidebar_label: "equal_rowcount"
id: "equal_rowcount"
description: "Asserts that two relations have the same number of rows."
category: "Generic Tests"
---

Asserts that two relations have the same number of rows.

**Usage:**

```yaml
version: 2

models:
  - name: model_name
    tests:
      - dbt_utils.equal_rowcount:
          arguments:
            compare_model: ref('other_table_name')

```

This test supports the `group_by_columns` parameter; see [Grouping in tests](#grouping-in-tests) for details.
