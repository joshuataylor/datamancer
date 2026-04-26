---
title: "not_accepted_values"
sidebar_label: "not_accepted_values"
id: "not_accepted_values"
description: "Asserts that there are no rows that match the given values."
category: "Generic Tests"
---

Asserts that there are no rows that match the given values.

Usage:

```yaml
version: 2

models:
  - name: my_model
    columns:
      - name: city
        tests:
          - dbt_utils.not_accepted_values:
              arguments:
                values: ['Barcelona', 'New York']
```
